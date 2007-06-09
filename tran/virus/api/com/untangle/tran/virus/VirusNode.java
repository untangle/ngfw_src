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

package com.untangle.tran.virus;

import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.tran.Transform;

public interface VirusTransform extends Transform
{
    void setVirusSettings(VirusSettings virusSettings);
    VirusSettings getVirusSettings();
    EventManager<VirusEvent> getEventManager();
}
