/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.internal;

/**
 * The Constants class contains constants used throughout the binding.
 *
 * @author Wim Vissers - Initial contribution
 */
public interface Constants {

    // Property names
    public static final String PROPERTY_LOCALE = "locale";
    public static final String PROPERTY_TIMEZONE = "timezone";
    public static final String PROPERTY_LOCATION = "location";

    // ClockManager settings
    public static final int TIME_RESOLUTION_SECONDS = 30; // Should be < 60 seconds.

}
