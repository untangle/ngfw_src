
package com.untangle.uvm;

import java.net.InetAddress;

/**
 * This class is an object that represents a conntrack session
 * It is used for JSON serialization of the ut-conntrack script
 */
public class SessionMonitorEntry
{
    private String protocol;
    private String state;

    private InetAddress preNatClient;
    private InetAddress preNatServer;
    private Integer preNatClientPort;
    private Integer preNatServerPort;

    private InetAddress postNatClient;
    private InetAddress postNatServer;
    private Integer postNatClientPort;
    private Integer postNatServerPort;

    private Integer qosPriority;
    private Boolean bypassed;
    private Boolean localTraffic;

    public String getProtocol() {return protocol;}
    public void   setProtocol( String protocol ) {this.protocol = protocol;}

    public String getState() {return state;}
    public void   setState( String state ) {this.state = state;}

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

    public Boolean getLocalTraffic() {return localTraffic;}
    public void    setLocalTraffic( Boolean localTraffic ) {this.localTraffic = localTraffic;}
    
    /**
     * The following properties are UVM properties and are only set if you call MergedSessionMonitorEntrys
     */
    private String policy;
    private Integer clientIntf;
    private Integer serverIntf;
    private Boolean portForwarded;
    private Boolean natted;
    private Integer priority;

    public String getPolicy() {return policy;}
    public void   setPolicy( String policy ) {this.policy = policy;}

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
        return getProtocol() + "| " + getPreNatClient() + ":" + getPreNatClientPort() + " -> " + getPostNatServer() + ":" + getPostNatServerPort();
    }
    
}