package uno.network;

public interface GameClientListener {

    default void onNotify(String message) {}

    default void onState(String handDesc, String topCard, int[] validInputs, String prompt) {
        onState(handDesc, topCard, validInputs, NetworkProtocol.D_UNKNOWN, prompt, new int[0]);
    }

    default void onState(String handDesc, String topCard, int[] validInputs, String prompt, int[] playableCardIds) {
        onState(handDesc, topCard, validInputs, NetworkProtocol.D_UNKNOWN, prompt, playableCardIds);
    }

    default void onState(String handDesc, String topCard, int[] validInputs, String decisionId, String prompt, int[] playableCardIds) {}

    default void onPlayers(String summary) {}

    default void onLobbyUpdate(String summary) {}

    default void onGameStart() {}

    default void onGameOver(String winnerName) {}

    default void onDisconnect(String reason) {}
}
