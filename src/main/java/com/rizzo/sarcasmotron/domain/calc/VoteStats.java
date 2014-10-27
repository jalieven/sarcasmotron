package com.rizzo.sarcasmotron.domain.calc;

import org.apache.commons.lang.builder.ToStringBuilder;

public class VoteStats {

    private Long count;

    private Double min;

    private Double max;

    private Double sum;

    public Long getCount() {
        return count;
    }

    public VoteStats setCount(Long count) {
        this.count = count;
        return this;
    }

    public Double getMin() {
        return min;
    }

    public VoteStats setMin(Double min) {
        this.min = min;
        return this;
    }

    public Double getMax() {
        return max;
    }

    public VoteStats setMax(Double max) {
        this.max = max;
        return this;
    }

    public Double getSum() {
        return sum;
    }

    public VoteStats setSum(Double sum) {
        this.sum = sum;
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("count", count)
                .append("min", min)
                .append("max", max)
                .append("sum", sum)
                .toString();
    }

}
