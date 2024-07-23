/**
 * $Id: SyslogServer.java,v 1.00 2024/03/27 12:13:17 rohitsingh Exp $
 */
package com.untangle.uvm.event;
import java.util.LinkedList;
import java.io.Serializable;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;
import org.json.JSONObject;

/**
 * This is the class which defines SyslogServer
 * 
 * A SyslogServer is a host which received logs
 * based on configured SyslogRules
 */
@SuppressWarnings("serial")
@ValidSerializable
public class SyslogServer implements Serializable, JSONString {
    private int serverId = -1;
    private boolean enabled = false;
    private String host;
    private int port = 514;
    private String protocol = "UDP";
    private String tag = "";
    private String description;

    /**
     * Initialize empty instance of SyslogServer. 
    */
    public SyslogServer() { }


    /**
     * Initialize instance of SyslogServer.
     * @param  serverId               integer serverID
     * @param  enabled                Boolean if true , logs should be send to this server.
     * @param  host                   String, ip address/host of server
     * @param  port                   integer, port of server  boolean if true remote syslog the event, otherwise don't syslog
     * @param  protocol               integer, protocol for communication UDP/TCP
     * @param  tag                    String, server identifier
     * @param  description            String, server description
     */
    public SyslogServer(int serverId, boolean enabled, String host, int port, String protocol, String tag, String description) {
        this.serverId = serverId;
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.protocol = protocol;
        this.tag = tag;
        this.description = description;
    }

    public int getServerId() {
        return serverId;
    }
    public void setServerId(int serverId) {
        this.serverId = serverId;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public String getProtocol() {
        return protocol;
    }
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
