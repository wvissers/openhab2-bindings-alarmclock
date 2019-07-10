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

import java.math.BigDecimal;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link TimerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Wim Vissers - Initial contribution
 */
public class TimerHandler extends BaseThingHandler {

    // Last trigger time or 0 if never triggered
    private long lastTriggerMillis;

    // Parameters
    private int maxSeconds;

    // Current status
    private OnOffType status;
    private OnOffType timeout;
    private int lastSeconds;

    // Scheduler
    ScheduledFuture<?> refreshJob;

    private Logger logger = LoggerFactory.getLogger(TimerHandler.class);

    public TimerHandler(Thing thing) {
        super(thing);
        status = OnOffType.OFF;
        timeout = OnOffType.OFF;
        lastSeconds = 0;
    }

    private int getCurrentSeconds() {
        if (status.equals(OnOffType.OFF)) {
            return 0;
        } else {
            return (int) (maxSeconds - ((System.currentTimeMillis() - lastTriggerMillis) / 1000));
        }
    }

    private void startTimer() {
        lastTriggerMillis = System.currentTimeMillis();
        status = OnOffType.ON;
        timeout = OnOffType.OFF;
        lastSeconds = getCurrentSeconds();
        updateState(new ChannelUID(thing.getUID(), CHANNEL_CURRENTSECONDS), new DecimalType(lastSeconds));
        updateState(new ChannelUID(thing.getUID(), CHANNEL_TIMEOUT), timeout);
        startAutomaticRefresh();
    }

    private void stopTimer() {
        status = OnOffType.OFF;
        lastSeconds = getCurrentSeconds();
        updateState(new ChannelUID(thing.getUID(), CHANNEL_CURRENTSECONDS), new DecimalType(lastSeconds));
        stopAutomaticRefresh();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        Number x = null;
        if (command instanceof Number) {
            x = (Number) command;
        }
        if (command instanceof Number || command instanceof RefreshType) {
            switch (channelUID.getId()) {
                case CHANNEL_MAXSECONDS:
                    if (x != null) {
                        maxSeconds = x.intValue();
                    }
                    updateState(channelUID, new DecimalType(maxSeconds));
                    break;
                case CHANNEL_CURRENTSECONDS:
                    updateState(channelUID, new DecimalType(getCurrentSeconds()));
                    break;
                case CHANNEL_STATUS:
                    updateState(channelUID, status);
                    break;
                case CHANNEL_TIMEOUT:
                    updateState(channelUID, timeout);
                    break;
                default:
                    logger.debug("Command received for an unknown channel: {}", channelUID.getId());
                    break;
            }
        } else if (command instanceof OnOffType && channelUID.getId().equals(CHANNEL_STATUS)) {
            if (!((OnOffType) command).equals(status)) {
                if (((OnOffType) command).equals(OnOffType.ON)) {
                    triggerChannel(new ChannelUID(thing.getUID(), CHANNEL_TRIGGERED), OnOffType.ON.toString());
                    startTimer();
                } else {
                    stopTimer();
                }
                updateState(channelUID, status);
            }
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing AlarmClock handler.");

        Configuration config = getThing().getConfiguration();

        maxSeconds = ((BigDecimal) config.get("maxSeconds")).intValue();

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
    }

    /**
     * Check on and off times with current time. If the status
     * changed from on to off or vice versa, return true.
     *
     * @return
     */
    private boolean updateAlarmStatus() {
        boolean result = false;
        if (lastSeconds != 0 && lastSeconds != getCurrentSeconds()) {
            lastSeconds = getCurrentSeconds();
            updateState(new ChannelUID(thing.getUID(), CHANNEL_CURRENTSECONDS), new DecimalType(lastSeconds));
            if (lastSeconds <= 0) {
                timeout = OnOffType.ON;
                updateState(new ChannelUID(thing.getUID(), CHANNEL_TIMEOUT), timeout);
                triggerChannel(new ChannelUID(thing.getUID(), CHANNEL_TRIGGERED), OnOffType.OFF.toString());
                stopTimer();
                result = true;
            }
        }
        return result;
    }

    /**
     * Check every 60 seconds if one of the alarm times is reached.
     */
    private void startAutomaticRefresh() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (updateAlarmStatus()) {
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_STATUS), status);
                    }
                } catch (Exception e) {
                    logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                }
            }
        };
        refreshJob = scheduler.scheduleAtFixedRate(runnable, 0, 1, TimeUnit.SECONDS);
    }

    private void stopAutomaticRefresh() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
    }

    /**
     * Dispose off the refreshJob nicely.
     */
    @Override
    public void dispose() {
        if (refreshJob != null) {
            refreshJob.cancel(true);
        }
    }
}
