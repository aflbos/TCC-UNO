TCC-UNO - Visao geral

Este repositorio reune um sistema UNO distribuido com Java para o jogo/servidor e Python para treinamento e inferencia de IA.

Arquivos principais:
- Java/: codigo-fonte do host, cliente, regras e IA do lado Java.
- Python/: scripts de treinamento, inferencia e TensorBoard.
- UNO-Host.bat / UNO-Host.sh: inicia o host do jogo.
- UNO-Client.bat / UNO-Client.sh: inicia o cliente do jogador.
- UNO-RunModel.bat / UNO-RunModel.sh: executa inferencia com um modelo treinado.
- UNO-Training.bat: atalho legado/placeholder para treino.
- file.bat: empacota o runner Python com PyInstaller.

Como usar:
1. Host:
   - Windows: UNO-Host.bat
   - Linux/macOS: ./UNO-Host.sh
2. Cliente:
   - Windows: UNO-Client.bat
   - Linux/macOS: ./UNO-Client.sh
3. Inferencia:
   - Windows: UNO-RunModel.bat
   - Linux/macOS: ./UNO-RunModel.sh
   - Direto: python Python/src/RunModel.py --model-path caminho/do/modelo.zip --algo auto
4. Treinamento:
   - python Python/src/Training.py --algo maskableppo
5. TensorBoard:
   - python Python/src/TensorBoardLauncher.py

Documentacao:
- Java/src/**.md
- Python/src/**.md

Se precisar entender uma classe ou script especifico, procure o arquivo .md correspondente na mesma pasta do codigo.
