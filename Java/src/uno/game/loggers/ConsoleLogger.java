package uno.game.loggers;

import uno.game.models.Card;
import uno.game.players.Player;

public class ConsoleLogger implements Logger {

    @Override
    public void onGameStart(Player[] players, boolean[] rules, Card firstCard) {
        StringBuilder playersList = new StringBuilder();
        for (int i = 0; i < players.length; i++) {
            playersList.append(players[i].getName());
            if (i < players.length - 1) playersList.append(", ");
        }
        System.out.println(StructuredLog.line("INFO", "game_start",
                "players=" + playersList + " first_card=" + firstCard));
    }

    @Override
    public void onGameOver(Player winner) {
        if (winner != null) {
            System.out.println(StructuredLog.line("INFO", "game_over", "winner=" + winner.getName()));
        } else {
            System.out.println(StructuredLog.line("INFO", "game_over", "winner=DRAW reason=turn_limit"));
        }
    }

    @Override
    public void onDebugTick(String gameId, int turn) {
        System.out.println(StructuredLog.line("DEBUG", "debug_tick", "game=" + gameId + " turn=" + turn));
    }
}
