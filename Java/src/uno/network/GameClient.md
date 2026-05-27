# `GameClient`

## Visao geral
`GameClient` e o cliente de rede que conecta no servidor, recebe mensagens do protocolo e envia acoes do jogador.

## Responsabilidades
- Estabelecer conexao com o servidor.
- Ler mensagens recebidas e despachar para `GameClientListener`.
- Enviar acoes do jogador e comandos de lobby.

## Fluxo principal
1. Conecta no servidor e registra o nome.
2. Escuta mensagens (lobby, estado, inicio/fim da partida).
3. Envia acoes quando o jogador decide.

## Metodos principais
### `connect(String host, int port, String playerName)`
Abre o socket, envia o nome e inicia a thread de leitura.

### `submitAction(int action)`
Enfileira a acao escolhida para envio ao servidor.

### `disconnect()`
Envia comando de saida do lobby e fecha o socket.

## Observacoes
- Usa `GameClientListener` para notificar a UI.
- O envio de acoes ocorre em loop dedicado.

