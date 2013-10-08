/**
 * $Id: SpamAssassinDaemon.java 34295 2013-03-17 20:24:07Z dmorris $
 */
package com.untangle.node.spam;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;

public class SpamAssassinDaemon
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String BASE_CMD = "/etc/init.d/spamassassin";
    private final static String START_CMD = BASE_CMD + " start";
    private final static String STOP_CMD = BASE_CMD + " stop";
    private final static String RESTART_CMD = BASE_CMD + " restart";

    public SpamAssassinDaemon() {}

    public boolean start()
    {
        return executeCmd(START_CMD);
    }

    public boolean stop()
    {
        return executeCmd(STOP_CMD);
    }

    public boolean restart()
    {
        return executeCmd(RESTART_CMD);
    }

    private boolean executeCmd(String cmdStr)
    {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(cmdStr);
        if (result == null)
            return false;
        return (result.getResult() == 0);
    }
}
