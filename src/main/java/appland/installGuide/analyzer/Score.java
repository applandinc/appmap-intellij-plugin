package appland.installGuide.analyzer;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Score {
    Bad(0), Okay(1), Good(2);

    private final int scoreValue;

    public int getScoreValue() {
        return scoreValue;
    }

    public int getOverallScoreValue() {
        return scoreValue + 1;
    }
}
