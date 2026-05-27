# `Card`

## Visão geral
A classe `Card` representa uma carta individual do jogo UNO. Ela encapsula as duas informações fundamentais de uma carta:

- `Color`: a cor da carta
- `Value`: o valor ou efeito da carta

Além disso, a classe oferece suporte a conversão bidirecional entre a carta e um identificador inteiro interno (`id`). Isso é útil para serialização, armazenamento, comparação e integração com rotinas de jogo que trabalham com índices numéricos.

## Responsabilidades
- Representar uma carta jogável.
- Armazenar cor e valor.
- Converter a carta em um identificador numérico.
- Reconstruir a carta a partir de um identificador numérico.
- Exibir uma representação textual legível.

## Estrutura interna
### Atributos
- `color`: cor da carta. Para cartas especiais, o codigo força `Color.BLACK`.
- `value`: valor/efeito da carta. Os valores especiais sao `WILD` e `DRAW_FOUR`.

## Construtores
### `Card(Color color, Value value)`
Cria uma carta informando explicitamente sua cor e seu valor.

### `Card(int id)`
Cria uma carta a partir do identificador numérico interno. Esse construtor delega a lógica para `setId(int)`.

## Métodos
### `getColor()` / `setColor(Color color)`
Acessa ou atualiza a cor. Nao ha validacao interna; o chamador deve garantir consistencia (ex.: `BLACK` para curinga).

### `getValue()` / `setValue(Value value)`
Acessa ou atualiza o valor. Nao ha validacao interna; o chamador deve garantir consistencia com `color`.

### `getId()`
Converte a carta em um id inteiro usado internamente.

#### Regras de mapeamento (detalhadas)
- `WILD` retorna `52`.
- `DRAW_FOUR` retorna `53`.
- Para as demais cartas (numeros e acoes):
  - `id = color.ordinal() * 13 + value.ordinal()`
  - Assim, cada cor ocupa um bloco de 13 ids, na ordem definida em `Color`.

#### Implicacoes
- A ordem de `Color` e `Value` (ate `DRAW_TWO`) nao deve mudar sem atualizar esse mapeamento.
- `WILD` e `DRAW_FOUR` ficam fora dos blocos de cor.

### `setId(int id)`
Reconstrui a carta a partir do id.

#### Regras de conversao (detalhadas)
- `52` -> `Color.BLACK` + `Value.WILD`.
- `53` -> `Color.BLACK` + `Value.DRAW_FOUR`.
- Demais ids:
  - `colorIndex = id / 13`
  - `valueIndex = id % 13`
  - Usa `Color.values()[colorIndex]` e `Value.values()[valueIndex]`.

#### Cuidados
- Nao ha validacao de faixa: ids fora do intervalo esperado podem gerar `ArrayIndexOutOfBoundsException`.
- O metodo assume que `id` foi gerado pela mesma regra de `getId()`.

### `toString()`
Retorna `"<cor> <valor>"` usando os `toString()` de `Color` e `Value`.

#### Observacao
Isso significa que a saida e em portugues (ex.: `VERMELHO 5`, `PRETO COMPRA 4`).

## Regras e limitações
- A consistencia entre `color` e `value` e responsabilidade do chamador.
- `Color.BLACK` e reservado para cartas especiais.
- Alterar a ordem dos enums quebra o mapeamento de ids.

## Exemplo de uso
```java
Card card = new Card(Color.RED, Value.FIVE);
int id = card.getId();

Card special = new Card(52); // BLACK WILD
System.out.println(special);
```

## Resumo
`Card` é a unidade básica do modelo de UNO no projeto. Ela representa uma carta com cor e valor, e também fornece a ponte entre representação orientada a objetos e representação numérica interna.
