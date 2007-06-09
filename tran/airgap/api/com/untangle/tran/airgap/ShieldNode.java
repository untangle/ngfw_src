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

package com.untangle.tran.airgap;

import java.util.List;

import com.untangle.mvvm.tran.Transform;

public interface AirgapTransform extends Transform
{
    public void setAirgapSettings(AirgapSettings settings);
    public AirgapSettings getAirgapSettings();

    List<ShieldRejectionLogEntry> getLogs( int limit );
}
