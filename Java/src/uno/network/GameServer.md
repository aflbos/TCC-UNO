# `GameServer`

## Visao geral
`GameServer` controla o lobby e executa a partida no servidor. Ele aceita conexoes, gerencia slots e inicia a `Simulation`.

## Responsabilidades
- Abrir socket e aceitar clientes.
- Manter slots do lobby (humanos, IA e random).
- Iniciar e encerrar partidas.
- Notificar clientes com atualizacoes de lobby e estado.

## Estruturas principais
### `LobbySlot`
Representa uma entrada no lobby com nome, tipo e conexao do cliente.

### `SlotType`
Indica o tipo do slot: `HUMAN`, `AI`, `RANDOM`.

## Metodos principais
### `start(int port)`
Inicia o servidor e o loop de accept.

### `stop()`
Encerra o servidor e limpa o lobby.

### `startGame(ConnectionAI connectionAI, boolean[] rules, String gameId, int seed)`
Cria jogadores, instancia `Simulation` e roda o loop da partida.

### `stopCurrentMatch(String reason)`
Interrompe a partida atual e notifica os clientes.

### `setAiCount(int n)` / `setRandomCount(int n)`
Atualiza a quantidade de bots e reconstrui o lobby.

## Observacoes
- O loop da partida roda em thread dedicada.
- O lobby e sincronizado com os clientes via `sendLobbyUpdate`.

