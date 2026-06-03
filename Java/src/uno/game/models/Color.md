# `Color`

## Visão Geral

`Color` é uma enumeração que define as cinco cores possíveis no sistema de cartas do UNO. Sua ordem de declaração é criticamente importante: o método `Card.getId()` usa `color.ordinal()` diretamente no cálculo do identificador numérico de cada carta.

**Pacote:** `uno.game.models`

---

## Valores Declarados

| Constante  | `ordinal()` | Bloco de ids em `Card` | Uso                                          |
|------------|-------------|------------------------|----------------------------------------------|
| `RED`      | 0           | 0 – 12                 | Cor normal                                   |
| `GREEN`    | 1           | 13 – 25                | Cor normal                                   |
| `BLUE`     | 2           | 26 – 38                | Cor normal                                   |
| `YELLOW`   | 3           | 39 – 51                | Cor normal                                   |
| `BLACK`    | 4           | 52 – 53 (fixo)         | Exclusivo para `WILD` e `DRAW_FOUR`          |

As quatro primeiras cores (`RED` a `YELLOW`, ordinals 0–3) são usadas como cores normais de jogo. `BLACK` (ordinal 4) é reservado para cartas especiais e nunca deve ser associado a cartas numéricas ou de ação.

---

## Método `toString()`

Retorna o nome da cor em português, usado em todas as saídas textuais via `Card.toString()` e logs do sistema:

| Constante  | `toString()` |
|------------|--------------|
| `RED`      | `VERMELHO`   |
| `GREEN`    | `VERDE`      |
| `BLUE`     | `AZUL`       |
| `YELLOW`   | `AMARELO`    |
| `BLACK`    | `PRETO`      |

O bloco `default` retorna `name()` (o nome Java da constante em inglês), garantindo que eventuais constantes futuras não adicionadas ao `switch` não quebrem silenciosamente.

---

## Papel no Sistema

### Em `Card`
`Color` é um dos dois atributos de `Card`. O `ordinal()` da cor é o multiplicador de bloco no cálculo de `getId()`:

```
id = color.ordinal() * 13 + value.ordinal()
```

Isso significa que cada cor ocupa um intervalo contíguo de 13 ids no vetor de observação da IA.

### Em `Simulation`
`Color.values()` é usado para:
- Gerar uma cor aleatória quando a primeira carta do baralho é `BLACK` (`initGame`).
- Converter o índice de escolha do jogador/IA de volta para um objeto `Color` após decisão de cor (`chooseColor`).
- Percorrer as quatro cores jogáveis (índices 0–3) para preencher a máscara de cor no `decisionMask`.

### Em `reshuffleDeck()`
Ao reciclar o descarte de volta para o baralho de compras, toda carta com `WILD` ou `DRAW_FOUR` tem sua cor resetada para `Color.BLACK`, removendo a cor escolhida anteriormente pelo jogador.

---

## Restrições e Cuidados

- **A ordem de declaração não pode ser alterada.** Alterar qualquer `ordinal()` quebra o mapeamento de ids em `Card`, invalida os vetores de observação e inutiliza modelos de IA já treinados.
- **Novas cores devem ser adicionadas antes de `BLACK`** se forem cores normais, ou após `BLACK` se forem categorias especiais — mas qualquer adição exige revisão completa do sistema de ids.
- **`BLACK` nunca deve aparecer em cartas numéricas ou de ação.** Sua presença em `isCardPlayable` é implicitamente tratada: `card.getColor() == Color.BLACK` torna qualquer carta preta sempre jogável, independentemente do topo do descarte.
