package uno.ai.training;

import uno.ai.RuleSpec;

import java.util.Properties;
import java.util.Random;

public class TrainingPhase {

    public enum Type {
        FIXED,
        RANDOM_AI,
        MIXED,
        EXHAUSTIVE
    }

    public final Type type;
    public final RuleSpec[] ruleSpecs;
    public final int games;
    public final int aiPlayers;
    public final int randomPlayers;
    public final int aiPlayersMin;
    public final int aiPlayersMax;
    public final int iterations;
    
    public static TrainingPhase fromProperties(Properties props, int index) {
        String prefix = "phase." + index + ".";
        
        String rawType = require(props, prefix + "type");
        Type type;

        try {
            type = Type.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown phase type '" + rawType + "' at " + prefix + "type. " +
                            "Valid types: FIXED, RANDOM_AI, MIXED, EXHAUSTIVE");
        }
        
        String rawRules = props.getProperty(prefix + "rules",
                "false,false,false,false,false,false,false,false");
        RuleSpec[] ruleSpecs = parseRuleSpecs(rawRules, prefix + "rules");
        
        int games = intProp(props, prefix + "games", 0);
        int aiPlayers = intProp(props, prefix + "aiPlayers", 0);
        int randomPlayers = intProp(props, prefix + "randomPlayers", 0);
        int aiPlayersMin = intProp(props, prefix + "aiPlayersMin", 2);
        int aiPlayersMax = intProp(props, prefix + "aiPlayersMax", 10);
        int iterations = intProp(props, prefix + "iterations", 1);

        return new TrainingPhase(type, ruleSpecs, games,
                aiPlayers, randomPlayers, aiPlayersMin, aiPlayersMax, iterations);
    }

    public boolean[] resolveRules(Random rng) {
        return RuleSpec.resolveAll(ruleSpecs, rng);
    }

    private TrainingPhase(Type type, RuleSpec[] ruleSpecs, int games, int aiPlayers, int randomPlayers, int aiPlayersMin, int aiPlayersMax, int iterations) {
        this.type = type;
        this.ruleSpecs = ruleSpecs;
        this.games = games;
        this.aiPlayers = aiPlayers;
        this.randomPlayers = randomPlayers;
        this.aiPlayersMin = aiPlayersMin;
        this.aiPlayersMax = aiPlayersMax;
        this.iterations = iterations;
    }

    private static RuleSpec[] parseRuleSpecs(String raw, String key) {
        String[] parts = raw.split(",");

        if (parts.length != 8) {
            throw new IllegalArgumentException("'" + key + "' must have exactly 8 comma-separated values, got " + parts.length);
        }

        RuleSpec[] specs = new RuleSpec[8];

        for (int i = 0; i < 8; i++) {
            specs[i] = RuleSpec.parse(parts[i]);
        }

        return specs;
    }

    private static String require(Properties props, String key) {
        String v = props.getProperty(key);
        if (v == null) throw new IllegalArgumentException("Missing required config key: " + key);
        return v;
    }

    private static int intProp(Properties props, String key, int defaultValue) {
        String v = props.getProperty(key);
        if (v == null) return defaultValue;

        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected integer for '" + key + "', got: " + v);
        }
    }

    @Override
    public String toString() {
        return "ai.training.TrainingPhase{type=" + type + ", games=" + games +
                ", aiPlayers=" + aiPlayers + ", randomPlayers=" + randomPlayers +
                ", aiPlayersMin=" + aiPlayersMin + ", aiPlayersMax=" + aiPlayersMax +
                ", iterations=" + iterations + '}';
    }
}