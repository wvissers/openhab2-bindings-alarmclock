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

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.UpDownType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.alarmclock.internal.SystemHelper;

/**
 * The {@link AlarmClockHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Wim Vissers - Initial contribution
 */
public class AlarmClockHandler extends AbstractClockHandler {

    // For performance, create numbers in advance.
    private Number INCREASE_NUMBER = new DecimalType(100);
    private Number DECREASE_NUMBER = new DecimalType(-100);

    public AlarmClockHandler(Thing thing) {
        super(thing);
    }

    /**
     * Update hour/minute channel.
     *
     * @param newValue value 0..maxValue, or < 0 = decrease, > maxValue is increase.
     */
    private int getNewValue(int currentValue, Number newValue, int maxValue) {
        if (newValue == null) {
            return currentValue;
        } else {
            int i = newValue.intValue();
            if (i < 0) {
                // Decrease by 1
                return currentValue > 0 ? currentValue - 1 : currentValue;
            } else if (i > maxValue) {
                // Increase by 1
                return currentValue < maxValue ? currentValue + 1 : currentValue;
            } else {
                return i;
            }
        }

    }

    private Number decodeCommand(Command command) {
        if (command instanceof Number) {
            return (Number) command;
        } else if (command instanceof IncreaseDecreaseType) {
            IncreaseDecreaseType idt = (IncreaseDecreaseType) command;
            return idt == IncreaseDecreaseType.INCREASE ? INCREASE_NUMBER : DECREASE_NUMBER;
        } else if (command instanceof UpDownType) {
            UpDownType udt = (UpDownType) command;
            return udt == UpDownType.UP ? INCREASE_NUMBER : DECREASE_NUMBER;
        } else {
            return null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (super.handleBaseCommand(channelUID, command)) {
            return;
        }
        Number x = null;
        if (command instanceof Number || command instanceof RefreshType || command instanceof IncreaseDecreaseType
                || command instanceof UpDownType) {
            if (!(command instanceof RefreshType)) {
                x = decodeCommand(command);
            }
            switch (channelUID.getId()) {
                case CHANNEL_ONHOUR:
                    onHour = getNewValue(onHour, x, 23);
                    updateState(channelUID, new DecimalType(onHour));
                    break;
                case CHANNEL_ONMINUTE:
                    onMinute = getNewValue(onMinute, x, 59);
                    updateState(channelUID, new DecimalType(onMinute));
                    break;
                case CHANNEL_OFFHOUR:
                    offHour = getNewValue(offHour, x, 23);
                    updateState(channelUID, new DecimalType(offHour));
                    break;
                case CHANNEL_OFFMINUTE:
                    offMinute = getNewValue(offMinute, x, 59);
                    updateState(channelUID, new DecimalType(offMinute));
                    break;
                case CHANNEL_DAYENABLED:
                    updateState(channelUID, getDayEnabled());
                    break;
                default:
                    logger.debug("Command received for an unknown channel: {}", channelUID.getId());
                    break;
            }
            refreshState();
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

    /**
     * Refresh the state of channels that may have changed by (re-)initialization.
     */
    @Override
    protected void refreshState() {
        super.refreshState();
        updateState(channelOnTime, SystemHelper.formatTime(onHour, onMinute));
        updateState(channelOffTime, SystemHelper.formatTime(offHour, offMinute));
    }

    @Override
    public void initialize() {
        logger.debug("Initializing AlarmClock handler.");
        super.initialize();

        Configuration config = getThing().getConfiguration();

        onHour = ((BigDecimal) config.get("onHour")).intValue();
        onMinute = ((BigDecimal) config.get("onMinute")).intValue();
        offHour = ((BigDecimal) config.get("offHour")).intValue();
        offMinute = ((BigDecimal) config.get("offMinute")).intValue();

        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);
        updateProperties();

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");
        initEventHandlers();
    }
}
