/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.alarmclock.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic Event emitter implementation.
 *
 * @param &lt;E&gt; the event type.
 * @param &lt;P1&gt; the type of first payload the consumer accepts.
 * @param &lt;P2&gt; the type of second payload the consumer accepts.
 *
 * @author Wim Vissers
 *
 */
public class EventEmitter<E, P1, P2> {

    // The collection with triggers.
    private final Map<E, List<CompoundConsumer<E, P1, P2>>> triggers;

    // The logger.
    private final Logger logger = LoggerFactory.getLogger(EventEmitter.class);

    /**
     * Create a generic EventEmitter.
     */
    public EventEmitter() {
        triggers = new ConcurrentHashMap<>();
    }
    
    /**
     * Add a handler for the given event. Give key as a unique object, used to be able to remove certain handlers.
     *
     * @param event    the event to trigger on.
     * @param callback the callback function to call.
     * @param key      a reference to typically the sender, used when removing things.
     * @return this EventEmitter to enable chaining.
     */
    public EventEmitter<E, P1, P2> on(E event, BiConsumer<P1, P2> callback, Object key) {
        return onHandler(event, callback, key, false);
    }
    
    /**
     * Add a handler for the given event. Give key as a unique object, used to be able to remove certain handlers.
     *
     * @param event    the event to trigger on.
     * @param callback the callback function to call.
     * @param key      a reference to typically the sender, used when removing things.
     * @return this EventEmitter to enable chaining.
     */
    public EventEmitter<E, P1, P2> once(E event, BiConsumer<P1, P2> callback, Object key) {
        return onHandler(event, callback, key, true);
    }

    /**
     * Add a handler for the given event. Give key as a unique object, used to be able to remove certain triggers.
     *
     * @param event    the event to trigger on.
     * @param callback the callback function to call.
     * @param key      a reference to typically the sender, used when removing things.
     * @return this EventEmitter to enable chaining.
     */
    protected EventEmitter<E, P1, P2> onHandler(E event, BiConsumer<P1, P2> callback, Object key, boolean once) {
        logger.debug("Added on trigger for event {} and key {}.", event, key);
        synchronized (this) {
            List<CompoundConsumer<E, P1, P2>> lst = triggers.get(event);
            if (lst == null) {
                lst = new CopyOnWriteArrayList<>();
                triggers.put(event, lst);
            }
            lst.add(new CompoundConsumer<E, P1, P2>(key, callback, once));
        }
        return this;
    }

    /**
     * Emit the given event.
     *
     * @param event the event.
     */
    public void emit(E event, P1 payload1, P2 payload2) {
        List<CompoundConsumer<E, P1, P2>> lst = triggers.get(event);
        if (lst != null) {
            lst.forEach(cc -> {
                cc.callback.accept(payload1, payload2);
                if (cc.once) {
                    lst.remove(cc);
                }
            });
        }
    }

    /**
     * Remove all triggers with the given key.
     *
     * @param key the key given when registering a trigger, typically the sender.
     */
    public void remove(Object key) {
        // Iterate over the lists
        for (List<CompoundConsumer<E, P1, P2>> lst : triggers.values()) {
            lst.removeIf(cf -> cf.key.equals(key));
        }
    }

    /**
     * Encapsulate a key (for reference and to be able to remove) and a callback function.
     *
     * @author Wim Vissers.
     *
     */
    private class CompoundConsumer<F, Q1, Q2> {
        private final boolean once;
        private final Object key;
        private final BiConsumer<Q1, Q2> callback;

        public CompoundConsumer(Object key, BiConsumer<Q1, Q2> callback, boolean once) {
            this.key = key;
            this.callback = callback;
            this.once = once;
        }
    }
}
