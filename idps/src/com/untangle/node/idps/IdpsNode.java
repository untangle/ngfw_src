/**
 * $Id: IdpsNode.java 31685 2012-04-15 15:50:30Z mahotz $
 */
package com.untangle.node.idps;

import java.util.Date;
import java.util.List;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface IdpsNode extends Node
{
    EventLogQuery[] getEventQueries();

    public String getSettingsFileName();
    public String getWizardSettingsFileName();
    
    public void initializeSettings();
    public void createDefaultSettings( String filename );
    public void saveSettings( String tempFileName );

    public void setUpdatedSettingsFlag( boolean updatedSettingsFlag);
    public boolean getUpdatedSettingsFlag();

    public void reloadEventMonitorMap();

    public void setScanCount( long value );
    public void setDetectCount( long value);
    public void setBlockCount( long value );
    public void reconfigure();

    public Date getLastUpdate();
    public Date getLastUpdateCheck();
}
