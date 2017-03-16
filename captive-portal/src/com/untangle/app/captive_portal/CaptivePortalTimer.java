/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.util.TimerTask;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.untangle.uvm.util.LoadAvg;

public class CaptivePortalTimer extends TimerTask
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptivePortalApp app;

    public CaptivePortalTimer(CaptivePortalApp app)
    {
        this.app = app;
    }

    public void run()
    {
        sessionCleanup();
    }

    public void sessionCleanup()
    {
        try {
            ArrayList<CaptivePortalUserTable.StaleUser> staleUsers = app.captureUserTable.buildStaleList(app.getCaptivePortalSettings().getIdleTimeout(), app.getCaptivePortalSettings().getUserTimeout());
            int counter = 0;

            for (CaptivePortalUserTable.StaleUser item : staleUsers) {
                app.userLogout(item.netaddr, item.reason);
                counter++;
            }

            if (counter > 0)
                logger.info("Cleaned up " + counter + " expired sessions");
        } catch (Exception e) {
            logger.warn("Exception in session cleanup",e);
        }
    }
}
