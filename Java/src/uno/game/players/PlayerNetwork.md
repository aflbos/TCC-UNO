# `PlayerNetwork`

## Visao geral
`PlayerNetwork` representa um jogador remoto conectado via rede. Ele envia o estado do jogo para o cliente e recebe a acao escolhida atraves de `PlayerConnection`.

## Responsabilidades
- Enviar estado atual (mao, carta do topo, entradas validas).
- Receber e validar a acao do jogador remoto.
- Notificar o cliente sobre eventos do jogo.

## Estrutura interna
### Atributos
- `conn`: conexao ativa com o cliente remoto (`PlayerConnection`).

## Metodos
### `getInput()`
Fluxo real:
1. Coleta `validInputs`, mao (`buildHandDescription`) e carta do topo.
2. Deriva o contexto de decisao com `buildDecisionContext()`.
3. Envia estado via `conn.sendGameState(...)`.
4. Aguarda `conn.receiveAction()` e valida com `isValid(...)`.
5. Em caso de invalido, envia mensagem com opcoes validas.

#### Falhas de rede
Qualquer `IOException` e convertida em `RuntimeException` (desconexao durante o turno).

### Eventos (`GameEventListener`)
Envia notificacoes e atualizacoes frequentes para manter o cliente sincronizado.

### Helpers
- `buildHandDescription()`: serializa a mao como `id:descricao`, separado por virgulas.
- `buildOpponentSummary()`: lista oponentes com `nome:quantidade`.
- `buildDecisionContext()`: usa flags 180-188 do `observationVector` para definir o prompt e `NetworkProtocol.D_*`.
- `isValid(...)`: valida se a entrada recebida esta na lista permitida.
- `joinInts(...)`: monta string com as opcoes validas para mensagens de erro.

## Observacoes
- `simulation.getPlayableCardIdsForNetwork()` e usado somente quando a decisao e "escolher carta".
- Se `observationVector` for nulo ou curto, o contexto vira `D_UNKNOWN`.
