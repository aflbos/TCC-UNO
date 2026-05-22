package uno.game.players;

import uno.game.engine.Simulation;
import uno.ai.network.ConnectionAI;

import java.util.ArrayList;

public class PlayerAI extends Player {
    private static final int DEFAULT_DECISION_DELAY_MS = 2000;
    private final ConnectionAI ConnectionAI;
    private boolean rewardBaselineInitialized = false;
    private int previousOwnHandSize = -1;
    private int previousNextOpponentHandSize = -1;
    private boolean backendUnavailable = false;

    public PlayerAI(String name, Simulation simulation, ConnectionAI ConnectionAI) {
        super(name, simulation);
        this.ConnectionAI = ConnectionAI;
    }

    public int getInput() {
        applyHumanFacingDecisionDelay();

        int input;
        double invalidActionPenalty = 0;
        double reward = 0;
        double[] observationVector = simulation.getObservationVector();
        double[] decisionMask = simulation.getDecisionMask();

        ArrayList<Integer> validInputList = new ArrayList<>();

        for (int i = 0; i < decisionMask.length; i++) {
            if (decisionMask[i] != 0) {
                validInputList.add(i);
            }
        }

        if (rewardBaselineInitialized) {
            reward = simulation.computeShapedReward(this, previousOwnHandSize, previousNextOpponentHandSize);
        } else {
            rewardBaselineInitialized = true;
        }

        previousOwnHandSize = simulation.getHandSize(this);
        previousNextOpponentHandSize = simulation.getNextOpponentHandSize(this);

        while (true) {
            try {
                input = ConnectionAI.askAction(observationVector, decisionMask, reward + invalidActionPenalty, false);
            } catch (RuntimeException e) {
                backendUnavailable = true;
                throw new RuntimeException("Network error: " + e.getMessage());
            }

            backendUnavailable = false;

            if (input != -1) {
                if (decisionMask[input] != 0) {
                    invalidActionPenalty -= 0.1;
                }
            }
        }
    }

    private void applyHumanFacingDecisionDelay() {
        if (simulation == null) {
            return;
        }

        int delayMs = getDecisionDelayMs();
        if (delayMs <= 0) {
            return;
        }

        if (isHumanOnlyDelay() && !hasHumanParticipant()) {
            return;
        }

        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean hasHumanParticipant() {

        for (Player player : simulation.getPlayers()) {
            if (player instanceof PlayerHuman || player instanceof PlayerNetwork) {
                return true;
            }
        }

        return false;
    }

    private static int getDecisionDelayMs() {
        String raw = System.getProperty("uno.ai.delay.ms", String.valueOf(DEFAULT_DECISION_DELAY_MS)).trim();
        try {
            return Math.max(0, Integer.parseInt(raw));
        } catch (NumberFormatException ignored) {
            return DEFAULT_DECISION_DELAY_MS;
        }
    }

    private static boolean isHumanOnlyDelay() {
        return Boolean.parseBoolean(System.getProperty("uno.ai.delay.humanOnly", "true"));
    }

    @Override
    public void onGameOver(Player winner) {
        if (backendUnavailable) {
            return;
        }

        double reward = 0;

        if (rewardBaselineInitialized) {
            reward += simulation.computeShapedReward(this, previousOwnHandSize, previousNextOpponentHandSize);
        }

        if (winner == this) reward += 1.0;
        else if (winner == null) reward -= 0.2;
        else reward -= 1.0;

        try {
            ConnectionAI.askAction(simulation.getObservationVector(), simulation.getDecisionMask(), reward, true);
        } catch (RuntimeException e) {
            backendUnavailable = true;
        }
    }
}
