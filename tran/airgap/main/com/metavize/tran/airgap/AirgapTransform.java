/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: AirgapTransform.java,v 1.2 2005/01/29 06:19:37 amread Exp $
 */

package com.metavize.tran.airgap;

import com.metavize.mvvm.tran.Transform;

public interface AirgapTransform extends Transform
{
    public void setAirgapSettings(AirgapSettings settings);
    public AirgapSettings getAirgapSettings();
}
