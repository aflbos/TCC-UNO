package uno.ai;

import java.util.Random;

public enum RuleSpec {

    TRUE, FALSE, RANDOM;

    public boolean resolve(Random rng) {
        switch (this) {
            case TRUE: return true;
            case FALSE: return false;
            case RANDOM: return rng.nextBoolean();
            default: throw new RuntimeException("How did I get here ?");
        }
    }

    public static RuleSpec parse(String raw) {
        switch (raw.trim().toLowerCase()) {
            case "true": return TRUE;
            case "false": return FALSE;
            case "random": return RANDOM;
            default: throw new IllegalArgumentException("Invalid rule value '" + raw + "'. Must be true, false, or random.");
        }
    }

    public static RuleSpec[] parseArray(String raw, String configKey, int expectedLength) {
        String[] parts = raw.split(",");

        if (parts.length != expectedLength) {
            throw new IllegalArgumentException(
                    "'" + configKey + "' must have exactly " + expectedLength +
                            " comma-separated values, got " + parts.length + ".");
        }
        RuleSpec[] specs = new RuleSpec[expectedLength];
        for (int i = 0; i < expectedLength; i++) {
            specs[i] = parse(parts[i]);
        }
        return specs;
    }

    public static boolean[] resolveAll(RuleSpec[] specs, Random rng) {
        boolean[] rules = new boolean[specs.length];
        for (int i = 0; i < specs.length; i++) {
            rules[i] = specs[i].resolve(rng);
        }
        return rules;
    }
}