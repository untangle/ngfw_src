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

package com.metavize.tran.virus;

import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tran.Transform;

public interface VirusTransform extends Transform
{
    void setVirusSettings(VirusSettings virusSettings);
    VirusSettings getVirusSettings();
    EventManager<VirusEvent> getEventManager();
}
