import tensorboard.program

def launch_tensorboard(logdir="./models/MaskablePPO/uno/logs/tensorboard_data/", port=6006):
    tb = tensorboard.program.TensorBoard()
    tb.configure(argv=[None, '--logdir', logdir, '--port', str(port)])
    url = tb.launch()
    print(f"TensorBoard started at {url}")
    return tb

if __name__ == "__main__":
    tb = launch_tensorboard()
    input("Press Enter to stop TensorBoard...")
