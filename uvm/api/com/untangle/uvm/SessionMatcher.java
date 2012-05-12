/*
 * $Id$
 */
package com.untangle.uvm;

import java.util.Map;
import com.untangle.uvm.node.SessionTuple;

public interface SessionMatcher
{
    /**
     * Tells if the session matches
     */
    boolean isMatch( Long policyId, SessionTuple clientSide, SessionTuple serverSide, Map<String,Object> attachments );
}
