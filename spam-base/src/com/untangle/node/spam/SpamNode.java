/**
 * $Id$
 */
package com.untangle.node.spam;

import java.util.Date;
import java.util.List;

import com.untangle.uvm.node.Node;

public interface SpamNode extends Node
{
    SpamSettings getSettings();
    void setSettings(final SpamSettings newSpamSettings);

    void enableSmtpSpamHeaders(boolean enableHeaders);

    void enableSmtpFailClosed(boolean failClosed);

    void updateScannerInfo();

    Date getLastUpdate();
    void setLastUpdate(Date newValue);
    
    Date getLastUpdateCheck();
    void setLastUpdateCheck(Date newValue);
    
    String getSignatureVersion();
    void setSignatureVersion(String newValue);

    String getVendor();
}
