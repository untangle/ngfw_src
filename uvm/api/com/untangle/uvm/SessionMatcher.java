/*
 * $Id$
 */
package com.untangle.uvm;

import java.util.Map;
import com.untangle.uvm.node.IPSessionDesc;

public interface SessionMatcher
{
    /**
     * Tells if the session matches
     */
    boolean isMatch( Long policyId, IPSessionDesc clientSide, IPSessionDesc serverSide, Map<String,Object> attachments );
}
