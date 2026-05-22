package uno.game.players;

import uno.game.engine.Simulation;

import java.util.Random;

public class PlayerRandom extends Player {
    private final Random random;

    public PlayerRandom(String name, Simulation simulation) {
        super(name, simulation);
        int seed = simulation != null ? simulation.getSeed() : -1;
        random = (seed == -1) ? new Random() : new Random(seed ^ name.hashCode());
    }

    public int getInput() {

        int[] validInputs = simulation.getValidInputs();
        int randomIndex = random.nextInt(validInputs.length);

        return validInputs[randomIndex];
    }
}
