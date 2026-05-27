# `PlayerHuman`

## Visao geral
`PlayerHuman` representa um jogador local que interage pelo console. Ele implementa `GameEventListener` para exibir mensagens e solicitar entradas do usuario.

## Responsabilidades
- Ler entrada do usuario via `System.in`.
- Validar escolhas com base nas entradas validas da `Simulation`.
- Exibir eventos do jogo no console.

## Metodos
### `getInput()`
Fluxo real:
1. Le um inteiro do console.
2. Converte para indice interno (`input - 1`).
3. Valida contra `simulation.getValidInputs()`.
4. Repete ate ser valido.

#### Detalhes
- Qualquer erro de leitura retorna `-1` e dispara nova tentativa.
- A lista de validos e gerada pela `Simulation`.

### Eventos (`GameEventListener`)
- Escreve mensagens para o jogador local.
- Mostra a mao com indices **0-based** (como em `getValidInputs()`).
- Exibe prompts para decisoes (jogar/comprar, escolher cor, desafiar, etc.).
- Inicio/fim de turno.
- Cartas jogadas/compradas.
- Decisoes (jogar/compra, desafio, cor, etc.).
- Inicio e fim de jogo.

## Observacoes
- Um novo `Scanner` e criado a cada chamada de `getInput()`.
- A entrada esperada e numerica; texto ou valores fora da faixa geram nova tentativa.
- A validacao de entrada e feita por loop ate o usuario escolher uma opcao valida.
- O console e a unica interface de interacao deste jogador.
