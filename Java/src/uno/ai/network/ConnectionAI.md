# `ConnectionAI`

## Visao geral
`ConnectionAI` encapsula a comunicacao com o backend de IA. Ele envia observacoes, mascaras e recompensas, retornando a acao sugerida.

## Responsabilidades
- Abrir e manter conexao com o backend.
- Enviar estado do jogo para inferencia.
- Enviar recompensa ao final de cada decisao/partida.

## Metodos principais
- `askAction(double[] obs, double[] mask, double reward, boolean done)`: envia estado e recebe acao.

## Protocolo JSON (linha por mensagem)
- **Java -> Python**
  - Payload: `{ "observation": [...], "action_mask": [...], "reward": 0.0, "done": false }`
- **Python -> Java**
  - Payload: `{ "action": 12 }`

## Observacoes
- Cada mensagem e enviada como uma unica linha JSON (terminada por `\n`).
- Erros de comunicacao sao propagados como excecoes.
- Usado por `PlayerAI` e rotinas de treino/avaliacao.
