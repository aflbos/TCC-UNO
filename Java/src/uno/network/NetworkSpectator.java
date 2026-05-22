package uno.network;

import uno.game.event.Spectator;
import uno.game.models.Card;
import uno.game.players.Player;

import java.util.ArrayList;

public class NetworkSpectator implements Spectator {

    private final String name;
    private final PlayerConnection conn;

    public NetworkSpectator(String name, PlayerConnection conn) {
        this.name = name;
        this.conn = conn;
    }

    @Override
    public void onGameStart(Player[] players, boolean[] rules, Card firstCard) {
        StringBuilder sb = new StringBuilder("Game started! Players: ");
        for (int i = 0; i < players.length; i++) {
            sb.append(players[i].getName());
            if (i < players.length - 1) sb.append(", ");
        }
        sb.append(". First card: ").append(firstCard);
        conn.sendNotification(sb.toString());
    }

    @Override
    public void onTurnStart(Player player, Card topCard, ArrayList<Card> hand, ArrayList<Card> playable) {
        conn.sendNotification("It's " + player.getName() + "'s turn. "
                + "Top: " + topCard + ". Hand: " + hand.size() + " cards.");
        sendPlayerUpdate(player);
    }

    @Override
    public void onCardPlayed(Player player, Card card) {
        conn.sendNotification(player.getName() + " played " + card + ".");
        sendPlayerUpdate(player);
    }

    @Override
    public void onCardDrawn(Player player, Card drawn) {
        conn.sendNotification(player.getName() + " drew a card.");
        sendPlayerUpdate(player);
    }

    @Override
    public void onGameOver(Player winner) {
        if (winner != null) {
            conn.sendGameOver(winner.getName());
        } else {
            conn.sendGameOver("DRAW");
        }
    }

    @Override
    public void onDebugTick(String gameId, int turn) {
        conn.sendNotification("[" + gameId + "] turn " + turn);
    }

    private void sendPlayerUpdate(Player changed) {
        conn.sendNotification(changed.getName() + " has "
                + changed.getCards().size() + " cards.");
    }
}
