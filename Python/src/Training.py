import os
import time

import numpy as np
from pathlib import Path
import argparse
import importlib
import inspect
from gymnasium import spaces
import torch

from sb3_contrib.common.wrappers import ActionMasker
from stable_baselines3.common.vec_env import SubprocVecEnv
from stable_baselines3.common.callbacks import CheckpointCallback, CallbackList, BaseCallback
from stable_baselines3.common.monitor import Monitor
from tensorboard import program

from UnoEnviroment import UnoEnv

def make_env(rank, host, start_port, connect_retries, connect_delay, connect_timeout, env_debug):
    def _init():
        env = UnoEnv(
            host=host,
            port=start_port + rank,
            connect_retries=connect_retries,
            connect_delay=connect_delay,
            connect_timeout=connect_timeout,
            debug=env_debug,
        )
        env = ActionMasker(env, lambda e: e.unwrapped.get_action_mask())
        env = Monitor(env)
        return env
    return _init

def board(dir, port):
    tb = program.TensorBoard()
    tb.configure(argv=[None, '--logdir', dir, '--port', str(port)])
    url = tb.launch()
    print(f"TensorBoard started at {url}")
    return tb

class ConsoleRewardCallback(BaseCallback):
    def __init__(self, print_every=1):
        super().__init__(verbose=0)
        self.print_every = max(1, int(print_every))
        self.rollout_count = 0

    def _on_step(self) -> bool:
        return True

    def _on_rollout_end(self) -> None:
        self.rollout_count += 1
        if self.rollout_count % self.print_every != 0:
            return

        if not self.model.ep_info_buffer:
            return

        rewards = [ep_info["r"] for ep_info in self.model.ep_info_buffer]
        mean_reward = float(np.mean(rewards))
        median_reward = float(np.median(rewards))
        print(
            f"[treino] ep_rew_mean={mean_reward:.3f} "
            f"ep_rew_median={median_reward:.3f} "
            f"n_episodes={len(rewards)}"
        )


class StepProgressCallback(BaseCallback):
    def __init__(self, print_every_steps=10000):
        super().__init__(verbose=0)
        self.print_every_steps = max(1, int(print_every_steps))
        self._next_print = self.print_every_steps

    def _on_step(self) -> bool:
        if self.num_timesteps >= self._next_print:
            print(f"[treino] steps={self.num_timesteps}")
            while self._next_print <= self.num_timesteps:
                self._next_print += self.print_every_steps
        return True

ALGO_SPECS = {
    "a2c": {"module": "stable_baselines3", "class": "A2C", "mask": False, "discrete": True, "continuous": True},
    "ppo": {"module": "stable_baselines3", "class": "PPO", "mask": False, "discrete": True, "continuous": True},
    "dqn": {"module": "stable_baselines3", "class": "DQN", "mask": False, "discrete": True, "continuous": False},
    "ddpg": {"module": "stable_baselines3", "class": "DDPG", "mask": False, "discrete": False, "continuous": True},
    "td3": {"module": "stable_baselines3", "class": "TD3", "mask": False, "discrete": False, "continuous": True},
    "sac": {"module": "stable_baselines3", "class": "SAC", "mask": False, "discrete": False, "continuous": True},
    "maskableppo": {"module": "sb3_contrib", "class": "MaskablePPO", "mask": True, "discrete": True, "continuous": False},
    "qrdqn": {"module": "sb3_contrib", "class": "QRDQN", "mask": False, "discrete": True, "continuous": False},
    "tqc": {"module": "sb3_contrib", "class": "TQC", "mask": False, "discrete": False, "continuous": True},
    "trpo": {"module": "sb3_contrib", "class": "TRPO", "mask": False, "discrete": True, "continuous": True},
    "ars": {"module": "sb3_contrib", "class": "ARS", "mask": False, "discrete": False, "continuous": True},
    "recurrentppo": {"module": "sb3_contrib", "class": "RecurrentPPO", "mask": False, "discrete": True, "continuous": True, "recurrent": True},
    "crossq": {"module": "sb3_contrib", "class": "CrossQ", "mask": False, "discrete": False, "continuous": True},
}


def parse_args():
    parser = argparse.ArgumentParser(description="UNO training (SB3/sb3_contrib).")
    parser.add_argument("--algo", default="maskableppo", choices=sorted(ALGO_SPECS.keys()))
    parser.add_argument("--total-timesteps", type=int, default=100000000)
    parser.add_argument("--device", default="cpu")
    parser.add_argument("--num-envs", type=int, default=16)
    parser.add_argument("--host", default="localhost")
    parser.add_argument("--start-port", type=int, default=5000)
    parser.add_argument("--connect-retries", type=int, default=30)
    parser.add_argument("--connect-delay", type=float, default=0.5)
    parser.add_argument("--connect-timeout", type=float, default=2.0)
    parser.add_argument("--console-log", action="store_true")
    parser.add_argument("--console-every", type=int, default=1)
    parser.add_argument("--step-log", type=int, default=0)
    parser.add_argument("--env-debug", action="store_true")
    return parser.parse_args()


def resolve_algo(algo_name):
    spec = ALGO_SPECS[algo_name]
    module = importlib.import_module(spec["module"])
    algo_cls = getattr(module, spec["class"])
    return spec, algo_cls


def validate_action_space(env, algo_name, spec):
    if isinstance(env.action_space, spaces.Discrete):
        if not spec.get("discrete", False):
            raise ValueError(f"Algoritmo '{algo_name}' nao suporta action space discreto.")
    else:
        if not spec.get("continuous", False):
            raise ValueError(f"Algoritmo '{algo_name}' nao suporta action space continuo.")


def filter_kwargs(algo_cls, kwargs):
    sig = inspect.signature(algo_cls.__init__)
    return {k: v for k, v in kwargs.items() if k in sig.parameters}


def algo_dir_name(algo_name):
    return "MaskablePPO" if algo_name == "maskableppo" else algo_name.upper()


def build_policy_kwargs(algo_name):
    if algo_name in {"dqn", "qrdqn"}:
        return dict(net_arch=[512, 512])
    if algo_name in {"ddpg", "td3", "sac", "tqc"}:
        return dict(net_arch=dict(pi=[512, 512], qf=[512, 512]))
    return dict(net_arch=dict(pi=[512, 512], vf=[512, 512]))


if __name__ == "__main__":

    args = parse_args()
    print(f"[startup] cuda_available={torch.cuda.is_available()}")

    python_dir = Path(__file__).resolve().parents[1]
    algo_dir = algo_dir_name(args.algo.lower())
    models_dir = python_dir / "models" / algo_dir / "uno" / "generated models"
    logs_dir = models_dir / "logs"
    file = "uno_ai_model"
    board_dir = logs_dir / "tensorboard_data"
    now = time.strftime("%Y-%m-%d_%H-%M-%S")

    os.makedirs(models_dir, exist_ok=True)
    os.makedirs(logs_dir, exist_ok=True)
    os.makedirs(models_dir / now, exist_ok=True)
    os.makedirs(board_dir, exist_ok=True)

    device = args.device

    num_cpu = args.num_envs
    host = args.host
    start_port = args.start_port
    connect_retries = args.connect_retries
    connect_delay = args.connect_delay
    connect_timeout = args.connect_timeout
    env_debug = args.env_debug

    env = SubprocVecEnv(
        [
            make_env(i, host, start_port, connect_retries, connect_delay, connect_timeout, env_debug)
            for i in range(num_cpu)
        ]
    )

    algo_name = args.algo.lower()
    spec, algo_cls = resolve_algo(algo_name)
    validate_action_space(env, algo_name, spec)

    existing_model_path = python_dir / "models" / algo_dir / "uno" / "generated models" / "2026-04-11_03-14-47" / "uno_ai_model_2026-04-11_03-14-47.zip.xxx"

    if os.path.exists(existing_model_path):
        print(f"Loading existing model {existing_model_path}")
        model = algo_cls.load(str(existing_model_path), env=env, tensorboard_log=str(board_dir), device=device)
    else:
        print("Creating new model")
        policy_kwargs = build_policy_kwargs(algo_name)
        algo_kwargs = {
            "policy": "MlpPolicy",
            "env": env,
            "tensorboard_log": str(board_dir),
            "policy_kwargs": policy_kwargs,
            "verbose": 1,
            "device": device,
            "n_steps": 8192,
            "batch_size": 2048,
            "learning_rate": 3e-4,
            "ent_coef": 0.05,
            "gamma": 0.995,
            "n_epochs": 10,
        }
        model = algo_cls(**filter_kwargs(algo_cls, algo_kwargs))

    checkpoint_callback = CheckpointCallback(
        save_freq=10000,
        save_path=str(models_dir / now),
        name_prefix=f"{file}_{now}_checkpoint"
    )

    callback_list = [checkpoint_callback]
    if args.console_log:
        callback_list.append(ConsoleRewardCallback(print_every=args.console_every))

    if args.step_log > 0:
        callback_list.append(StepProgressCallback(print_every_steps=args.step_log))

    callback_list = CallbackList(callback_list)

    tb = board(str(board_dir), port=6006)

    print("Beginning training...")
    model.learn(
        total_timesteps=args.total_timesteps,
        reset_num_timesteps=False,
        tb_log_name=f"{algo_name}_{now}",
        callback=callback_list
    )

    model.save(str(models_dir / now / f"{file}_{now}"))

    print(f"Training complete and model saved as {file}_{now}.zip")
    input("Press Enter to stop TensorBoard...")