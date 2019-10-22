/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.handler;

import static org.openhab.binding.alarmclock.AlarmClockBindingConstants.*;
import static org.openhab.binding.alarmclock.internal.Constants.PROPERTY_LOCATION;

import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.alarmclock.internal.ClockManager;
import org.openhab.binding.alarmclock.internal.ClockManager.Event;
import org.openhab.binding.alarmclock.internal.SunriseSunset;
import org.openhab.binding.alarmclock.internal.SystemHelper;
import org.openhab.binding.alarmclock.internal.SystemSunClock;

/**
 * This clock calculates the sunrise and sunset times based on the
 * location and current date and timezone.
 *
 * @author wim
 *
 */
public class AbstractSunClockHandler extends AbstractClockHandler {

    // Channel UIDs
    private final ChannelUID channelSunrise;
    private final ChannelUID channelSunset;

    public AbstractSunClockHandler(Thing thing) {
        super(thing);
        channelSunrise = new ChannelUID(thing.getUID(), CHANNEL_SUNRISE);
        channelSunset = new ChannelUID(thing.getUID(), CHANNEL_SUNSET);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (super.handleBaseCommand(channelUID, command)) {
            return;
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

    /**
     * Factory method for sunrise/sunset clock. If the system location is used,
     * the singleton System sunrise/sunset clock is used. Otherwise, a new
     * instance is created.
     *
     * @return
     */
    protected SunriseSunset getSunriseSunset() {
        return SystemSunClock.getInstance().getSunriseSunset();
    }

    /**
     * This method is called every hour. Override in subclasses if applicable.
     */
    protected void hourTick() {
        SystemSunClock.getInstance().reCalculate();
    }
    
    /**
     * Initialize the time triggers by registering the basic event handlers with the clock manager.
     */
    protected void initEventHandlers() {
        ClockManager clockManager = ClockManager.getInstance();
        clockManager.on(Event.HOUR_TICK, (previous, current) -> {
            hourTick();
        }, this);
        super.initEventHandlers();
    }
    
    /**
     * Update values and properties.
     */
    @SuppressWarnings("null")
    @Override
    protected void updateProperties() {
        super.updateProperties();
        Thing thing = getThing();
        PointType newLoc = SystemHelper.getLocation();
        String currentLoc = thing.getProperties().get(PROPERTY_LOCATION);
        if (currentLoc == null || !currentLoc.equals(newLoc.toString())) {
            thing.setProperty(PROPERTY_LOCATION, newLoc.toString());
            SystemSunClock.getInstance().reCalculate();
            refreshState();
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        // Make sure location is reininitialized
        getThing().setProperty(PROPERTY_LOCATION, "");
    }

    /**
     * Refresh the state of channels that may have changed by (re-)initialization.
     */
    @Override
    protected void refreshState() {
        super.refreshState();
        updateState(channelOnTime, SystemHelper.formatTime(onHour, onMinute));
        updateState(channelOffTime, SystemHelper.formatTime(offHour, offMinute));
        updateState(channelSunrise, SystemSunClock.getInstance().getSunrise());
        updateState(channelSunset, SystemSunClock.getInstance().getSunset());
    }

}
