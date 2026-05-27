# `Color`

## Visão geral
A enumeração `Color` define as cores possíveis das cartas do UNO no projeto. Ela também inclui a cor `BLACK`, usada para cartas especiais como `WILD` e `DRAW_FOUR`.

## Valores definidos
- `RED`
- `GREEN`
- `BLUE`
- `YELLOW`
- `BLACK`

## Estrutura interna
### Valores e ordem
A ordem e usada diretamente por `Card.getId()` e `Card.setId()`:

1. `RED`
2. `GREEN`
3. `BLUE`
4. `YELLOW`
5. `BLACK`

Manter essa ordem evita que o mapeamento de ids quebre.

## Metodos relevantes
### `toString()`
Converte a cor para texto em portugues, usado nas saidas de `Card.toString()`.

- `RED` -> `VERMELHO`
- `GREEN` -> `VERDE`
- `BLUE` -> `AZUL`
- `YELLOW` -> `AMARELO`
- `BLACK` -> `PRETO`

Se algum valor novo for adicionado e nao tratado, o metodo cai no `name()`.

## Papel no sistema
`Color` é usada pela classe `Card` para representar a cor de cada carta. A ordem dos valores também importa porque a lógica de conversão de `Card` depende de `ordinal()`.

## Observações
- A presenca de `BLACK` indica cartas especiais e nao deve ser usada para cartas numericas/acoes comuns.
- Alterar o `toString()` impacta logs e exibicao em UI.
- A ordem dos elementos não deve ser alterada sem revisar a lógica de `Card`.

## Exemplo de uso
```java
Color color = Color.RED;
System.out.println(color);
```

## Resumo
`Color` é uma enumeração pequena, mas fundamental para identificar e classificar as cartas do jogo.
