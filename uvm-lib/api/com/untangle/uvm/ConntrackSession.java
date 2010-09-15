
package com.untangle.uvm;

import java.net.InetAddress;

/**
 * This class is an object that represents a conntrack session
 * It is used for JSON serialization of the ut-conntrack script
 */
public class ConntrackSession
{
    private String protocol;
    private String state;

    private InetAddress preNatSrc;
    private InetAddress preNatDst;
    private int preNatSrcPort;
    private int preNatDstPort;

    private InetAddress postNatSrc;
    private InetAddress postNatDst;
    private int postNatSrcPort;
    private int postNatDstPort;

    private int qosPriority;
    private boolean bypassed;

    public String getProtocol() {return protocol;}
    public void   setProtocol( String protocol ) {this.protocol = protocol;}

    public String getState() {return state;}
    public void   setState( String state ) {this.state = state;}

    public InetAddress getPreNatSrc() {return preNatSrc;}
    public void        setPreNatSrc( InetAddress preNatSrc ) {this.preNatSrc = preNatSrc;}
    public InetAddress getPreNatDst() {return preNatDst;}
    public void        setPreNatDst( InetAddress preNatDst ) {this.preNatDst = preNatDst;}

    public Integer  getPreNatSrcPort() {return new Integer(preNatSrcPort);}
    public void   setPreNatSrcPort( Integer preNatSrcPort ) {this.preNatSrcPort = preNatSrcPort.intValue();}
    public Integer  getPreNatDstPort() {return new Integer(preNatDstPort);}
    public void   setPreNatDstPort( Integer preNatDstPort ) {this.preNatDstPort = preNatDstPort.intValue();}

    public InetAddress getPostNatSrc() {return postNatSrc;}
    public void        setPostNatSrc( InetAddress postNatSrc ) {this.postNatSrc = postNatSrc;}
    public InetAddress getPostNatDst() {return postNatDst;}
    public void        setPostNatDst( InetAddress postNatDst ) {this.postNatDst = postNatDst;}

    public Integer  getPostNatSrcPort() {return new Integer(postNatSrcPort);}
    public void   setPostNatSrcPort( Integer postNatSrcPort ) {this.postNatSrcPort = postNatSrcPort.intValue();}
    public Integer  getPostNatDstPort() {return new Integer(postNatDstPort);}
    public void   setPostNatDstPort( Integer postNatDstPort ) {this.postNatDstPort = postNatDstPort.intValue();}

    public Integer getQosPriority() {return new Integer(qosPriority);}
    public void   setQosPriority( Integer qosPriority ) {this.qosPriority = qosPriority.intValue();}

    public Boolean getBypassed() {return new Boolean(bypassed);}
    public void   setBypassed( Boolean bypassed ) {this.bypassed = bypassed.booleanValue();}

    /**
     * The following properties are UVM properties and are only set if you call MergedConntrackSessions
     */
    private String policy;
    
    public String getPolicy() {return policy;}
    public void   setPolicy( String policy ) {this.policy = policy;}
    

    
    
}