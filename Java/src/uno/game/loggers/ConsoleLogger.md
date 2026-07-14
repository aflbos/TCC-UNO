# `ConsoleLogger`

## Visao geral
`ConsoleLogger` escreve eventos da partida diretamente no console em formato estruturado.

## Responsabilidades
- Registrar o inicio da partida.
- Registrar o fim da partida com vencedor ou empate.
- Registrar ticks de debug.

## Comportamento
- Usa `StructuredLog.line(...)` para padronizar a saida.
- Exibe os nomes dos jogadores no inicio da partida.
- Em caso de empate, registra a saida com `winner=DRAW reason=turn_limit`.

## Observacoes
- A classe nao mantem estado proprio.
- E util para depuracao rapida durante testes e execucao local.

