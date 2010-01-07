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

import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.user.ADLoginEvent;

public interface CPD extends Node
{
	/**
	 * 
	 * @param settings
	 */
    public void setCPDSettings(CPDSettings settings);
    public CPDSettings getCPDSettings();
    
    public CPDBaseSettings getBaseSettings();
    public void setBaseSettings(CPDBaseSettings baseSettings);
    
    public List<CaptureRule> getCaptureRules();
    public void setCaptureRules( List<CaptureRule> captureRules );
    
    public List<PassedClient> getPassedClients();
    public void setPassedClients( List<PassedClient> newValue ); 
       
    public List<PassedServer> getPassedServers();
    public void setPassedServers( List<PassedServer> newValue );
    
    public void setAll( CPDBaseSettings baseSettings, List<CaptureRule> captureRules,
            List<PassedClient> passedClients, List<PassedServer> passedServers );
    
    /**
     * 
     * @param address The IP Address of the username to register
     * @param username The username associated with this IP Address.
     * @param expirationDate When this IP -> Username association is no longer valid.
     * @return
     * The current username at this address, or null if there isn't anything logged in there.
     */
    public String registerUser( String address, String username, Date expirationDate ) throws UnknownHostException;
    public Map<String,String> getUserMap();
    public String removeUser( String address ) throws UnknownHostException;
    
    public EventManager<ADLoginEvent> getLoginEventManager();
    public EventManager<BlockEvent> getBlockEventManager();

}
