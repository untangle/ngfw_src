/*
 * $Id: ConnectivityTester.java 34024 2013-02-26 19:42:44Z dmorris $
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
