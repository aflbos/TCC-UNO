package uno.game.event;

import uno.game.models.Card;
import uno.game.players.Player;

import java.util.ArrayList;

public class PrintSpectator implements Spectator {

    @Override
    public void onGameStart(Player[] players, boolean[] rules, Card firstCard) {
        System.out.println(">>> Game started. First card: " + firstCard);
        System.out.print(">>> Players: ");
        for (int i = 0; i < players.length; i++) {
            System.out.print(players[i].getName());
            if (i < players.length - 1) System.out.print(", ");
        }
        System.out.println();
    }

    @Override
    public void onTurnStart(Player player, Card topCard, ArrayList<Card> hand, ArrayList<Card> playableCards) {
        System.out.println(">>> " + player.getName() + "'s turn. " +
                "Hand: " + hand.size() + " card(s). " +
                "Top: " + topCard + ". " +
                "Playable: " + playableCards.size());
    }

    @Override
    public void onCardPlayed(Player player, Card card) {
        System.out.println(">>> " + player.getName() + " played  " + card);
    }

    @Override
    public void onCardDrawn(Player player, Card drawn) {
        System.out.println(">>> " + player.getName() + " drew    " + drawn);
    }

    @Override
    public void onForcedDraw(Player player) {
        System.out.println(">>> " + player.getName() +
                " has no playable cards — forced to draw.");
    }

    @Override
    public void onCannotPlayOrDraw(Player player) {
        System.out.println(">>> " + player.getName() +
                " cannot play or draw — turn skipped.");
    }

    @Override
    public void onPassedTurn(Player player) {
        System.out.println(">>> " + player.getName() + " passed their turn.");
    }

    @Override
    public void onDeckEmpty() {
        System.out.println(">>> WARNING: draw deck is empty and could not be reshuffled.");
    }

    @Override
    public void onGameOver(Player winner) {
        if (winner != null) {
            System.out.println(">>> Game over! " + winner.getName() + " wins.");
        } else {
            System.out.println(">>> Game over! Draw (turn limit reached).");
        }
    }

    @Override
    public void onDebugTick(String gameId, int turn) {
        System.out.println(">>> [" + gameId + "] turn " + turn);
    }
}
