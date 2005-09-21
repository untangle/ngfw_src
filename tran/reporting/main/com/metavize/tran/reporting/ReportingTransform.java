/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.reporting;

import com.metavize.mvvm.tran.Transform;

public interface ReportingTransform extends Transform
{
    public void setReportingSettings(ReportingSettings settings);
    public ReportingSettings getReportingSettings();
}
