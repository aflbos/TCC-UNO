# `TrainingConfig`

## Visao geral
`TrainingConfig` guarda parametros de treinamento (episodios, seeds, regras e etc.).

## Responsabilidades
- Armazenar configuracoes para `Training`.
- Oferecer valores padrao carregados de `.properties`.

## Estrutura interna
### Campos
- `startPort`: porta inicial para conexoes dos clientes de treino.
- `numClients`: quantidade de threads/clients.
- `phases`: lista imutavel de `TrainingPhase`.
- `loopStartPhase`: indice (1-based) da fase a partir da qual o loop reinicia.
- `loopInfinite`: se `true`, repete fases a partir de `loopStartPhase` indefinidamente.

## Carregamento
### `load(String path)`
1. Abre o arquivo (caminho absoluto, relativo, `Java/` ou classpath).
2. Le `startPort`, `numClients` e `phase.count`.
3. Cria `TrainingPhase` para cada fase.
4. Le `loop.startPhase` e `loop.infinite`.

#### Validacoes
- `phase.count >= 1`.
- `loop.startPhase` deve estar entre `1` e `phase.count + 1`.

### Formato esperado (exemplo)
```properties
startPort=5000
numClients=8
phase.count=2
phase.1.type=FIXED
phase.1.games=100
phase.1.aiPlayers=2
phase.1.randomPlayers=2
phase.2.type=EXHAUSTIVE
phase.2.iterations=3
loop.startPhase=2
loop.infinite=true
```

## Observacoes
- `phases` e imutavel para evitar alteracoes em runtime.
- A busca por arquivo tenta subir diretorios ate encontrar `Java/`.
