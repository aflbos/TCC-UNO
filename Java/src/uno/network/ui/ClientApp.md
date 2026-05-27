# `ClientApp`

## Visao geral
`ClientApp` e a interface de console para o jogador se conectar ao servidor, visualizar o lobby e jogar partidas online.

## Responsabilidades
- Coletar configuracoes de conexao (nome, host, porta).
- Conectar ao servidor via `GameClient`.
- Exibir notificacoes e estado do jogo.
- Ler comandos do usuario e enviar acoes.

## Fluxo principal
1. Menu de conexao (definir nome/host/porta).
2. Conectar e entrar no lobby.
3. Receber atualizacoes do servidor e exibir no console.
4. Durante o jogo, mostrar estado e aceitar acoes.

## Componentes principais
### `CliClientListener`
Implementa `GameClientListener` e traduz eventos em mensagens no console.

### `runConnectedLobbyLoop(...)`
Loop que aceita comandos de lobby enquanto o jogador nao esta em uma decisao ativa.

### `onState(...)`
Exibe estado do jogo (mao, carta do topo, prompt) e aguarda a entrada do jogador.

## Observacoes
- Comandos como `help`, `players`, `state` e `actions` auxiliam durante o jogo.
- A entrada do jogador e validada contra `validInputs` enviados pelo servidor.
