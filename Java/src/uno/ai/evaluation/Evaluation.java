package uno.ai.evaluation;

import uno.ai.network.ConnectionAI;
import uno.game.players.PlayerAI;
import uno.game.players.PlayerHerusitic;
import uno.game.engine.Simulation;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Evaluation {

    private static final String DEFAULT_CONFIG = "Java/res/evaluation profiles/evaluation_heuristic_house_rules_4p_100.properties";

    // Contadores globais thread-safe para agregar os resultados de todas as threads
    private static final AtomicInteger globalAiWins = new AtomicInteger(0);
    private static final AtomicInteger globalHeuristicWins = new AtomicInteger(0);
    private static final AtomicInteger globalRandomWins = new AtomicInteger(0);
    private static final AtomicInteger globalDraws = new AtomicInteger(0);

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

        // Aguarda todas as threads finalizarem
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        System.out.println("All evaluation sessions completed.");

        // Exibe os resultados globais no console ao final da execução
        printGlobalScores();
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
        int scoreHeuristic = 0;
        int scoreRandom = 0;
        int draws       = 0;

        for (int seed : seeds) {
            for (int total = 2; total <= 10; total++) {
                for (int ai = 1; ai < total; ai++) {
                    for (int heuristic = 0; heuristic <= total - ai; heuristic++) {
                        int random = total - ai - heuristic;
                        GameResult result = runGame(ai, heuristic, random, seed, suite.resolveRules(rng), id, ConnectionAI);
                        scoreAI        += result.aiWins;
                        scoreHeuristic += result.heuristicWins;
                        scoreRandom    += result.randomWins;
                        draws          += result.draws;
                    }
                }
            }
            System.out.println(id + ". [" + suite.name + "] Seed " + seed + " finished.");
        }

        printScore(suite.name, id, scoreAI, scoreHeuristic, scoreRandom, draws);
    }

    private static void runFixedSuite(EvaluationSuite suite, int[] seeds,
                                      String id, ConnectionAI ConnectionAI) {
        Random rng      = new Random();
        int scoreAI     = 0;
        int scoreHeuristic = 0;
        int scoreRandom = 0;
        int draws       = 0;

        for (int seed : seeds) {
            GameResult result = runGame(suite.aiPlayers, suite.heuristicPlayers, suite.randomPlayers, seed,
                    suite.resolveRules(rng), id, ConnectionAI);
            scoreAI        += result.aiWins;
            scoreHeuristic += result.heuristicWins;
            scoreRandom    += result.randomWins;
            draws          += result.draws;
            System.out.println(id + ". [" + suite.name + "] Seed " + seed + " finished.");
        }

        printScore(suite.name, id, scoreAI, scoreHeuristic, scoreRandom, draws);
    }

    private static void runKeepAlive(String id, ConnectionAI ConnectionAI) {
        boolean[] noRules = new boolean[8];
        Simulation dummy = new Simulation(0, 1, 0, 1, ConnectionAI, noRules, id, -1);
        while (true) {
            try {
                do {
                    dummy.playTurn();
                } while (!dummy.isGameOver());
                dummy = new Simulation(0, 1, 0, 1, ConnectionAI, noRules, id, -1);
            } catch (Exception e) {
                System.out.println(id + ". Keep-alive ended, connection closed.");
                break;
            }
        }
    }

    private static GameResult runGame(int aiPlayers, int heuristicPlayers, int randomPlayers, int seed,
                                      boolean[] rules, String id, ConnectionAI ConnectionAI) {
        Simulation simulation = new Simulation(0, aiPlayers, heuristicPlayers, randomPlayers, ConnectionAI, rules, id, seed);
        while (!simulation.isGameOver()) {
            simulation.playTurn();
        }

        if (simulation.getWinner() instanceof PlayerAI) return GameResult.AI_WIN;
        if (simulation.getWinner() instanceof PlayerHerusitic) return GameResult.HEURISTIC_WIN;
        if (simulation.getWinner() != null) return GameResult.RANDOM_WIN;
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
                                   int aiWins, int heuristicWins, int randomWins, int draws) {

        // Adiciona os resultados locais aos contadores globais
        globalAiWins.addAndGet(aiWins);
        globalHeuristicWins.addAndGet(heuristicWins);
        globalRandomWins.addAndGet(randomWins);
        globalDraws.addAndGet(draws);

        int totalGames = aiWins + heuristicWins + randomWins + draws;
        double aiWinRate = totalGames > 0 ? ((double) aiWins / totalGames) * 100.0 : 0.0;

        System.out.printf("%s. [%s] Done — %d AI wins (%.2f%% win rate), %d Heuristic wins, %d Random wins, %d draws.%n",
                id, suiteName, aiWins, aiWinRate, heuristicWins, randomWins, draws);
    }

    // Método que apresenta as estatísticas e as formata na visão de Vitórias, Derrotas e Empates
    private static void printGlobalScores() {
        int aiWins = globalAiWins.get();
        int heuristicWins = globalHeuristicWins.get();
        int randomWins = globalRandomWins.get();
        int draws = globalDraws.get();

        // Derrotas da IA equivalem às vitórias da Heurística somadas às vitórias Aleatórias
        int losses = heuristicWins + randomWins;
        int totalGames = aiWins + losses + draws;

        System.out.println("\n========================================");
        System.out.println("          RESULTADOS GLOBAIS            ");
        System.out.println("========================================");
        System.out.printf("Total de partidas jogadas: %d%n", totalGames);

        if (totalGames > 0) {
            System.out.printf("Vitórias (IA): %d (%.2f%%)%n", aiWins, (aiWins * 100.0) / totalGames);
            System.out.printf("Derrotas:      %d (%.2f%%)%n", losses, (losses * 100.0) / totalGames);
            System.out.printf("  ↳ Heurística:%d%n", heuristicWins);
            System.out.printf("  ↳ Aleatório: %d%n", randomWins);
            System.out.printf("Empates:       %d (%.2f%%)%n", draws, (draws * 100.0) / totalGames);
        }
        System.out.println("========================================\n");
    }

    private static final class GameResult {
        final int aiWins, heuristicWins, randomWins, draws;

        private GameResult(int aiWins, int heuristicWins, int randomWins, int draws) {
            this.aiWins        = aiWins;
            this.heuristicWins = heuristicWins;
            this.randomWins    = randomWins;
            this.draws         = draws;
        }

        static final GameResult AI_WIN        = new GameResult(1, 0, 0, 0);
        static final GameResult HEURISTIC_WIN = new GameResult(0, 1, 0, 0);
        static final GameResult RANDOM_WIN    = new GameResult(0, 0, 1, 0);
        static final GameResult DRAW          = new GameResult(0, 0, 0, 1);
    }
}