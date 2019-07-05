/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.internal;

import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.i18n.LocationProvider;
import org.eclipse.smarthome.core.i18n.TimeZoneProvider;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.PointType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * The DayOfWeek enum is used for setting the alarm clock auto enable/disable
 * dependent of the day of the week.
 *
 * @author Wim Vissers - Initial contribution
 */
public class SystemHelper {

    private static LocaleProvider localeProvider;
    private static LocationProvider locationProvider;
    private static TimeZoneProvider timeZoneProvider;
    private static final PointType DEFAULT_LOCATION = new PointType(new DecimalType(51.9166667), new DecimalType(4.5));

    /**
     * Get the providers for the services and store for future reference.
     */
    private static void getProviders() {
        Bundle bundle = FrameworkUtil.getBundle(LocaleProvider.class);
        BundleContext context = bundle.getBundleContext();
        localeProvider = (LocaleProvider) context
                .getService(context.getServiceReference(LocaleProvider.class.getName()));
        locationProvider = (LocationProvider) context
                .getService(context.getServiceReference(LocationProvider.class.getName()));
        timeZoneProvider = (TimeZoneProvider) context
                .getService(context.getServiceReference(TimeZoneProvider.class.getName()));
    }

    /**
     * Format hours and minutes as StringType.
     *
     * @param h
     * @param m
     * @return
     */
    public static StringType formatTime(int h, int m) {
        return new StringType(("" + (100 + h)).substring(1) + ":" + ("" + (100 + m)).substring(1));
    }

    /**
     * Get the Locale. Try the Eclipse smarthome system setting, or if
     * there is no provider service, the system default.
     *
     * @return the Locale.
     */
    public static Locale getLocale() {
        if (localeProvider == null) {
            getProviders();
        }
        if (localeProvider != null) {
            return localeProvider.getLocale();
        } else {
            return Locale.getDefault();
        }
    }

    /**
     * Get the TimeZone. Try the Eclipse smarthome system setting, or if
     * there is no provider service, the system default.
     *
     * @return the TimeZone.
     */
    public static TimeZone getTimeZone() {
        if (timeZoneProvider == null) {
            getProviders();
        }
        if (timeZoneProvider != null) {
            return TimeZone.getTimeZone(timeZoneProvider.getTimeZone());
        } else {
            return TimeZone.getDefault();
        }
    }

    /**
     * Get the Location. Try the Eclipse smarthome system setting, or if
     * there is no provider service, the system default.
     *
     * @return the Location.
     */
    public static PointType getLocation() {
        if (locationProvider == null) {
            getProviders();
        }
        if (locationProvider != null && locationProvider.getLocation() != null) {
            return locationProvider.getLocation();
        } else {
            return DEFAULT_LOCATION;
        }
    }
}
