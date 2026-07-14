# `GameEventListener`

## Visao geral
`GameEventListener` e a interface base de eventos do motor de jogo UNO.

## Responsabilidades
- Expor callbacks para fases da partida.
- Notificar decisoes do jogador.
- Permitir implementacoes parciais via metodos `default` vazios.

## Principais callbacks
- Inicio e fim de turno.
- Carta jogada, carta comprada e turno encerrado.
- Decisoes como compra, desafio, cor, empilhar, troca de maos e jogada identica.
- Inicio e fim da partida.
- Debug tick para inspecao de execucao.

## Observacoes
- Como todos os metodos sao `default`, implementacoes concretas podem sobrescrever apenas o que for necessario.
- A interface serve como contrato geral para loggers, UI e componentes de acompanhamento do jogo.

