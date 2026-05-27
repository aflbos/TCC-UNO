# `HostApp`

## Visao geral
`HostApp` e a interface de console para hospedar partidas. Ela gerencia o servidor, o lobby e as regras.

## Responsabilidades
- Iniciar e parar o `GameServer`.
- Gerenciar bots (IA/aleatorio) no lobby.
- Configurar regras e iniciar partidas.
- Exibir status do servidor e do lobby.
- Aplicar presets de regras.

## Fluxo principal
1. Menu principal (criar host).
2. Menu do servidor (comandos de lobby e partida).
3. Inicio da partida com regras atuais.

## Comandos principais
- `host [port]`: inicia o servidor.
- `status`: mostra status do servidor/partida.
- `list`: lista slots do lobby.
- `addai` / `addrandom`: adiciona bots.
- `rule` / `rules`: configura regras.
- `preset <nome>`: aplica um preset de regras.
- `presets`: lista presets disponiveis.
- `start`: inicia a partida.
- `stopmatch`: encerra a partida atual.

## Observacoes
- Usa callbacks do `GameServer` para atualizar o console.
- Requer backend de IA conectado quando ha bots de IA no lobby.
- O preset `official` aplica as regras padrao do UNO.
