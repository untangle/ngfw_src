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

package com.untangle.node.webfilter;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;

public interface WebFilter extends Node
{
    WebFilterSettings getWebFilterSettings();
    void setWebFilterSettings(WebFilterSettings settings);
    WebFilterBlockDetails getDetails(String nonce);

    EventManager<WebFilterEvent> getEventManager();
}
