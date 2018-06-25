/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Factory for creating nonces associated with some data. A thread
 * cleans out nonces after 600000 milliseconds have elapsed.
 */
public class NonceFactory<T>
{
    private final Map<String, T> nonces = new HashMap<String, T>();
    private final Random random = new Random();

    private Timer timer = null;

    /**
     * Constructor
     */
    public NonceFactory() { }

    /**
     * Generate a nonce
     * @param o Object
     * @return The nonce
     */
    public String generateNonce(T o)
    {
        String nonce;

        synchronized (this) {
            while (nonces.containsKey(nonce = Long.toHexString(random.nextLong())));

            nonces.put(nonce, o);
            if (null == timer) {
                timer = new Timer(true);
            }

            timer.schedule(new PurgeTask(nonce), 600000);
        }

        return nonce;
    }

    /**
     * Get the nonce data
     * @param nonce The nonce
     * @return The data
     */
    public T getNonceData(String nonce)
    {
        synchronized (this) {
            return nonces.get(nonce);
        }
    }

    /**
     * Remove a nonce
     * @param nonce The nonce to remove
     * @return The nonce data
     */
    public T removeNonce(String nonce)
    {
        T data;

        synchronized (this) {
            data = nonces.remove(nonce);
            if (0 == nonces.size()) {
                if (null != timer) {
                    timer.cancel();
                    timer = null;
                }
            }
        }

        return data;
    }

    /**
     * Time task to purge empty nonces 
     */
    private class PurgeTask extends TimerTask
    {
        private final String nonce;

        /**
         * Constructor
         * @param nonce The nonce to be purged
         */
        PurgeTask(String nonce)
        {
            this.nonce = nonce;
        }

        /**
         * Main thread run function
         */
        public void run()
        {
            synchronized (NonceFactory.this) {
                nonces.remove(nonce);
                if (0 == nonces.size()) {
                    timer.cancel();
                    timer = null;
                }
            }
        }
    }
}
