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
                System.out.println("Invalid input. Please try again.");
            }
        }

        return input;
    }

    @Override
    public void onTurnStart(Player player, Card topCard, ArrayList<Card> hand, ArrayList<Card> playableCards) {
        System.out.println("\nIt's " + player.getName() + "'s turn.");
        System.out.println("Top card: " + topCard);
        System.out.println("Your hand:");

        for (int i = 0; i < hand.size(); i++) {
            System.out.println("  " + i + ": " + hand.get(i));
        }

        if (playableCards.isEmpty()) {
            System.out.println("You have no playable cards.");
        } else {
            System.out.println("Playable cards: " + playableCards);
        }
    }

    @Override
    public void onCardPlayed(Player player, Card card) {
        if (player != this) {
            System.out.println(player.getName() + " played " + card + ".");
        }
    }

    @Override
    public void onCardDrawn(Player player, Card drawn) {
        if (player == this) {
            System.out.println("You drew: " + drawn);
        } else {
            System.out.println(player.getName() + " drew a card.");
        }
    }

    @Override
    public void onTurnEnd(Player player) {
        System.out.println("End of " + player.getName() + "'s turn.\n");
    }

    @Override
    public void onPlayOrDrawDecision(Player player) {
        System.out.println("Play a card or draw? (1. Play, 2. Draw)");
    }

    @Override
    public void onPlayDrawnCardDecision(Player player, Card drawn) {
        System.out.println("Do you want to play the drawn card (" + drawn + ")? (1. Yes, 2. No)");
    }

    @Override
    public void onStackDecision(Player player, int pendingPenalty) {
        System.out.println("Stack a card to pass the penalty of " + pendingPenalty + " cards, or draw them? (1. Stack, 2. Draw)");
    }

    @Override
    public void onChallengeDecision(Player player) {
        System.out.println("The top card is a Draw Four. Challenge it? (1. Yes, 2. No)");
    }

    @Override
    public void onColorChoiceDecision(Player player) {
        System.out.println("Choose a color. (1. RED, 2. GREEN, 3. BLUE, 4. YELLOW)");
    }

    @Override
    public void onSwapHandsDecision(Player player, int numPlayers) {
        System.out.println("SEVEN played! Swap hands with which player? (1-" + numPlayers + ")");
    }

    @Override
    public void onPlayWhenCannotDrawDecision(Player player) {
        System.out.println("You cannot draw. Do you want to play a card? (1. Yes, 2. No)");
    }

    @Override
    public void onIdenticalCardDecision(Player player, ArrayList<Card> identical) {
        System.out.println("You have identical cards that can also be played: " + identical);
        System.out.println("Play another? (1. Yes, 2. No)");
    }

    @Override
    public void onForcedDraw(Player player) {
        System.out.println("You have no playable cards and must draw.");
    }

    @Override
    public void onCannotPlayOrDraw(Player player) {
        System.out.println("You have no playable cards and cannot draw. Turn skipped.");
    }

    @Override
    public void onDrawnCardNotPlayable(Player player, Card drawn) {
        System.out.println("The drawn card (" + drawn + ") is not playable. Turn over.");
    }

    @Override
    public void onPassedTurn(Player player) {
        System.out.println("You chose not to play a card. Turn over.");
    }

    @Override
    public void onDeckEmpty() {
        System.out.println("The deck is empty and cannot be reshuffled.");
    }

    @Override
    public void onGameStart(Player[] players, boolean[] rules, Card firstCard) {
        System.out.println("Game started! First card: " + firstCard);
        System.out.print("Players: ");
        for (int i = 0; i < players.length; i++) {
            System.out.print(players[i].getName());
            if (i < players.length - 1) System.out.print(", ");
        }
        System.out.println();
    }

    @Override
    public void onGameOver(Player winner) {
        if (winner != null) {
            System.out.println("\n" + winner.getName() + " wins the game!");
        } else {
            System.out.println("\nThe game ended in a draw (turn limit reached).");
        }
    }
}
