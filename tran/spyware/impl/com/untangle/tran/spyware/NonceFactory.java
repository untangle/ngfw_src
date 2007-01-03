/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.spyware;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

class NonceFactory
{
    private static final NonceFactory FACTORY = new NonceFactory();

    private Map<String, BlockDetails> nonces = new HashMap<String, BlockDetails>();
    private Timer timer = null;

    private Random random = new Random();

    static NonceFactory factory()
    {
        return FACTORY;
    }

    private NonceFactory() { }

    String generateNonce(String host, String uri, InetAddress clientAddr)
    {
        String nonce;

        synchronized (this) {
            while (nonces.containsKey(nonce = Long.toHexString(random.nextLong())));

            BlockDetails bd = new BlockDetails(nonce, host, uri, clientAddr);

            nonces.put(nonce, bd);
            if (null == timer) {
                timer = new Timer(true);
            }

            timer.schedule(new PurgeTask(nonce), 600000);
        }

        return nonce;
    }

    BlockDetails getBlockDetails(String nonce)
    {
        synchronized (this) {
            return nonces.get(nonce);
        }
    }

    BlockDetails removeBlockDetails(String nonce)
    {
        BlockDetails bd;

        synchronized (this) {
            bd = nonces.remove(nonce);
            if (0 == nonces.size()) {
                timer.cancel();
                timer = null;
            }
        }

        return bd;
    }

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