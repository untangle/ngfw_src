/**
 * $Id$
 */
package com.untangle.app.wan_failover;

import java.io.Serializable;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for a given WAN Test
 */
@SuppressWarnings("serial")
public class WanTestSettings implements Serializable, JSONString
{
    private static final int BUCKET_SIZE_MIN = 10;
    private static final int BUCKET_SIZE_MAX = 50;

    private Boolean enabled;
    private Integer interfaceId;
    private String  description;
    private String  type;
    private Integer timeoutMilliseconds = 2000;
    private Integer delayMilliseconds = 5000;
    private Integer testHistorySize = 10;
    private Integer failureThreshold = 3;

    private String pingHostname;
    private String httpUrl;

    public Boolean getEnabled()
    {
        return this.enabled;
    }

    public void setEnabled( Boolean enabled )
    {
        this.enabled = enabled;
    }
    
    public Integer getInterfaceId()
    {
        return this.interfaceId;
    }

    public void setInterfaceId( Integer newValue )
    {
        this.interfaceId = newValue;
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription( String newValue )
    {
        this.description = newValue;
    }

    public String getType()
    {
        return this.type;
    }

    public void setType( String newValue )
    {
        this.type = newValue;
    }
    
    public Integer getTimeoutMilliseconds()
    {
        return this.timeoutMilliseconds;
    }

    public void setTimeoutMilliseconds( Integer newValue )
    {
        this.timeoutMilliseconds = newValue;
    }

    public Integer getDelayMilliseconds()
    {
        return this.delayMilliseconds;
    }

    public void setDelayMilliseconds( Integer newValue )
    {
        this.delayMilliseconds = newValue;
    }

    public Integer getTestHistorySize()
    {
        if ( this.testHistorySize < BUCKET_SIZE_MIN ) this.testHistorySize = BUCKET_SIZE_MIN;
        if ( this.testHistorySize > BUCKET_SIZE_MAX ) this.testHistorySize = BUCKET_SIZE_MAX;
        
        return this.testHistorySize;
    }

    public void setTestHistorySize( Integer newValue )
    {
        if ( newValue < BUCKET_SIZE_MIN ) newValue = BUCKET_SIZE_MIN;
        if ( newValue > BUCKET_SIZE_MAX ) newValue = BUCKET_SIZE_MAX;

        this.testHistorySize = newValue;
    }

    public Integer getFailureThreshold()
    {
        if ( this.failureThreshold < 1 ) this.failureThreshold = 1;
        return this.failureThreshold;
    }

    public void setFailureThreshold( Integer newValue )
    {
        if ( newValue < 1 ) newValue = 1;

        this.failureThreshold = newValue;
    }

    public String getPingHostname()
    {
        return this.pingHostname;
    }

    public void setPingHostname( String newValue )
    {
        this.pingHostname = newValue;
    }

    public String getHttpUrl()
    {
        return this.httpUrl;
    }

    public void setHttpUrl( String newValue )
    {
        this.httpUrl = newValue;
    }

    public String toString()
    {
        return toJSONString();
    }
            
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}

