package uno.game.players;

import uno.game.engine.Simulation;
import uno.game.models.Card;
import uno.game.models.Color;
import uno.game.models.Value;

public class PlayerHerusitic extends Player {

    public PlayerHerusitic(String name, Simulation simulation) {
        super(name, simulation);
    }

    @Override
    public int getInput() {
        double[] obs = simulation.getObservationVector();
        int[] validInputs = simulation.getValidInputs();

        // Check which decision the game is prompting based on the Simulation observation flags

        if (obs[188] != 0) {
            // FLAG_CHALLENGE_DRAW4: (1. Yes, 2. No)
            // It's safer not to challenge to avoid the penalty of drawing 6 cards if we guess wrong.
            return 2;
        }
        if (obs[187] != 0) {
            // FLAG_STACK: Stack to pass the penalty or Draw? (1. Stack, 2. Draw)
            // Always stack if possible to get rid of a card and pass the penalty.
            return 1;
        }
        if (obs[186] != 0) {
            // FLAG_PLAY_OR_DRAW: (1. Play, 2. Draw)
            // Always play a card to minimize hand.
            return 1;
        }
        if (obs[185] != 0) {
            // FLAG_PLAY_WHEN_NO_DRAW: (1. Play, 2. Pass)
            // Always play if we have the chance.
            return 1;
        }
        if (obs[184] != 0) {
            // FLAG_PLAY_DRAWN_CARD: (1. Play, 2. Pass)
            // Always play the newly drawn card if possible.
            return 1;
        }
        if (obs[183] != 0) {
            // FLAG_CHOOSE_CARD: Choose a card to play.
            int[] playableIds = simulation.getPlayableCardIdsForNetwork();
            return getBestCardInput(playableIds, validInputs);
        }
        if (obs[182] != 0) {
            // FLAG_SWAP_HANDS: (SEVEN played). Target a player.
            // Swap hands with whoever has the least amount of cards.
            return getBestSwapTarget(validInputs);
        }
        if (obs[181] != 0) {
            // FLAG_CHOOSE_COLOR: (1. RED, 2. GREEN, 3. BLUE, 4. YELLOW)
            // Choose the color we have the most of in our current hand.
            return getBestColor(validInputs);
        }
        if (obs[180] != 0) {
            // FLAG_PLAY_IDENTICAL: (1. Yes, 2. No)
            // Always play another identical card to empty the hand faster.
            return 1;
        }

        // Fallback for any unknown situation
        return validInputs[0];
    }

    /**
     * Prioritizes action cards (Draw 4, Draw 2, Skips, Reverses) to hurt opponents
     * while getting rid of a card. If none are present, drops whatever it can.
     */
    private int getBestCardInput(int[] playableIds, int[] validInputs) {
        int bestScore = -1;
        int bestInput = validInputs[0];

        for (int i = 0; i < playableIds.length; i++) {
            int id = playableIds[i];
            Card card = getCardById(id);
            if (card != null) {
                int score = 0;
                Value val = card.getValue();

                if (val == Value.DRAW_FOUR) score = 5;
                else if (val == Value.DRAW_TWO) score = 4;
                else if (val == Value.SKIP) score = 3;
                else if (val == Value.REVERSE) score = 2;
                else if (val == Value.WILD) score = 1;
                else score = 0;

                if (score > bestScore) {
                    bestScore = score;
                    bestInput = validInputs[i];
                }
            }
        }
        return bestInput;
    }

    private Card getCardById(int id) {
        for (Card c : getCards()) {
            if (c.getId() == id) {
                return c;
            }
        }
        return null;
    }

    /**
     * Looks through all possible players to swap hands with and chooses the one with the fewest cards.
     */
    private int getBestSwapTarget(int[] validInputs) {
        Player[] allPlayers = simulation.getPlayers();
        int minCards = Integer.MAX_VALUE;
        int bestInput = validInputs[0];

        for (int input : validInputs) {
            int targetIndex = input - 1; // Translate 1-indexed to 0-indexed array
            if (targetIndex >= 0 && targetIndex < allPlayers.length) {
                Player target = allPlayers[targetIndex];
                if (target.getCards().size() < minCards) {
                    minCards = target.getCards().size();
                    bestInput = input;
                }
            }
        }

        return bestInput;
    }

    /**
     * Scans the hand to see which color we hold the most, then selects that color for the Wildcard.
     */
    private int getBestColor(int[] validInputs) {
        int red = 0, green = 0, blue = 0, yellow = 0;

        for (Card c : getCards()) {
            if (c.getColor() == Color.RED) red++;
            else if (c.getColor() == Color.GREEN) green++;
            else if (c.getColor() == Color.BLUE) blue++;
            else if (c.getColor() == Color.YELLOW) yellow++;
        }

        int[] counts = {red, green, blue, yellow};
        int maxCount = -1;
        int bestIdx = 0; // 0=RED, 1=GREEN, 2=BLUE, 3=YELLOW

        for (int i = 0; i < counts.length; i++) {
            if (counts[i] > maxCount) {
                maxCount = counts[i];
                bestIdx = i;
            }
        }

        // Expected validInputs are commonly [1, 2, 3, 4] directly translating to the enum index
        int desiredInput = bestIdx + 1;
        for (int input : validInputs) {
            if (input == desiredInput) return input;
        }

        return validInputs[0];
    }
}