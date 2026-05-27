# Simulation

## Visao geral
`Simulation` executa uma partida de UNO. Ela controla baralhos, jogadores, regras, ordem de turno e emite eventos para observadores (UI, logs e rede). A cada chamada de `playTurn()` a simulacao processa exatamente um turno e avanca o estado do jogo ate terminar.

## Principais responsabilidades
- Inicializar jogadores e distribuir cartas.
- Aplicar regras opcionais (stacking, skip n flip, seven zero, etc.).
- Processar compras, jogadas, desafios e efeitos de cartas especiais.
- Manter observacao e mascara de decisoes para IA.
- Notificar listeners e espectadores sobre eventos do jogo.

## Construtores
### `Simulation(int numHumanPlayers, int numAIPlayers, int numRandomPlayers, ConnectionAI ConnectionAI, boolean[] rules, String id, int seed)`
Cria os jogadores (humanos, IA e random), embaralha e distribui 7 cartas para cada um, guarda o conjunto de regras e chama `initGame()`.

### `Simulation(Player[] players, ConnectionAI ConnectionAI, boolean[] rules, String id, int seed)`
Usa um array de jogadores ja criado, distribui as 7 cartas iniciais e chama `initGame()`.

## Fluxo de jogo (detalhado)
1. `initGame()` embaralha jogadores, embaralha o baralho e coloca a primeira carta no descarte.
2. Se a primeira carta for `BLACK`, uma cor e escolhida aleatoriamente.
3. Efeitos iniciais (reverse/skip/draw) sao aplicados antes do primeiro turno.
4. `startGame()` emite `onGameStart`.
5. `playTurn()` resolve empilhamento/blefe, calcula cartas jogaveis, decide acao e encerra o turno.
6. `finishRound()` troca jogador, atualiza observacao/mask e encerra a partida se houver vencedor ou 3000 turnos.

## Regras configuraveis (array `rules`)
A ordem esperada e:
1. stacking
2. skipNFlip
3. sevenZero
4. forcePlayDrawn
5. drawAndPlay
6. drawToMatch
7. bluffing
8. playIdentical

### Impactos principais
- `stacking`/`skipNFlip`: definem como DRAW_TWO/DRAW_FOUR podem ser respondidos.
- `sevenZero`: habilita troca de maos (7) e rotacao de maos (0).
- `forcePlayDrawn`/`drawAndPlay`/`drawToMatch`: controlam a forma de compra.
- `bluffing`: permite desafio do DRAW_FOUR.
- `playIdentical`: permite jogar cartas identicas em sequencia.

## Observacao e mascara (IA)
### `observationVector` (tamanho 189)
- `0-53`: cartas na mao do jogador atual (0.5 por carta, 1 se repetida).
- `54-107`: observacao da mao do jogador anterior (usado apenas em desafio de DRAW_FOUR).
- `108-161`: carta do topo do descarte (one-hot).
- `162-171`: tamanho das maos dos oponentes (normalizado por 20).
- `172-179`: regras ativas.
- `180-188`: flags do tipo de decisao atual.

### `decisionMask` (tamanho 70)
- `0-53`: cartas jogaveis.
- `54-57`: escolha de cor.
- `58-67`: escolha de jogador (swap no 7).
- `68-69`: escolha binaria (sim/nao).

## Metodos principais (comportamento)
### `playTurn()`
- Trata DRAW_FOUR com blefe (`handleBluffChallenge`).
- Resolve empilhamento e compra obrigatoria.
- Calcula cartas jogaveis com `isCardPlayable`.
- Decide entre jogar ou comprar (`handlePlayOrDrawChoice`).
- Encerra turno e chama `finishRound()`.

### `handlePlayOrDrawChoice(...)` / `handlePlayWhenCannotDraw(...)`
Montam o `decisionMask` para escolhas binarias e processam a resposta do jogador.

### `handleStackChoice(...)`
Permite jogar carta de empilhamento ou comprar o total acumulado.

### `handleDrawCardsChoice()`
Compra carta(s), aplica `drawToMatch` e `forcePlayDrawn`.

### `chooseCardToPlay(...)`
Prepara mascara de jogada, valida input (especialmente para IA) e retorna a carta.

### `playCard(...)`
Aplica efeitos da carta: reverse, skip, wild, draw, seven/zero e playIdentical.

### `finishRound()`
Atualiza observacao/mask, troca jogador e determina fim de jogo.

## Metodos de utilidade
- `isCardPlayable(Card)`: decide se a carta pode ser jogada considerando empilhamento.
- `canDraw()`: checa baralho e faz reshuffle se necessario.
- `reshuffleDeck()`: recicla o descarte, restaurando cor de curingas para `BLACK`.
- `nextPlayer()` / `invertDirection()`: controle de turno.
- `scramblePlayers()`: embaralha a ordem inicial (seeded).
- `generateObservationVector()` / `generateDecisionMask(...)`: constroem vetores para IA.

## Dados expostos
- `getPlayableCardIdsForNetwork()` fornece ids validos quando a decisao e escolher carta.
- `computeShapedReward(...)` calcula recompensa densa baseada no tamanho das maos.
- Getters/setters para logger, seed, vencedor e vetores da IA.

## Pontos de atencao
- `rules` deve ter tamanho 8 e a ordem correta.
- Alterar `Value`/`Color` afeta ids e observacoes.
- `playTurn()` e deterministico com `seed`, exceto quando `seed == -1`.
