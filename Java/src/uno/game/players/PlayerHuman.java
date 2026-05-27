package uno.game.players;

import uno.game.event.GameEventListener;
import uno.game.engine.Simulation;
import uno.game.models.Card;

import java.util.ArrayList;
import java.util.Scanner;

public class PlayerHuman extends Player implements GameEventListener {
    public PlayerHuman(String name, Simulation simulation) {
        super(name, simulation);
    }

    public int getInput() {
        Scanner scanner = new Scanner(System.in);
        int[] validInputs = simulation.getValidInputs();
        int input = -1;
        boolean validInput = false;

        while (!validInput) {
            try {
                input = scanner.nextInt();
                input -= 1;
            } catch (Exception e) {
                input = -1;
            }
            for (int i : validInputs) {
                if (input == i) {
                    validInput = true;
                    break;
                }
            }
            if (!validInput) {
                System.out.println("Entrada invalida. Tente novamente.");
            }
        }

        return input;
    }

    @Override
    public void onTurnStart(Player player, Card topCard, ArrayList<Card> hand, ArrayList<Card> playableCards) {
        System.out.println("\nVez de " + player.getName() + ".");
        System.out.println("Carta do topo: " + topCard);
        System.out.println("Sua mao:");

        for (int i = 0; i < hand.size(); i++) {
            System.out.println("  " + i + ": " + hand.get(i));
        }

        if (playableCards.isEmpty()) {
            System.out.println("Voce nao tem cartas jogaveis.");
        } else {
            System.out.println("Cartas jogaveis: " + playableCards);
        }
    }

    @Override
    public void onCardPlayed(Player player, Card card) {
        if (player != this) {
            System.out.println(player.getName() + " jogou " + card + ".");
        }
    }

    @Override
    public void onCardDrawn(Player player, Card drawn) {
        if (player == this) {
            System.out.println("Voce comprou: " + drawn);
        } else {
            System.out.println(player.getName() + " comprou uma carta.");
        }
    }

    @Override
    public void onTurnEnd(Player player) {
        System.out.println("Fim da vez de " + player.getName() + ".\n");
    }

    @Override
    public void onPlayOrDrawDecision(Player player) {
        System.out.println("Jogar uma carta ou comprar? (1. Jogar, 2. Comprar)");
    }

    @Override
    public void onPlayDrawnCardDecision(Player player, Card drawn) {
        System.out.println("Voce quer jogar a carta comprada (" + drawn + ")? (1. Sim, 2. Nao)");
    }

    @Override
    public void onStackDecision(Player player, int pendingPenalty) {
        System.out.println("Empilhar para passar a penalidade de " + pendingPenalty + " cartas ou comprar? (1. Empilhar, 2. Comprar)");
    }

    @Override
    public void onChallengeDecision(Player player) {
        System.out.println("A carta do topo e um COMPRA QUATRO. Desafiar? (1. Sim, 2. Nao)");
    }

    @Override
    public void onColorChoiceDecision(Player player) {
        System.out.println("Escolha uma cor. (1. VERMELHO, 2. VERDE, 3. AZUL, 4. AMARELO)");
    }

    @Override
    public void onSwapHandsDecision(Player player, int numPlayers) {
        System.out.println("SETE jogado! Trocar maos com qual jogador? (1-" + numPlayers + ")");
    }

    @Override
    public void onPlayWhenCannotDrawDecision(Player player) {
        System.out.println("Voce nao pode comprar. Deseja jogar uma carta? (1. Sim, 2. Nao)");
    }

    @Override
    public void onIdenticalCardDecision(Player player, ArrayList<Card> identical) {
        System.out.println("Voce tem cartas identicas que tambem podem ser jogadas: " + identical);
        System.out.println("Jogar outra? (1. Sim, 2. Nao)");
    }

    @Override
    public void onForcedDraw(Player player) {
        System.out.println("Voce nao tem cartas jogaveis e deve comprar.");
    }

    @Override
    public void onCannotPlayOrDraw(Player player) {
        System.out.println("Voce nao tem cartas jogaveis e nao pode comprar. Vez pulada.");
    }

    @Override
    public void onDrawnCardNotPlayable(Player player, Card drawn) {
        System.out.println("A carta comprada (" + drawn + ") nao pode ser jogada. Vez encerrada.");
    }

    @Override
    public void onPassedTurn(Player player) {
        System.out.println("Voce escolheu nao jogar uma carta. Vez encerrada.");
    }

    @Override
    public void onDeckEmpty() {
        System.out.println("O baralho acabou e nao pode ser reembaralhado.");
    }

    @Override
    public void onGameStart(Player[] players, boolean[] rules, Card firstCard) {
        System.out.println("Partida iniciada! Primeira carta: " + firstCard);
        System.out.print("Jogadores: ");
        for (int i = 0; i < players.length; i++) {
            System.out.print(players[i].getName());
            if (i < players.length - 1) System.out.print(", ");
        }
        System.out.println();
    }

    @Override
    public void onGameOver(Player winner) {
        if (winner != null) {
            System.out.println("\n" + winner.getName() + " venceu a partida!");
        } else {
            System.out.println("\nA partida terminou em empate (limite de turnos atingido).");
        }
    }
}
