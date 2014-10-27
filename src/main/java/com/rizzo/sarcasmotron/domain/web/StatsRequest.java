package com.rizzo.sarcasmotron.domain.web;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.elasticsearch.common.joda.time.*;

import java.beans.Transient;

public class StatsRequest {

    private String user;

    private String periodExpression;

    public String getUser() {
        return user;
    }

    public StatsRequest setUser(String user) {
        this.user = user;
        return this;
    }

    public String getPeriodExpression() {
        return periodExpression;
    }

    public StatsRequest setPeriodExpression(String periodExpression) {
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

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("user", user)
                .append("periodExpression", periodExpression)
                .toString();
    }

}
