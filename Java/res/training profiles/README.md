# Profiles de treinamento

Esta pasta contém os arquivos `.properties` usados pelo sistema de treinamento em Java.

## Objetivo
Cada profile define um conjunto de fases de treino, portas, quantidade de clientes e regras do jogo para experimentos de aprendizado de IA.

## Estrutura dos arquivos
Os nomes seguem, em geral, este padrão:

- `training_official_4p_100.properties`
- `training_house_rules_4p_100.properties`
- `training_official_4p_loop.properties`

### Partes do nome
- `training` — profile de treinamento.
- `official` — usa regras oficiais do UNO.
- `house_rules` — usa regras alternativas/customizadas.
- `4p`, `6p`, `8p`, `10p` — quantidade de jogadores alvo.
- `100` — quantidade de jogos/rodadas esperadas no profile.
- `loop` — profile com repetição contínua a partir de uma fase definida.
- `v2` — variação do profile original.

## Campos mais comuns
- `startPort` — porta inicial dos clientes.
- `numClients` — número de workers/clients de treinamento.
- `phase.count` — total de fases no profile.
- `phase.N.type` — tipo da fase, por exemplo `FIXED`.
- `phase.N.games` — quantidade de partidas na fase.
- `phase.N.aiPlayers` — número de jogadores de IA.
- `phase.N.heuristicPlayers` — número de jogadores heurísticos.
- `phase.N.randomPlayers` — número de jogadores aleatórios.
- `phase.N.rules` — vetor com as regras ativas/inativas.
- `loop.infinite` — define se o treino reinicia indefinidamente.
- `loop.startPhase` — fase a partir da qual o loop passa a repetir.

## Como usar
- Escolha um arquivo `.properties` desta pasta como entrada do treinamento.
- Ajuste as quantidades de jogadores e fases conforme o experimento desejado.
- Use a documentação principal do Java para entender como `Training` consome estes profiles.

## Observações
- Os profiles são textos simples e podem ser editados manualmente.
- Sempre confira se `numClients` e `startPort` são compatíveis com o ambiente disponível.

