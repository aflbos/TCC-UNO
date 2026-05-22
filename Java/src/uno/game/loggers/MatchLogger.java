package uno.game.loggers;

import uno.game.models.Card;
import uno.game.players.Player;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class MatchLogger implements Logger {

    private final String filePath;
    private BufferedWriter writer;

    public MatchLogger(String filePath) {
        this.filePath = filePath;
    }

    private void write(String line) {
        if (writer == null) return;
        try {
            writer.write(line);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[MatchLogger] write error: " + e.getMessage());
        }
    }

    private void flush() {
        if (writer == null) return;
        try { writer.flush(); } catch (IOException ignored) {}
    }

    private void openFile() {
        try {
            writer = new BufferedWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            System.err.println("[MatchLogger] Could not open log file: " + filePath);
        }
    }

    private void closeFile() {
        if (writer == null) return;
        try { writer.close(); } catch (IOException ignored) {}
        writer = null;
    }

    @Override
    public void onGameStart(Player[] players, boolean[] rules, Card firstCard) {
        openFile();
        String playersLine = Arrays.stream(players)
                .map(Player::getName)
                .reduce((a, b) -> a + "," + b).orElse("");
        String rulesLine = "stacking=" + rules[0] + " skipNFlip=" + rules[1]
                + " sevenZero=" + rules[2] + " forcePlayDrawn=" + rules[3]
                + " drawAndPlay=" + rules[4] + " drawToMatch=" + rules[5]
                + " bluffing=" + rules[6] + " playIdentical=" + rules[7];
        write(StructuredLog.line("INFO", "game_start", "players=" + playersLine + " " + rulesLine + " first_card=" + firstCard));
        flush();
    }

    @Override
    public void onCardPlayed(Player player, Card card) {
        write(StructuredLog.line("INFO", "card_played", "player=" + player.getName() + " card=" + card));
        flush();
    }

    @Override
    public void onCardDrawn(Player player, Card drawn) {
        write(StructuredLog.line("INFO", "card_drawn", "player=" + player.getName() + " card=" + drawn));
        flush();
    }

    @Override
    public void onForcedDraw(Player player) {
        write(StructuredLog.line("INFO", "forced_draw", "player=" + player.getName()));
        flush();
    }

    @Override
    public void onCannotPlayOrDraw(Player player) {
        write(StructuredLog.line("INFO", "cannot_play_or_draw", "player=" + player.getName()));
        flush();
    }

    @Override
    public void onPassedTurn(Player player) {
        write(StructuredLog.line("INFO", "turn_passed", "player=" + player.getName()));
        flush();
    }

    @Override
    public void onDeckEmpty() {
        write(StructuredLog.line("WARN", "deck_empty", "reshuffle_failed=true"));
        flush();
    }

    @Override
    public void onGameOver(Player winner) {
        if (winner != null) {
            write(StructuredLog.line("INFO", "game_over", "winner=" + winner.getName()));
        } else {
            write(StructuredLog.line("INFO", "game_over", "winner=DRAW reason=turn_limit"));
        }
        flush();
        closeFile();
    }

    @Override
    public void onDebugTick(String gameId, int turn) {
        write(StructuredLog.line("DEBUG", "debug_tick", "game=" + gameId + " turn=" + turn));
        flush();
    }
}
