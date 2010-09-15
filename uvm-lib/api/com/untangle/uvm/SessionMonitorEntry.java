
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
    private int preNatClientPort;
    private int preNatServerPort;

    private InetAddress postNatClient;
    private InetAddress postNatServer;
    private int postNatClientPort;
    private int postNatServerPort;

    private int qosPriority;
    private boolean bypassed;

    public String getProtocol() {return protocol;}
    public void   setProtocol( String protocol ) {this.protocol = protocol;}

    public String getState() {return state;}
    public void   setState( String state ) {this.state = state;}

    public InetAddress getPreNatClient() {return preNatClient;}
    public void        setPreNatClient( InetAddress preNatClient ) {this.preNatClient = preNatClient;}
    public InetAddress getPreNatServer() {return preNatServer;}
    public void        setPreNatServer( InetAddress preNatServer ) {this.preNatServer = preNatServer;}

    public Integer  getPreNatClientPort() {return new Integer(preNatClientPort);}
    public void   setPreNatClientPort( Integer preNatClientPort ) {this.preNatClientPort = preNatClientPort.intValue();}
    public Integer  getPreNatServerPort() {return new Integer(preNatServerPort);}
    public void   setPreNatServerPort( Integer preNatServerPort ) {this.preNatServerPort = preNatServerPort.intValue();}

    public InetAddress getPostNatClient() {return postNatClient;}
    public void        setPostNatClient( InetAddress postNatClient ) {this.postNatClient = postNatClient;}
    public InetAddress getPostNatServer() {return postNatServer;}
    public void        setPostNatServer( InetAddress postNatServer ) {this.postNatServer = postNatServer;}

    public Integer  getPostNatClientPort() {return new Integer(postNatClientPort);}
    public void   setPostNatClientPort( Integer postNatClientPort ) {this.postNatClientPort = postNatClientPort.intValue();}
    public Integer  getPostNatServerPort() {return new Integer(postNatServerPort);}
    public void   setPostNatServerPort( Integer postNatServerPort ) {this.postNatServerPort = postNatServerPort.intValue();}

    public Integer getQosPriority() {return new Integer(qosPriority);}
    public void   setQosPriority( Integer qosPriority ) {this.qosPriority = qosPriority.intValue();}

    public Boolean getBypassed() {return new Boolean(bypassed);}
    public void   setBypassed( Boolean bypassed ) {this.bypassed = bypassed.booleanValue();}

    /**
     * The following properties are UVM properties and are only set if you call MergedSessionMonitorEntrys
     */
    private String policy;
    private int clientIntf;
    private int serverIntf;
    
    public String getPolicy() {return policy;}
    public void   setPolicy( String policy ) {this.policy = policy;}

    public Integer getClientIntf() {return new Integer(clientIntf);}
    public void    setClientIntf( Integer clientIntf ) {this.clientIntf = clientIntf.intValue();}
    public Integer getServerIntf() {return new Integer(serverIntf);}
    public void    setServerIntf( Integer serverIntf ) {this.serverIntf = serverIntf.intValue();}
    

    
    
}