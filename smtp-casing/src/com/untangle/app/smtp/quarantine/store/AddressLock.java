/**
 * $Id$
 */
package com.untangle.app.smtp.quarantine.store;

import java.util.HashSet;
import java.util.Set;

/**
 * Global account lock.
 * 
 * Assumes all addresses have been lower-cased
 */
class AddressLock
{
    private final static AddressLock INSTANCE = new AddressLock();

    private static Set<String> m_set = new HashSet<>();
    private static boolean m_waiting = false;

    /**
     * Initialzie instance of AddressLock.
     * @return Instance of AddressLock.
     */
    private AddressLock() {
    }

    /**
     * Return instance.
     * @return AddressLock instance.
     */
    static AddressLock getInstance()
    {
        return INSTANCE;
    }

    /**
     * Attempt to lock the given address but if lock is not obtained after maxWait ms, give up
     * 
     * @param address
     *            the <b>lower cased</b> address
     * @param maxWait
     *            the max time to wait
     * 
     * @return true if locked, false if it could not be locked
     */
    synchronized boolean tryLock(String address, long maxWait)
    {
        // I'll assume getting clock time isn't too expensive,
        // so it can be done in a sync block
        long giveup = System.currentTimeMillis() + maxWait;
        while (true == m_set.contains(address)) {
            long remaining = giveup - System.currentTimeMillis();
            if (remaining < 2) {
                // out of time
                return false; // not locked
            }

            m_waiting = true;
            if (false == waitImpl(remaining)) {
                // interrupted; unable to continue try lock
                m_waiting = false;
                return false; // not locked
            } // else received notify or timed out
              // - if received notify,
              // will break out of while loop on next iteration and lock
              // - if timed out,
              // will return on next iteration and not lock if no time remains
              // or
              // will wait some more on next iteration if time remains
            m_waiting = false;
        }
        m_set.add(address); // locked
        return true;
    }

    /**
     * Attempt to lock the given address and if lock is not immediately obtained, give up
     * 
     * @param address
     *            the <b>lower cased</b> address
     * 
     * @return true if the address was locked
     */
    synchronized boolean tryLock(String address)
    {
        return tryLock(address, -1);
    }

    /**
     * Lock the given address (and do not return until lock is obtained)
     * 
     * @param address
     *            the <b>lower cased</b> address
     */
    synchronized void lock(String address)
    {
        while (m_set.contains(address)) {
            m_waiting = true;
            if (false == waitImpl()) {
                // interrupted; unable to continue try lock
                m_waiting = false;
                return; // not locked, assume shutdown
            } // else received notify
              // - will break out of while loop on next iteration and lock
            m_waiting = false;
        }
        m_set.add(address); // locked
        return;
    }

    /**
     * Unlock a previously locked address.
     * 
     * @param address
     *            the <b>lower cased</b> address
     */
    synchronized void unlock(String address)
    {
        m_set.remove(address);
        if (true == m_waiting) {
            // someone was waiting
            notifyAll(); // break them out of wait set (they will reset
                         // m_waiting)
        }
        return;
    }

    /**
     * Wait on the impl.
     * @param  time Time to wait.
     * @return      If true, wait succeeded, otherwise false.
     */
    private boolean waitImpl(long time)
    {
        try {
            wait(time);
            return true; // received notify or timed out
        } catch (Exception ex) {
            return false; // wait interrupted
        }
    }

    /**
     * Wait on the impl.
     * @return      If true, wait succeeded, otherwise false.
     */
    private boolean waitImpl()
    {
        try {
            wait();
            return true; // received notify
        } catch (Exception ex) {
            return false; // wait interrupted
        }
    }
}
