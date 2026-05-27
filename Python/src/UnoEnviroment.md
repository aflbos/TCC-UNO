# `UnoEnviroment.py`

## Visao geral
`UnoEnviroment.py` define o ambiente `UnoEnv` compatível com Gymnasium. Ele se comunica via socket com o servidor UNO, enviando acoes e recebendo observacoes.

## Responsabilidades
- Abrir conexao TCP com o servidor.
- Enviar acoes e receber estado/mascara/recompensa.
- Expor `reset()` e `step()` no formato Gymnasium.

## Estrutura do protocolo (JSON linha a linha)
- **Python -> Java**
  - Payload: `{ "action": 12 }`
- **Java -> Python**
  - Payload: `{ "observation": [...], "action_mask": [...], "reward": 0.0, "done": false }`

## Metodos
### `reset()`
Envia acao -1 para obter o estado inicial.

### `step(action)`
Envia a acao, atualiza `current_mask` e retorna `obs, reward, done, truncated, info`.

### `get_action_mask()`
Retorna a mascara atual de acoes validas.

## Observacoes
- A conexao possui retries e timeout configuraveis.
- Cada mensagem e enviada como uma unica linha JSON (terminada por `\n`).
