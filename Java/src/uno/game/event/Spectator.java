package uno.game.event;

import uno.game.models.Card;
import uno.game.players.Player;

import java.util.ArrayList;

public interface Spectator extends GameEventListener {
    void onTurnStart(Player player, Card topCard, ArrayList<Card> hand, ArrayList<Card> playable);
}
