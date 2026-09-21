package com.example.travel.domain.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai-match.score")
public class AiMatchScoreProperties {
    private double minimumSimilarity = 0.15;
    private double center = 0.35;
    private double scale = 0.08;
    private int floor = 55;
    private int range = 43;

    public double getMinimumSimilarity() { return minimumSimilarity; }
    public void setMinimumSimilarity(double minimumSimilarity) { this.minimumSimilarity = minimumSimilarity; }
    public double getCenter() { return center; }
    public void setCenter(double center) { this.center = center; }
    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = scale; }
    public int getFloor() { return floor; }
    public void setFloor(int floor) { this.floor = floor; }
    public int getRange() { return range; }
    public void setRange(int range) { this.range = range; }
}
