# `Logger`

## Visao geral
`Logger` e a interface contratual usada pelos loggers do jogo.

## Responsabilidades
- Reutilizar todos os callbacks de `GameEventListener`.
- Servir como marcador semantico para classes voltadas a logging.

## Observacoes
- A interface nao adiciona novos metodos; ela apenas herda o contrato de eventos.
- Implementacoes concretas normalmente gravam logs em console, arquivo ou outro destino.

