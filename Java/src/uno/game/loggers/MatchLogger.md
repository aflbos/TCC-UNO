# `MatchLogger`

## Visao geral
`MatchLogger` grava eventos da partida em arquivo, usando formato estruturado.

## Responsabilidades
- Abrir o arquivo de log em modo append.
- Registrar eventos relevantes da partida.
- Fazer flush frequente para reduzir perda de dados.
- Fechar o arquivo ao final do jogo.

## Eventos registrados
- Inicio de jogo.
- Carta jogada.
- Carta comprada.
- Compra forcada.
- Situacao em que o jogador nao consegue jogar ou comprar.
- Passagem de turno.
- Fim da partida.
- Tick de debug.

## Observacoes
- O caminho do arquivo e definido no construtor.
- Se a abertura do arquivo falhar, a classe registra o erro no `stderr` e continua sem writer ativo.
- Ao receber `onGameOver`, o arquivo e fechado automaticamente.

