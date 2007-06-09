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

package com.untangle.node.ids;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;

public interface IDSNode extends Node {
    IDSSettings getIDSSettings();
    void setIDSSettings(IDSSettings settings);
    EventManager<IDSLogEvent> getEventManager();
}
