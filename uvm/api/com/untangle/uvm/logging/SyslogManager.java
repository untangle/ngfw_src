/**
 * $Id$
 */
package com.untangle.uvm.logging;

/**
 * Interface for sending Syslog messages.
 */
public interface SyslogManager
{
    void sendSyslog( LogEvent e, String tag );
}
