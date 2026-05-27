# `EvaluationSuite`

## Visao geral
`EvaluationSuite` coordena varias avaliacoes com configuracoes diferentes.

## Responsabilidades
- Carregar multiplas configuracoes de avaliacao.
- Executar lote de partidas.
- Consolidar resultados.

## Estrutura interna
### Campos
- `name`: nome exibido nos logs.
- `type`: `EXHAUSTIVE` ou `FIXED`.
- `seeds`: conjunto de seeds atribuido a esta suite.
- `ruleSpecs`: regras com possivel aleatoriedade (ver `RuleSpec`).
- `aiPlayers` / `randomPlayers`: usados apenas em suites FIXED.

## Tipos de suite
### `EXHAUSTIVE`
- Ignora `aiPlayers`/`randomPlayers`.
- Avalia todas as combinacoes de AI vs Random para totais de 2 a 10 jogadores.

### `FIXED`
- Usa `aiPlayers` e `randomPlayers` diretamente.
- Exige `aiPlayers + randomPlayers >= 2`.

## Metodos
### `fromProperties(Properties props, int index, Map<String,int[]> seedGroups)`
- Le o prefixo `suite.<index>.`.
- Valida o tipo (`EXHAUSTIVE` ou `FIXED`).
- Resolve o grupo de seeds por nome.
- Parseia `rules` via `RuleSpec.parseArray(...)`.
- Valida contagem de jogadores em modo FIXED.

### `resolveRules(Random rng)`
Converte `RuleSpec[]` em boolean[] usando aleatoriedade quando necessario.

## Observacoes
- `rules` aceita 8 entradas (stacking, skipNFlip, sevenZero, forcePlayDrawn, drawAndPlay, drawToMatch, bluffing, playIdentical).
- Em `EXHAUSTIVE`, `aiPlayers` e `randomPlayers` sao ignorados.
