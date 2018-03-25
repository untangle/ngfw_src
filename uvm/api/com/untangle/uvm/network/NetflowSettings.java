/**
 * $Id: NetflowSettings.java 37267 2016-07-25 23:42:19Z cblaise $
 */
package com.untangle.uvm.network;

import org.apache.log4j.Logger;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * NetFlow settings.
 */
@SuppressWarnings("serial")
public class NetflowSettings implements Serializable, JSONString
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private boolean enabled = false;
    private String host = "1.2.3.4";
    private Integer port = 2055;
    private Integer version = 9;
    
    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public String getHost() { return this.host; }
    public void setHost( String newValue ) { this.host = newValue; }

    public Integer getPort() { return this.port; }
    public void setPort( Integer newValue ) { this.port = newValue; }

    public Integer getVersion() { return this.version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
