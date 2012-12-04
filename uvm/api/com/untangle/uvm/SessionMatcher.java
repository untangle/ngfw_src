/*
 * $Id$
 */
package com.untangle.uvm;

import java.util.Map;
import java.net.InetAddress;

import com.untangle.uvm.vnet.NodeSession;

/**
 * This is a simple class to overload to create a function/object for matching sessions using the NodeSession
 */
public interface SessionMatcher
{
    /**
     * Tells if the session matches
     */
    boolean isMatch( NodeSession sess );
}
