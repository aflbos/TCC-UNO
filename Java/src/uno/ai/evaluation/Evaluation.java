package uno.ai.evaluation;

import uno.ai.network.ConnectionAI;
import uno.game.players.PlayerAI;
import uno.game.engine.Simulation;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Evaluation {

    private static final String DEFAULT_CONFIG = "C:\\Users\\User\\Documents\\InteliJ\\TCC-UNO\\Java\\res\\evaluation_default_config.properties";

    public static void main(String[] args) {
        String configPath = args.length > 0 ? args[0] : DEFAULT_CONFIG;

        EvaluationConfig config;
        try {
            config = EvaluationConfig.load(configPath);
        } catch (IOException e) {
            System.err.println("Failed to load config from '" + configPath + "': " + e.getMessage());
            System.exit(1);
            return;
        }

        numClientsGlobal = config.numClients;

        System.out.println("Loaded config: " + config);
        
        ConnectionAI[] ConnectionAIS = new ConnectionAI[config.numClients];
        Thread[] threads = new Thread[config.numClients];

        for (int i = 0; i < config.numClients; i++) {
            int port = config.startPort + i;
            String id = "Eval " + (i + 1);
            ConnectionAIS[i] = new ConnectionAI();
            ConnectionAI conn = ConnectionAIS[i];

            threads[i] = new Thread(() -> runWorker(port, id, conn, config.suites), "eval-" + id);
            threads[i].start();
        }

        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("All evaluation sessions completed.");
    }

    private static void runWorker(int port, String id, ConnectionAI ConnectionAI, List<EvaluationSuite> suites) {
        try {
            ConnectionAI.connect(port);
        } catch (IOException e) {
            throw new RuntimeException("Thread " + id + " could not connect on port " + port, e);
        }
        
        for (EvaluationSuite suite : suites) {
            runSuite(suite, id, ConnectionAI);
        }
        
        runKeepAlive(id, ConnectionAI);
    }

    private static void runSuite(EvaluationSuite suite, String id, ConnectionAI ConnectionAI) {
        int[] assignedSeeds = divideSeeds(suite.seeds, id);
        if (assignedSeeds.length == 0) return;

        System.out.println(id + ". [" + suite.name + "] Starting " +
                assignedSeeds.length + " seeds, type=" + suite.type + ".");

        switch (suite.type) {
            case EXHAUSTIVE:
                runExhaustiveSuite(suite, assignedSeeds, id, ConnectionAI);
                break;
            case FIXED:
                runFixedSuite     (suite, assignedSeeds, id, ConnectionAI);
                break;
        }
    }

    private static void runExhaustiveSuite(EvaluationSuite suite, int[] seeds,
                                           String id, ConnectionAI ConnectionAI) {
        Random rng      = new Random();
        int scoreAI     = 0;
        int scoreRandom = 0;
        int draws       = 0;

        for (int seed : seeds) {
            for (int total = 2; total <= 10; total++) {
                for (int ai = 1; ai < total; ai++) {
                    int random = total - ai;
                    GameResult result = runGame(ai, random, seed, suite.resolveRules(rng), id, ConnectionAI);
                    scoreAI     += result.aiWins;
                    scoreRandom += result.randomWins;
                    draws       += result.draws;
                }
            }
            System.out.println(id + ". [" + suite.name + "] Seed " + seed + " finished.");
        }

        printScore(suite.name, id, scoreAI, scoreRandom, draws);
    }

    private static void runFixedSuite(EvaluationSuite suite, int[] seeds,
                                      String id, ConnectionAI ConnectionAI) {
        Random rng      = new Random();
        int scoreAI     = 0;
        int scoreRandom = 0;
        int draws       = 0;

        for (int seed : seeds) {
            GameResult result = runGame(suite.aiPlayers, suite.randomPlayers, seed,
                    suite.resolveRules(rng), id, ConnectionAI);
            scoreAI     += result.aiWins;
            scoreRandom += result.randomWins;
            draws       += result.draws;
            System.out.println(id + ". [" + suite.name + "] Seed " + seed + " finished.");
        }

        printScore(suite.name, id, scoreAI, scoreRandom, draws);
    }

    private static void runKeepAlive(String id, ConnectionAI ConnectionAI) {
        boolean[] noRules = new boolean[8];
        Simulation dummy = new Simulation(0, 1, 1, ConnectionAI, noRules, id, -1);
        while (true) {
            try {
                do {
                    dummy.playTurn();
                } while (!dummy.isGameOver());
                dummy = new Simulation(0, 1, 1, ConnectionAI, noRules, id, -1);
            } catch (Exception e) {
                System.out.println(id + ". Keep-alive ended, connection closed.");
                break;
            }
        }
    }

    private static GameResult runGame(int aiPlayers, int randomPlayers, int seed,
                                      boolean[] rules, String id, ConnectionAI ConnectionAI) {
        Simulation simulation = new Simulation(0, aiPlayers, randomPlayers, ConnectionAI, rules, id, seed);
        while (!simulation.isGameOver()) {
            simulation.playTurn();
        }

        if (simulation.getWinner() instanceof PlayerAI) return GameResult.AI_WIN;
        if (simulation.getWinner() != null)             return GameResult.RANDOM_WIN;
        return GameResult.DRAW;
    }
    
    private static int[] divideSeeds(int[] seeds, String id) {
        int workerIndex = parseWorkerIndex(id);
        int totalWorkers = countWorkers();

        int start = (int) ((long) seeds.length * workerIndex / totalWorkers);
        int end   = (int) ((long) seeds.length * (workerIndex + 1) / totalWorkers);
        int[] slice = new int[end - start];
        System.arraycopy(seeds, start, slice, 0, slice.length);
        return slice;
    }

    private static int parseWorkerIndex(String id) {
        try {
            return Integer.parseInt(id.split("\\s+")[1]) - 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static volatile int numClientsGlobal = 16;

    private static int countWorkers() {
        return numClientsGlobal;
    }

    private static void printScore(String suiteName, String id,
                                   int aiWins, int randomWins, int draws) {
        System.out.println(id + ". [" + suiteName + "] Done — " +
                aiWins + " AI wins, " + randomWins + " Random wins, " + draws + " draws.");
    }

    private static final class GameResult {
        final int aiWins, randomWins, draws;

        private GameResult(int aiWins, int randomWins, int draws) {
            this.aiWins     = aiWins;
            this.randomWins = randomWins;
            this.draws      = draws;
        }

        static final GameResult AI_WIN     = new GameResult(1, 0, 0);
        static final GameResult RANDOM_WIN = new GameResult(0, 1, 0);
        static final GameResult DRAW       = new GameResult(0, 0, 1);
    }
}