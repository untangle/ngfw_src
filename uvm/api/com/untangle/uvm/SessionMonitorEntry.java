/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.Tag;

/**
 * This class represents a conntrack entry
 */
@SuppressWarnings("serial")
public class SessionMonitorEntry implements Serializable, JSONString
{
    private String protocol;
    private String state;

    private String hostname;
    private String username;
    private InetAddress localAddr;
    private InetAddress remoteAddr;

    private InetAddress preNatClient;
    private InetAddress preNatServer;
    private Integer preNatClientPort;
    private Integer preNatServerPort;

    private InetAddress postNatClient;
    private InetAddress postNatServer;
    private Integer postNatClientPort;
    private Integer postNatServerPort;

    private Integer qosPriority;
    private Boolean bypassed = null;
    private Integer mark;
    
    private String clientCountry;
    private Double clientLatitude;
    private Double clientLongitude;
    
    private String serverCountry;
    private Double serverLatitude;
    private Double serverLongitude;

    private List<Tag> tags;
    private String tagsString;
    
    public String getProtocol() {return protocol;}
    public void   setProtocol( String protocol ) {this.protocol = protocol;}

    public String getState() {return state;}
    public void   setState( String state ) {this.state = state;}

    public String getHostname() {return hostname;}
    public void   setHostname( String hostname ) {this.hostname = hostname;}
    public String getUsername() {return username;}
    public void   setUsername( String username ) {this.username = username;}
    public InetAddress getLocalAddr() { return localAddr; }
    public void setLocalAddr(InetAddress newValue) { this.localAddr = newValue; }
    public InetAddress getRemoteAddr() { return remoteAddr; }
    public void setRemoteAddr(InetAddress newValue) { this.remoteAddr = newValue; }

    public InetAddress getPreNatClient() {return preNatClient;}
    public void        setPreNatClient( InetAddress preNatClient ) {this.preNatClient = preNatClient;}
    public InetAddress getPreNatServer() {return preNatServer;}
    public void        setPreNatServer( InetAddress preNatServer ) {this.preNatServer = preNatServer;}

    public Integer  getPreNatClientPort() {return preNatClientPort;}
    public void     setPreNatClientPort( Integer preNatClientPort ) {this.preNatClientPort = preNatClientPort;}
    public Integer  getPreNatServerPort() {return preNatServerPort;}
    public void     setPreNatServerPort( Integer preNatServerPort ) {this.preNatServerPort = preNatServerPort;}

    public InetAddress getPostNatClient() {return postNatClient;}
    public void        setPostNatClient( InetAddress postNatClient ) {this.postNatClient = postNatClient;}
    public InetAddress getPostNatServer() {return postNatServer;}
    public void        setPostNatServer( InetAddress postNatServer ) {this.postNatServer = postNatServer;}

    public Integer  getPostNatClientPort() {return postNatClientPort;}
    public void     setPostNatClientPort( Integer postNatClientPort ) {this.postNatClientPort = postNatClientPort;}
    public Integer  getPostNatServerPort() {return postNatServerPort;}
    public void     setPostNatServerPort( Integer postNatServerPort ) {this.postNatServerPort = postNatServerPort;}

    public Integer getQosPriority() {return qosPriority;}
    public void    setQosPriority( Integer qosPriority ) {this.qosPriority = qosPriority;}

    public Boolean getBypassed() {return bypassed;}
    public void    setBypassed( Boolean bypassed ) {this.bypassed = bypassed;}

    public Integer getMark() {return mark;}
    public void    setMark( Integer mark ) {this.mark = mark;}

    public String  getClientCountry() { return clientCountry; }
    public void    setClientCountry(String clientCountry) { this.clientCountry = clientCountry; }
    public Double  getClientLatitude() { return clientLatitude; }
    public void    setClientLatitude(Double clientLatitude) { this.clientLatitude = clientLatitude; }
    public Double  getClientLongitude() { return clientLongitude; }
    public void    setClientLongitude(Double clientLongitude) { this.clientLongitude = clientLongitude; }

    public String  getServerCountry() { return serverCountry; }
    public void    setServerCountry(String serverCountry) { this.serverCountry = serverCountry; }
    public Double  getServerLatitude() { return serverLatitude; }
    public void    setServerLatitude(Double serverLatitude) { this.serverLatitude = serverLatitude; }
    public Double  getServerLongitude() { return serverLongitude; }
    public void    setServerLongitude(Double serverLongitude) { this.serverLongitude = serverLongitude; }

    public List<Tag> getTags() { return this.tags; }
    public void setTags(List<Tag> newValue) { this.tags = newValue; }
    public String getTagsString() { return this.tagsString; }
    public void setTagsString(String newValue) { this.tagsString = newValue; }
    
    /**
     * The following properties are UVM properties and are only set if you call MergedSessionMonitorEntrys
     */
    private Long creationTime = null;
    private Long sessionId = null;
    private String policy;
    private Integer clientIntf;
    private Integer serverIntf;
    private Boolean portForwarded;
    private Boolean natted;
    private Integer priority;
    private String pipeline;

    private Map<String,Object> attachments;
    
    public Long getCreationTime() {return creationTime;}
    public void setCreationTime( Long newValue ) {this.creationTime = newValue;}

    public Long getSessionId() {return sessionId;}
    public void setSessionId( Long newValue ) {this.sessionId = newValue;}

    public String getPolicy() {return policy;}
    public void   setPolicy( String newValue ) {this.policy = newValue;}

    public Integer getClientIntf() {return clientIntf;}
    public void    setClientIntf( Integer clientIntf ) {this.clientIntf = clientIntf;}
    public Integer getServerIntf() {return serverIntf;}
    public void    setServerIntf( Integer serverIntf ) {this.serverIntf = serverIntf;}

    public Boolean getPortForwarded() {return portForwarded;}
    public void    setPortForwarded( Boolean portForwarded ) {this.portForwarded = portForwarded;}

    public Boolean getNatted() {return natted;}
    public void    setNatted( Boolean natted ) {this.natted = natted;}

    public Integer getPriority() {return priority;}
    public void    setPriority( Integer priority ) {this.priority = priority;}

    public String  getPipeline() {return pipeline;}
    public void    setPipeline( String newValue ) {this.pipeline = newValue;}

    public Map<String,Object> getAttachments() {return attachments;}

    /**
     * Set the attachments for this session
     * This only includes the Serializable attachments
     * Because these entries are sent to the UI all these attachments will be serialized.
     * This avoids any cases where non-serializable attachments since the UI can't use them anyway
     * and they may not serialize correctly
     */
    public void               setAttachments( Map<String,Object> attachments )
    {
        // create a copy to avoid CME
        HashMap<String,Object> oldMap = new HashMap<String,Object>( attachments );
        this.attachments = new HashMap<String,Object>();
        for ( String key : oldMap.keySet() ) {
            Object obj = oldMap.get( key );
            if ( obj instanceof java.io.Serializable ) {

                // Ignore lists and maps.
                // These can be very large and are currently not used by the UI
                // Also serializing them can cause exceptions if they are being modified
                if ( obj instanceof java.util.List )
                    continue;
                if ( obj instanceof java.util.Map )
                    continue;

                this.attachments.put( key, obj );
            }
        }
    }
    
    /**
     * The following properties come from jnettop
     */
    private Float clientKBps;
    private Float serverKBps;
    private Float totalKBps;

    public Float getClientKBps() {return clientKBps;}
    public void  setClientKBps( Float clientKBps ) {this.clientKBps = clientKBps;}
    public Float getServerKBps() {return serverKBps;}
    public void  setServerKBps( Float serverKBps ) {this.serverKBps = serverKBps;}
    public Float getTotalKBps() {return totalKBps;}
    public void  setTotalKBps( Float totalKBps ) {this.totalKBps = totalKBps;}

    public String toString()
    {
        return getProtocol() + "| " + getPreNatClient().getHostAddress() + ":" + getPreNatClientPort() + " -> " + getPostNatServer().getHostAddress() + ":" + getPostNatServerPort();
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
