package uno.ai.evaluation;

import uno.ai.RuleSpec;

import java.util.Properties;
import java.util.Random;

public class EvaluationSuite {

    public enum Type {
        EXHAUSTIVE,
        FIXED
    }

    public final String name;
    public final Type type;
    public final int[] seeds;
    public final RuleSpec[] ruleSpecs;
    public final int aiPlayers;
    public final int randomPlayers;

    public static EvaluationSuite fromProperties(Properties props, int index, java.util.Map<String, int[]> seedGroups) {
        String prefix = "suite." + index + ".";

        String name = props.getProperty(prefix + "name", "Suite " + index).trim();

        String rawType = require(props, prefix + "type");
        Type type;

        try {
            type = Type.valueOf(rawType.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unknown suite type '" + rawType + "' at " + prefix + "type. " +
                            "Valid types: EXHAUSTIVE, FIXED");
        }
        
        String seedGroupName = require(props, prefix + "seeds").trim();
        int[] seeds = seedGroups.get(seedGroupName);

        if (seeds == null) {
            throw new IllegalArgumentException(
                    "Suite " + index + " references unknown seed group '" + seedGroupName +
                            "'. Define it as: seeds." + seedGroupName + " = ...");
        }
        
        String rawRules = props.getProperty(prefix + "rules",
                "false,false,false,false,false,false,false,false");
        RuleSpec[] ruleSpecs = RuleSpec.parseArray(rawRules, prefix + "rules", 8);
        
        int aiPlayers     = intProp(props, prefix + "aiPlayers",     0);
        int randomPlayers = intProp(props, prefix + "randomPlayers", 0);

        if (type == Type.FIXED && aiPlayers + randomPlayers < 2) {
            throw new IllegalArgumentException(
                    "Suite " + index + " (FIXED) must have aiPlayers + randomPlayers >= 2.");
        }

        return new EvaluationSuite(name, type, seeds, ruleSpecs, aiPlayers, randomPlayers);
    }

    public boolean[] resolveRules(Random rng) {
        return RuleSpec.resolveAll(ruleSpecs, rng);
    }

    private EvaluationSuite(String name, Type type, int[] seeds, RuleSpec[] ruleSpecs, int aiPlayers, int randomPlayers) {
        this.name = name;
        this.type = type;
        this.seeds = seeds;
        this.ruleSpecs = ruleSpecs;
        this.aiPlayers = aiPlayers;
        this.randomPlayers = randomPlayers;
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
        return "ai.evaluation.EvaluationSuite{name='" + name + "', type=" + type +
                ", seeds=" + seeds.length + ", aiPlayers=" + aiPlayers +
                ", randomPlayers=" + randomPlayers + '}';
    }
}