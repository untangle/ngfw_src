/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.nat;


import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tran.Transform;

import com.metavize.mvvm.networking.SetupState;

public interface Nat extends Transform
{
    public NatCommonSettings getNatSettings();
    public void setNatSettings( NatCommonSettings settings ) throws Exception;

    public SetupState getSetupState();

    /* Reinitialize the settings to basic nat */
    public void resetBasic();
    
    /* Convert the basic settings to advanced Network Spaces */
    public void switchToAdvanced();

    public EventManager<LogEvent> getEventManager();
}
