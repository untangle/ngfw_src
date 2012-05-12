/**
 * $Id$
 */
package com.untangle.uvm.argon;

public interface ArgonSessionDesc extends com.untangle.uvm.node.SessionTuple, com.untangle.uvm.node.SessionStats
{
    /**
     * return the globally unique session ID
     */
    long id();
}

