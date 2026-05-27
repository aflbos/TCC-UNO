# `Deck`

## Visão geral
A classe `Deck` representa o baralho de cartas do jogo UNO. Ela mantém uma lista interna de cartas e oferece operações para montar, embaralhar, comprar, recolocar, esvaziar e inspecionar cartas.

## Papel no sistema
`Deck` é uma estrutura central da lógica de jogo, pois controla o fluxo das cartas durante a partida. O baralho é armazenado em uma lista, e o topo do baralho corresponde ao último elemento dessa lista.

## Estrutura interna
### Atributo
- `cards`: lista dinâmica que armazena todas as cartas do baralho.

### Convenção de topo
O topo do baralho é o **último elemento** da lista (`cards.size() - 1`).

## Construtor
### `Deck()`
Cria um novo baralho já preenchido com cartas iniciais.

#### Como o baralho é montado (detalhado)
1. **Cartas numéricas** (por cor):
   - `ZERO` aparece **1 vez** por cor.
   - `ONE` até `NINE` aparecem **2 vezes** por cor.
   - Total por cor: 19 cartas numéricas.
2. **Cartas de ação** (por cor):
   - `SKIP`, `REVERSE`, `DRAW_TWO` aparecem **2 vezes** por cor.
3. **Cartas especiais**:
   - `WILD` aparece **4 vezes**.
   - `DRAW_FOUR` aparece **4 vezes**.

#### Total de cartas
- 4 cores x 19 numéricas = 76
- 4 cores x 3 ações x 2 cópias = 24
- 8 especiais = 8
- **Total: 108 cartas**

#### Observação importante
A montagem depende da ordem de `Value` (índices 0-12 para numéricas e ações). Mudar a ordem do enum altera a composição.

## Métodos
### `shuffle(int seed)`
Embaralha o baralho usando Fisher-Yates.

#### Detalhe importante
Se `seed == -1`, o embaralhamento é não determinístico; caso contrário, é reprodutível.

### `drawCard()`
Compra e remove a carta do topo (último elemento).

#### Retorno
- Carta removida do topo.
- `null` se o baralho estiver vazio.

### `placeCard(Card card)`
Adiciona a carta no topo (fim da lista).

### `emptyDeck()`
Remove todas as cartas.

### `peekTopCard()`
Retorna a carta do topo sem remover.

### `peekBottomCard()`
Retorna a carta da base (índice `0`) sem remover.

### `getCards()` / `setCards(ArrayList<Card> cards)`
Acessa ou substitui a lista interna.

#### Cuidados
`getCards()` expõe a estrutura interna: alterações externas mudam o estado do baralho.

### `toString()`
Retorna uma representação textual do baralho.

#### Comportamento
- Se houver cartas, retorna a representação textual da carta do topo.
- Se não houver cartas, retorna `No cards in deck.`.

## Regras e limitações
- O topo é sempre o fim da lista.
- Não há proteção contra `null` em `placeCard`.
- `getCards()` permite mutação externa sem controle.

## Exemplo de uso
```java
Deck deck = new Deck();
deck.shuffle(1234);

Card first = deck.drawCard();
System.out.println(first);
```

## Resumo
`Deck` concentra toda a lógica básica de armazenamento e manipulação do baralho do UNO dentro do projeto.
