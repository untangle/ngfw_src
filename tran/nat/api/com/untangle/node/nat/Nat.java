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
package com.untangle.tran.nat;


import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.tran.Transform;

import com.untangle.mvvm.networking.SetupState;

public interface Nat extends Transform
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
