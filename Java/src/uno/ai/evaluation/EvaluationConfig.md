# `EvaluationConfig`

## Visao geral
`EvaluationConfig` centraliza parametros para rodar avaliacoes de IA.

## Responsabilidades
- Armazenar quantidade de partidas, seeds e regras.
- Configurar tamanho do lobby e tipos de jogadores.

## Estrutura interna
### Campos
- `startPort`: porta inicial para conexoes do backend de IA.
- `numClients`: quantidade de clientes/threads de avaliacao.
- `suites`: lista imutavel de `EvaluationSuite`.

## Carregamento
### `load(String path)`
1. Le o arquivo `.properties`.
2. Carrega `startPort` e `numClients` com defaults.
3. Parseia grupos de seeds (`seeds.<grupo>`).
4. Cria suites via `EvaluationSuite.fromProperties(...)`.

#### Validacoes
- Deve existir ao menos um grupo de seeds.
- `suite.count >= 1`.
- Valores inteiros invalidos geram `IllegalArgumentException`.

### Formato esperado (exemplo)
```properties
startPort=6000
numClients=16
seeds.standard=1,2,3,4,5
suite.count=1
suite.1.name=Baseline
suite.1.type=FIXED
suite.1.seeds=standard
suite.1.rules=false,false,false,false,false,false,false,false
suite.1.aiPlayers=1
suite.1.randomPlayers=1
```

## Observacoes
- A lista `suites` e imutavel (encapsulada com `Collections.unmodifiableList`).
- Grupos de seeds sao mapeados por nome (parte apos `seeds.`).
