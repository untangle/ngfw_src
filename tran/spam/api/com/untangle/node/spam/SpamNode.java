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

package com.untangle.node.spam;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;

public interface SpamNode extends Node
{
    void setSpamSettings(SpamSettings spamSettings);
    SpamSettings getSpamSettings();

    EventManager<SpamEvent> getEventManager();
    EventManager<SpamSMTPRBLEvent> getRBLEventManager();
}
