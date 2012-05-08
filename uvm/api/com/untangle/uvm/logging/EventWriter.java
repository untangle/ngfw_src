/**
 * $Id: EventWriter.java,v 1.00 2011/12/18 19:09:03 dmorris Exp $
 */
package com.untangle.uvm.logging;

import com.untangle.uvm.logging.LogEvent;

public interface EventWriter
{
    void logEvent( LogEvent evt );

    void forceFlush();
}