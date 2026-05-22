import os
import time

from sb3_contrib import MaskablePPO
from sb3_contrib.common.wrappers import ActionMasker
from stable_baselines3.common.vec_env import SubprocVecEnv, VecMonitor
from stable_baselines3.common.callbacks import CheckpointCallback, CallbackList
from tensorboard import program

from UnoEnviroment import UnoEnv

def make_env(rank, host, start_port, connect_retries, connect_delay, connect_timeout):
    def _init():
        env = UnoEnv(
            host=host,
            port=start_port + rank,
            connect_retries=connect_retries,
            connect_delay=connect_delay,
            connect_timeout=connect_timeout,
        )
        env = ActionMasker(env, lambda e: e.unwrapped.get_action_mask())
        return env
    return _init

def board(dir, port):
    tb = program.TensorBoard()
    tb.configure(argv=[None, '--logdir', dir, '--port', str(port)])
    url = tb.launch()
    print(f"TensorBoard started at {url}")
    return tb

if __name__ == "__main__":

    models_dir = "./models/MaskablePPO/uno/generated models/"
    logs_dir = f"{models_dir}logs/"
    file = "uno_ai_model"
    board_dir = f"{logs_dir}tensorboard_data/"
    now = time.strftime("%Y-%m-%d_%H-%M-%S")

    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(logs_dir, exist_ok=True)
    os.makedirs(f"{models_dir}{now}/", exist_ok=True)
    os.makedirs(f"{board_dir}/", exist_ok=True)

    device = "cpu"

    # device = "cuda" if torch.cuda.is_available() else "cpu"
    # print(f"Device detected: {torch.cuda.get_device_name(0) if device == 'cuda' else 'CPU'}")

    num_cpu = int(os.getenv("UNO_NUM_ENVS", "16"))
    host = os.getenv("UNO_HOST", "localhost")
    start_port = int(os.getenv("UNO_START_PORT", "5000"))
    connect_retries = int(os.getenv("UNO_CONNECT_RETRIES", "30"))
    connect_delay = float(os.getenv("UNO_CONNECT_DELAY", "0.5"))
    connect_timeout = float(os.getenv("UNO_CONNECT_TIMEOUT", "2.0"))

    env = VecMonitor(
        SubprocVecEnv(
            [
                make_env(i, host, start_port, connect_retries, connect_delay, connect_timeout)
                for i in range(num_cpu)
            ]
        )
    )

    policy_kwargs = dict(net_arch=dict(pi=[512, 512], vf=[512, 512]))

    existing_model_path = "models/MaskablePPO/uno/generated models/2026-04-11_03-14-47/uno_ai_model_2026-04-11_03-14-47.zip.xxx"

    if os.path.exists(existing_model_path):
        print(f"Loading existing model {existing_model_path}")
        model = MaskablePPO.load(existing_model_path, env=env, tensorboard_log=board_dir, device=device)
    else:
        print("Creating new model")
        model = MaskablePPO(
            "MlpPolicy",
            env=env,
            tensorboard_log=board_dir,
            policy_kwargs=policy_kwargs,
            verbose=1,
            device=device,
            n_steps=8192,
            batch_size=2048,
            learning_rate=3e-4,
            ent_coef=0.05,
            gamma=0.995,
            n_epochs=10
        )

    checkpoint_callback = CheckpointCallback(
        save_freq=10000,
        save_path=f"{models_dir}{now}/",
        name_prefix=f"{file}_{now}_checkpoint"
    )

    callback_list = CallbackList([checkpoint_callback])

    tb = board(board_dir, port=6006)

    print("Beginning training...")
    model.learn(
        total_timesteps=100000000,
        reset_num_timesteps=False,
        tb_log_name=f"Maskable_PPO_{now}",
        callback=callback_list
    )

    model.save(f"{models_dir}{now}/{file}_{now}")

    print(f"Training complete and model saved as {file}_{now}.zip")
    input("Press Enter to stop TensorBoard...")