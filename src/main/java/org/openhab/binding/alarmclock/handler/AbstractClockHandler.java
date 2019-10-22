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

import java.util.EnumSet;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.alarmclock.internal.ClockManager;
import org.openhab.binding.alarmclock.internal.ClockManager.Event;
import org.openhab.binding.alarmclock.internal.CompactTime;
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

    protected Logger logger = LoggerFactory.getLogger(AlarmClockHandler.class);

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
        daysOfWeek = EnumSet.allOf(DayOfWeek.class);
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
                    DayOfWeek dow = ClockManager.getInstance().getLastTime().getDayOfWeek();
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
                    }
                    break;
                default:
                    result = false;
                    break;
            }
        } else if (command instanceof Number || command instanceof RefreshType) {
            switch (channelUID.getId()) {
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
     * Determine if the alarm is currently enabled.
     * 
     * @return true when enabled.
     */
    protected boolean isEnabled() {
        return enabled.equals(OnOffType.ON);
    }

    /**
     * Determine if today the alarm clock is enabled.
     *
     * @return true if today is enabled.
     */
    protected boolean isDayEnabled() {
        return daysOfWeek.contains(ClockManager.getInstance().getLastTime().getDayOfWeek());
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
     * Clean up resources when removing handler.
     */
    @Override
    public void handleRemoval() {
        // Remove every trigger from the clock manager.
        ClockManager.getInstance().remove(this);
        super.handleRemoval();
    }

    /**
     * Change the status.
     * 
     * @param newStatus
     */
    protected void switchStatus(OnOffType newStatus) {
        if (!status.equals(newStatus)) {
            status = newStatus;
            updateState(new ChannelUID(getThing().getUID(), CHANNEL_STATUS), status);
            triggerChannel(new ChannelUID(thing.getUID(), CHANNEL_TRIGGERED), status.toString());
        }
    }

    /**
     * Initialize the time triggers by registering the basic event handlers with the clock manager.
     */
    protected void initEventHandlers() {
        ClockManager clockManager = ClockManager.getInstance();
        
        // Init the alarm state.
        clockManager.once(Event.MINUTE_TICK, (previous, current) -> {
            CompactTime onTime = new CompactTime(onHour, onMinute);
            CompactTime offTime = new CompactTime(offHour, offMinute);
            if (onTime.isLessThan(offTime)) {
                status = (onTime.isLessThan(current) && (current.isLessThanOrEqual(offTime))) ? OnOffType.ON : OnOffType.OFF;
            } else {
                status = (offTime.isLessThan(current) && (current.isLessThanOrEqual(onTime))) ? OnOffType.OFF : OnOffType.ON;
            }
            updateState(new ChannelUID(getThing().getUID(), CHANNEL_STATUS), status);            
        }, this);

        // Handle the minute tick by checking if a status change is needed.
        clockManager.on(Event.MINUTE_TICK, (previous, current) -> {
            if (isEnabled() && isDayEnabled()) {
                // The day is enabled and the alarm is enabled.
                CompactTime onTime = new CompactTime(onHour, onMinute);
                CompactTime offTime = new CompactTime(offHour, offMinute);
                if (onTime.isSwitchTime(previous, current)) {
                    switchStatus(OnOffType.ON);
                }
                if (offTime.isSwitchTime(previous, current)) {
                    switchStatus(OnOffType.OFF);
                }
                refreshState();
                updateState(channelTime, SystemHelper.formatTime(current.getHour(), current.getMinute()));
            }

        }, this);

        // Make sure the clock manager is started. Subsequent calls to this method has no effect.
        //clockManager.start(scheduler);
        
        // After initialization, refresh channels that may have changed.
        refreshState();
    }

    @Override
    public void initialize() {

        logger.debug("Initializing AbstractClock handler.");
        Configuration config = getThing().getConfiguration();
        
        // First remove handlers that may exist (when changing settings).
        ClockManager clockManager = ClockManager.getInstance();
        clockManager.init(scheduler);
        clockManager.remove(this);

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
    }

    @SuppressWarnings("null")
    protected void updateProperties() {
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
     * Dispose off the refreshJob nicely.
     */
    @Override
    public void dispose() {
        //stopAutomaticRefresh();
       // ClockManager.getInstance().dispose();
    }

}
