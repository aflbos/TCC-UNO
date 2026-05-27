# `Training.py`

## Visao geral
`Training.py` executa o treinamento de IA com algoritmos do SB3/sb3_contrib, conectando varios ambientes UNO em paralelo.

## Responsabilidades
- Criar ambientes paralelos com `UnoEnv`.
- Configurar hiperparametros e callbacks.
- Selecionar algoritmo e validar compatibilidade de action space.
- Iniciar o TensorBoard e o loop de treinamento.

## Funcoes principais
### `make_env(...)`
Instancia `UnoEnv` com retries e timeouts configuraveis.

### `board(dir, port)`
Inicia o TensorBoard apontando para o diretorio de logs.

## Argumentos e variaveis
- `--algo`: algoritmo (ex.: `maskableppo`, `ppo`, `a2c`, `dqn`, `trpo`, `qrdqn`, ...).
- `--total-timesteps`: total de passos.
- `--device`: dispositivo (`cpu`, `cuda`, ...).
- `UNO_ALGO`, `UNO_TIMESTEPS`, `UNO_DEVICE`: equivalentes via variavel de ambiente.

## Fluxo principal
1. Prepara diretorios de modelos e logs com base em `Python/models/<ALGO>/uno/`.
2. Define quantidade de envs e parametros via variaveis de ambiente.
3. Resolve algoritmo e valida action space (discreto/continuo).
4. Carrega ou cria modelo e treina com checkpoints.

## Observacoes
- Os paths sao relativos ao diretorio `Python/` (pai de `src/`).
- Algoritmos continuos (SAC/TD3/DDPG/TQC/ARS/CrossQ) nao sao compativeis com o action space discreto do UNO.
- Hiperparametros podem ser ajustados diretamente no script.
