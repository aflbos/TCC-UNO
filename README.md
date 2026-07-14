# TCC-UNO

## Visão geral

Este repositório reúne a implementação de um sistema UNO distribuído com três camadas principais:

- **Java**: servidor, cliente e regras principais do jogo.
- **Python**: treinamento, inferência e integração com modelos de IA.
- **Scripts auxiliares**: inicializadores para Windows e Linux/macOS, além de um script de empacotamento.

A documentação detalhada de cada classe e script fica ao lado do código-fonte:

- `Java/src/**.md`
- `Python/src/**.md`

Os arquivos de documentação novos e antigos foram organizados para servir como referência rápida de uso e manutenção.

## Estrutura do projeto

- `Java/` — código-fonte e artefatos do lado do jogo/servidor.
- `Python/` — scripts de IA, treino e inferência.
- `UNO-Host.bat` / `UNO-Host.sh` — inicia o host do jogo.
- `UNO-Client.bat` / `UNO-Client.sh` — abre o cliente do jogador.
- `UNO-RunModel.bat` / `UNO-RunModel.sh` — executa um modelo treinado em inferência.
- `UNO-Training.bat` — placeholder/atalho legado para treino.
- `file.bat` — comando de empacotamento com PyInstaller para o runner Python.

## Como usar cada parte

### 1. Host do jogo

Use o host para criar a sala, configurar regras e iniciar partidas.

```bat
UNO-Host.bat
```

Ou, no Linux/macOS:

```bash
./UNO-Host.sh
```

### 2. Cliente do jogador

Use o cliente para conectar um jogador humano ao host.

```bat
UNO-Client.bat
```

Ou:

```bash
./UNO-Client.sh
```

### 3. Execução de modelo treinado

O runner Python conecta em um servidor UNO e executa inferência com um modelo salvo.

```bat
UNO-RunModel.bat
```

Ou:

```bash
./UNO-RunModel.sh
```

Se preferir executar diretamente o script:

```bash
python Python/src/RunModel.py --model-path caminho/do/modelo.zip --algo auto
```

### 4. Treinamento de IA

O treinamento é conduzido pelo script Python de treino.

```bash
python Python/src/Training.py --algo maskableppo
```

Os parâmetros de treino, logs e diretórios de saída ficam descritos em `Python/src/Training.md`.

### 5. TensorBoard

Para visualizar métricas e logs de treino:

```bash
python Python/src/TensorBoardLauncher.py
```

## Arquivos de apoio

### `file.bat`

Script de empacotamento do runner Python com PyInstaller. Ele cria uma versão executável do `RunModel.py`.

### `UNO-Training.bat`

Arquivo legado/placeholder. Se quiser automatizar o treinamento no Windows, use diretamente o script Python ou adapte este arquivo ao seu ambiente.

## Observações importantes

- Os scripts de host e cliente assumem a presença do JAR correspondente no diretório raiz do projeto.
- Os scripts Python dependem do ambiente virtual e das bibliotecas instaladas no diretório `Python/`.
- A documentação em Markdown ao lado do código é a fonte mais detalhada para entender classes, responsabilidades e fluxo de execução.

## Mapa rápido da documentação

### Java

- `Java/src/uno/network/ui/HostApp.md`
- `Java/src/uno/network/ui/ClientApp.md`
- `Java/src/uno/ai/training/Training.md`
- `Java/src/uno/ai/evaluation/Evaluation.md`
- Demais classes Java com documentação própria em `Java/src/**.md`

### Python

- `Python/src/RunModel.md`
- `Python/src/Training.md`
- `Python/src/TensorBoardLauncher.md`
- `Python/src/UnoEnviroment.md`

## Dica de uso

Se estiver em dúvida sobre a função de uma classe ou script, procure primeiro o arquivo `.md` correspondente na mesma pasta do código. Este README serve como entrada geral, enquanto os arquivos de documentação local detalham cada componente.

