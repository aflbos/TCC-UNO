# `PlayerAI`

## Visao geral
`PlayerAI` representa um jogador controlado por IA. Ele consulta um backend externo via `ConnectionAI` usando o vetor de observacao e a mascara de decisao fornecidos pela `Simulation`.

## Responsabilidades
- Transformar o estado do jogo em uma solicitacao para a IA.
- Enviar recompensas parciais para treinamento.
- Validar a resposta da IA e propagar erros de conexao.

## Estrutura interna
### Atributos
- `ConnectionAI`: gateway para o backend de IA.
- `rewardBaselineInitialized`: controla quando o shaped reward passa a ser calculado.
- `previousOwnHandSize`: tamanho da mao anterior do proprio jogador.
- `previousNextOpponentHandSize`: tamanho da mao anterior do proximo oponente.
- `backendUnavailable`: sinaliza indisponibilidade do backend.

## Metodos
### `getInput()`
Fluxo real:
1. Le `observationVector` e `decisionMask` da `Simulation`.
2. Monta a lista de entradas validas a partir da mascara (indices com valor != 0).
3. Calcula shaped reward se o baseline ja foi inicializado.
4. Atualiza tamanhos anteriores de mao (para a proxima iteracao).
5. Envia a acao para o backend via `ConnectionAI.askAction(...)`.
6. Se a IA retornar uma acao invalida, aplica penalidade (`invalidActionPenalty -= 0.1`) e tenta novamente.

#### Erros de rede
Falhas no backend resultam em `RuntimeException` e `backendUnavailable = true`.

### `onGameOver(Player winner)`
- Calcula recompensa final (vitoria/derrota/empate).
- Envia a ultima chamada para o backend com `terminal = true`.
- Se o backend estiver indisponivel, o envio e ignorado.

## Observacoes
- A mascara de decisao deve ser consistente; entradas invalidas forcam penalidades adicionais.
- `rewardBaselineInitialized` evita recompensa no primeiro passo (baseline).
