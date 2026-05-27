# `Value`

## Visão geral
A enumeração `Value` representa os valores das cartas do UNO, incluindo cartas numéricas, cartas de ação e cartas especiais.

## Valores definidos
### Cartas numéricas
- `ZERO`
- `ONE`
- `TWO`
- `THREE`
- `FOUR`
- `FIVE`
- `SIX`
- `SEVEN`
- `EIGHT`
- `NINE`

### Cartas de ação
- `SKIP`
- `REVERSE`
- `DRAW_TWO`

### Cartas especiais
- `WILD`
- `DRAW_FOUR`

## Estrutura interna
### Ordem e grupos
A ordem e relevante para o mapeamento de ids em `Card` e para a montagem do `Deck`:

1. Numericas: `ZERO` ate `NINE` (indices 0-9)
2. Acoes: `SKIP`, `REVERSE`, `DRAW_TWO` (indices 10-12)
3. Especiais: `WILD`, `DRAW_FOUR` (indices 13-14)

Alterar essa ordem impacta `Card.getId()` e a composicao do baralho.

## Metodos relevantes
### `toString()`
Converte o valor para texto em portugues, usado nas saidas de `Card.toString()`:

- Numeros: `"0"` a `"9"`
- `SKIP` -> `PULAR`
- `REVERSE` -> `INVERTER`
- `DRAW_TWO` -> `COMPRA 2`
- `WILD` -> `CURINGA`
- `DRAW_FOUR` -> `COMPRA 4`

Se algum valor novo for adicionado e nao tratado, o metodo cai no `name()`.

## Papel no sistema
`Value` é usada pela classe `Card` para representar o conteúdo principal da carta. A ordem dos valores também é relevante porque a conversão de cartas para identificador numérico depende de `ordinal()`.

## Observações de implementação
- Os indices 0-12 sao usados para ids por cor (0-51).
- `WILD` e `DRAW_FOUR` sao tratados separadamente por id (52 e 53).
- A ordem dos elementos da enumeração não deve ser alterada sem revisar a lógica de `Card`.

## Exemplo de uso
```java
Value value = Value.DRAW_TWO;
System.out.println(value);
```

## Resumo
`Value` reúne os números, ações e curingas do UNO em uma única enumeração, servindo de base para a modelagem das cartas.
