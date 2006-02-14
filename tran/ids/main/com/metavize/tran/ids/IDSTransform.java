/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids;

import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tran.Transform;

public interface IDSTransform extends Transform {
    IDSSettings getIDSSettings();
    void setIDSSettings(IDSSettings settings);
    EventManager<IDSLogEvent> getEventManager();
}
