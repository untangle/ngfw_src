
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
    private short preNatSrcPort;
    private short preNatDstPort;

    private InetAddress postNatSrc;
    private InetAddress postNatDst;
    private short postNatSrcPort;
    private short postNatDstPort;

    private int qosPriority;
    private boolean bypassed;

    private String getProtocol() {return protocol;}
    private void   setProtocol( String protocol ) {this.protocol = protocol;}

    private String getState() {return state;}
    private void   setState( String state ) {this.state = state;}

    private InetAddress getPreNatSrc() {return preNatSrc;}
    private void        setPreNatSrc( InetAddress preNatSrc ) {this.preNatSrc = preNatSrc;}
    private InetAddress getPreNatDst() {return preNatDst;}
    private void        setPreNatDst( InetAddress preNatDst ) {this.preNatDst = preNatDst;}

    private Short  getPreNatSrcPort() {return new Short(preNatSrcPort);}
    private void   setPreNatSrcPort( Short preNatSrcPort ) {this.preNatSrcPort = preNatSrcPort.shortValue();}
    private Short  getPreNatDstPort() {return new Short(preNatDstPort);}
    private void   setPreNatDstPort( Short preNatDstPort ) {this.preNatDstPort = preNatDstPort.shortValue();}

    private InetAddress getPostNatSrc() {return postNatSrc;}
    private void        setPostNatSrc( InetAddress postNatSrc ) {this.postNatSrc = postNatSrc;}
    private InetAddress getPostNatDst() {return postNatDst;}
    private void        setPostNatDst( InetAddress postNatDst ) {this.postNatDst = postNatDst;}

    private Short  getPostNatSrcPort() {return new Short(postNatSrcPort);}
    private void   setPostNatSrcPort( Short postNatSrcPort ) {this.postNatSrcPort = postNatSrcPort.shortValue();}
    private Short  getPostNatDstPort() {return new Short(postNatDstPort);}
    private void   setPostNatDstPort( Short postNatDstPort ) {this.postNatDstPort = postNatDstPort.shortValue();}

    private Integer getQosPriority() {return new Integer(qosPriority);}
    private void   setQosPriority( Integer qosPriority ) {this.qosPriority = qosPriority.intValue();}

    private Boolean getBypassed() {return new Boolean(bypassed);}
    private void   setBypassed( Boolean bypassed ) {this.bypassed = bypassed.booleanValue();}
    
}