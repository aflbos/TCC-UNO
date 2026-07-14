# `StructuredLog`

## Visao geral
`StructuredLog` e uma classe utilitaria interna para formatar linhas de log estruturadas.

## Responsabilidades
- Montar registros no formato `ts=... level=... event=... msg="..."`.
- Aplicar valores padrao quando nivel, evento ou mensagem nao forem informados.
- Escapar caracteres especiais para manter a linha de log legivel e segura.

## Metodos
### `line(String level, String event, String message)`
Cria uma linha estruturada com timestamp atual, nivel, evento e mensagem.

### `escape(String value)`
Escapa barras invertidas e aspas duplas na mensagem.

## Observacoes
- A classe nao pode ser instanciada.
- E usada por `ConsoleLogger` e `MatchLogger` para manter um formato consistente de auditoria.

