/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.cpd;

import java.util.List;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeException;

public interface CPD extends Node
{
	/**
	 * 
	 * @param settings
	 * @throws NodeException 
	 */
    public void setCPDSettings(CPDSettings settings) throws NodeException;
    public CPDSettings getCPDSettings();
    
    public CPDBaseSettings getBaseSettings();
    public void setBaseSettings(CPDBaseSettings baseSettings) throws NodeException;
    
    public List<HostDatabaseEntry> getCaptiveStatus();

    public List<CaptureRule> getCaptureRules();
    public void setCaptureRules( List<CaptureRule> captureRules ) throws NodeException;
    
    public List<PassedClient> getPassedClients();
    public void setPassedClients( List<PassedClient> newValue ) throws NodeException; 
       
    public List<PassedServer> getPassedServers();
    public void setPassedServers( List<PassedServer> newValue ) throws NodeException;
    
    public void setAll( CPDBaseSettings baseSettings, List<CaptureRule> captureRules,
            List<PassedClient> passedClients, List<PassedServer> passedServers ) throws NodeException;
    
    
    /**
     * Return true iff the username and password can be authenticated in the current parameters.
     * @param username Username
     * @param password Password
     * @param credentials  unused.  Could be used for alternative schemes in the future.
     * @return True if the user is authenticated.
     */
    public boolean authenticate( String address, String username, String password, String credentials );
    
    /**
     * Return truee iff the user was logged out.
     * @param address Address to remove from the cache.
     * @return
     */
    public boolean logout( String address );
    
    public EventManager<CPDLoginEvent> getLoginEventManager();
    public EventManager<BlockEvent> getBlockEventManager();
    
    public enum BlingerType { BLOCK, AUTHORIZE };

    /** 
     * Increment a blinger.
     * @param blingerType The type of blinger.
     * @param delta Amount to increment it by.  Agreggate events and then send this periodically.
     */
    public void incrementCount(BlingerType blingerType, long delta);

}
