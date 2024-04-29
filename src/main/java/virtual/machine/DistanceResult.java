package virtual.machine;

import java.util.Map;

public class DistanceResult {
    private Map<String, Integer> distances;
    private Map<String, String> nextHops;

    public DistanceResult(Map<String, Integer> distances, Map<String, String> nextHops) {
        this.distances = distances;
        this.nextHops = nextHops;
    }

    public Map<String, Integer> getDistances() {
        return distances;
    }

    public Map<String, String> getNextHops() {
        return nextHops;
    }
}
