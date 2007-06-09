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

package com.untangle.tran.reporting;

import com.untangle.mvvm.tran.Transform;

public interface ReportingTransform extends Transform
{
    public void setReportingSettings(ReportingSettings settings);
    public ReportingSettings getReportingSettings();
}
