package uno.game.players;

import uno.game.event.GameEventListener;
import uno.game.engine.Simulation;
import uno.game.models.Card;
import uno.network.NetworkProtocol;
import uno.network.PlayerConnection;

import java.io.IOException;
import java.util.ArrayList;

public class PlayerNetwork extends Player implements GameEventListener {

    private final PlayerConnection conn;

    public PlayerNetwork(String name, Simulation simulation, PlayerConnection conn) {
        super(name, simulation);
        this.conn = conn;
    }

    public PlayerConnection getPlayerConnection() {
        return conn;
    }

    @Override
    public int getInput() {
        int[] validInputs = simulation.getValidInputs();
        String handDesc = buildHandDescription();
        String topCard = simulation.getTopCard() != null
                ? simulation.getTopCard().toString() : "?";
        DecisionContext decision = buildDecisionContext();

        int[] playableIds = simulation.getPlayableCardIdsForNetwork();
        conn.sendGameState(handDesc, topCard, validInputs, decision.id, decision.prompt, playableIds);

        while (true) {
            try {
                int input = conn.receiveAction();
                if (isValid(input, validInputs)) {
                    return input;
                }
                conn.sendNotification("Invalid action (" + input
                        + "). Valid options: " + joinInts(validInputs));
            } catch (IOException e) {
                throw new RuntimeException(
                        "Player '" + getName() + "' disconnected during their turn.", e);
            }
        }
    }

    @Override
    public void onTurnStart(Player player, Card topCard, ArrayList<Card> hand, ArrayList<Card> playableCards) {
        if (player == this) {
            conn.sendNotification("It's your turn!");
            conn.sendPlayerUpdate(buildOpponentSummary());
        } else {
            conn.sendNotification("It's " + player.getName() + "'s turn.  "
                    + "Top card: " + topCard + ".  "
                    + "They have " + hand.size() + " cards.");
            conn.sendPlayerUpdate(buildOpponentSummary());
        }
    }

    @Override
    public void onCardPlayed(Player player, Card card) {
        conn.sendNotification(player.getName() + " played " + card + ".");
        conn.sendPlayerUpdate(buildOpponentSummary());
    }

    @Override
    public void onCardDrawn(Player player, Card drawn) {
        if (player == this) {
            conn.sendNotification("You drew: " + drawn + ".");
        } else {
            conn.sendNotification(player.getName() + " drew a card.");
            conn.sendPlayerUpdate(buildOpponentSummary());
        }
    }

    @Override
    public void onTurnEnd(Player player) {
        if (player != this) {
            conn.sendNotification("End of " + player.getName() + "'s turn.");
        }
    }

    @Override
    public void onPlayOrDrawDecision(Player player) {
        if (player == this) {
            conn.sendNotification("Decision: play a card or draw?");
        } else {
            conn.sendNotification(player.getName() + " is deciding whether to play or draw.");
        }
    }

    @Override
    public void onPlayDrawnCardDecision(Player player, Card drawn) {
        if (player == this) {
            conn.sendNotification("Decision: play the drawn card (" + drawn + ")?");
        } else {
            conn.sendNotification(player.getName()
                    + " drew a card and is deciding whether to play it.");
        }
    }

    @Override
    public void onStackDecision(Player player, int pendingPenalty) {
        if (player == this) {
            conn.sendNotification("Decision: stack a card or draw "
                    + pendingPenalty + " cards?");
        } else {
            conn.sendNotification(player.getName()
                    + " is deciding whether to stack or absorb "
                    + pendingPenalty + " cards.");
        }
    }

    @Override
    public void onChallengeDecision(Player player) {
        if (player == this) {
            conn.sendNotification("Decision: challenge the Draw Four?");
        } else {
            conn.sendNotification(player.getName()
                    + " is deciding whether to challenge the Draw Four.");
        }
    }

    @Override
    public void onColorChoiceDecision(Player player) {
        if (player == this) {
            conn.sendNotification("Decision: choose a color for your wild card.");
        } else {
            conn.sendNotification(player.getName() + " is choosing a color.");
        }
    }

    @Override
    public void onSwapHandsDecision(Player player, int numPlayers) {
        if (player == this) {
            conn.sendNotification("Decision: choose a player to swap hands with.");
        } else {
            conn.sendNotification(player.getName()
                    + " played a SEVEN and is choosing who to swap hands with.");
        }
    }

    @Override
    public void onPlayWhenCannotDrawDecision(Player player) {
        if (player == this) {
            conn.sendNotification("Decision: you cannot draw — play a card?");
        } else {
            conn.sendNotification(player.getName()
                    + " cannot draw and is deciding whether to play.");
        }
    }

    @Override
    public void onIdenticalCardDecision(Player player, ArrayList<Card> identical) {
        if (player == this) {
            conn.sendNotification("Decision: you have identical cards — play another?");
        } else {
            conn.sendNotification(player.getName()
                    + " may play an additional identical card.");
        }
    }

    @Override
    public void onForcedDraw(Player player) {
        if (player == this) {
            conn.sendNotification("You have no playable cards and must draw.");
        } else {
            conn.sendNotification(player.getName()
                    + " has no playable cards and must draw.");
        }
    }

    @Override
    public void onCannotPlayOrDraw(Player player) {
        if (player == this) {
            conn.sendNotification(
                    "You have no playable cards and cannot draw — turn skipped.");
        } else {
            conn.sendNotification(player.getName()
                    + " cannot play or draw — turn skipped.");
        }
    }

    @Override
    public void onDrawnCardNotPlayable(Player player, Card drawn) {
        if (player == this) {
            conn.sendNotification(
                    "The drawn card (" + drawn + ") is not playable — turn over.");
        } else {
            conn.sendNotification(player.getName()
                    + " drew a card that wasn't playable — turn over.");
        }
    }

    @Override
    public void onPassedTurn(Player player) {
        if (player == this) {
            conn.sendNotification("You chose not to play — turn over.");
        } else {
            conn.sendNotification(player.getName() + " passed their turn.");
        }
    }

    @Override
    public void onDeckEmpty() {
        conn.sendNotification("The draw deck is empty and could not be reshuffled.");
    }

    @Override
    public void onGameStart(Player[] players, boolean[] rules, Card firstCard) {
        StringBuilder sb = new StringBuilder("Game started!  Players: ");

        for (int i = 0; i < players.length; i++) {
            sb.append(players[i].getName());
            if (i < players.length - 1) sb.append(", ");
        }

        sb.append(".  First card: ").append(firstCard).append(".");
        conn.sendNotification(sb.toString());
        conn.sendPlayerUpdate(buildOpponentSummary());
    }

    @Override
    public void onGameOver(Player winner) {
        String winnerName = (winner != null) ? winner.getName() : "DRAW";
        conn.sendGameOver(winnerName);
    }

    @Override
    public void onDebugTick(String gameId, int turn) {
        
    }

    private String buildHandDescription() {
        ArrayList<Card> cards = getCards();
        if (cards.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(cards.get(i).getId())
                    .append(':')
                    .append(cards.get(i).toString());
        }
        return sb.toString();
    }

    private String buildOpponentSummary() {
        if (simulation == null) return "";
        Player[] all = simulation.getPlayers();
        StringBuilder sb = new StringBuilder();
        for (Player p : all) {
            if (p == this) continue;
            if (sb.length() > 0) sb.append(',');
            sb.append(p.getName()).append(':').append(p.getCards().size());
        }
        return sb.toString();
    }

    private DecisionContext buildDecisionContext() {
        double[] obs = simulation.getObservationVector();
        if (obs == null || obs.length < 189) {
            return new DecisionContext(NetworkProtocol.D_UNKNOWN, "Choose an action.");
        }

        if (obs[188] != 0) {
            return new DecisionContext(NetworkProtocol.D_CHALLENGE_DRAW_FOUR,
                    "The top card is a DRAW FOUR - challenge it?");
        }
        if (obs[187] != 0) {
            return new DecisionContext(NetworkProtocol.D_STACK_OR_DRAW,
                    "Stack a card to pass the penalty of " + simulation.getStackingAmount() + " cards, or draw them?");
        }
        if (obs[186] != 0) {
            return new DecisionContext(NetworkProtocol.D_PLAY_OR_DRAW, "Play a card or draw?");
        }
        if (obs[185] != 0) {
            return new DecisionContext(NetworkProtocol.D_PLAY_WHEN_CANNOT_DRAW,
                    "You cannot draw - play a card?");
        }
        if (obs[184] != 0) {
            return new DecisionContext(NetworkProtocol.D_PLAY_DRAWN_CARD, "Play the drawn card?");
        }
        if (obs[183] != 0) {
            return new DecisionContext(NetworkProtocol.D_CHOOSE_CARD, "Choose a card to play.");
        }
        if (obs[182] != 0) {
            return new DecisionContext(NetworkProtocol.D_SWAP_HANDS_TARGET,
                    "SEVEN played - choose a player to swap hands with.");
        }
        if (obs[181] != 0) {
            return new DecisionContext(NetworkProtocol.D_CHOOSE_COLOR,
                    "Choose a color for your wild card.");
        }
        if (obs[180] != 0) {
            return new DecisionContext(NetworkProtocol.D_PLAY_IDENTICAL,
                    "You have identical cards - play another?");
        }

        return new DecisionContext(NetworkProtocol.D_UNKNOWN, "Choose an action.");
    }

    private static final class DecisionContext {
        private final String id;
        private final String prompt;

        private DecisionContext(String id, String prompt) {
            this.id = id;
            this.prompt = prompt;
        }
    }

    private static boolean isValid(int input, int[] validInputs) {
        for (int v : validInputs)
            if (v == input) return true;

        return false;
    }

    private static String joinInts(int[] arr) {
        StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(arr[i]);
        }

        return sb.append(']').toString();
    }
}