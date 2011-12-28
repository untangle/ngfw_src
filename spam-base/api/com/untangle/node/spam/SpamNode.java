/**
 * $Id$
 */
package com.untangle.node.spam;

import java.util.Date;
import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface SpamNode extends Node
{
    SpamSettings getSettings();
    void setSettings(final SpamSettings newSpamSettings);

    void enableSmtpSpamHeaders(boolean enableHeaders);
    void enablePopSpamHeaders(boolean enableHeaders);
    void enableImapSpamHeaders(boolean enableHeaders);

    void enableSmtpFailClosed(boolean failClosed);

    void updateScannerInfo();

    EventLogQuery[] getEventQueries();
    EventLogQuery[] getTarpitEventQueries();

    Date getLastUpdate();
    void setLastUpdate(Date newValue);
    
    Date getLastUpdateCheck();
    void setLastUpdateCheck(Date newValue);
    
    String getSignatureVersion();
    void setSignatureVersion(String newValue);

    String getVendor();
}
