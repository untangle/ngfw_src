/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;

public class SpamAssassinDaemon
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String BASE_CMD = "/etc/init.d/spamassassin";
    private final static String START_CMD = BASE_CMD + " start";
    private final static String STOP_CMD = BASE_CMD + " stop";
    private final static String RESTART_CMD = BASE_CMD + " restart";

    // these replies are customized for /etc/init.d/spamassassin output
    private final static String STARTING_REPLY = "Starting ";
    private final static String STOPPING_REPLY = "Stopping ";
    private final static String RESTARTING_REPLY = "Restarting ";

    public SpamAssassinDaemon() {}

    public boolean start() {
        return executeCmd(START_CMD, STARTING_REPLY);
    }

    public boolean stop() {
        return executeCmd(STOP_CMD, STOPPING_REPLY);
    }

    public boolean restart() {
        return executeCmd(RESTART_CMD, RESTARTING_REPLY);
    }

    // command successfully finished -> true
    // command did not finish or finished with errors -> false
    private boolean executeCmd(String cmdStr, String replyStr) {
        Process cmdProcess = null;
        try {
            cmdProcess = UvmContextFactory.context().exec(cmdStr);
        } catch (Exception e) {
            logger.error(BASE_CMD + " could not be exec'ed: ", e);
            return false;
        }

        InputStream iStream = null;
        InputStreamReader iStreamReader = null;
        BufferedReader ibufReader = null;

        InputStream eStream = null;
        InputStreamReader eStreamReader = null;
        BufferedReader ebufReader = null;

        boolean killProcess = false;

        try {
            iStream = cmdProcess.getInputStream(); // output stream of process
            iStreamReader = new InputStreamReader(iStream);
            ibufReader = new BufferedReader(iStreamReader);

            eStream = cmdProcess.getErrorStream(); // error stream of process
            eStreamReader = new InputStreamReader(eStream);
            ebufReader = new BufferedReader(eStreamReader);

            boolean exitOK = false;

            String tStr;

            try {
                while (null != (tStr = ibufReader.readLine())) {
                    if (true == tStr.startsWith(replyStr))
                        exitOK = true;

                    logger.debug(BASE_CMD + " reported output: " + tStr);
                }

                while (null != (tStr = ebufReader.readLine())) {
                    if (0 != tStr.length() && false == tStr.equals("0"))
                        logger.error(BASE_CMD + " reported error: " + tStr);
                }
            } catch (IOException e) {
                logger.warn("I/O error occurred while reading output or error stream of " + cmdStr + " operation; killing process: ", e);
                killProcess = true;
                return false; // return after finally
            }

            int exitVal = cmdProcess.waitFor();
            switch(exitVal) {
            case 0:
                if (true == exitOK) {
                    logger.debug(cmdStr + " operation successfully finished");
                    return true; // return after finally
                } else {
                    logger.error(cmdStr + " operation finished with error(s); SpamAssassin installation may be corrupt");
                    return false; // return after finally
                }

            default:
            case 1:
                logger.error(cmdStr + " operation finished with error(s): " + exitVal);
                return false; // return after finally
            }
        } catch (InterruptedException e) {
            logger.error(cmdStr + " operation interrupted before it could finish: ", e);
            return false; // return after finally
        } finally {
            try {
                ibufReader.close();
                iStreamReader.close();
                iStream.close();
                ibufReader = null;
                iStreamReader = null;
                iStream = null;

                ebufReader.close();
                eStreamReader.close();
                eStream.close();
                ebufReader = null;
                eStreamReader = null;
                eStream = null;
            } catch (Exception ec) {}

            if (true == killProcess)
                cmdProcess.destroy();
        }
    }
}
