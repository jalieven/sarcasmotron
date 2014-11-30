package com.rizzo.sarcasmotron.domain.elasticsearch;

import org.apache.commons.lang.builder.ToStringBuilder;

public class ESSentiment {

    private Type type;

    private Double value;

    public Type getType() {
        return type;
    }

    public ESSentiment setType(Type type) {
        this.type = type;
        return this;
    }

    public Double getValue() {
        return value;
    }

    public ESSentiment setValue(Double value) {
        this.value = value;
        return this;
    }

    public Type setTypeByLabel(String sentimentLabel) {
        return Type.fromLabel(sentimentLabel);
    }

    public enum Type {
        POSITIVE("pos"),
        NEGATIVE("neg"),
        NEUTRAL("neutral");

        private String label;

        Type(String label) {
            this.label = label;
        }

        public static Type fromLabel(String label) {
            for (Type type : Type.values()) {
                if(type.label.equals(label)) {
                    return type;
                }
            }
            return null;
        }

        public String getLabel() {
            return label;
        }
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", type)
                .append("value", value)
                .toString();
    }
}

