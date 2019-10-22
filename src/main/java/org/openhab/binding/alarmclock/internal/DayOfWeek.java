/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.internal;

import java.text.DateFormatSymbols;
import java.util.Locale;

/**
 * The DayOfWeek enum is used for setting the alarm clock auto enable/disable
 * dependent of the day of the week.
 *
 * @author Wim Vissers - Initial contribution
 */
public enum DayOfWeek {

    SUNDAY,
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY;

    private static Locale locale;
    private static DateFormatSymbols dateFormatSymbols;

    /**
     * Set a (new) Locale.
     *
     * @param newLocale the new Locale to use.
     */
    public static void setLocale(Locale newLocale) {
        if (locale == null || !newLocale.toString().equalsIgnoreCase(locale.toString())) {
            locale = newLocale;
            dateFormatSymbols = new DateFormatSymbols(locale);
        }
    }

    /**
     * Return the configuration parameter key. It is used
     * to retrieve the parameter from the thing's config
     * object.
     *
     * @return the config parameter key.
     */
    public String getConfigKey() {
        return name().toLowerCase();
    }

    /**
     * The Calendar value is SUNDAY=1, SATURDAY=7.
     *
     * @return the value to use with the Calendar class.
     */
    public int getCalendarValue() {
        return ordinal() + 1;
    }

    /**
     * Initialize from the system Locale.
     */
    private void init() {
        if (dateFormatSymbols == null) {
            setLocale(SystemHelper.getLocale());
        }
    }

    /**
     * Get the short name.
     *
     * @return the short name.
     */
    public String getShortName() {
        init();
        return dateFormatSymbols.getShortWeekdays()[getCalendarValue()];
    }

    /**
     * Get the long name.
     *
     * @return the long name.
     */
    public String getName() {
        init();
        return dateFormatSymbols.getWeekdays()[getCalendarValue()];
    }

    /**
     * Get the DayOfWeek constant from the Calendar value.
     *
     * @param value the Calendar value.
     * @return the corresponding DayOfWeek constant.
     */
    public static DayOfWeek getFromCalendar(int value) {
        for (DayOfWeek dow : DayOfWeek.values()) {
            if (dow.getCalendarValue() == value) {
                return dow;
            }
        }
        throw new IllegalArgumentException("No DayOfWeek for value " + value);
    }
}
