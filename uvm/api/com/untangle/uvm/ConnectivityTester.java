/**
 * $Id$
 */
package com.untangle.uvm;

import org.json.JSONObject;

/**
 * An interface for testing whether or not network connectivity is working.
 */
public interface ConnectivityTester
{
    /**
     * Retrieve the connectivity status of the network.
     *
     * Returns JSON, with two fields isTcpWorking and isDnsWorking
     * { "isTcpWorking": "true/false", "isDnsWorking":"true/false" }
     *
     *
     * @return The current status of the network.
     */
    JSONObject getStatus();
}
