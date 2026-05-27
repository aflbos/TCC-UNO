import argparse
import contextlib
import socket
from pathlib import Path
import importlib
from gymnasium import spaces
import zipfile
import pickle
import numpy as np
import multiprocessing
from sb3_contrib.common.wrappers import ActionMasker
from stable_baselines3.common.vec_env import SubprocVecEnv, VecMonitor
from UnoEnviroment import UnoEnv

try:
    import cloudpickle
except Exception:
    cloudpickle = None

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


def resolve_algo(algo_name):
    spec = ALGO_SPECS[algo_name]
    module = importlib.import_module(spec["module"])
    algo_cls = getattr(module, spec["class"])
    return spec, algo_cls


def make_env(rank, start_port, host):
    def _init():
        env = UnoEnv(host=host, port=start_port + rank)
        env = ActionMasker(env, lambda e: e.unwrapped.get_action_mask())
        return env

    return _init

def parse_args():
    parser = argparse.ArgumentParser(description="Run inference with a trained UNO model.")
    parser.add_argument("--model-path", required=True, help="Path to the saved model (.zip)")
    parser.add_argument(
        "--algo",
        default="auto",
        choices=["auto"] + sorted(ALGO_SPECS.keys()),
        help="Model algorithm type (auto para detectar pelo conteudo do arquivo)",
    )
    parser.add_argument("--num-envs", type=int, default=16, help="Number of parallel environments")
    parser.add_argument("--host", default="localhost", help="UNO server host")
    parser.add_argument("--start-port", type=int, default=5000, help="Base port for parallel envs")
    parser.add_argument(
        "--connect-timeout",
        type=float,
        default=1.0,
        help="Socket timeout (seconds) for preflight port checks",
    )
    parser.add_argument(
        "--port-check",
        action="store_true",
        help=(
            "Run preflight checks that verify host:port is reachable before spawning workers. "
            "Disabled by default because some UNO servers only allow a single connection."
        ),
    )
    # Backward-compatible no-op alias for older scripts.
    parser.add_argument("--skip-port-check", action="store_true", help=argparse.SUPPRESS)
    parser.add_argument("--device", default="cpu", help="Torch device (cpu, cuda, cuda:0, ...)")
    parser.add_argument("--max-steps", type=int, default=0, help="0 means run indefinitely")
    parser.add_argument(
        "--show-outputs",
        action="store_true",
        help="Print AI outputs (actions, rewards, dones) while running",
    )
    parser.add_argument(
        "--output-every",
        type=int,
        default=1,
        help="Print outputs every N steps when --show-outputs is enabled",
    )
    parser.add_argument(
        "--stochastic",
        action="store_true",
        help="Use stochastic actions (default is deterministic)",
    )
    return parser.parse_args()

def load_model_data_from_zip(model_path: str):
    import json

    with zipfile.ZipFile(model_path, "r") as zipf:
        candidates = [name for name in zipf.namelist() if name.endswith("/data") or name == "data" or name.endswith("data.pkl")]
        if not candidates:
            return None
        name = candidates[0]
        raw = zipf.read(name)

    # SB3 >= 1.7 serialises 'data' as JSON; older versions used cloudpickle.
    # Try JSON first, then fall back to pickle so both formats are handled.
    try:
        return json.loads(raw.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError):
        pass

    if cloudpickle is not None:
        return cloudpickle.loads(raw)
    return pickle.loads(raw)


def infer_algo_from_contents(model_path: str):
    data = load_model_data_from_zip(model_path)
    if not isinstance(data, dict):
        return None

    policy_class = data.get("policy_class")

    # SB3 JSON format stores policy_class as a plain string like
    # "sb3_contrib.common.maskable.policies.MaskableActorCriticPolicy".
    # Pickle format stores the actual class object; read __module__ from it.
    if isinstance(policy_class, str):
        module = policy_class
    else:
        module = getattr(policy_class, "__module__", "") if policy_class is not None else ""

    if "sb3_contrib.common.maskable" in module or "sb3_contrib.ppo_mask" in module:
        return "maskableppo"
    if "sb3_contrib.common.recurrent" in module:
        return "recurrentppo"
    if "stable_baselines3.dqn" in module:
        return "dqn"
    if "sb3_contrib.qrdqn" in module:
        return "qrdqn"
    if "stable_baselines3.ddpg" in module:
        return "ddpg"
    if "stable_baselines3.td3" in module:
        return "td3"
    if "stable_baselines3.sac" in module:
        return "sac"
    if "sb3_contrib.tqc" in module:
        return "tqc"
    if "sb3_contrib.trpo" in module:
        return "trpo"
    if "sb3_contrib.ars" in module:
        return "ars"
    if "sb3_contrib.crossq" in module:
        return "crossq"

    if "stable_baselines3.common" in module:
        if "clip_range" in data or "n_epochs" in data:
            return "ppo"
        return "a2c"

    return None

def validate_args(args):
    # Be tolerant of accidental surrounding whitespace/quotes in CLI input.
    normalized_model_path = str(args.model_path).strip().strip('"').strip("'")
    model_path = Path(normalized_model_path).expanduser()
    if not model_path.exists():
        raise FileNotFoundError(f"Model file not found: {model_path}")
    args.model_path = str(model_path)
    if args.algo == "auto":
        inferred = infer_algo_from_contents(args.model_path)
        if inferred is None:
            raise ValueError("Nao foi possivel inferir o algoritmo pelo conteudo do modelo. Informe --algo explicitamente.")
        args.algo = inferred
    if args.num_envs <= 0:
        raise ValueError("--num-envs must be greater than 0")
    if args.start_port <= 0:
        raise ValueError("--start-port must be greater than 0")
    if args.connect_timeout <= 0:
        raise ValueError("--connect-timeout must be greater than 0")
    if args.max_steps < 0:
        raise ValueError("--max-steps must be >= 0")
    if args.output_every <= 0:
        raise ValueError("--output-every must be greater than 0")


def preflight_ports(host, start_port, num_envs, timeout_seconds):
    unavailable = []
    for rank in range(num_envs):
        port = start_port + rank
        try:
            with socket.create_connection((host, port), timeout=timeout_seconds):
                pass
        except OSError as exc:
            unavailable.append((port, exc))

    if unavailable:
        failed_ports = ", ".join(str(port) for port, _ in unavailable)
        first_error = unavailable[0][1]
        raise ConnectionError(
            f"Unable to connect to UNO server at {host} on port(s): {failed_ports}. "
            f"First error: {first_error}. If your server starts at 5000, use --start-port 5000."
        )

def validate_action_space(env, algo_name, spec):
    if isinstance(env.action_space, spaces.Discrete):
        if not spec.get("discrete", False):
            raise ValueError(f"Algoritmo '{algo_name}' nao suporta action space discreto.")
    else:
        if not spec.get("continuous", False):
            raise ValueError(f"Algoritmo '{algo_name}' nao suporta action space continuo.")

def load_model(args, env):
    spec, algo_cls = resolve_algo(args.algo)
    return algo_cls.load(args.model_path, device=args.device, env=env)

if __name__ == "__main__":
    multiprocessing.freeze_support()

    args = parse_args()
    validate_args(args)

    if args.port_check and not args.skip_port_check:
        preflight_ports(args.host, args.start_port, args.num_envs, args.connect_timeout)

    env = VecMonitor(SubprocVecEnv([make_env(i, args.start_port, args.host) for i in range(args.num_envs)]))
    validate_action_space(env, args.algo, resolve_algo(args.algo)[0])

    try:
        obs = env.reset()
    except EOFError as exc:
        with contextlib.suppress(Exception):
            env.close()
        raise RuntimeError(
            "One or more UNO worker connections terminated during reset. "
            "Verify that the server has one live instance per requested port and that --num-envs, "
            "--start-port, and --host are correct. If your UNO server accepts only one client per "
            "port lifecycle, keep preflight disabled (default) and do not use --port-check."
        ) from exc

    model = load_model(args, env)

    deterministic = not args.stochastic
    steps = 0
    spec, _ = resolve_algo(args.algo)
    use_masks = spec.get("mask", False)
    is_recurrent = spec.get("recurrent", False)
    masks = None
    states = None
    episode_starts = None
    if use_masks:
        masks = np.ones((args.num_envs, env.action_space.n), dtype=np.float32)
    if is_recurrent:
        episode_starts = np.ones((args.num_envs,), dtype=bool)

    try:
        while args.max_steps == 0 or steps < args.max_steps:
            if use_masks:
                actions, states = model.predict(obs, action_masks=masks, state=states, episode_start=episode_starts, deterministic=deterministic)
            elif is_recurrent:
                actions, states = model.predict(obs, state=states, episode_start=episode_starts, deterministic=deterministic)
            else:
                actions, _ = model.predict(obs, deterministic=deterministic)

            obs, rewards, dones, infos = env.step(actions)
            steps += 1

            if is_recurrent:
                episode_starts = dones

            if args.show_outputs and steps % args.output_every == 0:
                print(
                    f"[step {steps}] actions={np.asarray(actions).tolist()} "
                    f"rewards={np.asarray(rewards).tolist()} dones={np.asarray(dones).tolist()}"
                )

            if use_masks:
                for i, info in enumerate(infos):
                    if "action_mask" in info:
                        masks[i] = info["action_mask"]
    except KeyboardInterrupt:
        print("Interrupted by user. Closing environments...")
    finally:
        env.close()