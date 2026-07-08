package uno.network;

import uno.game.players.Player;
import uno.game.models.Card;

/**
 * Interface para o HOST receber notificações de eventos do jogo.
 * Permite ao servidor exibir informações sobre o estado atual da partida.
 */
public interface GameHostListener {
    /**
     * Chamado quando o estado do jogo é atualizado (após cada turno).
     *
     * @param players Array de jogadores com seus nomes e quantidade de cartas
     * @param currentPlayerName Nome do jogador atual
     * @param topCard Carta no topo da pilha de descarte
     * @param turnCounter Número do turno
     */
    /**
     * Variante incluindo uma lista recente de histórico de jogadas (mais nova por último).
     */
    void onGameStateUpdate(Player[] players, String currentPlayerName, Card topCard, int turnCounter, java.util.List<String> recentPlays);

    /**
     * Padrão compatível com versões anteriores: delega para o novo método sem histórico.
     */
    default void onGameStateUpdate(Player[] players, String currentPlayerName, Card topCard, int turnCounter) {
        onGameStateUpdate(players, currentPlayerName, topCard, turnCounter, new java.util.ArrayList<>());
    }

    /**
     * Chamado quando uma carta é jogada.
     *
     * @param playerName Nome do jogador que jogou a carta
     * @param card Carta que foi jogada
     */
    void onCardPlayedNotification(String playerName, Card card);

    /**
     * Chamado quando um jogador compra cartas.
     *
     * @param playerName Nome do jogador
     * @param cardCount Quantidade de cartas compradas
     */
    void onCardsDrawnNotification(String playerName, int cardCount);
}

