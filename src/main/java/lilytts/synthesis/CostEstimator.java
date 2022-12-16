package lilytts.synthesis;

public interface CostEstimator {
    double getEstimatedCost(String ssml);
}
