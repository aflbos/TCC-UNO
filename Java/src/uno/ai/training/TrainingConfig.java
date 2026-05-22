package uno.ai.training;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class TrainingConfig {

    public final int startPort;
    public final int numClients;
    public final List<TrainingPhase> phases;
    public final int loopStartPhase;
    public final boolean loopInfinite;

    private TrainingConfig(int startPort, int numClients, List<TrainingPhase> phases, int loopStartPhase, boolean loopInfinite) {
        this.startPort = startPort;
        this.numClients = numClients;
        this.phases = Collections.unmodifiableList(phases);
        this.loopStartPhase = loopStartPhase;
        this.loopInfinite   = loopInfinite;
    }

    public static TrainingConfig load(String path) throws IOException {
        Properties props = new Properties();

        try (InputStream in = openConfigStream(path)) {
            if (in == null) {
                throw new IOException("Config file not found: " + path);
            }
            props.load(in);
        }

        int startPort = intProp(props, "startPort", 5000);
        int numClients = intProp(props, "numClients", 16);
        int phaseCount = intProp(props, "phase.count", 0);

        if (phaseCount < 1) {
            throw new IllegalArgumentException("Config must define at least one phase (phase.count >= 1).");
        }

        List<TrainingPhase> phases = new ArrayList<>(phaseCount);
        for (int i = 1; i <= phaseCount; i++) {
            phases.add(TrainingPhase.fromProperties(props, i));
        }

        int loopStartPhase = intProp(props,  "loop.startPhase", 1);
        String loopInfiniteRaw = props.getProperty("loop.infinite");
        boolean loopInfinite = loopInfiniteRaw != null && Boolean.parseBoolean(loopInfiniteRaw.trim());

        if (loopStartPhase < 1 || loopStartPhase > phaseCount + 1) {
            throw new IllegalArgumentException("loop.startPhase (" + loopStartPhase + ") must be between 1 and " + (phaseCount + 1) + ".");
        }

        return new TrainingConfig(startPort, numClients, phases, loopStartPhase, loopInfinite);
    }

    private static InputStream openConfigStream(String path) throws IOException {
        Path requested = Paths.get(path);
        if (Files.isRegularFile(requested)) {
            return Files.newInputStream(requested);
        }

        if (!requested.isAbsolute()) {
            Path cwd = Paths.get("").toAbsolutePath();

            for (Path current = cwd; current != null; current = current.getParent()) {
                Path direct = current.resolve(requested).normalize();
                if (Files.isRegularFile(direct)) {
                    return Files.newInputStream(direct);
                }

                Path javaDir = current.resolve("Java").resolve(requested).normalize();
                if (Files.isRegularFile(javaDir)) {
                    return Files.newInputStream(javaDir);
                }
            }
        }

        return TrainingConfig.class.getClassLoader().getResourceAsStream(stripLeadingSlash(path));
    }

    private static String stripLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
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
        return "ai.training.TrainingConfig{startPort=" + startPort +
                ", numClients=" + numClients +
                ", phases=" + phases.size() +
                ", loopInfinite=" + loopInfinite +
                ", loopStartPhase=" + loopStartPhase + '}';
    }
}