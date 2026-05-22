package uno.ai.evaluation;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class EvaluationConfig {

    public final int startPort;
    public final int numClients;
    public final List<EvaluationSuite> suites;

    private EvaluationConfig(int startPort, int numClients, List<EvaluationSuite> suites) {
        this.startPort = startPort;
        this.numClients = numClients;
        this.suites = Collections.unmodifiableList(suites);
    }

    public static EvaluationConfig load(String path) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        }

        int startPort  = intProp(props, "startPort",  6000);
        int numClients = intProp(props, "numClients", 16);
        
        Map<String, int[]> seedGroups = parseSeedGroups(props);
        if (seedGroups.isEmpty()) {
            throw new IllegalArgumentException("Config must define at least one seed group (e.g. seeds.standard = 1,2,3,...).");
        }

        int suiteCount = intProp(props, "suite.count", 0);

        if (suiteCount < 1) {
            throw new IllegalArgumentException("Config must define at least one suite (suite.count >= 1).");
        }

        List<EvaluationSuite> suites = new ArrayList<>(suiteCount);
        for (int i = 1; i <= suiteCount; i++) {
            suites.add(EvaluationSuite.fromProperties(props, i, seedGroups));
        }

        return new EvaluationConfig(startPort, numClients, suites);
    }

    private static Map<String, int[]> parseSeedGroups(Properties props) {
        Map<String, int[]> groups = new LinkedHashMap<>();

        for (String key : props.stringPropertyNames()) {
            if (!key.startsWith("seeds.")) continue;
            String groupName = key.substring("seeds.".length());
            String raw = props.getProperty(key).replaceAll("\\s+", "");
            String[] parts = raw.split(",");
            int[] seeds = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    seeds[i] = Integer.parseInt(parts[i].trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(
                            "Non-integer value in seed group '" + groupName + "': " + parts[i]);
                }
            }
            groups.put(groupName, seeds);
        }

        return groups;
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
        return "ai.evaluation.EvaluationConfig{startPort=" + startPort +
                ", numClients=" + numClients +
                ", suites=" + suites.size() + '}';
    }
}