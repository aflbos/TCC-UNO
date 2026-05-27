# `Evaluation`

## Visao geral
`Evaluation` executa uma avaliacao unica de IA, configurando regras, jogadores e parametros de execucao.

## Responsabilidades
- Montar uma simulacao para avaliacao.
- Executar partidas e coletar resultados.
- Reportar metricas basicas (vitorias, empates, etc.).

## Observacoes
- Usa `EvaluationConfig` para configuracao.
- Pode ser orquestrado por `EvaluationSuite`.

## Estrutura interna
### Constantes
- `DEFAULT_CONFIG`: caminho padrao do arquivo `.properties` usado quando nao ha argumento.

### Campos internos relevantes
- `numClientsGlobal`: usado para dividir seeds entre workers (threads).

## Fluxo de execucao
1. Carrega `EvaluationConfig` (arquivo `.properties`).
2. Cria N `ConnectionAI` (um por cliente) e inicia N threads.
3. Cada thread conecta na porta (`startPort + i`) e roda suas suites.
4. Ao final, entra em modo "keep-alive" rodando partidas dummy.

## Metodos
### `main(String[] args)`
- Define o arquivo de configuracao (argumento ou `DEFAULT_CONFIG`).
- Carrega a configuracao e inicializa as threads.
- Aguarda todas as threads e imprime mensagem final.

### `runWorker(int port, String id, ConnectionAI ConnectionAI, List<EvaluationSuite> suites)`
- Conecta ao backend (`ConnectionAI.connect(port)`).
- Executa todas as suites atribuídas ao worker.
- Mantem conexao ativa com `runKeepAlive()`.

### `runSuite(EvaluationSuite suite, String id, ConnectionAI ConnectionAI)`
- Divide seeds entre os workers via `divideSeeds(...)`.
- Executa `runExhaustiveSuite` ou `runFixedSuite` conforme o tipo.

### `runExhaustiveSuite(...)`
- Itera por combinacoes de quantidade total de jogadores (2 a 10).
- Para cada total, avalia todas as distribuicoes de AI vs Random.

### `runFixedSuite(...)`
- Executa apenas a configuracao fixa de `aiPlayers` e `randomPlayers`.

### `runKeepAlive(...)`
- Executa partidas dummy com 1 AI e 1 Random para manter a conexao.
- Termina quando ocorre uma excecao (backend caiu).

### `runGame(...)`
- Cria `Simulation` com `aiPlayers` e `randomPlayers`.
- Roda ate `isGameOver()` e retorna `GameResult`.

### `divideSeeds(int[] seeds, String id)`
Divide o array de seeds proporcionalmente ao indice do worker (`Eval 1`, `Eval 2`, ...).

## Observacoes
- `runGame(...)` considera vitoria AI se `winner` for instancia de `PlayerAI`.
- O modo keep-alive impede que a conexao feche entre batches longos.
- O numero de workers e controlado por `numClientsGlobal`.
