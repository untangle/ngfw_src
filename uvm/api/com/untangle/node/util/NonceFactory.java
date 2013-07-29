/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
