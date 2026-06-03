# `Training.py`

## Visão Geral

`Training.py` é o script principal de treinamento de IA para o jogo UNO. Ele orquestra a criação de múltiplos ambientes paralelos (`UnoEnv`), a configuração do modelo de RL, callbacks de progresso e salvamento, o servidor TensorBoard, e o loop de aprendizado via Stable Baselines 3 (SB3) ou `sb3_contrib`. Suporta criação de novos modelos ou retomada de treinamento a partir de um checkpoint existente.

---

## Dependências

| Biblioteca / Módulo    | Uso                                                                         |
|------------------------|-----------------------------------------------------------------------------|
| `stable_baselines3`    | Algoritmos PPO, A2C, DQN, DDPG, TD3, SAC; callbacks; Monitor; SubprocVecEnv |
| `sb3_contrib`          | MaskablePPO, QRDQN, TQC, TRPO, ARS, RecurrentPPO, CrossQ; ActionMasker      |
| `gymnasium`            | Verificação de tipo de `action_space`                                       |
| `torch`                | Verificação de disponibilidade de CUDA                                      |
| `tensorboard.program`  | Inicialização do servidor TensorBoard                                       |
| `UnoEnviroment.UnoEnv` | Ambiente Gymnasium que conecta ao servidor UNO Java                         |
| `numpy`                | Cálculo de métricas de recompensa nos callbacks                             |
| `pathlib.Path`         | Manipulação de caminhos de forma multiplataforma                            |
| `argparse`             | Parsing de argumentos de linha de comando                                   |
| `importlib`            | Carregamento dinâmico dos módulos de algoritmo                              |
| `inspect`              | Filtragem dinâmica de kwargs por assinatura do construtor                   |

---

## Catálogo de Algoritmos — `ALGO_SPECS`

Dicionário global que mapeia nomes de algoritmo (minúsculas) para seus metadados:

| Chave          | Módulo              | Classe         | Suporta Máscara | Discreto | Contínuo | Recorrente |
|----------------|---------------------|----------------|-----------------|----------|----------|------------|
| `a2c`          | `stable_baselines3` | `A2C`          | Não             | Sim      | Sim      | —          |
| `ppo`          | `stable_baselines3` | `PPO`          | Não             | Sim      | Sim      | —          |
| `dqn`          | `stable_baselines3` | `DQN`          | Não             | Sim      | Não      | —          |
| `ddpg`         | `stable_baselines3` | `DDPG`         | Não             | Não      | Sim      | —          |
| `td3`          | `stable_baselines3` | `TD3`          | Não             | Não      | Sim      | —          |
| `sac`          | `stable_baselines3` | `SAC`          | Não             | Não      | Sim      | —          |
| `maskableppo`  | `sb3_contrib`       | `MaskablePPO`  | **Sim**         | Sim      | Não      | —          |
| `qrdqn`        | `sb3_contrib`       | `QRDQN`        | Não             | Sim      | Não      | —          |
| `tqc`          | `sb3_contrib`       | `TQC`          | Não             | Não      | Sim      | —          |
| `trpo`         | `sb3_contrib`       | `TRPO`         | Não             | Sim      | Sim      | —          |
| `ars`          | `sb3_contrib`       | `ARS`          | Não             | Não      | Sim      | —          |
| `recurrentppo` | `sb3_contrib`       | `RecurrentPPO` | Não             | Sim      | Sim      | **Sim**    |
| `crossq`       | `sb3_contrib`       | `CrossQ`       | Não             | Não      | Sim      | —          |

> **Atenção:** Algoritmos marcados como apenas "Contínuo" (DDPG, TD3, SAC, TQC, ARS, CrossQ) são **incompatíveis** com o espaço de ações discreto do UNO e serão rejeitados em `validate_action_space`.

---

## Argumentos de Linha de Comando

Definidos em `parse_args()` via `argparse`:

| Argumento           | Tipo    | Padrão        | Descrição                                                                |
|---------------------|---------|---------------|--------------------------------------------------------------------------|
| `--algo`            | `str`   | `maskableppo` | Algoritmo a usar. Deve estar em `ALGO_SPECS`.                            |
| `--total-timesteps` | `int`   | `100_000_000` | Total de timesteps de treinamento.                                       |
| `--device`          | `str`   | `cpu`         | Dispositivo PyTorch: `cpu`, `cuda`, `cuda:0`, etc.                       |
| `--num-envs`        | `int`   | `16`          | Quantidade de ambientes paralelos (`SubprocVecEnv`).                     |
| `--host`            | `str`   | `localhost`   | Host do servidor Java UNO.                                               |
| `--start-port`      | `int`   | `5000`        | Porta base. Env `i` usa porta `start_port + i`.                          |
| `--connect-retries` | `int`   | `30`          | Tentativas de conexão por ambiente.                                      |
| `--connect-delay`   | `float` | `0.5`         | Delay em segundos entre tentativas.                                      |
| `--connect-timeout` | `float` | `2.0`         | Timeout do socket durante a conexão.                                     |
| `--console-log`     | flag    | `False`       | Habilita log de recompensas no console via `ConsoleRewardCallback`.      |
| `--console-every`   | `int`   | `1`           | Imprime métricas a cada N rollouts (requer `--console-log`).             |
| `--step-log`        | `int`   | `0`           | Se > 0, imprime progresso a cada N timesteps via `StepProgressCallback`. |
| `--env-debug`       | flag    | `False`       | Passa `debug=True` para cada `UnoEnv`, ativando logs de socket.          |

---

## Funções

### `make_env(rank, host, start_port, connect_retries, connect_delay, connect_timeout, env_debug) → callable`

Fábrica de ambientes. Retorna uma função `_init()` (sem argumentos) que, quando chamada, instancia e configura um ambiente completo para o índice `rank`.

**Pipeline de wrappers criado internamente:**
1. `UnoEnv(host, port=start_port+rank, ...)` — ambiente base com socket TCP.
2. `ActionMasker(env, lambda e: e.unwrapped.get_action_mask())` — expõe a máscara de ações para algoritmos como `MaskablePPO`.
3. `Monitor(env)` — registra estatísticas de episódio (recompensa, comprimento) no buffer `ep_info_buffer` do modelo, necessário para os callbacks de log.

O padrão de retornar uma closure sem argumentos é exigido pelo `SubprocVecEnv`.

---

### `board(dir, port) → TensorBoard`

Inicializa um servidor TensorBoard programaticamente apontando para o diretório de logs.

- `dir`: caminho para o diretório com dados do TensorBoard.
- `port`: porta HTTP (padrão `6006`).

Imprime a URL gerada no stdout. Retorna o objeto `TensorBoard` ativo.

---

### `resolve_algo(algo_name) → (dict, class)`

Resolve dinamicamente o algoritmo pelo nome usando `importlib`.

1. Busca `algo_name` em `ALGO_SPECS`.
2. Importa o módulo correspondente (ex.: `stable_baselines3` ou `sb3_contrib`).
3. Obtém a classe pelo atributo (ex.: `MaskablePPO`).

**Retorna:** tupla `(spec_dict, AlgoClass)`.

---

### `validate_action_space(env, algo_name, spec)`

Verifica se o espaço de ações do ambiente é compatível com o algoritmo escolhido.

- Se `env.action_space` for `Discrete` e o algoritmo não suportar discreto → `ValueError`.
- Se for contínuo e o algoritmo não suportar contínuo → `ValueError`.

Esta validação previne erros silenciosos ao tentar usar SAC/TD3 com ações discretas.

---

### `filter_kwargs(algo_cls, kwargs) → dict`

Usa `inspect.signature` para filtrar apenas os kwargs que o construtor do algoritmo aceita. Isso permite passar um conjunto amplo de hiperparâmetros (`algo_kwargs`) sem se preocupar se cada algoritmo suporta todos os parâmetros.

**Exemplo:** `n_epochs` só existe em PPO/MaskablePPO; para A2C ou DQN, seria silenciosamente ignorado.

---

### `algo_dir_name(algo_name) → str`

Normaliza o nome do algoritmo para o nome de diretório de saída:
- `"maskableppo"` → `"MaskablePPO"`
- Qualquer outro → `ALGO_NAME.upper()` (ex.: `"ppo"` → `"PPO"`)

---

### `build_policy_kwargs(algo_name) → dict`

Retorna `policy_kwargs` adequados para a arquitetura de rede neural conforme o algoritmo:

| Algoritmos                       | `net_arch`                                                   |
|----------------------------------|--------------------------------------------------------------|
| `dqn`, `qrdqn`                   | `[512, 512]` (rede única)                                    |
| `ddpg`, `td3`, `sac`, `tqc`      | `dict(pi=[512, 512], qf=[512, 512])` (ator/critic separados) |
| Todos os outros (PPO, A2C, etc.) | `dict(pi=[512, 512], vf=[512, 512])` (ator/valor separados)  |

---

## Callbacks

### `ConsoleRewardCallback(print_every=1)`

Callback de rollout que imprime métricas de recompensa no console ao final de cada rollout.

- Acessa `self.model.ep_info_buffer` para coletar as recompensas acumuladas dos episódios finalizados.
- Imprime `ep_rew_mean`, `ep_rew_median` e `n_episodes`.
- `print_every` controla a frequência (a cada N rollouts).
- Requer que `Monitor` esteja no pipeline de wrappers para popular `ep_info_buffer`.

**Exemplo de saída:**
```
[treino] ep_rew_mean=0.342 ep_rew_median=0.500 n_episodes=64
```

---

### `StepProgressCallback(print_every_steps=10000)`

Callback de step que imprime o contador de timesteps globais a intervalos regulares.

- `_next_print` é atualizado progressivamente (não reiniciado), garantindo que o intervalo seja respeitado mesmo em ambientes onde `_on_step` é chamado múltiplas vezes por "step lógico".
- Útil para monitorar o progresso quando `--console-log` não está ativo.

**Exemplo de saída:**
```
[treino] steps=10000
[treino] steps=20000
```

---

## Estrutura de Diretórios Gerada

O script calcula todos os caminhos relativos ao diretório pai de `Training.py` (`python_dir = Path(__file__).parents[1]`):

```
Python/
├── training logs/   ← board_dir (logs do TensorBoard)
└── models/
    └── MaskablePPO/          ← algo_dir (ex.: "MaskablePPO" para maskableppo)
        └── uno/
            └── generated models/
                └── YYYY-MM-DD_HH-MM-SS/    ← pasta timestampada por execução
                    ├── uno_ai_model_<timestamp>_checkpoint_NNNNN_steps.zip
                    └── uno_ai_model_<timestamp>.zip   ← modelo final
```

A pasta com timestamp (`now`) é criada a cada execução, garantindo que checkpoints e o modelo final nunca sobreescrevam execuções anteriores. Os logs do TensorBoard ficam centralizados em `Python/training logs/`, com cada execução identificada pelo `tb_log_name`.

---

## Hiperparâmetros Padrão do Modelo

Quando nenhum modelo existente é encontrado, o modelo é criado com os seguintes parâmetros (filtrados por `filter_kwargs` conforme o algoritmo):

| Parâmetro       | Valor       | Descrição                                             |
|-----------------|-------------|-------------------------------------------------------|
| `policy`        | `MlpPolicy` | Política de rede densa (MLP)                          |
| `n_steps`       | `8192`      | Steps por rollout por ambiente                        |
| `batch_size`    | `2048`      | Tamanho do minibatch para update                      |
| `learning_rate` | `3e-4`      | Taxa de aprendizado                                   |
| `ent_coef`      | `0.05`      | Coeficiente de entropia (incentiva exploração)        |
| `gamma`         | `0.995`     | Fator de desconto (valor alto = visão de longo prazo) |
| `n_epochs`      | `10`        | Épocas de otimização por rollout (PPO/MaskablePPO)    |
| `verbose`       | `1`         | Nível de verbosidade do SB3                           |

---

## Fluxo de Execução Principal

```
parse_args()
    │
    ├─ Detecta CUDA / Define device
    ├─ Calcula caminhos de modelos e logs
    ├─ Cria diretórios (models_dir, logs_dir, pasta timestampada)
    │
    ├─ SubprocVecEnv([make_env(i, ...) for i in range(num_cpu)])
    │       └─ Cada worker: UnoEnv → ActionMasker → Monitor
    │
    ├─ resolve_algo() → spec, AlgoClass
    ├─ validate_action_space()
    │
    ├─ Se existing_model_path existe:
    │       └─ AlgoClass.load(path, env, tensorboard_log, device)
    ├─ Senão:
    │       └─ AlgoClass(**filter_kwargs(AlgoClass, algo_kwargs))
    │
    ├─ CheckpointCallback(save_freq=10000, save_path=..., name_prefix=...)
    ├─ ConsoleRewardCallback (opcional, se --console-log)
    ├─ StepProgressCallback  (opcional, se --step-log > 0)
    ├─ CallbackList([...])
    │
    ├─ board(board_dir, port=6006)  → TensorBoard ativo
    │
    ├─ model.learn(total_timesteps, reset_num_timesteps=False, ...)
    │
    └─ model.save(models_dir / now / "uno_ai_model_<timestamp>")
```

---

## Retomada de Treinamento

O carregamento de modelo existente é controlado pela variável `existing_model_path`, definida com um caminho hardcoded no corpo do `__main__`. Para retomar um treinamento:

1. Edite `existing_model_path` para apontar para o `.zip` desejado.
2. O modelo será carregado com `AlgoClass.load(...)`, preservando os pesos e hiperparâmetros salvos.
3. `reset_num_timesteps=False` em `model.learn(...)` garante que o contador de timesteps continue de onde parou, mantendo a consistência dos logs no TensorBoard.

> **Atenção:** O caminho `existing_model_path` ainda contém um exemplo com extensão `.zip.xxx`, que nunca existirá como arquivo real. Para uso em produção, remova o sufixo `.xxx`.

---

## Observações e Cuidados

- **Portas:** Com `--num-envs 16` e `--start-port 5000`, o servidor Java deve aceitar conexões nas portas 5000–5015.
- **Checkpoints:** Salvos a cada `save_freq=10000` timesteps (total, não por ambiente). Com 16 ambientes, isso equivale a ~625 steps por ambiente entre checkpoints.
- **TensorBoard:** Iniciado antes de `model.learn()`. A URL é impressa no console. O processo permanece ativo até o `input("Press Enter...")` ao final.
- **`filter_kwargs`:** Permite reutilizar o mesmo bloco `algo_kwargs` para diferentes algoritmos sem erros de parâmetros desconhecidos.
- **Algoritmos contínuos:** Se tentados com o UNO (ação discreta), `validate_action_space` levanta `ValueError` antes de criar o modelo.
