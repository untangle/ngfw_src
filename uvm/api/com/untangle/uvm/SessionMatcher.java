/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.Map;
import java.net.InetAddress;

/**
 * This is a simple class to overload to create a function/object for matching sessions using its attributes
 */
public interface SessionMatcher
{
    /**
     * Alternate session match test
     */
    boolean isMatch( Integer policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String,Object> attachments );
}
