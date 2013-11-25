/*
 * $Id: RackManager.java,v 1.00 2011/09/24 20:53:51 dmorris Exp $
 */
package com.untangle.uvm;

/**
 * A utility class for the UI to render the rack and perform
 * rack-related operations.
 */
public interface RackManager
{
    /**
     * Get the view of the rack when the specified rack is the current rack
     *
     * @param p policy.
     * @return visible nodes for this policy.
     */
    RackView getRackView( Long policyId );

    /**
     * Install a Package in the Toolbox and instantiate in the Rack.
     *
     * @param name the name of the Package.
     * @param p the policy to install
     * @exception Exception when <code>name</code> cannot
     *     be installed.
     */
    void instantiate(String name, Long policyId) throws Exception;
}
