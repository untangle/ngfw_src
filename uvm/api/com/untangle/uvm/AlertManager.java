/**
 * $Id: ReportsManager.java,v 1.00 2015/03/04 13:45:51 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;

/**
 * The API for interacting/viewing/editing alerts
 */
public interface AlertManager
{
    public void logEvent( LogEvent evt );

}
