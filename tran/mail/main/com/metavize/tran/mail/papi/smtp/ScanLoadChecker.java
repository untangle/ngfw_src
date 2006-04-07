/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi.smtp;

import org.apache.log4j.Logger;

import com.metavize.mvvm.util.LoadAvg;

public final class ScanLoadChecker {

    public static final String SMTP_SCAN_REJECT_LOAD_PROPERTY = "mvvm.smtp.scan.reject.load";
    public static final float SMTP_SCAN_REJECT_LOAD_DEFAULT = 7.0f;

    public static final String SMTP_SCAN_REJECT_RUNNING_PROPERTY = "mvvm.smtp.scan.reject.running";
    public static final int SMTP_SCAN_REJECT_RUNNING_DEFAULT = 30;

    public static float SMTP_SCAN_REJECT_LOAD;
    public static int SMTP_SCAN_REJECT_RUNNING;

    static {
        SMTP_SCAN_REJECT_LOAD = SMTP_SCAN_REJECT_LOAD_DEFAULT;
        String p = System.getProperty(SMTP_SCAN_REJECT_LOAD_PROPERTY);
        if (p != null) {
            try {
                SMTP_SCAN_REJECT_LOAD = Float.parseFloat(p);
            } catch (NumberFormatException x) {
                System.err.println("cannot parse reject load: " + p);
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
    public static boolean reject(Logger logger) {
        LoadAvg la = LoadAvg.get();
        float oneMinLA = la.getOneMin();
        int numRunning = la.getNumRunning();
        if (oneMinLA >= SMTP_SCAN_REJECT_LOAD ||
            numRunning >= SMTP_SCAN_REJECT_RUNNING) {
            logger.warn("Load too high: " + la);
            return true;
        }
        return false;
    }
}
