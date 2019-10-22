/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.internal;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The System Sun clock, initialized with system location and time zone.
 *
 * @author Wim Vissers - Initial contribution
 */
public class SystemSunClock {

    private static final long REFRESH_INTERVAL = 1000 * 3600 * 6;
    private SunriseSunset sunriseSunset;
    private Date lastInit;
    private PointType location;
    private TimeZone timeZone;
    private StringType sunrise;
    private StringType sunset;
    private final Logger logger = LoggerFactory.getLogger(SystemSunClock.class);

    private SystemSunClock() {
        reCalculate();
    }

    private static class SingletonHelper {
        private static final SystemSunClock INSTANCE = new SystemSunClock();
    }

    /**
     * Get the singleton system sun clock.
     *
     * @return the system sun clock.
     */
    public static SystemSunClock getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Reinitialize the clock when the refresh interval is exceeded or forced
     * by change of location or time zone.
     */
    public synchronized void reCalculate() {

        // Check if the time zone is changed
        TimeZone newTimeZone = SystemHelper.getTimeZone();
        boolean force = timeZone == null || !newTimeZone.getDisplayName().equals(timeZone.getDisplayName());
        timeZone = newTimeZone;

        // Check if the system location changed
        PointType newLocation = SystemHelper.getLocation();
        force = force || location == null || newLocation.distanceFrom(location).intValue() > 100;
        location = newLocation;

        // Current date and time
        Date now = Calendar.getInstance(timeZone).getTime();

        if (force || sunriseSunset == null || (now.getTime() - lastInit.getTime()) > REFRESH_INTERVAL) {
            sunriseSunset = new SunriseSunset(location.getLatitude().doubleValue(),
                    location.getLongitude().doubleValue(), now, 0);
            lastInit = now;
            SunriseSunset sunriseSunset = getSunriseSunset();
            Date currentSunrise = sunriseSunset.getSunrise();

            Calendar calendar = Calendar.getInstance(timeZone); // creates a new calendar instance
            calendar.setTime(currentSunrise); // assigns calendar to given date
            sunrise = SystemHelper.formatTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

            Date currentSunset = sunriseSunset.getSunset();
            calendar.setTime(currentSunset); // assigns calendar to given date
            sunset = SystemHelper.formatTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));

            // Log new sunrise/sunset time
            logger.info("Reinit SystemSunClock, sunrise: {},  sunset: {}.", sunrise, sunset);
        }
    }

    /**
     * Get the system SunriseSunset instance.
     *
     * @return the single instance of SunriseSunset.
     */
    public SunriseSunset getSunriseSunset() {
        return sunriseSunset;
    }

    public StringType getSunrise() {
        return sunrise;
    }

    public StringType getSunset() {
        return sunset;
    }
}
