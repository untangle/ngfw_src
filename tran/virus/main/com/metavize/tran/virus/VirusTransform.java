/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusTransform.java,v 1.3 2005/01/18 05:44:04 amread Exp $
 */

package com.metavize.tran.virus;

import com.metavize.mvvm.tran.Transform;

public interface VirusTransform extends Transform
{
    void setVirusSettings(VirusSettings virusSettings);
    VirusSettings getVirusSettings();
}
