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

package com.untangle.tran.mail.papi;

/**
 * Class to encapsulate the strategy
 * used to determine if we're going to
 * time-out for POP/IMAP/SMTP.
 * <br><br>
 * Currently, based on the assumption that
 * transmission times between endpoints on each
 * interface are the same.  If it takes "n" seconds
 * to transfer from client->MVVM, it will take "n"
 * seconds to transfer from MVVM -> server.  The total
 * time that the client must wait is assumed to be 2*n.
 * <br><br>
 * Since checking for timeouts is performed while
 * receiving data from the first endpoint, we evaluate
 * timeout danger by the max wait time for either side
 * divided by 2.
 */
public class MessageTransmissionTimeoutStrategy {

    /**
     * Test if there is a danger of timeout
     *
     * @param maxWaitPeriod the max wait period (in relative milliseconds).
     *        If client and server have differerent periods, pass the smallest
     * @param lastTimestamp the last time client or server received a message.
     *        Pass oldest (i.e. smallest number) of the two.
     *
     * @return true if a timeout is pending.
     */
    public static boolean inTimeoutDanger(
                                          long maxWaitPeriod,
                                          long lastTimestamp) {

        if(maxWaitPeriod <= 0) {
            //Time equal-to or below zero means give-up
            return false;
        }
        maxWaitPeriod = (long) (maxWaitPeriod * 0.95);//TODO bscott a real "slop" factor - not a guess

        return (System.currentTimeMillis() - lastTimestamp) < maxWaitPeriod ? false : true;
    }
}
