import gymnasium as gym
from gymnasium import spaces
import numpy as np
import struct
import socket
import time

class UnoEnv(gym.Env):
    def __init__(self, host='localhost', port=5000, connect_retries=30, connect_delay=0.5, connect_timeout=2.0):
        super(UnoEnv, self).__init__()
        self.action_space = gym.spaces.Discrete(70)
        self.observation_space = gym.spaces.Box(low=-1, high=1, shape=(189,), dtype=np.float32)

        self.current_mask = np.ones(70, dtype=np.int8)

        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
        self.sock.settimeout(connect_timeout)
        last_exc = None
        for _ in range(int(connect_retries)):
            try:
                self.sock.connect((host, port))
                last_exc = None
                break
            except OSError as exc:
                last_exc = exc
                time.sleep(connect_delay)
        if last_exc is not None:
            raise last_exc

        self.fmt = "!260d?"
        self.expected_size = struct.calcsize(self.fmt)
        self.sock.settimeout(None)

    def _comunicate(self, action_to_send):
        self.sock.sendall(struct.pack('!i', int(action_to_send)))

        chunks = []
        bytes_recd = 0
        while bytes_recd < self.expected_size:
            chunk = self.sock.recv(min(self.expected_size - bytes_recd, 2048))
            if not chunk: raise ConnectionError("Socket closed")
            chunks.append(chunk)
            bytes_recd += len(chunk)

        data = b''.join(chunks)
        unpacked = struct.unpack(self.fmt, data)

        return {
            'observation': list(unpacked[0:189]),
            'action_mask': list(unpacked[189:259]),
            'reward': unpacked[259],
            'done': unpacked[260]
        }

    def get_action_mask(self):
        return self.current_mask

    def reset(self, seed=None, options=None):
        state = self._comunicate(-1)
        obs = np.clip(np.array(state['observation'], dtype=np.float64), -1e6, 1e6).astype(np.float32)
        return obs, {"action_mask" : state['action_mask']}

    def step(self, action):
        state = self._comunicate(action)

        self.current_mask = np.array(state['action_mask'], dtype=np.int8)

        obs = np.array(state['observation'], dtype=np.float32)
        reward = state['reward']
        done = state['done']

        info = {"action_mask" : state['action_mask']}

        return obs, reward, done, False, info

    def close(self):
        self.sock.close()