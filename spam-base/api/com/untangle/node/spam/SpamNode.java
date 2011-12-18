package com.untangle.node.spam;

import java.util.Date;
import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface SpamNode extends Node
{
    public SpamSettings getSettings();
    public void setSettings(final SpamSettings newSpamSettings);

    void enableSmtpSpamHeaders(boolean enableHeaders);
    void enablePopSpamHeaders(boolean enableHeaders);
    void enableImapSpamHeaders(boolean enableHeaders);

    void enableSmtpFailClosed(boolean failClosed);

    void updateScannerInfo();

    EventLogQuery[] getEventQueries();
    EventLogQuery[] getRBLEventQueries();

    public int getSpamRBLListLength();
    public void setSpamRBLListLength(int newValue);
    
    public Date getLastUpdate();
    public void setLastUpdate(Date newValue);
    
    public Date getLastUpdateCheck();
    public void setLastUpdateCheck(Date newValue);
    
    public String getSignatureVersion();
    public void setSignatureVersion(String newValue);

    public String getVendor();
}
