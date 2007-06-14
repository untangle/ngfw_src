/*
 * $HeadURL:$
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

package com.untangle.node.mail.papi.smtp;

import org.apache.log4j.Logger;

import com.untangle.uvm.util.LoadAvg;
import com.untangle.uvm.util.MetaEnv;

public final class ScanLoadChecker {

    // For every concurrent scan that is running, wait this many milliseconds before
    // accepting a new one.
    public static final String SMTP_SCAN_CONCURRENT_DELAY_PROPERTY = "uvm.smtp.scan.concurrent.delay";
    public static final long SMTP_SCAN_CONCURRENT_DELAY_DEFAULT = 200;

    public static final String SMTP_SCAN_REJECT_SCANS_PROPERTY = "uvm.smtp.scan.reject.scans";
    public static final int SMTP_SCAN_REJECT_SCANS_DEFAULT = 15;

    public static final String SMTP_SCAN_REJECT_LOAD_PROPERTY = "uvm.smtp.scan.reject.load";
    public static final float SMTP_SCAN_REJECT_LOAD_DEFAULT = 7.0f;

    public static final String SMTP_SCAN_REJECT_RUNNING_PROPERTY = "uvm.smtp.scan.reject.running";
    public static final int SMTP_SCAN_REJECT_RUNNING_DEFAULT = 30;

    public static final float ALLOW_ANYWAY_CHANCE = 0.05f;

    public static long SMTP_SCAN_CONCURRENT_DELAY;
    public static float SMTP_SCAN_REJECT_LOAD;
    public static int SMTP_SCAN_REJECT_SCANS;
    public static int SMTP_SCAN_REJECT_RUNNING;

    static {
        SMTP_SCAN_CONCURRENT_DELAY = SMTP_SCAN_CONCURRENT_DELAY_DEFAULT;
        String p = System.getProperty(SMTP_SCAN_CONCURRENT_DELAY_PROPERTY);
        if (p != null) {
            try {
                SMTP_SCAN_CONCURRENT_DELAY = Long.parseLong(p);
            } catch (NumberFormatException x) {
                System.err.println("cannot parse concurrent delay: " + p);
            }
        }
        SMTP_SCAN_REJECT_LOAD = SMTP_SCAN_REJECT_LOAD_DEFAULT;
        p = System.getProperty(SMTP_SCAN_REJECT_LOAD_PROPERTY);
        if (p != null) {
            try {
                SMTP_SCAN_REJECT_LOAD = Float.parseFloat(p);
            } catch (NumberFormatException x) {
                System.err.println("cannot parse reject load: " + p);
            }
        }
        SMTP_SCAN_REJECT_SCANS = SMTP_SCAN_REJECT_SCANS_DEFAULT;
        p = System.getProperty(SMTP_SCAN_REJECT_SCANS_PROPERTY);
        if (p != null) {
            try {
                SMTP_SCAN_REJECT_SCANS = Integer.parseInt(p);
            } catch (NumberFormatException x) {
                System.err.println("cannot parse reject scans: " + p);
            }
        }
        SMTP_SCAN_REJECT_RUNNING = SMTP_SCAN_REJECT_RUNNING_DEFAULT;
        p = System.getProperty(SMTP_SCAN_REJECT_RUNNING_PROPERTY);
        if (p != null) {
            try {
                SMTP_SCAN_REJECT_RUNNING = Integer.parseInt(p);
            } catch (NumberFormatException x) {
                System.err.println("cannot parse reject running: " + p);
            }
        }
    }

    private ScanLoadChecker() { }

    /**
     * Call this to determine if session request should be rejected for load too high.
     *
     * @param logger a <code>Logger</code> used to log the warning if the load is too high
     * @return a <code>boolean</code> value
     */
    public static boolean reject(int activeCount, Logger logger) {
        LoadAvg la = LoadAvg.get();
        float oneMinLA = la.getOneMin();
        int numRunning = la.getNumRunning();
        if (activeCount >= SMTP_SCAN_REJECT_SCANS) {
            logger.warn("Too many concurrent scans: " + activeCount);
            return true;
        }
        if (oneMinLA >= SMTP_SCAN_REJECT_LOAD ||
            numRunning >= SMTP_SCAN_REJECT_RUNNING) {
            if (MetaEnv.rng().nextFloat() < ALLOW_ANYWAY_CHANCE) {
                logger.warn("Load too high, but allowing anyway: " + la);
                return false;
            }
            logger.warn("Load too high: " + la);
            return true;
        }
        long delay = activeCount * SMTP_SCAN_CONCURRENT_DELAY;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException x) {
        }
        return false;
    }
}
