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

package com.metavize.tran.airgap;

import java.util.List;

import com.metavize.mvvm.tran.Transform;

public interface AirgapTransform extends Transform
{
    public void setAirgapSettings(AirgapSettings settings);
    public AirgapSettings getAirgapSettings();

    List<ShieldRejectionLogEntry> getLogs( int limit );
}
