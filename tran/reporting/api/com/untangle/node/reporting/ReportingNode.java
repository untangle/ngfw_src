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

package com.untangle.node.reporting;

import com.untangle.uvm.node.Node;

public interface ReportingNode extends Node
{
    public void setReportingSettings(ReportingSettings settings);
    public ReportingSettings getReportingSettings();
}
