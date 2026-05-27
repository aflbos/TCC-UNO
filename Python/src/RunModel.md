# `RunModel.py`

## Visao geral
`RunModel.py` executa inferencia com um modelo treinado de UNO. Ele cria varios ambientes paralelos, conecta ao servidor e usa um algoritmo escolhido para escolher acoes.

## Responsabilidades
- Ler argumentos de linha de comando.
- Validar parametros e caminho do modelo.
- Detectar automaticamente o algoritmo pelo conteudo do arquivo quando `--algo auto`.
- Criar ambientes paralelos com `UnoEnv`.
- Carregar o modelo e rodar o loop de inferencia.

## Funcoes principais
### `make_env(rank, start_port, host)`
Cria um ambiente `UnoEnv` para um dado indice e porta.

### `parse_args()`
Define parametros como caminho do modelo, algoritmo, quantidade de envs, host e portas.

### `validate_args(args)`
Valida argumentos, verificando limites e existencia do arquivo de modelo.

### `preflight_ports(...)`
Opcionalmente testa conectividade com `host:port` antes de iniciar os workers.

### `load_model(args, env)`
Carrega o algoritmo escolhido via `--algo` (SB3/sb3_contrib).

## Observacoes
- `--algo auto` usa apenas o conteudo do arquivo `.zip` para inferir o algoritmo.
- Se nao for possivel inferir, informe `--algo` explicitamente.
- Algoritmos recorrentes (`recurrentppo`) sao suportados com estado e `episode_start`.
- Algoritmos continuos (SAC/TD3/DDPG/TQC/ARS/CrossQ) nao sao compativeis com action space discreto do UNO.
- `--port-check` e `--skip-port-check` evitam falhas quando o servidor aceita so uma conexao por porta.
- O loop pode rodar indefinidamente quando `--max-steps 0`.
