package uno.game.players;

import uno.game.engine.Simulation;
import uno.ai.network.ConnectionAI;

import java.util.ArrayList;

public class PlayerAI extends Player {
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
            reward = simulation.computeShapedReward(this);
        } else {
            rewardBaselineInitialized = true;
        }

        while (true) {
            try {
                input = ConnectionAI.askAction(observationVector, decisionMask, reward + invalidActionPenalty, false);
            } catch (RuntimeException e) {
                backendUnavailable = true;
                throw new RuntimeException("Network error: " + e.getMessage());
            }

            backendUnavailable = false;

            if (input != -1 && decisionMask[input] != 0) {
                return input;
            }

            invalidActionPenalty -= 0.1;
        }
    }


    @Override
    public void onGameOver(Player winner) {
        if (backendUnavailable) {
            return;
        }

        double reward = 0;

        if (rewardBaselineInitialized) {
            reward += simulation.computeShapedReward(this);
        }

        if (winner == this) reward += 10.0;
        else reward -= 10.0;

        try {
            ConnectionAI.askAction(simulation.getObservationVector(), simulation.getDecisionMask(), reward, true);
        } catch (RuntimeException e) {
            backendUnavailable = true;
        }
    }
}
