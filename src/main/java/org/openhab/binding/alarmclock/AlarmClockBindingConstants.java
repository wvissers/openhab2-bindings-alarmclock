/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link AlarmClockBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Wim Vissers - Initial contribution
 */
public class AlarmClockBindingConstants {

    public static final String BINDING_ID = "alarmclock";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_ALARM = new ThingTypeUID(BINDING_ID, "alarm");
    public final static ThingTypeUID THING_TYPE_SUN = new ThingTypeUID(BINDING_ID, "sun");
    public final static ThingTypeUID THING_TYPE_SUNRISE = new ThingTypeUID(BINDING_ID, "sunrise");
    public final static ThingTypeUID THING_TYPE_SUNSET = new ThingTypeUID(BINDING_ID, "sunset");
    public final static ThingTypeUID THING_TYPE_TIMER = new ThingTypeUID(BINDING_ID, "timer");

    // List of all Channel ids
    public final static String CHANNEL_ONHOUR = "onHour";
    public final static String CHANNEL_ONMINUTE = "onMinute";
    public final static String CHANNEL_ONTIME = "onTime";
    public final static String CHANNEL_OFFHOUR = "offHour";
    public final static String CHANNEL_OFFMINUTE = "offMinute";
    public final static String CHANNEL_OFFTIME = "offTime";
    public final static String CHANNEL_SUNRISE = "sunrise";
    public final static String CHANNEL_SUNSET = "sunset";
    public final static String CHANNEL_STATUS = "status";
    public final static String CHANNEL_TIMEOUT = "timeout";
    public final static String CHANNEL_ENABLED = "enabled";
    public final static String CHANNEL_DAYENABLED = "dayEnabled";
    public final static String CHANNEL_DAYS = "days";
    public final static String CHANNEL_TIME = "time";
    public final static String CHANNEL_DAYOFWEEK = "dayOfWeek";
    public final static String CHANNEL_MAXSECONDS = "maxSeconds";
    public final static String CHANNEL_CURRENTSECONDS = "currentSeconds";
    public final static String CHANNEL_TRIGGERED = "triggered";

}
