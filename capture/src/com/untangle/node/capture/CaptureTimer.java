/**
 * $Id$
 */

package com.untangle.node.capture;

import java.util.TimerTask;
import java.util.ArrayList;
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
        sessionCleanup();
    }

    public void sessionCleanup()
    {
        try {
            ArrayList<CaptureUserTable.StaleUser> staleUsers = node.captureUserTable.buildStaleList(node.getCaptureSettings().getIdleTimeout(), node.getCaptureSettings().getUserTimeout());
            int counter = 0;

            for (CaptureUserTable.StaleUser item : staleUsers) {
                node.userLogout(item.address, item.reason);
                counter++;
            }

            if (counter > 0)
                logger.info("Cleaned up " + counter + " expired sessions");
        } catch (Exception e) {
            logger.warn("Exception in session cleanup",e);
        }
    }
}
