# `Card`

## Visão Geral

A classe `Card` representa uma carta individual do jogo UNO. Ela encapsula dois atributos fundamentais — `Color` e `Value` — e fornece conversão bidirecional entre a representação orientada a objetos e um identificador inteiro compacto (`id`). Esse identificador é a base de todo o sistema de indexação usado na observação e na máscara de decisão da IA.

**Pacote:** `uno.game.models`

---

## Atributos

| Atributo | Tipo    | Descrição                                                                  |
|----------|---------|----------------------------------------------------------------------------|
| `color`  | `Color` | Cor da carta. Cartas especiais (`WILD`, `DRAW_FOUR`) sempre têm `Color.BLACK`. |
| `value`  | `Value` | Valor ou efeito da carta.                                                  |

A consistência entre `color` e `value` é responsabilidade do código chamador. Nenhum método da classe valida internamente se a combinação é legal (ex.: `Color.RED` com `Value.WILD` é tecnicamente possível de construir, mas semanticamente incorreto).

---

## Construtores

### `Card(Color color, Value value)`
Cria uma carta especificando cor e valor diretamente.

```java
Card card = new Card(Color.RED, Value.FIVE);
```

### `Card(int id)`
Cria uma carta a partir de um identificador numérico. Delega integralmente a lógica para `setId(int)`. Usado na cópia de cartas (`new Card(card.getId())`) e na reconstrução a partir de mensagens de rede ou armazenamento.

```java
Card wildCard = new Card(52); // BLACK WILD
Card drawFour = new Card(53); // BLACK DRAW_FOUR
Card blueSkip = new Card(/*...*/); // Calculado por cor * 13 + valor
```

---

## Sistema de Identificação — `getId()` e `setId(int)`

O identificador inteiro é o elo entre o modelo de objetos Java e os vetores numéricos do agente de IA. Todo o `observationVector` e o `decisionMask` usam esses ids como índices.

### `getId() → int`

Converte a carta em um inteiro único no intervalo `[0, 53]`:

| Condição              | Id retornado                              |
|-----------------------|-------------------------------------------|
| `value == WILD`       | `52`                                      |
| `value == DRAW_FOUR`  | `53`                                      |
| Demais cartas         | `color.ordinal() * 13 + value.ordinal()`  |

**Mapeamento completo por cor** (para as 13 cartas por cor, índices `value.ordinal()` de `0` a `12`):

| `Color`  | `ordinal()` | Bloco de ids   |
|----------|-------------|----------------|
| `RED`    | 0           | 0 – 12         |
| `GREEN`  | 1           | 13 – 25        |
| `BLUE`   | 2           | 26 – 38        |
| `YELLOW` | 3           | 39 – 51        |
| `BLACK`  | 4           | 52 – 53 (fixo) |

Dentro de cada bloco, o id relativo segue a ordem de `Value.ordinal()`:

| `Value`    | `ordinal()` | Id relativo no bloco |
|------------|-------------|----------------------|
| `ZERO`     | 0           | +0                   |
| `ONE`      | 1           | +1                   |
| ...        | ...         | ...                  |
| `NINE`     | 9           | +9                   |
| `SKIP`     | 10          | +10                  |
| `REVERSE`  | 11          | +11                  |
| `DRAW_TWO` | 12          | +12                  |

> `WILD` (ordinal 13) e `DRAW_FOUR` (ordinal 14) nunca entram na fórmula; são tratados pelos casos especiais `52` e `53`.

---

### `setId(int id)`

Reconstrói `color` e `value` a partir do id seguindo a lógica inversa:

| Condição  | `color` resultante | `value` resultante |
|-----------|--------------------|--------------------|
| `id == 52`| `Color.BLACK`      | `Value.WILD`       |
| `id == 53`| `Color.BLACK`      | `Value.DRAW_FOUR`  |
| Demais    | `Color.values()[id / 13]` | `Value.values()[id % 13]` |

**Atenção:** Não há validação de faixa. Um `id` fora de `[0, 53]` causará `ArrayIndexOutOfBoundsException` em `Color.values()` ou `Value.values()`.

---

## Outros Métodos

### `getColor() / setColor(Color color)`
Getter e setter padrão para a cor. `setColor` é chamado externamente por `Simulation` quando o jogador escolhe a cor de um `WILD` ou `DRAW_FOUR`, sobrescrevendo temporariamente `Color.BLACK` com a cor escolhida.

### `getValue() / setValue(Value value)`
Getter e setter padrão para o valor.

### `toString() → String`
Retorna `"<cor> <valor>"` usando os `toString()` de `Color` e `Value`, que produzem texto em português:

```
VERMELHO 5
PRETO CURINGA
AZUL COMPRA 2
VERDE PULAR
```

---

## Impacto na IA

O `id` da carta é usado diretamente como índice em dois vetores críticos:

- **`observationVector[card.getId()]`** — registra se o jogador tem a carta na mão (índices 0–53) e qual carta está no topo do descarte (`card.getId() + 108`, índices 108–161).
- **`decisionMask[card.getId()]`** — marca ações jogáveis (índices 0–53).

Por isso, a ordem dos enums `Color` e `Value` **não pode ser alterada** sem revisar `generateObservationVector()`, `generateDecisionMask()` e o lado Python (`UnoEnv`).

---

## Restrições e Cuidados

- A consistência entre `color` e `value` deve ser mantida pelo chamador (ex.: `Color.BLACK` para `WILD`/`DRAW_FOUR`).
- `setId` não valida a faixa de entrada.
- Alterar a ordem de `Color` ou `Value` quebra o mapeamento de ids e invalida modelos treinados.
- `setColor` é chamado em objetos `Card` dentro do `discardPile` para registrar a cor escolhida; isso muta a carta persistida no baralho de descarte.
