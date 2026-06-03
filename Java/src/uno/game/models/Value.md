# `Value`

## Visão Geral

`Value` é uma enumeração que define todos os valores possíveis de cartas no UNO: números (0–9), cartas de ação (`SKIP`, `REVERSE`, `DRAW_TWO`) e cartas especiais (`WILD`, `DRAW_FOUR`). A ordem de declaração é fundamental para o mapeamento de ids usado em `Card`, para a composição do baralho em `Deck`, e para a indexação dos vetores de IA em `Simulation`.

**Pacote:** `uno.game.models`

---

## Valores Declarados

### Cartas Numéricas (ordinals 0–9)

| Constante | `ordinal()` | `toString()` |
|-----------|-------------|--------------|
| `ZERO`    | 0           | `0`          |
| `ONE`     | 1           | `1`          |
| `TWO`     | 2           | `2`          |
| `THREE`   | 3           | `3`          |
| `FOUR`    | 4           | `4`          |
| `FIVE`    | 5           | `5`          |
| `SIX`     | 6           | `6`          |
| `SEVEN`   | 7           | `7`          |
| `EIGHT`   | 8           | `8`          |
| `NINE`    | 9           | `9`          |

### Cartas de Ação (ordinals 10–12)

| Constante   | `ordinal()` | `toString()` | Efeito em `playCard()`                              |
|-------------|-------------|--------------|-----------------------------------------------------|
| `SKIP`      | 10          | `PULAR`      | Avança o ponteiro de jogador mais uma vez (`nextPlayer()`), pulando o próximo. |
| `REVERSE`   | 11          | `INVERTER`   | Inverte a direção do turno (`invertDirection()`).   |
| `DRAW_TWO`  | 12          | `COMPRA 2`   | Adiciona 2 ao `stackingAmount`, forçando o próximo jogador a comprar (ou empilhar). |

### Cartas Especiais (ordinals 13–14)

| Constante    | `ordinal()` | `toString()` | Id fixo | Efeito em `playCard()`                                                              |
|--------------|-------------|--------------|---------|--------------------------------------------------------------------------------------|
| `WILD`       | 13          | `CURINGA`    | `52`    | Ativa escolha de cor pelo jogador (`chooseColor()`).                                |
| `DRAW_FOUR`  | 14          | `COMPRA 4`   | `53`    | Ativa escolha de cor e adiciona 4 ao `stackingAmount`. Detecta e registra blefe. |

---

## Método `toString()`

Retorna o rótulo em português de cada valor. Usado em todas as saídas de `Card.toString()` e logs do sistema. O bloco `default` retorna `name()` para constantes futuras não mapeadas.

---

## Papel no Sistema

### Em `Card.getId()` e `Card.setId(int)`

Os ordinals de 0 a 12 são usados diretamente como o componente `value.ordinal()` na fórmula:

```
id = color.ordinal() * 13 + value.ordinal()
```

`WILD` (ordinal 13) e `DRAW_FOUR` (ordinal 14) **não entram nessa fórmula** — seus ids são fixos em `52` e `53`, tratados como casos especiais em `getId()` e `setId()`.

### Em `Deck()` — Composição do Baralho

A composição do baralho em `Deck` depende diretamente dos ordinals:

**Cartas numéricas** (loop `j` de 1 a 19, incrementando por metade via `values[j/2]`):

| `j`        | `j/2` | `Value.values()[j/2]` | Ocorrências por cor |
|------------|-------|----------------------|---------------------|
| 1          | 0     | `ZERO`               | 1×                  |
| 2–3        | 1     | `ONE`                | 2×                  |
| 4–5        | 2     | `TWO`                | 2×                  |
| ...        | ...   | ...                  | ...                 |
| 18–19      | 9     | `NINE`               | 2×                  |

**Cartas de ação** (loop `j` de 10 a 12, 2 cópias por cor):
- `values[10]` = `SKIP`, `values[11]` = `REVERSE`, `values[12]` = `DRAW_TWO`

Isso confirma que a montagem correta do baralho depende inteiramente de `ZERO`–`NINE` estar nos ordinals 0–9 e `SKIP`–`DRAW_TWO` nos ordinals 10–12.

### Em `Simulation.isCardPlayable(Card)`

`SKIP`, `REVERSE`, `DRAW_TWO` e `DRAW_FOUR` têm comportamento especial durante empilhamento:

- Quando `stackingAmount > 0` e o topo é `DRAW_TWO`:
  - Com `RULE_STACKING` ou `RULE_SKIP_N_FLIP`: `DRAW_TWO`, `DRAW_FOUR`, `SKIP`, `REVERSE` são jogáveis.
  - Sem essas regras: apenas `DRAW_TWO` e `DRAW_FOUR`.
- Quando o topo é `DRAW_FOUR`:
  - Com `RULE_SKIP_N_FLIP`: `DRAW_TWO`, `DRAW_FOUR`, `SKIP`, `REVERSE`.
  - Sem: apenas `DRAW_FOUR`.

### Em `Simulation.playCard(Card)`

Cada `Value` dispara efeitos distintos no `switch`:

| `Value`     | Efeito                                                                            |
|-------------|-----------------------------------------------------------------------------------|
| `REVERSE`   | Inverte direção do turno                                                          |
| `SKIP`      | Chama `nextPlayer()` — o próximo jogador é pulado                                 |
| `WILD`      | Abre decisão de cor; seta `card.setColor(chosenColor)`                            |
| `DRAW_TWO`  | `stackingAmount += 2`                                                             |
| `DRAW_FOUR` | Detecta blefe, abre decisão de cor, `stackingAmount += 4`                         |
| `ZERO`      | Com `RULE_SEVEN_ZERO`: rotação de mãos entre todos os jogadores em ordem de turno |
| `SEVEN`     | Com `RULE_SEVEN_ZERO`: escolha de jogador para trocar mãos                        |

### Em `Simulation.initGame()`

A primeira carta virada do baralho pode acionar efeitos imediatos antes do primeiro turno:

| `Value` inicial | Efeito                              |
|-----------------|-------------------------------------|
| `REVERSE`       | Inverte a direção inicial           |
| `SKIP`          | Pula o primeiro jogador             |
| `DRAW_TWO`      | `stackingAmount += 2`               |
| `DRAW_FOUR`     | `stackingAmount += 4`               |

---

## Restrições e Cuidados

- **A ordem de declaração é imutável em produção.** Qualquer alteração nos ordinals 0–14 invalida `Card.getId()`, a composição do `Deck`, os vetores de observação e todos os modelos de IA treinados.
- **Adicionar novos valores** exige revisão completa de `Card`, `Deck`, `Simulation` e do lado Python (`UnoEnv`, espaços de ação/observação).
- O bloco `default: return name()` em `toString()` previne falha silenciosa para valores não mapeados, mas não substitui a necessidade de atualizar o `switch`.
