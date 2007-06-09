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

package com.untangle.node.virus;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;

public interface VirusNode extends Node
{
    void setVirusSettings(VirusSettings virusSettings);
    VirusSettings getVirusSettings();
    EventManager<VirusEvent> getEventManager();
}
