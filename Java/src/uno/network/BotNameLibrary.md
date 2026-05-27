# `BotNameLibrary`

## Visao geral
`BotNameLibrary` gera nomes unicos para bots (IA ou aleatorios) no lobby. Ele ajuda a evitar colisoes com nomes ja usados.

## Responsabilidades
- Manter listas de nomes base para bots.
- Gerar nomes com prefixo e evitar duplicatas.

## Metodos
### `generate(Random rng, Set<String> reserved, String prefix)`
- Escolhe um nome base usando o RNG.
- Aplica o prefixo informado.
- Garante que o nome final nao exista em `reserved`.
- Retorna um nome pronto para uso no lobby.

## Observacoes
- O metodo tenta varias combinacoes antes de retornar um fallback seguro.
- O conjunto `reserved` deve conter todos os nomes ja usados.

