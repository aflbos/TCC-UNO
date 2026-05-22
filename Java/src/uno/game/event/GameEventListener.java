package uno.game.event;

import uno.game.models.Card;
import uno.game.players.*;

import java.util.ArrayList;

public interface GameEventListener {
    
    default void onTurnStart(Player player, Card topCard, ArrayList<Card> hand, ArrayList<Card> playableCards) {}
    
    default void onCardPlayed(Player player, Card card) {}
    
    default void onCardDrawn(Player player, Card drawn) {}
    
    default void onTurnEnd(Player player) {}

    default void onPlayOrDrawDecision(Player player) {}

    default void onPlayDrawnCardDecision(Player player, Card drawn) {}

    default void onStackDecision(Player player, int pendingPenalty) {}

    default void onChallengeDecision(Player player) {}

    default void onColorChoiceDecision(Player player) {}
    
    default void onSwapHandsDecision(Player player, int numPlayers) {}

    default void onPlayWhenCannotDrawDecision(Player player) {}

    default void onIdenticalCardDecision(Player player, ArrayList<Card> identical) {}

    default void onForcedDraw(Player player) {}

    default void onCannotPlayOrDraw(Player player) {}

    default void onDrawnCardNotPlayable(Player player, Card drawn) {}
    
    default void onPassedTurn(Player player) {}

    default void onDeckEmpty() {}
    
    default void onGameStart(Player[] players, boolean[] rules, Card firstCard) {}
    
    default void onGameOver(Player winner) {}
    
    default void onDebugTick(String gameId, int turn) {}
}