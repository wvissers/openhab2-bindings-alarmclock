/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The singleton ClockManager service is used to control the several instances of ClockHandler (sub) classes. It is in a
 * separate class to be able to have lightweight AlarmClock things.
 *
 * @author Wim Vissers
 *
 */
public class ClockManager extends EventEmitter<ClockManager.Event, CompactTime, CompactTime> implements Constants {

    private static final ClockManager INSTANCE = new ClockManager();

    // The logger instance.
    private final Logger logger = LoggerFactory.getLogger(ClockManager.class);

    // The task.
    private ScheduledFuture<?> mainTask;

    // The last CompactTime.
    private CompactTime lastTime;

    /**
     * Singleton, so private constructor.
     */
    private ClockManager() {
    }
 
    /**
     * Start processing (when not already running).
     */
    public void init(ScheduledExecutorService scheduler) {
        synchronized (this) {
            if (mainTask == null) {
                lastTime = new CompactTime();
                
                // Refresh the locale.
                on(Event.MINUTE_TICK, (previous, current) -> {
                    DayOfWeek.setLocale(SystemHelper.getLocale());
                }, this);
                
                // Refresh the sunrise/sunset times.
                on(Event.HOUR_TICK, (previous, current) -> {
                    
                }, this);
                mainTask = scheduler.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        logger.debug("Executing Main Task");
                        // Get current time
                        CompactTime now = new CompactTime();

                        try {
                            // Emit events when applicable
                            if (lastTime.getMinute() != now.getMinute()) {
                                logger.debug("Minute tick");
                                emit(Event.MINUTE_TICK, lastTime, now);
                            }
                            if (lastTime.getHour() != now.getHour()) {
                                logger.debug("Hour tick");
                                emit(Event.HOUR_TICK, lastTime, now);
                                if (lastTime.getHour() % 6 == 0) {
                                    logger.debug("Six hour tick");
                                    emit(Event.SIX_HOUR_TICK, lastTime, now);
                                }
                            }
                            if (lastTime.getDayOfWeek() != now.getDayOfWeek()) {
                                logger.debug("Day tick");
                                emit(Event.DAY_TICK, lastTime, now);
                            }

                            // Store last time
                            lastTime = now;
                        } catch (Exception ex) {
                            logger.error("Error executing main task.", ex);
                        }
                    }
                }, 0, TIME_RESOLUTION_SECONDS, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Get the last time the main task run loop was executed. With a resolution of TIME_RESOLUTION_SECONDS this could be
     * considered the
     * current time.
     * 
     * @return
     */
    public CompactTime getLastTime() {
        return lastTime;
    }

    /**
     * Stop running.
     */
    public void stop() {
        synchronized (this) {
            if (mainTask != null) {
                mainTask.cancel(true);
                mainTask = null;
            }
        }
    }


    /**
     * Return the singleton instance.
     *
     * @return the singleton instance.
     */
    public static ClockManager getInstance() {
        return INSTANCE;
    }

    public enum Event {
        MINUTE_TICK,
        HOUR_TICK,
        SIX_HOUR_TICK,
        DAY_TICK
    }
}
