# `Player`

## Visao geral
A classe abstrata `Player` define a base para todos os tipos de jogadores no projeto (humano, IA, rede e aleatorio). Ela guarda a mao de cartas, o nome e a referencia para a `Simulation` atual.

## Responsabilidades
- Manter a mao de cartas do jogador.
- Oferecer operacoes de compra/descartar/ordenar cartas.
- Expor nome e simulacao.
- Definir o contrato de entrada (`getInput()`).

## Estrutura interna
### Atributos
- `cards`: lista mutavel com a mao do jogador.
- `name`: nome exibido em eventos e logs.
- `simulation`: referencia para a simulacao atual (usada para inputs/estado).

#### Observacao
`getCards()` retorna a lista interna; alteracoes externas afetam a mao diretamente.

## Metodos
### `addCardToHand(Card card)`
Adiciona a carta ao fim da lista.

### `discardCard(int index)`
Remove a carta pelo indice (0-based). Lanca `IndexOutOfBoundsException` se o indice for invalido.

### `discardCardId(int id)`
Remove a primeira carta cujo `getId()` seja igual ao id informado. Lanca excecao se nao encontrar.

### `discardCard(Card card)`
Remove a carta que corresponda por referencia, id ou combinacao de cor/valor. Se nao encontrar, retorna o proprio `card` sem alterar a mao.

### `peekCard(int index)`
Retorna a carta pelo indice sem remover. Lanca excecao se o indice for invalido.

### `sortCards()`
Ordena primeiro por cor (`Color.compareTo`) e, em seguida, por `Value.ordinal()`.

### `getCards()` / `setCards(ArrayList<Card> cards)`
Acessa ou substitui a mao. `setCards` troca a lista inteira.

### `getInput()` (abstrato)
Cada subclasse deve respeitar `simulation.getValidInputs()` e retornar uma escolha valida.

### `toString()`
Formata a mao com indices **1-based** para exibicao humana.

## Observacoes
- A classe base nao valida coerencia entre `cards` e estado do jogo; a `Simulation` controla isso.
- Em subclasses, `getInput()` deve considerar que a simulacao ja montou observacao/mascara quando aplicavel.
