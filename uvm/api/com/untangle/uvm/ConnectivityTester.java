/*
 * $Id$
 */
package com.untangle.uvm;

/**
 * An interface for testing whether or not network connectivity is working.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface ConnectivityTester
{
    /**
     * Retrieve the connectivity status of the network.
     *
     * @return The current status of the network.
     */
    Status getStatus();

    /**
     * Interface that describes the current state of the network.
     */
    interface Status
    {
        /**
         * Whether or not DNS is working.
         *
         * @return True if DNS is working
         */
        boolean isDnsWorking();

        /**
         * Whether or not the Untangle is able to establish a TCP
         * connection to a server on the internet.
         *
         * @return True if the untangle is able to establish a TCP
         * connection to a sever on the internet.
         */
        boolean isTcpWorking();
    }
}
