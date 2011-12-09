/**
 * $Id: SystemInfo.java,v 1.00 2011/12/08 16:07:15 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

/**
 * The system info (UID, build version, java version) for the Untangle customer
 * @version 1.0
 */
@SuppressWarnings("serial")
public class SystemInfo implements Serializable
{
    private String uid;
    private String fullVersion;
    private Boolean terminalActivated;
    
    public SystemInfo() { }

    public SystemInfo(String uid, String fullVersion, Boolean terminalActivated)
    {
        this.uid = uid;
        this.fullVersion = fullVersion;
        this.terminalActivated = terminalActivated;
    }

    public String getServerUID()
    {
        return uid;
    }

    public void setUID(String uid)
    {
        this.uid = uid;
    }

    public String getFullVersion()
    {
        return fullVersion;
    }

    public void setFullVersion(String fullVersion)
    {
        this.fullVersion = fullVersion;
    }

    public Boolean getTerminalActivated()
    {
        return this.terminalActivated;
    }

    public void setTerminalActivated( Boolean activated )
    {
        this.terminalActivated = activated;
    }
}
