/*
 * $Id: SessionMatcher.java,v 1.00 2012/12/04 13:17:13 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.Map;
import java.net.InetAddress;

import com.untangle.uvm.vnet.NodeSession;

/**
 * This is a simple class to overload to create a function/object for matching sessions using its attributes
 */
public interface SessionMatcher
{
    /**
     * Alternate session match test
     */
    boolean isMatch( Long policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String,Object> attachments );
}
