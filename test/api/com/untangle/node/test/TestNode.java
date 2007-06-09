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

package com.untangle.node.test;

import com.untangle.uvm.node.Node;

public interface TestNode extends Node
{
    public void setTestSettings(TestSettings settings);
    public TestSettings getTestSettings();
}
