/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Spyware.java,v 1.2 2005/01/20 08:26:28 amread Exp $
 */

package com.metavize.tran.spyware;

import com.metavize.mvvm.tran.Transform;

public interface Spyware extends Transform
{
    public SpywareSettings getSpywareSettings();

    public void setSpywareSettings(SpywareSettings settings);
}
