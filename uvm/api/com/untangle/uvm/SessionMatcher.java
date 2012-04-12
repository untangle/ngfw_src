/*
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.node.IPSessionDesc;

public interface SessionMatcher
{
    /**
     * Tells if the session matches
     */
    boolean isMatch( Long policyId, IPSessionDesc clientSide, IPSessionDesc serverSide );
}
