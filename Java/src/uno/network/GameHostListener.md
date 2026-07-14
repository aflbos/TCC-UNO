# `GameHostListener`

## Visao geral
`GameHostListener` define os callbacks usados pelo `HostApp` para receber notificacoes do andamento de uma partida.

## Responsabilidades
- Receber atualizacoes do estado geral do jogo.
- Notificar jogadas e compras de cartas.
- Manter compatibilidade com a assinatura antiga de `onGameStateUpdate`.

## Metodos
### `onGameStateUpdate(Player[] players, String currentPlayerName, Card topCard, int turnCounter, List<String> recentPlays)`
Recebe o estado atual do jogo e um historico recente de jogadas.

### `onGameStateUpdate(Player[] players, String currentPlayerName, Card topCard, int turnCounter)`
Versao legada que delega para a assinatura com historico vazio.

### `onCardPlayedNotification(String playerName, Card card)`
Chamada quando uma carta e jogada.

### `onCardsDrawnNotification(String playerName, int cardCount)`
Chamada quando um jogador compra cartas.

## Observacoes
- A interface e usada principalmente pelo servidor/host para atualizar a interface de console.
- A sobrecarga padrao evita quebra de compatibilidade com implementacoes antigas.

