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

package com.untangle.node.shield;

import java.util.List;

import com.untangle.uvm.node.Node;

public interface ShieldNode extends Node
{
    public void setShieldSettings(ShieldSettings settings);
    public ShieldSettings getShieldSettings();

    List<ShieldRejectionLogEntry> getLogs( int limit );
}
