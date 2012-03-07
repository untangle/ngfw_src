/**
 * $Id$
 */
package com.untangle.node.cpd;

import java.util.List;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.EventLogQuery;

public interface CPD extends Node
{
	/**
	 *
	 * @param settings
	 * @throws Exception
	 */
    void setSettings(CPDSettings settings) throws Exception;
    CPDSettings getSettings();

    List<HostDatabaseEntry> getCaptiveStatus();

    List<CaptureRule> getCaptureRules();
    void setCaptureRules( List<CaptureRule> captureRules ) throws Exception;

    List<PassedClient> getPassedClients();
    void setPassedClients( List<PassedClient> newValue ) throws Exception;

    List<PassedServer> getPassedServers();
    void setPassedServers( List<PassedServer> newValue ) throws Exception;

    /**
     * Return true iff the username and password can be authenticated in the current parameters.
     * @param username Username
     * @param password Password
     * @param credentials  unused.  Could be used for alternative schemes in the future.
     * @return True if the user is authenticated.
     */
    boolean authenticate( String address, String username, String password, String credentials );

    /**
     * Return truee iff the user was logged out.
     * @param address Address to remove from the cache.
     * @return
     */
    boolean logout( String address );

    EventLogQuery[] getLoginEventQueries();
    EventLogQuery[] getBlockEventQueries();

    enum BlingerType { BLOCK, AUTHORIZE };

    /**
     * Increment a blinger.
     * @param blingerType The type of blinger.
     * @param delta Amount to increment it by.  Agreggate events and then send this periodically.
     */
    void incrementCount(BlingerType blingerType, long delta);
}
