/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.nat;


import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.node.Node;

import com.untangle.uvm.networking.SetupState;

public interface Nat extends Node
{
    public NatCommonSettings getNatSettings();
    public void setNatSettings( NatCommonSettings settings ) throws Exception;

    public SetupState getSetupState();

    /* Reinitialize the settings to basic nat */
    public void resetBasic() throws Exception;
    
    /* Convert the basic settings to advanced Network Spaces */
    public void switchToAdvanced() throws Exception;

    public EventManager<LogEvent> getEventManager();
}
