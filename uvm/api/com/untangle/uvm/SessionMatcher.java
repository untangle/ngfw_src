/*
 * $Id$
 */
package com.untangle.uvm.localapi;

import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.policy.Policy;

public interface SessionMatcher
{
    /**
     * Tells if the session matches
     */
    boolean isMatch( Policy policy, IPSessionDesc clientSide, IPSessionDesc serverSide );
}
