# `GameClientListener`

## Visao geral
`GameClientListener` define callbacks para eventos recebidos pelo cliente de rede.

## Responsabilidades
- Receber notificacoes do servidor.
- Atualizar UI com lobby, estado do jogo e fim de partida.

## Metodos (callbacks)
- `onNotify(String message)`: notificacoes gerais.
- `onLobbyUpdate(String summary)`: atualizacao do lobby.
- `onPlayers(String summary)`: lista de jogadores.
- `onGameStart()`: inicio da partida.
- `onState(...)`: estado do jogo e prompt de decisao.
- `onGameOver(String winner)`: fim da partida.
- `onDisconnect(String reason)`: desconexao.

## Observacoes
- As implementacoes tipicas ficam em `uno.network.ui`.

