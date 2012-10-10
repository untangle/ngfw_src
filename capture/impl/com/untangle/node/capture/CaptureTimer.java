/**
 * $Id: CaptureTimer.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // IMPL

import java.util.TimerTask;
import org.apache.log4j.Logger;
import com.untangle.uvm.util.LoadAvg;

public class CaptureTimer extends TimerTask
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptureNodeImpl node;

    public CaptureTimer(CaptureNodeImpl node)
    {
        this.node = node;
    }

    public void run()
    {
        SessionCleanup();
    }

    private void SessionCleanup()
    {
        int cleanup = node.captureUserTable.cleanupStaleUsers(node.getSettings().getIdleTimeout(),node.getSettings().getUserTimeout());
        if (cleanup > 0) logger.info("Cleaned up " + cleanup + " expired sessions");
    }
}
