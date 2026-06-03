# `UnoEnviroment.py`

## Visão Geral

`UnoEnviroment.py` define a classe `UnoEnv`, um ambiente de aprendizado por reforço totalmente compatível com a API `gymnasium.Env`. O ambiente funciona como uma ponte TCP entre o agente Python (treinado via Stable Baselines 3) e o servidor de simulação UNO em Java. Toda comunicação é feita via sockets TCP usando mensagens JSON delimitadas por `\n`, sem uso de cabeçalhos binários ou frames especiais.

---

## Dependências

| Biblioteca         | Uso                                                     |
|--------------------|---------------------------------------------------------|
| `gymnasium`        | Classe base `Env`, definição de espaços de ação/obs     |
| `numpy`            | Conversão e normalização das observações                |
| `json`             | Serialização/deserialização das mensagens               |
| `socket`           | Comunicação TCP com o servidor Java                     |
| `time`             | Controle de delay entre tentativas de conexão           |
| `os`               | (importado, reservado para uso futuro ou compatibilidade)|

---

## Espaços de Ação e Observação

### Espaço de Ação — `Discrete(70)`
Representa 70 ações possíveis no jogo UNO. O índice de cada ação corresponde a uma carta específica ou à ação de comprar carta (draw). Ações ilegais no estado atual são mascaradas via `action_mask` e não devem ser selecionadas pelo agente.

### Espaço de Observação — `Box(low=-1, high=1, shape=(189,), dtype=np.float32)`
Um vetor contínuo de 189 floats representando o estado completo do jogo visível ao agente. Os valores são normalizados no intervalo `[-1, 1]`. O conteúdo exato do vetor (cartas na mão, topo do baralho, estado dos adversários etc.) é definido pelo servidor Java.

---

## Parâmetros do Construtor `__init__`

| Parâmetro          | Tipo    | Padrão      | Descrição                                                        |
|--------------------|---------|-------------|------------------------------------------------------------------|
| `host`             | `str`   | `localhost` | Endereço do servidor Java                                        |
| `port`             | `int`   | `5000`      | Porta TCP do servidor Java                                       |
| `connect_retries`  | `int`   | `30`        | Número máximo de tentativas de conexão                           |
| `connect_delay`    | `float` | `0.5`       | Intervalo em segundos entre tentativas de conexão                |
| `connect_timeout`  | `float` | `2.0`       | Timeout do socket durante a fase de conexão inicial              |
| `debug`            | `bool`  | `False`     | Se `True`, imprime logs de diagnóstico no stdout                 |

### Lógica de Conexão

O construtor tenta conectar ao servidor repetidamente em um loop `for` até atingir `connect_retries`. A cada tentativa falha (exceção `OSError`), aguarda `connect_delay` segundos. Se todas as tentativas falharem, a última exceção é relançada. Após conexão bem-sucedida, o timeout do socket é removido (`settimeout(None)`) para evitar interrupções durante o treinamento.

O socket usa `TCP_NODELAY` para desabilitar o algoritmo de Nagle, reduzindo a latência em mensagens pequenas e frequentes.

Após a conexão, são criados dois wrappers de arquivo sobre o socket:
- `self.reader` — para leitura linha a linha (modo `'r'`, encoding UTF-8)
- `self.writer` — para escrita linha a linha (modo `'w'`, encoding UTF-8)

---

## Atributos de Instância

| Atributo           | Tipo             | Descrição                                                        |
|--------------------|------------------|------------------------------------------------------------------|
| `action_space`     | `Discrete(70)`   | Espaço de ações do Gymnasium                                     |
| `observation_space`| `Box(189,)`      | Espaço de observações do Gymnasium                               |
| `current_mask`     | `np.ndarray`     | Máscara de ações válidas no estado atual (int8, shape `(70,)`)   |
| `debug`            | `bool`           | Controla verbosidade dos logs internos                           |
| `_step_counter`    | `int`            | Contador de steps executados (usado para logs de debug parciais) |
| `sock`             | `socket.socket`  | Socket TCP subjacente                                            |
| `reader`           | `TextIOWrapper`  | Leitor de linhas sobre o socket                                  |
| `writer`           | `TextIOWrapper`  | Escritor de linhas sobre o socket                                |

---

## Protocolo de Comunicação

Toda troca de dados segue o formato **JSON por linha** (`\n` como delimitador). Cada mensagem é um objeto JSON completo em uma única linha, sem fragmentação.

### Python → Java (Envio de Ação)
```json
{"action": 12}
```
- `action`: inteiro entre `0` e `69` para jogadas normais, ou `-1` para sinalizar `reset`.

### Java → Python (Resposta do Estado)
```json
{
  "observation": [0.1, -0.5, ...],
  "action_mask": [1, 0, 1, ...],
  "reward":  0.5,
  "done": false
}
```
- `observation`: lista de 189 floats representando o estado do jogo.
- `action_mask`: lista de 70 inteiros (0 ou 1), onde `1` indica ação legal.
- `reward`: recompensa numérica (float) para o step atual. Pode ser `null` no reset.
- `done`: booleano indicando fim de episódio. Pode ser `null` no reset.

---

## Métodos

### `_comunicate(action_to_send) → dict`

Método privado central responsável por toda I/O com o servidor.

**Fluxo:**
1. Serializa `{"action": action_to_send}` como JSON e envia via `self.writer`, seguido de `flush()`.
2. Aguarda resposta com `self.reader.readline()`.
3. Se a linha retornada for vazia (`""`), o servidor fechou a conexão — levanta `ConnectionError`.
4. Tenta parsear a linha como JSON; falha levanta `ValueError`.
5. Extrai e retorna `observation`, `action_mask`, `reward` e `done` do dicionário recebido.
6. Levanta `ValueError` se `observation` ou `action_mask` estiverem ausentes.

**Retorno:** dicionário com as chaves `observation`, `action_mask`, `reward`, `done`.

**Exceções possíveis:**
- `ConnectionError` — falha no envio ou socket fechado pelo servidor.
- `ValueError` — JSON inválido ou campos obrigatórios ausentes.

---

### `reset(seed=None, options=None) → (np.ndarray, dict)`

Reinicia o episódio enviando a ação especial `-1` ao servidor, que devolve o estado inicial do jogo.

**Fluxo:**
1. Chama `_comunicate(-1)`.
2. Converte `observation` para `np.float64`, clipa no intervalo `[-1e6, 1e6]` (proteção contra valores extremos), e converte para `np.float32`.
3. Retorna `(obs, {"action_mask": state['action_mask']})` no formato Gymnasium 0.26+.

**Nota:** `seed` e `options` são aceitos por compatibilidade com a API mas não têm efeito — a seed do jogo é controlada pelo servidor Java.

---

### `step(action) → (np.ndarray, float, bool, bool, dict)`

Executa um passo no ambiente com a ação fornecida.

**Fluxo:**
1. Incrementa `_step_counter`.
2. Imprime log de debug nos primeiros 3 steps, se `debug=True`.
3. Chama `_comunicate(action)`.
4. Atualiza `self.current_mask` com a nova máscara recebida.
5. Converte observação para `np.float32`.
6. Retorna `(obs, reward, done, False, {"action_mask": ...})`.

**Retorno:**
- `obs` — vetor de observação `np.float32` com shape `(189,)`.
- `reward` — float com a recompensa do step.
- `done` — booleano de fim de episódio (terminado).
- `False` — `truncated` sempre `False` (sem limite de steps no lado Python).
- `info` — dicionário com `action_mask` atualizada.

---

### `get_action_mask() → np.ndarray`

Retorna `self.current_mask`, a máscara de ações válidas mais recente. Usado pelo wrapper `ActionMasker` do `sb3_contrib` para alimentar o `MaskablePPO` e similares.

**Retorno:** `np.ndarray` de shape `(70,)`, dtype `int8`, com valores `0` (inválido) ou `1` (válido).

---

### `close()`

Fecha os wrappers de leitura/escrita e o socket subjacente de forma segura, suprimindo exceções individuais para garantir que todos os recursos sejam liberados mesmo em caso de falha parcial.

---

## Fluxo de Uso Típico

```
UnoEnv.__init__()        → Conecta ao servidor Java
    │
    ├─ reset()           → Envia action=-1, recebe estado inicial
    │
    └─ loop:
         step(action)    → Envia ação, recebe novo estado
         ...
         step(action)    → done=True → episódio encerrado
         │
         reset()         → Reinicia para novo episódio
```

---

## Observações e Cuidados

- **Uma instância por porta:** Cada `UnoEnv` ocupa exclusivamente uma porta TCP. Para treinamento paralelo, use `SubprocVecEnv` com portas distintas (ex.: 5000, 5001, ..., 5000+N-1).
- **Sem reconexão automática:** Se o servidor Java cair durante o treinamento, a próxima chamada a `_comunicate` levantará `ConnectionError`. Não há lógica de reconexão após a inicialização.
- **Timeout removido após conexão:** O socket opera em modo bloqueante ilimitado após a conexão. Chamadas a `readline()` aguardarão indefinidamente até o servidor responder. Isso é intencional para evitar falsos timeouts durante simulações longas.
- **Clipping na observação:** O `reset()` aplica `np.clip(..., -1e6, 1e6)` como salvaguarda contra valores NaN/Inf vindos do servidor. O `step()` não aplica clipping — os dados são usados diretamente.
- **`truncated` sempre False:** O limite de duração do episódio, se houver, é gerenciado inteiramente pelo servidor Java via `done=true`.
