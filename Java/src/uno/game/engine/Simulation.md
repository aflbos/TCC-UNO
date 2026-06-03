# `Simulation`

## Visão Geral

`Simulation` é a classe central do motor de jogo UNO. Ela controla o estado completo de uma partida: baralhos, jogadores, direção de turno, empilhamento de compras, regras opcionais e a geração de vetores de observação e máscara de decisão para o agente de IA. A partida avança por chamadas sucessivas a `playTurn()`, que processam exatamente um turno cada.

**Pacote:** `uno.game.engine`

---

## Dependências

| Tipo                     | Papel                                                                                |
|--------------------------|--------------------------------------------------------------------------------------|
| `Player` / subclasses    | `PlayerHuman`, `PlayerAI`, `PlayerRandom`, `PlayerNetwork` — tipos de jogador        |
| `Card`, `Color`, `Value` | Modelo de cartas                                                                     |
| `Deck`                   | Baralho de compras e pilha de descarte                                               |
| `ConnectionAI`           | Canal de comunicação com o agente Python via socket                                  |
| `GameEventListener`      | Interface de eventos de jogo (logger, rede, espectadores)                            |
| `Logger`                 | Implementação de `GameEventListener` para logging                                    |

---

## Constantes

### Índices da Máscara de Decisão (`decisionMask`)

| Constante            | Valor | Significado                                      |
|----------------------|-------|--------------------------------------------------|
| `MASK_COLOR_RED`     | 55    | Índice na máscara para escolher vermelho         |
| `MASK_COLOR_GREEN`   | 56    | Índice para verde                                |
| `MASK_COLOR_BLUE`    | 57    | Índice para azul                                 |
| `MASK_COLOR_YELLOW`  | 58    | Índice para amarelo                              |
| `MASK_BINARY_YES`    | 68    | Índice para resposta binária "Sim"               |
| `MASK_BINARY_NO`     | 69    | Índice para resposta binária "Não"               |

### Índices de Flags no Vetor de Observação (`observationVector`)

| Constante                | Índice | Situação sinalizada                                                  |
|--------------------------|--------|----------------------------------------------------------------------|
| `FLAG_PLAY_IDENTICAL`    | 180    | Jogador decide se joga carta idêntica                                |
| `FLAG_CHOOSE_COLOR`      | 181    | Jogador escolhe cor após `WILD`/`DRAW_FOUR`                          |
| `FLAG_SWAP_HANDS`        | 182    | Jogador escolhe com quem trocar mãos (regra `SEVEN`)                 |
| `FLAG_CHOOSE_CARD`       | 183    | Jogador escolhe qual carta jogar                                     |
| `FLAG_PLAY_DRAWN_CARD`   | 184    | Jogador decide se joga a carta que acabou de comprar                 |
| `FLAG_PLAY_WHEN_NO_DRAW` | 185    | Jogador decide jogar ou passar quando não pode comprar               |
| `FLAG_PLAY_OR_DRAW`      | 186    | Jogador decide jogar ou comprar (tendo cartas jogáveis)              |
| `FLAG_STACK`             | 187    | Jogador decide empilhar ou absorver compras pendentes                |
| `FLAG_CHALLENGE_DRAW4`   | 188    | Jogador decide desafiar `DRAW_FOUR` do adversário                    |

---

## Atributos de Estado

| Atributo                       | Tipo          | Descrição                                                                     |
|--------------------------------|---------------|-------------------------------------------------------------------------------|
| `players`                      | `Player[]`    | Array de jogadores na ordem de turno (embaralhado em `scramblePlayers`)        |
| `drawDeck`                     | `Deck`        | Baralho de compras                                                            |
| `discardPile`                  | `Deck`        | Pilha de descarte                                                             |
| `ConnectionAI`                 | `ConnectionAI`| Conexão com o agente Python                                                   |
| `rules`                        | `boolean[]`   | Array com 8 regras opcionais (ver seção Regras)                               |
| `gameOver`                     | `boolean`     | `true` quando a partida terminou                                              |
| `winner`                       | `Player`      | Vencedor da partida; `null` em empate por limite de turnos                    |
| `turnCounter`                  | `int`         | Contador de turnos (partida encerra com `null` winner ao atingir 3000)        |
| `currentPlayerIndex`           | `int`         | Índice do jogador atual em `players`                                          |
| `direction`                    | `int`         | `+1` = sentido normal; `-1` = sentido invertido                               |
| `stackingAmount`               | `int`         | Total de cartas acumuladas por empilhamento ainda não absorvidas              |
| `previousPlayerBluffed`        | `boolean`     | `true` se o jogador anterior jogou `DRAW_FOUR` tendo carta da cor ativa       |
| `human` / `ai` / `random`      | `boolean`     | Tipo do jogador atual (mutuamente exclusivos por turno)                       |
| `debug`                        | `boolean`     | Habilita eventos `onDebugTick` a cada 100 turnos                             |
| `id`                           | `String`      | Identificador único desta simulação                                           |
| `seed`                         | `int`         | Seed para reprodutibilidade; `-1` = não determinístico                        |
| `observationVector`            | `double[189]` | Vetor de observação atual para o agente de IA                                 |
| `decisionMask`                 | `double[70]`  | Máscara de decisão atual (ações válidas)                                      |
| `previousPlayerHandObservation`| `double[54]`  | Observação da mão do jogador anterior (usada em desafio de `DRAW_FOUR`)       |
| `validInputs`                  | `int[]`       | Entradas numéricas válidas para o jogador/IA na decisão atual                 |
| `playableCardIdsForNetwork`    | `int[]`       | Ids das cartas jogáveis quando a decisão é "escolher carta" (para rede)       |
| `logger`                       | `Logger`      | Logger opcional; `null` desabilita eventos de log                             |

---

## Construtores

### `Simulation(int numHumanPlayers, int numAIPlayers, int numRandomPlayers, ConnectionAI, boolean[] rules, String id, int seed)`

Cria os jogadores automaticamente a partir das contagens fornecidas:
- `PlayerHuman` nomeados `"H. Player 1"`, `"H. Player 2"`, ...
- `PlayerAI` nomeados `"AI. Player 2"`, ... (índice global)
- `PlayerRandom` nomeados `"R. Player 3"`, ...

Para cada jogador criado, distribui 7 cartas do `drawDeck` e chama `sortCards()`. Em seguida, extrai as 8 regras do array `rules` (índices 0–7) para constantes booleanas individuais e chama `initGame()`.

### `Simulation(Player[] players, ConnectionAI, boolean[] rules, String id, int seed)`

Usa um array de jogadores já instanciados externamente. Distribui 7 cartas para cada jogador, aplica `sortCards()`, extrai as regras e chama `initGame()`. Útil para cenários de teste ou redes multiplayer onde os jogadores são criados antes da simulação.

---

## Regras Opcionais — Array `rules`

O array `rules` deve ter exatamente 8 elementos booleanos na seguinte ordem:

| Índice | Constante              | Efeito quando `true`                                                                                  |
|--------|------------------------|-------------------------------------------------------------------------------------------------------|
| 0      | `RULE_STACKING`        | Permite empilhar `DRAW_TWO` sobre `DRAW_TWO` e `DRAW_FOUR` sobre qualquer draw card                  |
| 1      | `RULE_SKIP_N_FLIP`     | Amplia empilhamento: `SKIP` e `REVERSE` também podem ser jogados sobre draw cards                    |
| 2      | `RULE_SEVEN_ZERO`      | Jogar `SEVEN` permite trocar mão com outro jogador; jogar `ZERO` rotaciona todas as mãos              |
| 3      | `RULE_FORCE_PLAY_DRAWN`| Carta comprada que seja jogável é obrigatoriamente jogada (sem escolha)                              |
| 4      | `RULE_DRAW_AND_PLAY`   | Após comprar (ou absorver empilhamento), o turno não encerra automaticamente — jogador pode jogar     |
| 5      | `RULE_DRAW_TO_MATCH`   | Jogador continua comprando cartas até comprar uma que seja jogável (ou baralho esgote)               |
| 6      | `RULE_BLUFFING`        | Permite desafiar `DRAW_FOUR`: se o jogador anterior tinha cor ativa, sofre a penalidade              |
| 7      | `RULE_PLAY_IDENTICAL`  | Após jogar uma carta, se o jogador tiver carta idêntica (mesma cor e valor), pode jogá-la em cadeia   |

---

## Fluxo de Jogo

### `initGame()` — Inicialização

1. `scramblePlayers()` — embaralha a ordem dos jogadores usando a seed.
2. `drawDeck.shuffle(seed)` — segundo embaralhamento do baralho.
3. `discardPile.emptyDeck()` — limpa o descarte.
4. Compra a primeira carta: se for `BLACK`, escolhe uma cor aleatória (usando a seed).
5. Aplica efeitos imediatos da primeira carta:
   - `REVERSE` → inverte direção antes do primeiro turno.
   - `SKIP` → pula o primeiro jogador.
   - `DRAW_TWO` → `stackingAmount += 2`.
   - `DRAW_FOUR` → `stackingAmount += 4`.
6. Coloca a carta no descarte.

### `startGame()` — Emissão de Início

Dispara `onGameStart(players, rules, topCard)` para todos os listeners registrados. Deve ser chamado pelo código externo após construir a `Simulation` e antes de `playTurn()`.

### `playTurn()` — Processamento de Um Turno

Fluxo completo de um turno:

```
1. Inicializa previousPlayerHandObservation com -1 (54 posições)

2. Obtém currentPlayer e topCard

3. [BLUFFING] Se topCard == DRAW_FOUR e stackingAmount != 0:
   → handleBluffChallenge()
     - Se encerrar o turno: fire(onTurnEnd) + finishRound() + return

4. [STACKING] Se topCard == DRAW_TWO ou DRAW_FOUR e stackingAmount != 0:
   Se RULE_STACKING ou RULE_SKIP_N_FLIP:
     - Calcula cartas jogáveis
     - Se vazio: drawCards(stackingAmount), stackingAmount=0
       → [RULE_DRAW_AND_PLAY=false]: fire(onTurnEnd) + finishRound() + return
     - Se não vazio: handleStackChoice(playableCards)
       → Encerra turno conforme retorno
   Senão (sem regra de stacking):
     - drawCards(stackingAmount), stackingAmount=0
     → [RULE_DRAW_AND_PLAY=false]: fire(onTurnEnd) + finishRound() + return

5. Recalcula cartas jogáveis normais
   fire(onTurnStart)

6. Se canDraw():
   - Se sem jogáveis: fire(onForcedDraw) → handleDrawCardsChoice()
   - Se com jogáveis: handlePlayOrDrawChoice(playableCards)
   Senão (baralho vazio):
   - Se sem jogáveis: fire(onCannotPlayOrDraw)
   - Se com jogáveis: handlePlayWhenCannotDraw(playableCards)

7. fire(onTurnEnd)
8. finishRound()
```

### `finishRound()` — Finalização do Turno

1. Incrementa `turnCounter`.
2. Chama `nextPlayer()` — avança o ponteiro.
3. Regenera `observationVector` e `decisionMask` para o próximo jogador.
4. Copia `previousPlayerHandObservation` para o segmento 54–107 da observação.
5. Verifica fim de jogo:
   - Se o próximo jogador ficou sem cartas: `gameOver = true`, `winner = currentPlayer`, dispara `onGameOver(winner)` e `player.onGameOver(winner)` para todos.
   - Se `turnCounter >= 3000`: `gameOver = true`, `winner = null` (empate por tempo), dispara `onGameOver(null)`.

---

## Métodos de Decisão

### `handlePlayOrDrawChoice(ArrayList<Card> playableCards)`

Ativado quando o jogador tem cartas jogáveis e também pode comprar. Define:
- `observationVector[FLAG_PLAY_OR_DRAW] = 1`
- `decisionMask[68] = 1` (Sim = jogar), `decisionMask[69] = 1` (Não = comprar)
- `validInputs = {1, 2}`

Input `0` (após `decodeInput`) → `chooseCardToPlay()` + `playCard()`.
Input `1` → `handleDrawCardsChoice()`.

---

### `handlePlayWhenCannotDraw(ArrayList<Card> playableCards)`

Ativado quando o baralho está vazio mas o jogador tem cartas jogáveis. Mecânica idêntica a `handlePlayOrDrawChoice`, com flag `FLAG_PLAY_WHEN_NO_DRAW`.

Input `0` → joga carta. Input `1` → `onPassedTurn` (passa a vez sem jogar nem comprar).

---

### `handleStackChoice(ArrayList<Card> playableCards)`

Ativado quando há empilhamento ativo e o jogador tem como responder. Define flag `FLAG_STACK` e mesmas opções binárias.

Input `0` → `chooseCardToPlay()` + `playCard()` (empilha).
Input `1` → `drawCards(stackingAmount)` (absorve).

Retorna `!RULE_DRAW_AND_PLAY`: se `true`, o turno encerra após absorver; se `false`, o jogador pode ainda jogar.

---

### `handleBluffChallenge()`

Ativado quando o topo é `DRAW_FOUR` com `stackingAmount != 0` e `RULE_BLUFFING` está ativo. Define flag `FLAG_CHALLENGE_DRAW4`.

Input `1` (desafia):
- Se `previousPlayerBluffed = true`: gera `previousPlayerHandObservation`, `drawCards(getPreviousPlayer(), 4)`, `stackingAmount -= 4`.
- Se `previousPlayerBluffed = false`: `drawCards(getCurrentPlayer(), stackingAmount + 6)`, stackingAmount=0.
  - Se `!RULE_DRAW_AND_PLAY`: avança turno e retorna `true` (encerra turno).

Input `0` (não desafia): retorna `false` (segue fluxo normal).

---

### `handleDrawCardsChoice()`

Lógica de compra padrão:

1. Compra uma carta do `drawDeck`.
2. **`RULE_DRAW_TO_MATCH`:** Enquanto a carta comprada não for jogável e houver cartas no baralho, adiciona à mão e compra outra.
3. Adiciona a carta final à mão e emite `onCardDrawn`.
4. Se a carta for jogável:
   - `RULE_FORCE_PLAY_DRAWN`: descarta e joga obrigatoriamente.
   - Caso contrário: pergunta ao jogador com flag `FLAG_PLAY_DRAWN_CARD`.
     - Input `0` → joga; Input `1` → `onPassedTurn`.
5. Se não for jogável: emite `onDrawnCardNotPlayable`.

---

### `handlePlayIdenticalCard(Card card)`

Ativado após cada `playCard()` quando `RULE_PLAY_IDENTICAL` está ativo. Busca na mão atual cartas com mesma cor **e** valor da carta recém-jogada (usando `cardCopy` para preservar a carta original antes de possíveis mutações de cor).

Se encontrar, define flag `FLAG_PLAY_IDENTICAL` e opções binárias. Input `0` → nova chamada de `chooseCardToPlay()` e `playCard()` (recursivo indiretamente via `playCard`).

---

### `chooseCardToPlay(ArrayList<Card> playableCards) → Card`

Prepara o estado para a decisão de escolha de carta:
1. Define `observationVector[FLAG_CHOOSE_CARD] = 1`.
2. Gera `decisionMask` com as cartas jogáveis.
3. Para cada carta jogável, adiciona `0.5` ao `decisionMask[card.getId()]` (somando ao `1` da base → capped em `1`).
4. Popula `validInputs` e `playableCardIdsForNetwork` com os ids das cartas.
5. Obtém input do jogador:
   - **IA:** O input é o `id` da carta. Busca o índice correspondente em `playableCards`. Levanta `RuntimeException` se o id não corresponder a nenhuma carta jogável (máscara incorreta).
   - **Humano/Random:** O input é o índice direto em `playableCards`.

---

### `chooseColor() → int`

Define flag `FLAG_CHOOSE_COLOR` e habilita os 4 índices de cor na máscara (`55`–`58`). Retorna o índice de cor escolhido (0=vermelho, 1=verde, 2=azul, 3=amarelo) após `decodeInput(input, 55)`.

---

## Métodos de Efeitos de Carta — `playCard(Card card)`

Aplica os efeitos da carta em duas passagens: primeiro `RULE_SEVEN_ZERO`, depois o `switch` principal.

### Efeitos de `RULE_SEVEN_ZERO`

| `Value` | Efeito                                                                                |
|---------|---------------------------------------------------------------------------------------|
| `ZERO`  | Rotaciona mãos: copia a mão atual, desloca todos os players um índice, e atribui a mão copiada ao último player. |
| `SEVEN` | Abre decisão de swap: `decisionMask[58+i]` para cada jogador `i`. Troca mãos entre o jogador atual e o escolhido. |

### Efeitos Padrão (switch)

| `Value`     | Efeito                                                                                     |
|-------------|--------------------------------------------------------------------------------------------|
| `REVERSE`   | `invertDirection()`                                                                        |
| `SKIP`      | `nextPlayer()` — o próximo jogador será pulado pois `finishRound()` chamará `nextPlayer()` novamente |
| `WILD`      | `chooseColor()` → `card.setColor(chosenColor)` (muta a carta no descarte)                  |
| `DRAW_TWO`  | `stackingAmount += 2`                                                                      |
| `DRAW_FOUR` | Detecta blefe (verifica se jogador tem carta da cor ativa), `chooseColor()`, `stackingAmount += 4` |

Após o switch: `sortCards()`, `discardPile.placeCard(card)`, `onCardPlayed`.

Se `RULE_PLAY_IDENTICAL`: itera sobre um **snapshot** da mão (para evitar `ConcurrentModificationException`) e chama `handlePlayIdenticalCard` para cada carta idêntica à `cardCopy` (cópia pré-mutação da carta jogada).

---

## Vetores de IA

### `generateObservationVector() → double[189]`

Constrói o vetor de observação completo do estado atual:

| Segmento    | Índices   | Conteúdo                                                                         |
|-------------|-----------|----------------------------------------------------------------------------------|
| Mão própria | 0 – 53    | `+0.5` por ocorrência do id; cap em `1.0` (carta duplicada = 1.0)                |
| Mão anterior| 54 – 107  | Preenchido por `copyPreviousHandIntoObs()` após a geração (método separado)       |
| Topo descarte| 108 – 161| One-hot: `observationVector[card.getId() + 108] = 1`                             |
| Tamanho mãos | 162 – 171 | Oponentes em ordem de turno: `handSize / 20.0`; posições não usadas = `-1`       |
| Regras ativas| 172 – 179 | `1.0` se a regra está ativa, `0.0` se não                                        |
| Flags decisão| 180 – 188 | Todas `0.0` na geração base; setadas individualmente pelos métodos de decisão     |

### `generateDecisionMask(ArrayList<Card> playableCards) → double[70]`

| Segmento         | Índices | Conteúdo                                                                      |
|------------------|---------|-------------------------------------------------------------------------------|
| Cartas jogáveis  | 0 – 53  | `1.0` para cada carta em `playableCards` (por `card.getId()`)                 |
| Escolha de cor   | 54 – 57 | `0.0` na geração base; setados por `chooseColor()`                            |
| Escolha de jogador| 58 – 67 | `0.0` na geração base; setados por `playCard()` no caso `SEVEN`               |
| Binário          | 68 – 69 | `0.0` na geração base; setados pelos métodos de decisão binária               |

### `copyPreviousHandIntoObs()`

Copia `previousPlayerHandObservation[0..53]` para `observationVector[54..107]` via `System.arraycopy`. Chamado após toda regeneração da observação para garantir que a informação do jogador anterior (relevante para desafio de `DRAW_FOUR`) esteja presente.

### `generatePreviousPlayerHandObservation() → double[54]`

Gera a observação da mão do jogador anterior com o mesmo esquema de `0.5` por carta (cap `1.0`). Chamado apenas dentro de `handleBluffChallenge()` quando o desafio é bem-sucedido.

---

## Decodificação de Input — `decodeInput(int raw, int maskOffset)`

Normaliza o input recebido do jogador para um índice interno:

| Tipo de jogador | Cálculo             | Motivo                                                  |
|-----------------|---------------------|---------------------------------------------------------|
| `ai`            | `raw - maskOffset`  | IA retorna o índice absoluto da máscara; subtrai o offset para obter índice relativo |
| Outros          | `raw - 1`           | Input humano/random começa em 1 (exibição para usuário) |

Exemplo para escolha binária (maskOffset=68): IA retorna `68` (Sim) → `68-68=0` (jogar) ou `69` → `1` (não jogar).

---

## Recompensa Modelada — `computeShapedReward(...)`

Calcula uma recompensa densa para o agente de IA baseada em três componentes:

| Componente             | Fórmula                                                          | Peso  |
|------------------------|------------------------------------------------------------------|-------|
| Proximidade própria    | `(currProximity - prevProximity)` onde `proximity = 1 - min(handSize, 20) / 20` | `0.2 × delta + 0.02 × curr` |
| Pressão sobre oponente | `currentNextOpponentHandSize - previousNextOpponentHandSize`     | `0.005` |
| Custo de step          | Constante negativa por turno                                     | `-0.0001` |

A recompensa incentiva o agente a reduzir sua própria mão (proximidade de vitória), aumentar a mão do próximo oponente, e penaliza turnos longos.

---

## Métodos Auxiliares

### `isCardPlayable(Card) → boolean`

Com `stackingAmount == 0` (turno normal): carta é jogável se mesma cor, mesmo valor, ou `Color.BLACK` (curinga).

Com `stackingAmount > 0` (empilhamento ativo):

| Topo do descarte | `RULE_SKIP_N_FLIP = true`                         | `RULE_SKIP_N_FLIP = false`         |
|------------------|---------------------------------------------------|------------------------------------|
| `DRAW_TWO`       | `DRAW_TWO`, `DRAW_FOUR`, `SKIP`, `REVERSE`        | `DRAW_TWO`, `DRAW_FOUR`            |
| `DRAW_FOUR`      | `DRAW_TWO`, `DRAW_FOUR`, `SKIP`, `REVERSE`        | apenas `DRAW_FOUR`                 |

### `canDraw() → boolean`

Verifica se há cartas disponíveis. Se `drawDeck` estiver vazio, tenta `reshuffleDeck()` antes de verificar. Retorna `false` apenas se mesmo após o reshuffle o baralho estiver vazio.

### `reshuffleDeck()`

1. Preserva a carta do topo do descarte.
2. Move todas as demais cartas do descarte para o baralho de compras.
3. Reseta a cor de curingas para `Color.BLACK` (remove cor escolhida anteriormente).
4. Restaura o topo no descarte.
5. Embaralha o baralho de compras com `getSeed()`.

### `nextPlayer()`

Avança `currentPlayerIndex` usando `Math.floorMod(currentPlayerIndex + direction, players.length)`, garantindo comportamento correto com índices negativos quando `direction = -1`.

### `invertDirection()`

Multiplica `direction` por `-1`.

### `scramblePlayers()`

Embaralha o array `players` com Fisher-Yates usando a seed. Garante que a ordem inicial dos jogadores seja reprodutível (ou aleatória se `seed == -1`).

### `getPlayerIndex(Player) → int`

Busca o índice de um jogador no array por referência (`==`). Levanta `IllegalArgumentException` se não encontrado.

---

## Sistema de Eventos — `fire(Consumer<GameEventListener>)`

Distribui eventos para todos os listeners registrados:
1. O `logger` (se não `null`).
2. Qualquer `Player` que implemente `GameEventListener` (ex.: `PlayerNetwork`).

Eventos emitidos ao longo do jogo incluem: `onGameStart`, `onTurnStart`, `onTurnEnd`, `onCardPlayed`, `onCardDrawn`, `onGameOver`, `onChallengeDecision`, `onStackDecision`, `onPlayOrDrawDecision`, `onSwapHandsDecision`, `onColorChoiceDecision`, `onIdenticalCardDecision`, `onPlayDrawnCardDecision`, `onForcedDraw`, `onCannotPlayOrDraw`, `onPassedTurn`, `onDrawnCardNotPlayable`, `onPlayWhenCannotDrawDecision`, `onDeckEmpty`, e `onDebugTick`.

---

## Getters e Setters Expostos

| Método                      | Retorno / Ação                                                              |
|-----------------------------|-----------------------------------------------------------------------------|
| `isGameOver()`              | `boolean` — se a partida encerrou                                           |
| `getWinner()`               | `Player` ou `null` (empate)                                                 |
| `getCurrentPlayer()`        | Player atual                                                                |
| `getPreviousPlayer()`       | Player anterior na direção atual                                            |
| `getNextPlayer()`           | Próximo player na direção atual                                             |
| `getPlayers()`              | Array completo de jogadores                                                 |
| `getTopCard()`              | Carta do topo do descarte                                                   |
| `getStackingAmount()`       | Total de compras acumuladas                                                 |
| `getObservationVector()`    | Referência ao vetor de observação atual                                     |
| `getDecisionMask()`         | Referência à máscara de decisão atual                                       |
| `getValidInputs()`          | Inputs válidos para o jogador/IA na decisão atual                           |
| `getPlayableCardIdsForNetwork()` | Cópia dos ids de cartas jogáveis (não-nulo apenas ao escolher carta)   |
| `hasNetworkHumanPlayer()`   | `true` se algum jogador é `PlayerNetwork`                                   |
| `computeShapedReward(...)`  | Recompensa densa calculada a partir de tamanhos de mão                      |
| `getId()` / `setId()`       | Identificador da simulação                                                  |
| `getSeed()` / `setSeed()`   | Seed de reprodutibilidade                                                   |
| `getLogger()` / `setLogger()` | Logger de eventos                                                         |
| `isDebug()` / `setDebug()`  | Modo debug (eventos `onDebugTick` a cada 100 turnos)                        |

---

## Pontos de Atenção

- **Array `rules` deve ter exatamente 8 elementos** na ordem documentada. Índices errados silenciosamente aplicam a regra errada.
- **`seed == -1` desabilita reprodutibilidade** em `shuffle`, `scramblePlayers` e escolha de cor aleatória na primeira carta.
- **`playTurn()` é bloqueante** para jogadores humanos e de rede — aguarda input via `getInput()` do `Player`.
- **`playCard()` muta a carta no `discardPile`** ao setar a cor de curingas; `cardCopy` (via `new Card(card.getId())`) é usado para comparações pós-jogada em `RULE_PLAY_IDENTICAL`.
- **`getPlayableCardIdsForNetwork()`** retorna array vazio em todos os momentos exceto quando a decisão atual é escolher uma carta para jogar; `setValidInputs()` reseta o array para `int[0]`.
- **Limite de 3000 turnos** encerra a partida com `winner = null` para evitar loops infinitos.
- **Alterar `Value`, `Color` ou `Card.getId()`** invalida os vetores de observação e os modelos treinados.
