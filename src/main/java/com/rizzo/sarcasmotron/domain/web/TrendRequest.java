package com.rizzo.sarcasmotron.domain.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.elasticsearch.common.joda.time.*;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogram;

import java.beans.Transient;

public class TrendRequest {

    private String user;

    private String intervalExpression;

    private String periodExpression;

    public String getUser() {
        return user;
    }

    public TrendRequest setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPeriodExpression() {
        return periodExpression;
    }

    public TrendRequest setPeriodExpression(String periodExpression) {
        this.periodExpression = periodExpression;
        return this;
    }

    @Transient
    @JsonIgnore
    public ReadablePeriod getPeriod() {
        ReadablePeriod period;
        char periodUnit = periodExpression.charAt(periodExpression.length() - 1);
        int periodInt = Integer.parseInt(periodExpression.substring(0, periodExpression.length() - 1));
        switch (periodUnit) {
            case 'w':
                period = Weeks.weeks(periodInt);
                break;
            case 'd':
                period = Days.days(periodInt);
                break;
            case 'h':
                period = Hours.hours(periodInt);
                break;
            case 'm':
                period = Minutes.minutes(periodInt);
                break;
            case 's':
                period = Seconds.seconds(periodInt);
                break;
            default:
                throw new IllegalArgumentException("Invalid period unit '" + periodUnit + "'");
        }
        return period;
    }

    public String getIntervalExpression() {
        return intervalExpression;
    }

    public TrendRequest setIntervalExpression(String intervalExpression) {
        this.intervalExpression = intervalExpression;
        return this;
    }

    @Transient
    @JsonIgnore
    public DateHistogram.Interval getInterval() {
        return new DateHistogram.Interval(intervalExpression);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("intervalExpression", intervalExpression)
                .append("periodExpression", periodExpression)
                .toString();
    }

}
