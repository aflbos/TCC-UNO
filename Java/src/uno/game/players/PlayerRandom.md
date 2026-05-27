# `PlayerRandom`

## Visao geral
`PlayerRandom` e um jogador automatico que escolhe uma entrada valida de forma aleatoria.

## Responsabilidades
- Selecionar um movimento aleatorio dentre as opcoes validas.

## Estrutura interna
### Atributos
- `random`: gerador inicializado com `seed` da simulacao (se disponivel) ou aleatorio.

#### Detalhe do seed
Quando `seed` existe, o gerador usa `seed ^ name.hashCode()` para variar entre jogadores.

## Metodos
### `getInput()`
- Consulta `simulation.getValidInputs()`.
- Escolhe um indice aleatorio dentro do array e retorna a entrada correspondente.

#### Observacao
Se `validInputs` estiver vazio, ocorrera excecao de `Random.nextInt(0)`.

## Observacoes
- Usa `seed` da simulacao para reproducibilidade quando possivel.
