# `Training`

## Visao geral
`Training` executa o loop principal de treinamento de IA, conectando simulacao e backend.

## Responsabilidades
- Rodar episodios de jogo para treino.
- Enviar observacoes/recompensas ao backend.
- Controlar progresso e logs.

## Estrutura interna
### Constantes
- `DEFAULT_CONFIG`: caminho padrao de configuracao (`.properties`).

## Fluxo de execucao
1. Carrega `TrainingConfig`.
2. Inicia `numClients` threads, cada uma conectando em `startPort + i`.
3. Cada thread executa as fases definidas em `TrainingConfig`.
4. Opcionalmente entra em loop infinito a partir de `loopStartPhase`.

## Metodos
### `main(String[] args)`
- Define o caminho de configuracao.
- Carrega config e cria as threads de treinamento.

### `runRoutine(int port, String id, TrainingConfig config)`
- Conecta ao backend (`ConnectionAI.connect`).
- Executa fases iniciais ate `loopStartPhase - 1`.
- Executa o restante em loop infinito ou uma unica vez.

### `runPhase(TrainingPhase phase, String id, ConnectionAI ConnectionAI)`
Despacha para o metodo correto com base em `phase.type`.

### `runFixedPhase(...)`
Executa `phase.games` partidas com `aiPlayers` e `randomPlayers` fixos.

### `runRandomAIPhase(...)`
Escolhe `aiPlayers` aleatoriamente entre `aiPlayersMin` e `aiPlayersMax`.

### `runMixedPhase(...)`
Escolhe `aiPlayers` aleatoriamente e completa o total com random players.

### `runExhaustivePhase(...)`
Percorre todas as combinacoes de AI vs Random para 2 a 10 jogadores, repetindo `iterations` vezes.

### `runGame(...)`
Cria uma `Simulation` e roda ate `isGameOver()`.

### `displayRules(...)`
Imprime os `RuleSpec` de cada regra na fase.

### `printCycleComplete(...)`
Loga o fim de uma fase com o tipo atual.

## Observacoes
- `seed` das partidas e sempre `-1` (nao deterministico) no treinamento.
- `displayRules(...)` imprime o `RuleSpec` (pode ser FIXED/PROBABILITY/etc.).
