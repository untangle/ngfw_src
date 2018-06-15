/**
 * $Id$
 */

package com.untangle.app.wan_failover;

import java.io.Serializable;
import org.json.JSONString;

/**
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class WanStatus implements Serializable, JSONString
{
    private Integer interfaceId;
    private String interfaceName;
    private String systemName;
    private Boolean online;

    private Integer totalTestsRun = 0;
    private Integer totalTestsPassed = 0;
    private Integer totalTestsFailed = 0;

    public WanStatus(Integer interfaceId, String interfaceName, String systemName, Boolean online)
    {
        this.interfaceId = interfaceId;
        this.interfaceName = interfaceName;
        this.systemName = systemName;
        this.online = online;
    }

    public Integer getInterfaceId()
    {
        return this.interfaceId;
    }

    public void setInterfaceId(Integer newValue)
    {
        this.interfaceId = newValue;
    }

    public String getInterfaceName()
    {
        return this.interfaceName;
    }

    public void setInterfaceName(String newValue)
    {
        this.interfaceName = newValue;
    }

    public String getSystemName()
    {
        return this.systemName;
    }

    public void setSystemName(String newValue)
    {
        this.systemName = newValue;
    }

    public Boolean getOnline()
    {
        return this.online;
    }

    public void setOnline(Boolean newValue)
    {
        this.online = newValue;
    }

    public Integer getTotalTestsRun()
    {
        return this.totalTestsRun;
    }

    public void setTotalTestsRun(Integer newValue)
    {
        this.totalTestsRun = newValue;
    }

    public Integer getTotalTestsPassed()
    {
        return this.totalTestsPassed;
    }

    public void setTotalTestsPassed(Integer newValue)
    {
        this.totalTestsPassed = newValue;
    }

    public Integer getTotalTestsFailed()
    {
        return this.totalTestsFailed;
    }

    public void setTotalTestsFailed(Integer newValue)
    {
        this.totalTestsFailed = newValue;
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
