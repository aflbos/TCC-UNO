# `TensorBoardLauncher.py`

## Visão Geral

`TensorBoardLauncher.py` é um utilitário autônomo para inicializar o servidor TensorBoard apontando para o diretório de logs gerado pelo treinamento UNO. Permite visualizar métricas de treinamento (recompensas, perdas, entropia, etc.) em tempo real ou após o treinamento, sem precisar invocar o TensorBoard manualmente pelo terminal.

---

## Dependências

| Biblioteca             | Uso                                              |
|------------------------|--------------------------------------------------|
| `tensorboard.program`  | API programática de inicialização do TensorBoard |

Não requer nenhuma dependência do projeto UNO (sem `UnoEnv`, SB3 ou socket).

---

## Funções

### `launch_tensorboard(logdir, port) → TensorBoard`

Inicializa e inicia o servidor TensorBoard programaticamente.

**Parâmetros:**

| Parâmetro | Tipo  | Padrão                                                          | Descrição                                       |
|-----------|-------|-----------------------------------------------------------------|-------------------------------------------------|
| `logdir`  | `str` | `"<Python>/training logs"` | Caminho para o diretório com arquivos de evento do TensorBoard (gerados por `Training.py`). |
| `port`    | `int` | `6006`                                                          | Porta HTTP em que o servidor ficará disponível. |

**Fluxo:**
1. Instancia `tensorboard.program.TensorBoard()`.
2. Configura via `tb.configure(argv=[None, '--logdir', logdir, '--port', str(port)])`. O primeiro elemento `None` simula o nome do executável, como exigido pela API interna do TensorBoard.
3. Chama `tb.launch()`, que inicia o servidor em background e retorna a URL de acesso (ex.: `http://localhost:6006/`).
4. Imprime a URL no stdout.
5. Retorna o objeto `tb` ativo, que mantém o servidor em execução enquanto o processo Python estiver vivo.

**Retorno:** Instância de `tensorboard.program.TensorBoard` com o servidor ativo.

---

## Execução como Script

Quando executado diretamente (`python TensorBoardLauncher.py`):

1. Chama `launch_tensorboard()` com os valores padrão (`logdir` e porta `6006`).
2. Imprime a URL do servidor.
3. Bloqueia com `input("Press Enter to stop TensorBoard...")`, mantendo o processo ativo até o usuário pressionar Enter.
4. Ao pressionar Enter, o processo encerra e o servidor TensorBoard é desligado automaticamente junto.

---

## Diretório de Logs Padrão

O `logdir` padrão é:
```
<Python>/training logs
```

Este caminho é calculado automaticamente a partir da localização do arquivo `TensorBoardLauncher.py`, evitando problemas de diretório de trabalho.

O diretório correspondente na estrutura do projeto é:
```
Python/
└── training logs/   ← logdir padrão
```

Este é o mesmo diretório configurado como `board_dir` em `Training.py`.

---

## Uso como Módulo

`TensorBoardLauncher.py` pode ser importado e `launch_tensorboard` chamada com parâmetros customizados:

```python
from TensorBoardLauncher import launch_tensorboard

# Apontar para um diretório customizado de logs
custom_logdir = "C:/caminho/para/training logs"

tb = launch_tensorboard(
    logdir=custom_logdir,
    port=6007
)

# ... fazer outras coisas enquanto o TensorBoard está no ar ...

# O servidor para quando o processo Python terminar
```

---

## Relação com `Training.py`

`Training.py` possui sua própria chamada equivalente à função `board()`, definida localmente. `TensorBoardLauncher.py` existe como **utilitário independente** para:

- Visualizar logs de treinamentos **já concluídos** sem precisar re-executar o treinamento.
- Inspecionar métricas **durante** um treinamento em andamento em um terminal separado.
- Usar um ponto de entrada mais simples sem dependências do ambiente UNO.

---

## Observações e Cuidados

- **Caminho relativo:** O `logdir` padrão agora é absoluto (calculado a partir do arquivo). Você ainda pode passar um caminho relativo manualmente se preferir.
- **Porta em uso:** Se a porta `6006` já estiver ocupada (por outra instância do TensorBoard ou outro processo), `tb.launch()` pode falhar silenciosamente ou escolher outra porta automaticamente, dependendo da versão do TensorBoard instalada. Verifique a URL impressa no console.
- **Processo em background:** O servidor TensorBoard roda em uma thread de background dentro do mesmo processo Python. Ele não é um subprocesso independente — encerrar o processo Python encerra o servidor.
- **Múltiplas sessões:** Para comparar diferentes runs de treinamento, basta apontar `logdir` para o diretório pai que contém múltiplas subpastas de runs. O TensorBoard listará cada run separadamente no painel.
