package uno.ai.training;

import uno.ai.network.ConnectionAI;
import uno.game.engine.Simulation;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Training {

    private static final String DEFAULT_CONFIG = "res/training_default_config.properties";

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;

        TrainingConfig config;
        try {
            config = TrainingConfig.load(configPath);
        } catch (IOException e) {
            System.err.println("Failed to load config from '" + configPath + "': " + e.getMessage());
            System.exit(1);
            return;
        }

        System.out.println("Loaded config: " + config);
        System.out.println("From: " + configPath);

        for (int i = 0; i < config.numClients; i++) {
            int port = config.startPort + i;
            String id = String.valueOf(i + 1);
            new Thread(() -> runRoutine(port, id, config), "trainer-" + id).start();
        }
    }

    private static void runRoutine(int port, String id, TrainingConfig config) {
        ConnectionAI ConnectionAI = new ConnectionAI();

        try {
            ConnectionAI.connect(port);
        } catch (IOException e) {
            throw new RuntimeException("Thread " + id + " could not connect on port " + port, e);
        }

        List<TrainingPhase> phases = config.phases;
        int loopStart = config.loopStartPhase - 1;
        
        for (int i = 0; i < loopStart; i++) {
            runPhase(phases.get(i), id, ConnectionAI);
        }
        
        if (config.loopInfinite) {
            while (true) {
                try {
                    for (int i = loopStart; i < phases.size(); i++) {
                        runPhase(phases.get(i), id, ConnectionAI);
                    }
                } catch (Exception e) {
                    System.err.println(id + ". Unhandled exception in training loop.");
                    throw new RuntimeException(e);
                }
            }
        } else {
            for (int i = loopStart; i < phases.size(); i++) {
                runPhase(phases.get(i), id, ConnectionAI);
            }
        }
    }

    private static void runPhase(TrainingPhase phase, String id, ConnectionAI ConnectionAI) {
        switch (phase.type) {
            case FIXED: runFixedPhase (phase, id, ConnectionAI); break;
            case RANDOM_AI: runRandomAIPhase (phase, id, ConnectionAI); break;
            case MIXED: runMixedPhase (phase, id, ConnectionAI); break;
            case EXHAUSTIVE: runExhaustivePhase(phase, id, ConnectionAI); break;
        }
    }

    private static void runFixedPhase(TrainingPhase phase, String id, ConnectionAI ConnectionAI) {
        Random rng = new Random();
        System.out.println(id + ". [FIXED] " + phase.games + " games - " + phase.aiPlayers + " AI, " + phase.randomPlayers + " random players.");
        displayRules(phase);

        for (int i = 0; i < phase.games; i++) {
            runGame(phase.aiPlayers, phase.randomPlayers, phase.resolveRules(rng), id, ConnectionAI);
        }
    }

    private static void runRandomAIPhase(TrainingPhase phase, String id, ConnectionAI ConnectionAI) {
        Random rng = new Random();
        int range = phase.aiPlayersMax - phase.aiPlayersMin + 1;
        System.out.println(id + ". [RANDOM_AI] " + phase.games + " games - minimum of " + phase.aiPlayersMin + " AI players and maximum of " + phase.aiPlayersMax + " AI players.");
        displayRules(phase);

        for (int i = 0; i < phase.games; i++) {
            int aiPlayers = rng.nextInt(range) + phase.aiPlayersMin;
            runGame(aiPlayers, 0, phase.resolveRules(rng), id, ConnectionAI);
        }
    }

    private static void runMixedPhase(TrainingPhase phase, String id, ConnectionAI ConnectionAI) {
        Random rng = new Random();
        int range = phase.aiPlayersMax - phase.aiPlayersMin + 1;
        System.out.println(id + ". [MIXED] " + phase.games + " games - minimum of " + phase.aiPlayersMin + " AI players and maximum of " + phase.aiPlayersMax + " AI players.");
        displayRules(phase);

        for (int i = 0; i < phase.games; i++) {
            int aiPlayers = rng.nextInt(range) + phase.aiPlayersMin;
            int maxRandom = 10 - aiPlayers;
            int randomPlayers = maxRandom == 0 ? 1 : rng.nextInt(maxRandom) + 1;
            runGame(aiPlayers, randomPlayers, phase.resolveRules(rng), id, ConnectionAI);
        }
    }

    private static void runExhaustivePhase(TrainingPhase phase, String id, ConnectionAI ConnectionAI) {
        Random rng = new Random();
        System.out.println(id + ". [EXHAUSTIVE] " + phase.iterations + " iterations.");
        displayRules(phase);

        for (int k = 0; k < phase.iterations; k++) {
            for (int total = 2; total <= 10; total++) {
                for (int ai = 1; ai < total; ai++) {
                    runGame(ai, total - ai, phase.resolveRules(rng), id, ConnectionAI);
                }
                System.out.println(id + ". All possible games with " + total + " players done.");
            }
        }
    }

    private static void runGame(int aiPlayers, int randomPlayers, boolean[] rules, String id, ConnectionAI ConnectionAI) {
        Simulation simulation = new Simulation(0, aiPlayers, randomPlayers, ConnectionAI, rules, id, -1);

        while (!simulation.isGameOver()) {
            simulation.playTurn();
        }
    }

    private static void displayRules(TrainingPhase phase) {
        System.out.println("Rules: "+
                "Stacking: " + phase.ruleSpecs[0] + ", " +
                "Skip N' Flip: " + phase.ruleSpecs[1] + ", " +
                "Seven Zero: " + phase.ruleSpecs[2] + ", " +
                "Force Play Drawn: " + phase.ruleSpecs[3] + ", " +
                "Draw and Play: " + phase.ruleSpecs[4] + ", " +
                "Draw to Match: " + phase.ruleSpecs[5] + ", " +
                "Bluffing: " + phase.ruleSpecs[6] + ", " +
                "Play Identical: " + phase.ruleSpecs[7]
        );
    }
}