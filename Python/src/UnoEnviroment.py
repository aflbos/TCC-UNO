import gymnasium as gym
from gymnasium import spaces
import numpy as np
import json
import socket
import time
import os

class UnoEnv(gym.Env):
    def __init__(self, host='localhost', port=5000, connect_retries=30, connect_delay=0.5, connect_timeout=2.0, debug=False):
        super(UnoEnv, self).__init__()
        self.action_space = gym.spaces.Discrete(70)
        self.observation_space = gym.spaces.Box(low=-1, high=1, shape=(189,), dtype=np.float32)

        self.current_mask = np.ones(70, dtype=np.int8)
        self.debug = bool(debug)
        self._step_counter = 0

        self.sock = None
        last_exc = None
        for attempt in range(1, int(connect_retries) + 1):
            if self.sock is None:
                self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
                self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
                self.sock.settimeout(connect_timeout)
            try:
                if self.debug:
                    print(f"[env] connecting to {host}:{port} (attempt {attempt}/{connect_retries})")
                self.sock.connect((host, port))
                last_exc = None
                if self.debug:
                    print(f"[env] connected to {host}:{port}")
                break
            except OSError as exc:
                last_exc = exc
                try:
                    self.sock.close()
                finally:
                    self.sock = None
                time.sleep(connect_delay)
        if last_exc is not None:
            raise last_exc

        self.sock.settimeout(None)
        self.reader = self.sock.makefile('r', encoding='utf-8', newline='\n')
        self.writer = self.sock.makefile('w', encoding='utf-8', newline='\n')

    def _comunicate(self, action_to_send):
        payload = {"action": int(action_to_send)}
        try:
            self.writer.write(json.dumps(payload, separators=(",", ":")) + "\n")
            self.writer.flush()
        except OSError as exc:
            raise ConnectionError("Failed to send action payload") from exc

        if self.debug:
            print(f"[env] waiting reply for action={action_to_send}")

        line = self.reader.readline()
        if not line:
            raise ConnectionError("Socket closed")

        try:
            message = json.loads(line)
        except json.JSONDecodeError as exc:
            raise ValueError("Invalid JSON payload from server") from exc

        if not isinstance(message, dict):
            raise ValueError("Invalid payload type from server")

        observation = message.get("observation")
        action_mask = message.get("action_mask")
        reward = message.get("reward")
        done = message.get("done")

        if observation is None or action_mask is None:
            raise ValueError("Missing observation or action_mask in payload")

        return {
            'observation': list(observation),
            'action_mask': list(action_mask),
            'reward': reward,
            'done': done
        }

    def get_action_mask(self):
        return self.current_mask

    def reset(self, seed=None, options=None):
        if self.debug:
            print("[env] reset called")
        state = self._comunicate(-1)
        obs = np.clip(np.array(state['observation'], dtype=np.float64), -1e6, 1e6).astype(np.float32)
        return obs, {"action_mask" : state['action_mask']}

    def step(self, action):
        self._step_counter += 1
        if self.debug and self._step_counter <= 3:
            print(f"[env] step={self._step_counter} action={action}")
        state = self._comunicate(action)

        self.current_mask = np.array(state['action_mask'], dtype=np.int8)

        obs = np.array(state['observation'], dtype=np.float32)
        reward = state['reward']
        done = state['done']


        info = {"action_mask" : state['action_mask']}

        return obs, reward, done, False, info

    def close(self):
        try:
            self.reader.close()
        except Exception:
            pass
        try:
            self.writer.close()
        except Exception:
            pass
        self.sock.close()