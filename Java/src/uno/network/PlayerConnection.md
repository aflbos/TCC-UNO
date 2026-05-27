# `PlayerConnection`

## Visao geral
`PlayerConnection` encapsula a comunicacao com um cliente conectado via socket. Ele envia mensagens do servidor e recebe comandos do cliente.

## Responsabilidades
- Enviar notificacoes e estados do jogo.
- Ler comandos do cliente e manter o nome do jogador.
- Fornecer utilitarios de lobby (updates, disconnect, etc.).

## Metodos principais
### `readName()`
Recebe o nome inicial do jogador.

### `sendLobbyUpdate(String summary)`
Envia o resumo do lobby.

### `sendGameState(...)`
Envia mao, carta do topo, entradas validas e prompt de decisao.

### `receiveAction()`
Bloqueia ate receber uma acao valida do cliente.

### `sendGameOver(String winner)`
Envia o resultado da partida.

## Observacoes
- Atua como facade de IO para evitar espalhar logica de rede pelo servidor.

