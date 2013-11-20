/**
 * $Id$
 */
package com.untangle.node.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Factory for creating nonces associated with some data. A thread
 * cleans out nonces after 600000 milliseconds have elapsed.
 *
 */
public class NonceFactory<T>
{
    private final Map<String, T> nonces = new HashMap<String, T>();
    private final Random random = new Random();

    private Timer timer = null;

    // constructors -----------------------------------------------------------

    public NonceFactory() { }

    // public methods ---------------------------------------------------------

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

    public T getNonceData(String nonce)
    {
        synchronized (this) {
            return nonces.get(nonce);
        }
    }

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

    // private classes --------------------------------------------------------

    private class PurgeTask extends TimerTask
    {
        private final String nonce;

        PurgeTask(String nonce)
        {
            this.nonce = nonce;
        }

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
