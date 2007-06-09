/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareHttpHandler.java 8668 2007-01-29 19:17:09Z amread $
 */

package com.untangle.node.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

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
                timer.cancel();
                timer = null;
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
