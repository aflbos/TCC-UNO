# `PlayerHerusitic`

## Visao geral
`PlayerHerusitic` e uma IA heuristica simples baseada nas flags de decisao expostas pela simulacao.

## Responsabilidades
- Ler o vetor de observacao da simulacao.
- Escolher a melhor acao disponivel de forma deterministica.
- Priorizar jogadas agressivas ou seguras conforme o contexto da decisao.

## Estrategia geral
- Nao desafiar `+4` por padrao.
- Empilhar penalidades quando possivel.
- Sempre jogar uma carta se o jogo pedir uma decisao binaria entre jogar e comprar.
- Escolher a carta mais forte disponivel quando o jogo pedir selecao de carta.
- Em troca de maos, escolher o oponente com menos cartas.
- Na escolha de cor, selecionar a cor que mais aparece na propria mao.

## Observacoes
- O nome da classe preserva a grafia original `Herusitic` do codigo.
- A heuristica e simples e foi pensada para servir como baseline, nao como estrategia otimizada.

