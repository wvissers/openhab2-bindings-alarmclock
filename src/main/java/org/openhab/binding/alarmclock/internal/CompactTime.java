/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.internal;

import java.time.Instant;
import java.time.ZonedDateTime;

/**
 * Object to encapsulate a compact local time hours/minutes/dayofweek.
 *
 * @author Wim Vissers.
 *
 */
public class CompactTime {

    private final int hour;
    private final int minute;
    private final DayOfWeek dayOfWeek;

    /**
     * Create a local CompactTime from the system.
     */
    public CompactTime() {
        ZonedDateTime now = Instant.now().atZone(SystemHelper.getTimeZone().toZoneId());
        dayOfWeek = DayOfWeek.valueOf(now.getDayOfWeek().name());
        hour = now.getHour();
        minute = now.getMinute();
    }

    /**
     * Create a local CompactTime with only hours and minutes, but no DayOfWeek.
     * 
     * @param hour
     * @param minute
     */
    public CompactTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
        this.dayOfWeek = null;
    }

    /**
     * Return true if the time (hour:minute) of this instance is within the timeframe previous and current. The reason
     * is to make sure a switching event is not missed, even when for whatever reason to time between ticks exceeds the
     * minute.
     * 
     * @param previous the previous time. Must be non null.
     * @param current  the current time. Must be non null.
     * @return
     */
    public boolean isSwitchTime(CompactTime previous, CompactTime current) {
        return getCanonicalTime() > previous.getCanonicalTime() && getCanonicalTime() <= current.getCanonicalTime();
    }

    /**
     * Return true if this time is less than the given other time.
     * 
     * @param other the other time. Must be non null.
     * @return
     */
    public boolean isLessThan(CompactTime other) {
        return getCanonicalTime() < other.getCanonicalTime();
    }

    /**
     * Return true if this time is less than or equal to the given other time.
     * 
     * @param other the other time. Must be non null.
     * @return
     */
    public boolean isLessThanOrEqual(CompactTime other) {
        return getCanonicalTime() <= other.getCanonicalTime();
    }

    /**
     * Convert the time to a single integer value for easy handling.
     * 
     * @return hour * 100 + minute.
     */
    private int getCanonicalTime() {
        return hour * 100 + minute;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

}
