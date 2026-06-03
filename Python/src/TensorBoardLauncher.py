import tensorboard.program
from pathlib import Path

DEFAULT_LOGDIR = str(Path(__file__).resolve().parents[1] / "training logs")


def launch_tensorboard(logdir=DEFAULT_LOGDIR, port=6006):
    tb = tensorboard.program.TensorBoard()
    tb.configure(argv=[None, '--logdir', logdir, '--port', str(port)])
    url = tb.launch()
    print(f"TensorBoard started at {url}")
    return tb

if __name__ == "__main__":
    tb = launch_tensorboard()
    input("Press Enter to stop TensorBoard...")
