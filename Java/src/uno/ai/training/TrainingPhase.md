# `TrainingPhase`

## Visao geral
`TrainingPhase` representa uma fase do treinamento, com regras e parametros especificos.

## Responsabilidades
- Descrever uma etapa (duracao, regras, tipos de jogadores).
- Permitir encadeamento de fases.

## Estrutura interna
### Campos
- `type`: tipo da fase (`FIXED`, `RANDOM_AI`, `MIXED`, `EXHAUSTIVE`).
- `ruleSpecs`: regras com possivel aleatoriedade (ver `RuleSpec`).
- `games`: numero de partidas em fases FIXED/RANDOM_AI/MIXED.
- `aiPlayers` / `randomPlayers`: usados em `FIXED`.
- `aiPlayersMin` / `aiPlayersMax`: faixa usada em `RANDOM_AI` e `MIXED`.
- `iterations`: repeticoes completas em `EXHAUSTIVE`.

## Tipos de fase
### `FIXED`
Usa `aiPlayers` e `randomPlayers` fixos por `games` partidas.

### `RANDOM_AI`
Sorteia `aiPlayers` entre `aiPlayersMin` e `aiPlayersMax`, sem jogadores random.

### `MIXED`
Sorteia `aiPlayers` e completa com `randomPlayers` aleatorios (1..max possivel).

### `EXHAUSTIVE`
Itera por todas as combinacoes de AI vs Random para 2..10 jogadores, repetindo `iterations` vezes.

## Metodos
### `fromProperties(Properties props, int index)`
- Le o prefixo `phase.<index>.`.
- Valida `type`.
- Parseia `rules` via `RuleSpec` (8 valores).
- Le `games`, `aiPlayers`, `randomPlayers`, `aiPlayersMin`, `aiPlayersMax`, `iterations`.

### `resolveRules(Random rng)`
Converte `RuleSpec[]` em boolean[] usando aleatoriedade quando necessario.

## Observacoes
- `rules` deve ter exatamente 8 entradas.
- Valores invalidos disparam `IllegalArgumentException` durante o parsing.
- Usado por `Training` para alternar configuracoes ao longo do treino.
