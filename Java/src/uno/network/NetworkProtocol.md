# `NetworkProtocol`

## Visao geral
`NetworkProtocol` define strings e utilitarios do protocolo de rede usado entre cliente e servidor.

## Responsabilidades
- Centralizar tokens de mensagens (C_* e S_*).
- Padronizar mensagens e textos.
- Fornecer encode/decode de pacotes simples.

## Metodos
### `encode(String type, String... fields)`
Monta uma mensagem do protocolo com tipo e campos.

### `decode(String line)`
Quebra uma linha recebida em tipo e campos.

### `encodeInts(int[] arr)` / `decodeInts(String csv)`
Serializa e desserializa arrays inteiros.

## Observacoes
- A sanitizacao de campos evita caracteres reservados no protocolo.

