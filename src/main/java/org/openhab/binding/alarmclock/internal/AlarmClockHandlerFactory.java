/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.internal;

import java.util.HashSet;
import java.util.Set;

import static org.openhab.binding.alarmclock.AlarmClockBindingConstants.*;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.alarmclock.handler.AlarmClockHandler;
import org.openhab.binding.alarmclock.handler.SunClockHandler;
import org.openhab.binding.alarmclock.handler.SunriseClockHandler;
import org.openhab.binding.alarmclock.handler.SunsetClockHandler;
import org.openhab.binding.alarmclock.handler.TimerHandler;
import org.osgi.service.component.ComponentContext;

/**
 * The {@link AlarmClockHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Wim Vissers - Initial contribution
 */
public class AlarmClockHandlerFactory extends BaseThingHandlerFactory {

    private final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS;

    public AlarmClockHandlerFactory() {
        SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ALARM);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_SUN);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_SUNRISE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_SUNSET);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_TIMER);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }
    
    /**
     * Activate the binding.
     */
    @Override
    public void activate(ComponentContext componentContext) {
        super.activate(componentContext);
        // Create the ClockManager, but delay initialization until the first ThingHandler is initialized,
        // because otherwise startup will not succeed, since the system localization service is not availabe.
        ClockManager.getInstance();
    }
    
    /**
     * Deactivate the binding.
     */
    @Override
    public void deactivate(ComponentContext componentContext) {
        ClockManager.getInstance().stop();
        super.deactivate(componentContext);
    }

    
    
    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(THING_TYPE_ALARM)) {
            return new AlarmClockHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_SUN)) {
            return new SunClockHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_SUNRISE)) {
            return new SunriseClockHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_SUNSET)) {
            return new SunsetClockHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_TIMER)) {
            return new TimerHandler(thing);
        }

        return null;
    }
}
