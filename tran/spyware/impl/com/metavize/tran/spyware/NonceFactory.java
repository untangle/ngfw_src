/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spyware;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class NonceFactory
{
    private static final NonceFactory FACTORY = new NonceFactory();

    private Map<String, BlockDetails> nonces = new HashMap<String, BlockDetails>();
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
        synchronized (this) {
            return nonces.remove(nonce);
        }
    }
}