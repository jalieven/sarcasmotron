package com.rizzo.sarcasmotron.sentiment;

public class SentimentProbability {

    private String label;

    private Probability probability;

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Probability getProbability() {
        return probability;
    }

    public void setProbability(Probability probability) {
        this.probability = probability;
    }

    public Double calculateSentiment() {
        switch (label) {
            case "neutral":
                return this.probability.getNeutral();
            case "pos":
                return this.probability.getPos();
            case "neg":
                return this.probability.getNeg();
            default:
                return -1D;
        }
    }
}
