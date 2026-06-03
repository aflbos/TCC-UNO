# `RunModel.py`

## Visão Geral

`RunModel.py` é o script de inferência para modelos de UNO treinados. Ele carrega um modelo salvo (`.zip`), cria múltiplos ambientes paralelos conectados ao servidor Java, e executa o loop de predição/ação de forma contínua. Suporta detecção automática do algoritmo pelo conteúdo do arquivo, múltiplos algoritmos SB3/sb3_contrib, ações determinísticas ou estocásticas, e exibição opcional de outputs em tempo real.

---

## Dependências

| Biblioteca / Módulo                          | Uso                                                              |
|----------------------------------------------|------------------------------------------------------------------|
| `stable_baselines3`                          | A2C, PPO, DQN, DDPG, TD3, SAC; SubprocVecEnv; VecMonitor        |
| `sb3_contrib`                                | MaskablePPO, QRDQN, TQC, TRPO, ARS, RecurrentPPO, CrossQ; ActionMasker |
| `gymnasium.spaces`                           | Verificação do tipo de espaço de ações                          |
| `numpy`                                      | Manipulação de arrays de ações, recompensas e máscaras          |
| `zipfile`                                    | Leitura do conteúdo do `.zip` para inferência de algoritmo      |
| `pickle` / `cloudpickle`                     | Deserialização de dados do modelo em formato legado             |
| `json`                                       | Parsing de dados do modelo em formato moderno (SB3 >= 1.7)      |
| `socket`                                     | Verificação de conectividade preflight                          |
| `pathlib.Path`                               | Resolução e validação do caminho do modelo                      |
| `importlib`                                  | Carregamento dinâmico dos módulos de algoritmo                  |
| `argparse`                                   | Parsing de argumentos de linha de comando                       |
| `multiprocessing`                            | `freeze_support()` para suporte a executáveis empacotados       |
| `UnoEnviroment.UnoEnv`                       | Ambiente Gymnasium com socket TCP para o servidor UNO Java       |

`cloudpickle` é importado com `try/except` — se não estiver disponível, `pickle` padrão é usado como fallback.

---

## Catálogo de Algoritmos — `ALGO_SPECS`

Idêntico ao de `Training.py`. Mapeia nomes em minúscula para metadados de cada algoritmo:

| Chave          | Módulo              | Classe         | Máscara | Discreto | Contínuo | Recorrente |
|----------------|---------------------|----------------|---------|----------|----------|------------|
| `a2c`          | `stable_baselines3` | `A2C`          | Não     | Sim      | Sim      | —          |
| `ppo`          | `stable_baselines3` | `PPO`          | Não     | Sim      | Sim      | —          |
| `dqn`          | `stable_baselines3` | `DQN`          | Não     | Sim      | Não      | —          |
| `ddpg`         | `stable_baselines3` | `DDPG`         | Não     | Não      | Sim      | —          |
| `td3`          | `stable_baselines3` | `TD3`          | Não     | Não      | Sim      | —          |
| `sac`          | `stable_baselines3` | `SAC`          | Não     | Não      | Sim      | —          |
| `maskableppo`  | `sb3_contrib`       | `MaskablePPO`  | **Sim** | Sim      | Não      | —          |
| `qrdqn`        | `sb3_contrib`       | `QRDQN`        | Não     | Sim      | Não      | —          |
| `tqc`          | `sb3_contrib`       | `TQC`          | Não     | Não      | Sim      | —          |
| `trpo`         | `sb3_contrib`       | `TRPO`         | Não     | Sim      | Sim      | —          |
| `ars`          | `sb3_contrib`       | `ARS`          | Não     | Não      | Sim      | —          |
| `recurrentppo` | `sb3_contrib`       | `RecurrentPPO` | Não     | Sim      | Sim      | **Sim**    |
| `crossq`       | `sb3_contrib`       | `CrossQ`       | Não     | Não      | Sim      | —          |

---

## Argumentos de Linha de Comando

Definidos em `parse_args()`:

| Argumento           | Tipo    | Padrão       | Obrigatório | Descrição                                                                    |
|---------------------|---------|--------------|-------------|------------------------------------------------------------------------------|
| `--model-path`      | `str`   | —            | **Sim**     | Caminho para o arquivo `.zip` do modelo salvo.                               |
| `--algo`            | `str`   | `auto`       | Não         | Algoritmo. `auto` detecta pelo conteúdo do `.zip`. Aceita qualquer chave de `ALGO_SPECS`. |
| `--num-envs`        | `int`   | `16`         | Não         | Número de ambientes paralelos.                                               |
| `--host`            | `str`   | `localhost`  | Não         | Host do servidor Java UNO.                                                   |
| `--start-port`      | `int`   | `5000`       | Não         | Porta base. Env `i` usa `start_port + i`.                                    |
| `--connect-timeout` | `float` | `1.0`        | Não         | Timeout de socket para verificações preflight.                               |
| `--port-check`      | flag    | `False`      | Não         | Ativa verificação de conectividade preflight antes de criar workers.         |
| `--skip-port-check` | flag    | —            | Não         | Alias legado sem efeito (ignorado silenciosamente).                          |
| `--device`          | `str`   | `cpu`        | Não         | Dispositivo PyTorch para inferência.                                         |
| `--max-steps`       | `int`   | `0`          | Não         | Máximo de steps. `0` = loop infinito.                                        |
| `--show-outputs`    | flag    | `False`      | Não         | Imprime ações, recompensas e dones no console durante a inferência.          |
| `--output-every`    | `int`   | `1`          | Não         | Frequência de impressão (a cada N steps) quando `--show-outputs` está ativo. |
| `--stochastic`      | flag    | `False`      | Não         | Usa ações estocásticas. O padrão é determinístico.                           |

---

## Funções

### `make_env(rank, start_port, host) → callable`

Fábrica de ambientes para uso no `SubprocVecEnv`. Retorna uma closure `_init()` que instancia:

1. `UnoEnv(host=host, port=start_port+rank)` — ambiente base TCP.
2. `ActionMasker(env, lambda e: e.unwrapped.get_action_mask())` — expõe a máscara de ações.

**Nota:** Diferente de `Training.py`, `RunModel.py` não envolve com `Monitor`, pois não há coleta de métricas de treino durante a inferência.

---

### `parse_args() → Namespace`

Define e parseia todos os argumentos de CLI. Retorna um `argparse.Namespace` com os valores. `--skip-port-check` é registrado com `help=argparse.SUPPRESS` para não aparecer na ajuda, mantendo compatibilidade retroativa.

---

### `validate_args(args)`

Valida e normaliza os argumentos antes da execução principal:

1. **Normaliza `model_path`:** Remove espaços, aspas simples e duplas acidentais, e expande `~`. Verifica se o arquivo existe; levanta `FileNotFoundError` se não.
2. **Infere o algoritmo:** Se `--algo auto`, chama `infer_algo_from_contents()`. Se a inferência falhar, levanta `ValueError` solicitando `--algo` explícito.
3. Valida `num_envs > 0`, `start_port > 0`, `connect_timeout > 0`, `max_steps >= 0`, `output_every > 0`.

---

### `load_model_data_from_zip(model_path) → dict | None`

Abre o arquivo `.zip` do modelo e extrai os metadados internos. A lógica lida com dois formatos históricos do SB3:

**Localização do arquivo de dados:**
- Procura por entradas cujo nome termine em `/data`, seja exatamente `data`, ou termine em `data.pkl`.
- Usa o primeiro candidato encontrado.

**Formatos suportados:**
1. **JSON (SB3 >= 1.7):** Tenta `json.loads(raw.decode("utf-8"))`. Este é o formato padrão moderno.
2. **Pickle (versões antigas):** Se o JSON falhar (`UnicodeDecodeError` ou `JSONDecodeError`), tenta `cloudpickle.loads(raw)` ou `pickle.loads(raw)` como fallback.

Retorna `None` se nenhum arquivo de dados for encontrado no `.zip`.

---

### `infer_algo_from_contents(model_path) → str | None`

Usa `load_model_data_from_zip` para extrair `policy_class` dos metadados e inferir o algoritmo usado.

**Lógica de inferência por módulo:**

| Substring no módulo da policy             | Algoritmo inferido  |
|-------------------------------------------|---------------------|
| `sb3_contrib.common.maskable`             | `maskableppo`       |
| `sb3_contrib.ppo_mask`                    | `maskableppo`       |
| `sb3_contrib.common.recurrent`            | `recurrentppo`      |
| `stable_baselines3.dqn`                   | `dqn`               |
| `sb3_contrib.qrdqn`                       | `qrdqn`             |
| `stable_baselines3.ddpg`                  | `ddpg`              |
| `stable_baselines3.td3`                   | `td3`               |
| `stable_baselines3.sac`                   | `sac`               |
| `sb3_contrib.tqc`                         | `tqc`               |
| `sb3_contrib.trpo`                        | `trpo`              |
| `sb3_contrib.ars`                         | `ars`               |
| `sb3_contrib.crossq`                      | `crossq`            |
| `stable_baselines3.common` + `n_epochs`   | `ppo`               |
| `stable_baselines3.common` (sem n_epochs) | `a2c`               |

**Formato JSON vs. Pickle:** Se `policy_class` for string (formato JSON), usa diretamente como `module`. Se for objeto de classe (formato pickle), usa `getattr(policy_class, "__module__", "")`.

Retorna `None` se não for possível determinar o algoritmo.

---

### `preflight_ports(host, start_port, num_envs, timeout_seconds)`

Verifica a conectividade com cada porta antes de criar os workers. Tenta abrir uma conexão TCP temporária para `host:port` para cada `rank` de `0` a `num_envs-1`.

Coleta todas as falhas e, se houver alguma, levanta `ConnectionError` com a lista de portas inacessíveis e o primeiro erro encontrado.

> **Atenção:** Este check consome uma conexão por porta. Se o servidor UNO só aceitar **uma** conexão por porta (comportamento comum), o check esgotará a conexão antes do worker poder usá-la. Por isso está **desabilitado por padrão** e requer `--port-check` explícito para ativar.

---

### `validate_action_space(env, algo_name, spec)`

Idêntico ao de `Training.py`. Verifica compatibilidade entre o tipo de espaço de ações do ambiente e o algoritmo carregado. Levanta `ValueError` em caso de incompatibilidade.

---

### `resolve_algo(algo_name) → (dict, class)`

Mesmo comportamento de `Training.py`. Importa dinamicamente e retorna `(spec_dict, AlgoClass)`.

---

### `load_model(args, env) → model`

Wrapper simples que resolve o algoritmo e chama `AlgoClass.load(model_path, device=device, env=env)`. Passa o ambiente para que o modelo possa verificar/recriar o espaço de ações se necessário.

---

## Loop de Inferência

O loop principal é executado enquanto `max_steps == 0` (infinito) ou `steps < max_steps`:

### Predição por tipo de algoritmo

| Tipo             | Chamada de `predict`                                                               |
|------------------|------------------------------------------------------------------------------------|
| Com máscara      | `model.predict(obs, action_masks=masks, state=states, episode_start=episode_starts, deterministic=...)` |
| Recorrente       | `model.predict(obs, state=states, episode_start=episode_starts, deterministic=...)`|
| Padrão           | `model.predict(obs, deterministic=...)`                                            |

### Estado recorrente (`RecurrentPPO`)

Para algoritmos recorrentes, o estado LSTM entre steps é mantido em `states`. `episode_starts` é atualizado com `dones` após cada step para sinalizar ao modelo quando um episódio foi reiniciado.

### Atualização da máscara de ações

A cada step, quando o algoritmo usa máscaras (`use_masks=True`), o array `masks` é atualizado a partir do campo `action_mask` em `infos[i]` para cada ambiente `i`. Isso garante que o próximo `predict` use a máscara correta para o estado atual de cada ambiente.

### Saída de diagnóstico (`--show-outputs`)

Quando `--show-outputs` está ativo, imprime a cada `--output-every` steps:
```
[step 100] actions=[3, 12, 7, ...] rewards=[0.5, -0.1, 0.0, ...] dones=[False, False, True, ...]
```

---

## Fluxo de Execução

```
parse_args()
    │
validate_args(args)
    ├─ Normaliza model_path
    ├─ Infere algoritmo (se --algo auto)
    └─ Valida limites numéricos
    │
[--port-check] preflight_ports(...)    ← Opcional, desabilitado por padrão
    │
SubprocVecEnv([make_env(i, ...) for i in range(num_envs)])
VecMonitor(env)
    │
validate_action_space(env, algo, spec)
    │
env.reset()
    ├─ EOFError → RuntimeError com mensagem diagnóstica detalhada
    │
load_model(args, env)
    │
Inicializa masks / states / episode_starts conforme o tipo de algoritmo
    │
Loop:
    ├─ model.predict(obs, ...)     → actions
    ├─ env.step(actions)           → obs, rewards, dones, infos
    ├─ Atualiza states / episode_starts / masks
    └─ [--show-outputs] Imprime diagnóstico
    │
KeyboardInterrupt → env.close()
```

---

## Casos de Uso

### Inferência básica com detecção automática de algoritmo
```bash
python RunModel.py --model-path models/MaskablePPO/uno/generated\ models/2026-04-11.../uno_ai_model.zip
```

### Forçar algoritmo específico com saída de diagnóstico
```bash
python RunModel.py --model-path model.zip --algo maskableppo --show-outputs --output-every 10
```

### Rodar por número fixo de steps com ações estocásticas
```bash
python RunModel.py --model-path model.zip --max-steps 10000 --stochastic
```

### Servidor em host remoto com múltiplos ambientes
```bash
python RunModel.py --model-path model.zip --host 192.168.1.100 --start-port 5000 --num-envs 8
```

---

## Observações e Cuidados

- **`--port-check` vs. servidor de conexão única:** A maioria dos servidores UNO aceita apenas uma conexão por porta. Use `--port-check` somente se o servidor puder lidar com a conexão do preflight antes da do worker.
- **`--skip-port-check`:** Aceito por compatibilidade com scripts antigos, mas não tem efeito — a verificação já é desabilitada por padrão.
- **`--algo auto`:** Funciona com modelos SB3 >= 1.7 (formato JSON) e versões anteriores (pickle). Se a inferência falhar (modelo corrompido ou formato não reconhecido), especifique `--algo` manualmente.
- **Algoritmos recorrentes:** `RecurrentPPO` mantém estado LSTM entre steps. O loop lida corretamente com isso via `states` e `episode_starts`, diferente dos algoritmos padrão que são stateless por step.
- **`VecMonitor` vs. `Monitor`:** `RunModel` usa `VecMonitor` (wrapper de vetor), adequado para inferência com `SubprocVecEnv`. `Training.py` usa `Monitor` individualmente em cada sub-ambiente.
- **Interrupção:** `KeyboardInterrupt` é capturado para garantir `env.close()` e limpeza dos processos worker.
