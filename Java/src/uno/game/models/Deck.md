# `Deck`

## Visão Geral

`Deck` é uma estrutura de dados que representa um baralho de cartas do UNO. É usada em dois papéis distintos dentro de `Simulation`: como **baralho de compras** (`drawDeck`) e como **pilha de descarte** (`discardPile`). Ambos compartilham a mesma implementação; o comportamento esperado de cada um é determinado pelo código que os manipula.

**Pacote:** `uno.game.models`

---

## Convenção de Topo

O topo do baralho é sempre o **último elemento** da lista interna (`cards.get(cards.size() - 1)`). Comprar remove do fim; adicionar insere no fim. Essa convenção é seguida consistentemente por todos os métodos.

---

## Atributo Interno

| Atributo | Tipo                  | Descrição                           |
|----------|-----------------------|-------------------------------------|
| `cards`  | `ArrayList<Card>`     | Lista dinâmica de cartas do baralho |

---

## Construtor — `Deck()`

Cria um baralho completo e padrão do UNO com 108 cartas. A montagem é feita em três etapas:

### 1. Cartas Numéricas (76 cartas)

Loop externo: 4 cores (`colors[0]` a `colors[3]`, i.e., `RED`, `GREEN`, `BLUE`, `YELLOW`).
Loop interno: `j` de 1 a 19 (exclusive), usando `values[j/2]`:

| `j` | `j/2` | `Value`  | Adições por cor |
|-----|-------|----------|-----------------|
| 1   | 0     | `ZERO`   | 1               |
| 2,3 | 1     | `ONE`    | 2               |
| 4,5 | 2     | `TWO`    | 2               |
| ... | ...   | ...      | ...             |
|18,19| 9     | `NINE`   | 2               |

Total: 1 (ZERO) + 9×2 (ONE–NINE) = 19 por cor × 4 cores = **76 cartas**.

### 2. Cartas de Ação (24 cartas)

Dois loops externos (`n = 0` e `n = 1`) × 4 cores × 3 ações (`values[10]`=`SKIP`, `values[11]`=`REVERSE`, `values[12]`=`DRAW_TWO`):

2 × 4 × 3 = **24 cartas**.

### 3. Cartas Especiais (8 cartas)

4 `WILD` (`Color.BLACK`, `Value.WILD`) + 4 `DRAW_FOUR` (`Color.BLACK`, `Value.DRAW_FOUR`) = **8 cartas**.

### Total: 108 cartas

| Categoria         | Por cor | Cores | Total |
|-------------------|---------|-------|-------|
| `ZERO`            | 1       | 4     | 4     |
| `ONE` – `NINE`    | 2 cada  | 4     | 72    |
| `SKIP`            | 2       | 4     | 8     |
| `REVERSE`         | 2       | 4     | 8     |
| `DRAW_TWO`        | 2       | 4     | 8     |
| `WILD`            | —       | —     | 4     |
| `DRAW_FOUR`       | —       | —     | 4     |
| **Total**         |         |       | **108** |

> **Dependência crítica:** A montagem usa `Value.values()[j/2]` e `Value.values()[j]` com índices fixos. Se a ordem de `Value` mudar, a composição do baralho muda junto.

---

## Métodos

### `shuffle(int seed)`

Embaralha o baralho usando o algoritmo **Fisher-Yates** (variante de Knuth): percorre a lista de trás para frente e troca cada elemento com um elemento aleatório de posição igual ou anterior.

| `seed`         | Comportamento                              |
|----------------|--------------------------------------------|
| `-1`           | `new Random()` — não determinístico        |
| Qualquer outro | `new Random(seed)` — reprodutível com a mesma seed |

Usado em `Simulation` em três momentos:
1. No construtor, antes de distribuir as cartas iniciais.
2. Em `initGame()`, para um segundo embaralhamento após `scramblePlayers()`.
3. Em `reshuffleDeck()`, ao reciclar o descarte de volta ao baralho.

---

### `drawCard() → Card`

Remove e retorna a carta do topo (último elemento da lista).

- Retorna `null` se a lista estiver vazia.
- Em `Simulation`, `canDraw()` sempre é verificado antes de chamar `drawCard()`, evitando o `null`.

---

### `placeCard(Card card)`

Insere a carta no topo (fim da lista). Usado para:
- Adicionar cartas ao `discardPile` após uma jogada.
- Reciclar cartas de volta ao `drawDeck` em `reshuffleDeck()`.

Não valida `null` — passar `null` insere `null` na lista, o que causará `NullPointerException` em operações posteriores.

---

### `emptyDeck()`

Chama `cards.clear()`, removendo todas as cartas. Usado em `initGame()` para limpar o `discardPile` antes de reiniciar.

---

### `peekTopCard() → Card`

Retorna a carta do topo sem remover. Retorna `null` se a lista estiver vazia.

Usado extensivamente em `Simulation` para inspecionar a carta ativa do descarte sem consumi-la.

---

### `peekBottomCard() → Card`

Retorna o elemento de índice `0` (base da lista) sem remover. Retorna `null` se vazio.

Uso menos frequente — presente para inspeção em casos específicos.

---

### `getCards() → ArrayList<Card>`

Retorna a referência direta para a lista interna.

> **Atenção:** Expõe o estado interno. Modificações externas na lista retornada (adicionar, remover, ou alterar cartas) afetam diretamente o baralho. Usado em `reshuffleDeck()` para iterar sobre as cartas do descarte antes de reciclá-las.

### `setCards(ArrayList<Card> cards)`

Substitui a lista interna por completo. Usado em `Simulation.playCard()` para implementar os efeitos de `ZERO` (rotação de mãos) e `SEVEN` (troca de mãos), diretamente sobre os decks dos jogadores.

---

### `toString() → String`

Retorna:
- A representação textual da carta do topo (via `Card.toString()`) se o baralho não estiver vazio.
- `"No cards in deck."` se estiver vazio.

---

## Uso em `Simulation`

| Instância       | Papel                      | Operações típicas                                    |
|-----------------|----------------------------|------------------------------------------------------|
| `drawDeck`      | Baralho de compras         | `drawCard()`, `shuffle()`, `getCards().isEmpty()`    |
| `discardPile`   | Pilha de descarte          | `placeCard()`, `peekTopCard()`, `drawCard()` (reshuffle), `emptyDeck()` |

### Ciclo de Reshuffle (`reshuffleDeck()`)

Quando `drawDeck` fica vazio durante o jogo:
1. A carta do topo de `discardPile` é preservada com `drawCard()`.
2. Todas as cartas restantes do descarte são transferidas para `drawDeck` via `placeCard()`.
3. Cartas `WILD` e `DRAW_FOUR` têm sua cor resetada para `Color.BLACK` antes de serem recolocadas.
4. `discardPile` é esvaziado e a carta do topo é restaurada com `placeCard(topCard)`.
5. `drawDeck` é embaralhado com a seed atual.

---

## Restrições e Cuidados

- `drawCard()` e `peekTopCard()` retornam `null` com baralho vazio — sempre verifique `canDraw()` antes de comprar em `Simulation`.
- `getCards()` expõe a estrutura interna sem proteção; use com cautela.
- A composição automática em `Deck()` depende dos ordinals de `Value`; não altere a ordem do enum.
- Não há método de tamanho explícito (`size()`); use `getCards().size()` para contar as cartas.
