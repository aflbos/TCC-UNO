import argparse
import contextlib
import socket
from pathlib import Path

import numpy as np
from sb3_contrib import MaskablePPO
from sb3_contrib.common.wrappers import ActionMasker
from stable_baselines3 import PPO
from stable_baselines3.common.vec_env import SubprocVecEnv, VecMonitor

from UnoEnviroment import UnoEnv

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
        default="maskableppo",
        choices=["maskableppo", "ppo"],
        help="Model algorithm type",
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

def validate_args(args):
    # Be tolerant of accidental surrounding whitespace/quotes in CLI input.
    normalized_model_path = str(args.model_path).strip().strip('"').strip("'")
    model_path = Path(normalized_model_path).expanduser()
    if not model_path.exists():
        raise FileNotFoundError(f"Model file not found: {model_path}")
    args.model_path = str(model_path)
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

def load_model(args, env):
    if args.algo == "maskableppo":
        return MaskablePPO.load(args.model_path, device=args.device, env=env)
    return PPO.load(args.model_path, device=args.device, env=env)

if __name__ == "__main__":
    args = parse_args()
    validate_args(args)

    if args.port_check and not args.skip_port_check:
        preflight_ports(args.host, args.start_port, args.num_envs, args.connect_timeout)

    env = VecMonitor(SubprocVecEnv([make_env(i, args.start_port, args.host) for i in range(args.num_envs)]))
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
    use_masks = args.algo == "maskableppo"
    masks = None
    if use_masks:
        masks = np.ones((args.num_envs, env.action_space.n), dtype=np.float32)

    try:
        while args.max_steps == 0 or steps < args.max_steps:
            if use_masks:
                actions, _ = model.predict(obs, action_masks=masks, deterministic=deterministic)
            else:
                actions, _ = model.predict(obs, deterministic=deterministic)

            obs, rewards, dones, infos = env.step(actions)
            steps += 1

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