# Profiles de avaliação

Esta pasta contém os arquivos `.properties` usados nas rotinas de avaliação em Java.

## Objetivo
Cada profile define o conjunto de seeds, a quantidade de clientes, o tipo de suíte e a composição de jogadores usada para medir o desempenho de uma IA.

## Estrutura dos arquivos
Os nomes seguem, em geral, este padrão:

- `evaluation_random_official_4p_100.properties`
- `evaluation_heuristic_house_rules_10p_100.properties`

### Partes do nome
- `evaluation` — profile de avaliação.
- `random` — avalia contra jogadores aleatórios.
- `heuristic` — avalia contra jogadores heurísticos.
- `official` — usa regras oficiais do UNO.
- `house_rules` — usa regras alternativas/customizadas.
- `4p`, `6p`, `8p`, `10p` — quantidade de jogadores alvo.
- `100` — quantidade de partidas/seeds esperadas no profile.

## Campos mais comuns
- `startPort` — porta inicial usada pelos workers.
- `numClients` — quantidade de clientes paralelos.
- `seeds.standard` — lista de seeds padrão usada pelos testes.
- `suite.count` — quantidade de suítes de avaliação.
- `suite.N.name` — nome da suíte.
- `suite.N.type` — tipo da suíte, por exemplo `FIXED`.
- `suite.N.seeds` — nome do conjunto de seeds a utilizar.
- `suite.N.aiPlayers` — número de jogadores de IA.
- `suite.N.randomPlayers` — número de jogadores aleatórios.
- `suite.N.rules` — vetor com as regras ativas/inativas.

## Como usar
- Escolha um arquivo `.properties` desta pasta como entrada do processo de avaliação.
- Ajuste a lista de seeds e a composição dos jogadores conforme o experimento.
- Consulte a documentação principal do Java para entender como `Evaluation` consome estes profiles.

## Observações
- Os profiles são textos simples e podem ser editados manualmente.
- O número de clientes deve ser compatível com a distribuição de seeds e workers.

