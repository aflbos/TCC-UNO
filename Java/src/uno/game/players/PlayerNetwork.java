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
        String topCard = simulation.getTopCard() != null ? simulation.getTopCard().toString() : "?";
        DecisionContext decision = buildDecisionContext();

        int[] playableIds = simulation.getPlayableCardIdsForNetwork();
        conn.sendGameState(handDesc, topCard, validInputs, decision.id, decision.prompt, playableIds);

        while (true) {
            try {
                int input = conn.receiveAction();
                if (isValid(input, validInputs)) {
                    return input;
                }
                conn.sendNotification("Acao invalida (" + input
                        + "). Opcoes validas: " + joinInts(validInputs));
            } catch (IOException e) {
                throw new RuntimeException(
                        "Player '" + getName() + "' disconnected during their turn.", e);
            }
        }
    }

    @Override
    public void onTurnStart(Player player, Card topCard, ArrayList<Card> hand, ArrayList<Card> playableCards) {
        if (player == this) {
            conn.sendNotification("E a sua vez!");
            conn.sendPlayerUpdate(buildOpponentSummary());
        } else {
            conn.sendNotification("E a vez de " + player.getName() + ".  "
                    + "Carta do topo: " + topCard + ".  "
                    + "Ele(a) tem " + hand.size() + " cartas.");
            conn.sendPlayerUpdate(buildOpponentSummary());
        }
    }

    @Override
    public void onCardPlayed(Player player, Card card) {
        conn.sendNotification(player.getName() + " jogou " + card + ".");
        conn.sendPlayerUpdate(buildOpponentSummary());
    }

    @Override
    public void onCardDrawn(Player player, Card drawn) {
        if (player == this) {
            conn.sendNotification("Voce comprou: " + drawn + ".");
        } else {
            conn.sendNotification(player.getName() + " comprou uma carta.");
            conn.sendPlayerUpdate(buildOpponentSummary());
        }
    }

    @Override
    public void onTurnEnd(Player player) {
        if (player != this) {
            conn.sendNotification("Fim da vez de " + player.getName() + ".");
        }
    }

    @Override
    public void onPlayOrDrawDecision(Player player) {
        if (player == this) {
            conn.sendNotification("Decisao: jogar uma carta ou comprar?");
        } else {
            conn.sendNotification(player.getName() + " esta decidindo se joga ou compra.");
        }
    }

    @Override
    public void onPlayDrawnCardDecision(Player player, Card drawn) {
        if (player == this) {
            conn.sendNotification("Decisao: jogar a carta comprada (" + drawn + ")?");
        } else {
            conn.sendNotification(player.getName() + " comprou uma carta e esta decidindo se joga.");
        }
    }

    @Override
    public void onStackDecision(Player player, int pendingPenalty) {
        if (player == this) {
            conn.sendNotification("Decisao: empilhar uma carta ou comprar " + pendingPenalty + " cartas?");
        } else {
            conn.sendNotification(player.getName() + " esta decidindo se empilha ou absorve " + pendingPenalty + " cartas.");
        }
    }

    @Override
    public void onChallengeDecision(Player player) {
        if (player == this) {
            conn.sendNotification("Decisao: desafiar o COMPRA QUATRO?");
        } else {
            conn.sendNotification(player.getName() + " esta decidindo se desafia o COMPRA QUATRO.");
        }
    }

    @Override
    public void onColorChoiceDecision(Player player) {
        if (player == this) {
            conn.sendNotification("Decisao: escolher uma cor para o seu coringa.");
        } else {
            conn.sendNotification(player.getName() + " esta escolhendo uma cor.");
        }
    }

    @Override
    public void onSwapHandsDecision(Player player, int numPlayers) {
        if (player == this) {
            conn.sendNotification("Decisao: escolher um jogador para trocar as maos.");
        } else {
            conn.sendNotification(player.getName() + " jogou um SETE e esta escolhendo com quem trocar as maos.");
        }
    }

    @Override
    public void onPlayWhenCannotDrawDecision(Player player) {
        if (player == this) {
            conn.sendNotification("Decisao: voce nao pode comprar — jogar uma carta?");
        } else {
            conn.sendNotification(player.getName() + " nao pode comprar e esta decidindo se joga.");
        }
    }

    @Override
    public void onIdenticalCardDecision(Player player, ArrayList<Card> identical) {
        if (player == this) {
            conn.sendNotification("Decisao: voce tem cartas identicas — jogar outra?");
        } else {
            conn.sendNotification(player.getName() + " pode jogar outra carta identica.");
        }
    }

    @Override
    public void onForcedDraw(Player player) {
        if (player == this) {
            conn.sendNotification("Voce nao tem cartas jogaveis e deve comprar.");
        } else {
            conn.sendNotification(player.getName() + " nao tem cartas jogaveis e deve comprar.");
        }
    }

    @Override
    public void onCannotPlayOrDraw(Player player) {
        if (player == this) {
            conn.sendNotification("Voce nao tem cartas jogaveis e nao pode comprar — vez pulada.");
        } else {
            conn.sendNotification(player.getName() + " nao pode jogar ou comprar — vez pulada.");
        }
    }

    @Override
    public void onDrawnCardNotPlayable(Player player, Card drawn) {
        if (player == this) {
            conn.sendNotification("A carta comprada (" + drawn + ") nao pode ser jogada — vez encerrada.");
        } else {
            conn.sendNotification(player.getName() + " comprou uma carta que nao era jogavel — vez encerrada.");
        }
    }

    @Override
    public void onPassedTurn(Player player) {
        if (player == this) {
            conn.sendNotification("Voce escolheu nao jogar — vez encerrada.");
        } else {
            conn.sendNotification(player.getName() + " passou a vez.");
        }
    }

    @Override
    public void onDeckEmpty() {
        conn.sendNotification("O baralho de compras acabou e nao pode ser reembaralhado.");
    }

    @Override
    public void onGameStart(Player[] players, boolean[] rules, Card firstCard) {
        StringBuilder sb = new StringBuilder("Partida iniciada!  Jogadores: ");

        for (int i = 0; i < players.length; i++) {
            sb.append(players[i].getName());
            if (i < players.length - 1) sb.append(", ");
        }

        sb.append(".  Primeira carta: ").append(firstCard).append(".");
        conn.sendNotification(sb.toString());
        conn.sendPlayerUpdate(buildOpponentSummary());
    }

    @Override
    public void onGameOver(Player winner) {
        String winnerName = (winner != null) ? winner.getName() : "EMPATE";
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
            return new DecisionContext(NetworkProtocol.D_UNKNOWN, "Escolha uma acao.");
        }

        if (obs[188] != 0) {
            return new DecisionContext(NetworkProtocol.D_CHALLENGE_DRAW_FOUR, "A carta do topo e um COMPRA QUATRO - desafiar?");
        }
        if (obs[187] != 0) {
            return new DecisionContext(NetworkProtocol.D_STACK_OR_DRAW, "Empilhar uma carta para passar a penalidade de " + simulation.getStackingAmount() + " cartas ou comprar?");
        }
        if (obs[186] != 0) {
            return new DecisionContext(NetworkProtocol.D_PLAY_OR_DRAW, "Jogar uma carta ou comprar?");
        }
        if (obs[185] != 0) {
            return new DecisionContext(NetworkProtocol.D_PLAY_WHEN_CANNOT_DRAW, "Voce nao pode comprar - jogar uma carta?");
        }
        if (obs[184] != 0) {
            return new DecisionContext(NetworkProtocol.D_PLAY_DRAWN_CARD, "Jogar a carta comprada?");
        }
        if (obs[183] != 0) {
            return new DecisionContext(NetworkProtocol.D_CHOOSE_CARD, "Escolha uma carta para jogar.");
        }
        if (obs[182] != 0) {
            return new DecisionContext(NetworkProtocol.D_SWAP_HANDS_TARGET, "SETE jogado - escolha um jogador para trocar as maos.");
        }
        if (obs[181] != 0) {
            return new DecisionContext(NetworkProtocol.D_CHOOSE_COLOR, "Escolha uma cor para o seu coringa.");
        }
        if (obs[180] != 0) {
            return new DecisionContext(NetworkProtocol.D_PLAY_IDENTICAL, "Voce tem cartas identicas - jogar outra?");
        }

        return new DecisionContext(NetworkProtocol.D_UNKNOWN, "Escolha uma acao.");
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