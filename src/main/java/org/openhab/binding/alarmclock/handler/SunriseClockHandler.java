/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.handler;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.openhab.binding.alarmclock.internal.SunriseSunset;

/**
 * This clock calculates the sunrise and sunset times based on the
 * location and current date and timezone.
 *
 * @author wim
 *
 */
public class SunriseClockHandler extends AbstractSunClockHandler {

    private int offOffset;

    public SunriseClockHandler(Thing thing) {
        super(thing);
    }

    /**
     * Override in subclasses
     */
    @Override
    protected void hourTick() {
        super.hourTick();
        SunriseSunset sunriseSunset = getSunriseSunset();
        Date sunrise = sunriseSunset.getSunrise();

        Calendar calendar = Calendar.getInstance(); // creates a new calendar instance
        calendar.setTime(sunrise); // assigns calendar to given date
        calendar.add(Calendar.MINUTE, offOffset);
        offHour = calendar.get(Calendar.HOUR_OF_DAY); // gets hour in 24h format
        offMinute = calendar.get(Calendar.MINUTE);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing SunriseClock handler.");
        super.initialize();

        Configuration config = getThing().getConfiguration();

        onHour = ((BigDecimal) config.get("onHour")).intValue();
        onMinute = ((BigDecimal) config.get("onMinute")).intValue();
        offOffset = ((BigDecimal) config.get("offOffset")).intValue();

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);
        updateValues();
        hourTick();

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
        startAutomaticRefresh();
    }

}
