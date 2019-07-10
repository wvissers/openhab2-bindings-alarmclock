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
import static org.openhab.binding.alarmclock.internal.Constants.*;

import java.util.Calendar;
import java.util.EnumSet;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.alarmclock.internal.DayOfWeek;
import org.openhab.binding.alarmclock.internal.SystemHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AlarmClockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Wim Vissers - Initial contribution
 */
public abstract class AbstractClockHandler extends BaseThingHandler {

    // Time of switching on.
    protected int onHour;
    protected int onMinute;

    // Time of switching off.
    protected int offHour;
    protected int offMinute;

    // Current time.
    private int currentHour;
    private int currentMinute;
    private int currentDayOfWeek;

    // Enabled days of week.
    private final EnumSet<DayOfWeek> daysOfWeek;

    // Current status.
    protected OnOffType status;
    protected OnOffType enabled;

    // Channel UIDs.
    protected final ChannelUID channelOnTime;
    protected final ChannelUID channelOffTime;
    protected final ChannelUID channelTime;
    protected final ChannelUID channelDayEnabled;
    protected final ChannelUID channelDayOfWeek;
    protected final ChannelUID channelDays;

    // Init delay for startup.
    private int initDelay;

    protected Logger logger = LoggerFactory.getLogger(AlarmClockHandler.class);

    // Scheduler
    ScheduledFuture<?> refreshJob;

    /**
     * There is no default constructor. We have to define a
     * constructor with Thing object as parameter.
     *
     * @param thing
     */
    public AbstractClockHandler(Thing thing) {
        super(thing);
        status = OnOffType.OFF;
        enabled = OnOffType.ON;
        initDelay = 2;
        daysOfWeek = EnumSet.allOf(DayOfWeek.class);
        currentDayOfWeek = 1; // Init to valid day
        channelTime = new ChannelUID(getThing().getUID(), CHANNEL_TIME);
        channelOnTime = new ChannelUID(thing.getUID(), CHANNEL_ONTIME);
        channelOffTime = new ChannelUID(thing.getUID(), CHANNEL_OFFTIME);
        channelDayEnabled = new ChannelUID(thing.getUID(), CHANNEL_DAYENABLED);
        channelDayOfWeek = new ChannelUID(thing.getUID(), CHANNEL_DAYOFWEEK);
        channelDays = new ChannelUID(thing.getUID(), CHANNEL_DAYS);
    }

    protected boolean handleBaseCommand(ChannelUID channelUID, Command command) {
        boolean result = false;
        if (command instanceof OnOffType) {
            result = true;
            OnOffType xcommand = (OnOffType) command;
            switch (channelUID.getId()) {
                case CHANNEL_DAYENABLED:
                    DayOfWeek dow = DayOfWeek.getFromCalendar(currentDayOfWeek);
                    if (xcommand.equals(OnOffType.ON)) {
                        daysOfWeek.add(dow);
                    } else {
                        daysOfWeek.remove(dow);
                    }
                    refreshState();
                    break;
                case CHANNEL_STATUS:
                    if (!status.equals(xcommand)) {
                        status = xcommand;
                        updateState(channelUID, status);
                        triggerChannel(new ChannelUID(thing.getUID(), CHANNEL_TRIGGERED), status.toString());
                    }
                    break;
                case CHANNEL_ENABLED:
                    if (!enabled.equals(xcommand)) {
                        enabled = xcommand;
                        updateState(channelUID, enabled);
                        if (enabled.equals(OnOffType.ON)) {
                            startAutomaticRefresh();
                        } else {
                            stopAutomaticRefresh();
                        }
                    }
                    break;
                default:
                    result = false;
                    break;
            }
        } else if (command instanceof Number || command instanceof RefreshType) {
            switch (channelUID.getId()) {
                case CHANNEL_TIME:
                    updateState(channelUID, getNowString());
                    break;
                case CHANNEL_STATUS:
                    updateState(channelUID, status);
                    break;
                case CHANNEL_ENABLED:
                    updateState(channelUID, enabled);
                    break;
                case CHANNEL_DAYENABLED:
                    updateState(channelUID, getDayEnabled());
                    break;
                default:
            }
            refreshState();
        }
        if (!result) {
            logger.debug("Command not handled by handleBaseCommand: {}", channelUID.getId());
        }
        return result;
    }

    /**
     * Get current time as String.
     *
     * @return
     */
    protected StringType getNowString() {
        return SystemHelper.formatTime(currentHour, currentMinute);
    }

    /**
     * Determine if today the alarm clock is enabled.
     *
     * @return true if today is enabled.
     */
    protected boolean isDayEnabled() {
        return daysOfWeek.contains(DayOfWeek.getFromCalendar(currentDayOfWeek));
    }

    /**
     * Return the state of today enabled as OnOffType.
     *
     * @return ON when today is enabled, otherwise OFF.
     */
    protected OnOffType getDayEnabled() {
        return isDayEnabled() ? OnOffType.ON : OnOffType.OFF;
    }

    /**
     * Determine initial state. Will be delayed to avoid race
     * condition at startup.
     */
    private void initState() {
        if (initDelay != 0) {
            initDelay--;
            if (initDelay == 0) {
                // Determine initial state by comparing minutes since midnight
                Calendar now = Calendar.getInstance(SystemHelper.getTimeZone());
                int time = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
                int onTime = onHour * 60 + onMinute;
                int offTime = offHour * 60 + offMinute;
                if (onTime < offTime) {
                    status = (onTime < time) && (offTime > time) ? OnOffType.ON : OnOffType.OFF;
                } else {
                    status = (offTime < time) && (onTime > time) ? OnOffType.OFF : OnOffType.ON;
                }
                updateState(new ChannelUID(getThing().getUID(), CHANNEL_STATUS), status);
            }
        }
    }

    /**
     * Check on and off times with current time. If the status
     * changed from on to off or vice versa, return true.
     *
     * @return
     */
    private boolean updateAlarmStatus() {
        boolean result = false;
        if (initDelay != 0) {
            initState();
        }
        if (isDayEnabled()) {
            if (currentHour == onHour && currentMinute == onMinute && status.equals(OnOffType.OFF)) {
                status = OnOffType.ON;
                result = true;
            }
            if (currentHour == offHour && currentMinute == offMinute && status.equals(OnOffType.ON)) {
                status = OnOffType.OFF;
                result = true;
            }
        }
        return result;
    }

    @Override
    public void initialize() {
        logger.debug("Initializing AbstractClock handler.");
        Configuration config = getThing().getConfiguration();

        // Configure days of week to enable the clock. The
        // daysOfWeek is initialized with all days. By
        // default, all days are enabled.
        daysOfWeek.addAll(EnumSet.allOf(DayOfWeek.class));
        for (DayOfWeek dow : DayOfWeek.values()) {
            Object cfgParam = config.get(dow.getConfigKey());
            if (cfgParam != null && !(Boolean) cfgParam) {
                daysOfWeek.remove(dow);
            }
        }

        // 'Unset' properties to reinitialize
        getThing().setProperty(PROPERTY_LOCALE, "");
        getThing().setProperty(PROPERTY_TIMEZONE, "");

        // Set/update the properties
        // updateValues();
    }

    @SuppressWarnings("null")
    protected void updateValues() {
        // Set/update the things properties
        Thing thing = getThing();
        thing.setProperty(Thing.PROPERTY_VENDOR, "DWG software");

        // Check for changes in TimeZone
        String timeZone = SystemHelper.getTimeZone().getDisplayName();
        String currentTimeZone = thing.getProperties().get(PROPERTY_TIMEZONE);
        if (currentTimeZone == null || !currentTimeZone.equals((timeZone))) {
            thing.setProperty(PROPERTY_TIMEZONE, SystemHelper.getTimeZone().getDisplayName());
        }

        // Check for changes in locale
        String locale = SystemHelper.getLocale().toString();
        String currentLocale = thing.getProperties().get(PROPERTY_LOCALE);
        if (currentLocale == null || !currentLocale.equals(locale)) {
            // New Locale or Locale changed
            thing.setProperty(PROPERTY_LOCALE, SystemHelper.getLocale().toString());

            // Update DayOfWeek
            DayOfWeek.setLocale(SystemHelper.getLocale());
        }
    }

    /**
     * Override in subclasses
     */
    protected void hourTick() {
    }

    /**
     * Refresh the state of channels that may have changed by
     * (re-)initialization.
     */
    protected void refreshState() {
        // Update days
        StringBuilder b = new StringBuilder();
        for (DayOfWeek dow : DayOfWeek.values()) {
            if (daysOfWeek.contains(dow)) {
                b.append(b.length() != 0 ? ", " : "").append(dow.getShortName());
            }
        }
        updateState(channelDays, new StringType(b.toString()));
        updateState(channelDayEnabled, getDayEnabled());
    }

    /**
     * Check every 60 seconds if one of the alarm times is reached.
     */
    protected void startAutomaticRefresh() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    Calendar now = Calendar.getInstance(SystemHelper.getTimeZone());
                    currentHour = now.get(Calendar.HOUR_OF_DAY);
                    currentMinute = now.get(Calendar.MINUTE);
                    currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK);
                    if (updateAlarmStatus()) {
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_STATUS), status);
                        triggerChannel(new ChannelUID(thing.getUID(), CHANNEL_TRIGGERED), status.toString());
                    }
                    updateValues();
                    DayOfWeek.setLocale(SystemHelper.getLocale());
                    updateState(channelTime, getNowString());
                    updateState(channelDayOfWeek,
                            new StringType(DayOfWeek.getFromCalendar(currentDayOfWeek).getShortName()));

                    if (currentMinute == 0) {
                        hourTick();
                        refreshState();
                    }
                } catch (Exception e) {
                    logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                }
            }
        };

        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, 60, TimeUnit.SECONDS);
        refreshState();
    }

    protected void stopAutomaticRefresh() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    /**
     * Dispose off the refreshJob nicely.
     */
    @Override
    public void dispose() {
        stopAutomaticRefresh();
    }

}
