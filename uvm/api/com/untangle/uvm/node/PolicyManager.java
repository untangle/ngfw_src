/**
 * $Id$
 */
package com.untangle.uvm.node;

import java.util.List;
import java.net.InetAddress;

import com.untangle.uvm.node.Node;

public interface PolicyManager
{
    /**
     * Find the correct policy ID based on the Policy Manager rules for a provided session parameters
     */
    Long findPolicyId( short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, String username, String hostname );
    
    /**
     * @param child: The node to test if this is a child.
     * @param parent: The node to see the distance from the child to parent.
     * @return.  The number of racks in between the child policy and parent.
     *   This is 0 if child == parent.
     *   This is -1 if child is not a child of parent.
     *   The null rack is a child of every parent.
     *   The null rack is never the parent of any child.
     */
    int getPolicyGenerationDiff( Long childId, Long parentId );

    /**
     * Returns the policy ID of the parent for the provided policy ID
     * Or null if the provided policy ID has no parent
     */
    Long getParentPolicyId( Long policyId );
}
