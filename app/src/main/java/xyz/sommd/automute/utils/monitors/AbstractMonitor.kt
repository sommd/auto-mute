/*
 * Copyright (C) 2018 David Sommerich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.sommd.automute.utils.monitors

/**
 * An abstract base class for a "monitor" which is a class that takes listeners and automatically
 * starts and stops monitoring when it has listeners.
 *
 * @param L The type of listener this Monitor takes.
 */
abstract class AbstractMonitor<L> {
    /** Listeners to be notified of changes. */
    protected val listeners = mutableSetOf<L>()
    
    /**
     * Add a listener to be notified of changes.
     */
    fun addListener(listener: L) {
        // Start if this is the first listener
        if (listeners.isEmpty()) {
            start()
        }
        
        listeners.add(listener)
    }
    
    /**
     * Remove a listener.
     */
    fun removeListener(listener: L) {
        listeners.remove(listener)
        
        // Stop if this was the last listener
        if (listeners.isEmpty()) {
            stop()
        }
    }
    
    /**
     * Called when the first listener is added to start monitoring for changes.
     */
    protected abstract fun start()
    
    /**
     * Called when the last listener is removed to stop monitoring for changes.
     */
    protected abstract fun stop()
}