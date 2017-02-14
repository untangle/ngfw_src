/**
 * $Id$
 */

package com.untangle.node.captive_portal;

import java.util.TimerTask;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.untangle.uvm.util.LoadAvg;

public class CaptivePortalTimer extends TimerTask
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptivePortalApp node;

    public CaptivePortalTimer(CaptivePortalApp node)
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
            ArrayList<CaptivePortalUserTable.StaleUser> staleUsers = node.captureUserTable.buildStaleList(node.getCaptivePortalSettings().getIdleTimeout(), node.getCaptivePortalSettings().getUserTimeout());
            int counter = 0;

            for (CaptivePortalUserTable.StaleUser item : staleUsers) {
                node.userLogout(item.netaddr, item.reason);
                counter++;
            }

            if (counter > 0)
                logger.info("Cleaned up " + counter + " expired sessions");
        } catch (Exception e) {
            logger.warn("Exception in session cleanup",e);
        }
    }
}
