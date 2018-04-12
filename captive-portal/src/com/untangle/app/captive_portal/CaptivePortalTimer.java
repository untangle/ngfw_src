/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.util.TimerTask;
import java.util.ArrayList;
import org.apache.log4j.Logger;

/**
 * This is a timer task that runs periodically, gets a list of stale users, and
 * logs those users out of captive portal.
 * 
 * @author mahotz
 */

public class CaptivePortalTimer extends TimerTask
{
    private final Logger logger = Logger.getLogger(getClass());
    private final CaptivePortalApp app;

    /**
     * Constructor
     * 
     * @param app
     *        The application instance that created us
     */
    public CaptivePortalTimer(CaptivePortalApp app)
    {
        this.app = app;
    }

    /**
     * This is the timer run function
     */
    public void run()
    {
        sessionCleanup();
    }

    /**
     * Gets a list of stale users and logs each one out
     */
    public void sessionCleanup()
    {
        try {
            ArrayList<CaptivePortalUserTable.StaleUser> staleUsers = app.captureUserTable.buildStaleList(app.getSettings().getIdleTimeout(), app.getSettings().getUserTimeout());
            int counter = 0;

            for (CaptivePortalUserTable.StaleUser item : staleUsers) {
                app.userForceLogout(item.useraddr, item.reason);
                counter++;
            }

            if (counter > 0) logger.info("Cleaned up " + counter + " expired sessions");
        } catch (Exception e) {
            logger.warn("Exception in session cleanup", e);
        }
    }
}
