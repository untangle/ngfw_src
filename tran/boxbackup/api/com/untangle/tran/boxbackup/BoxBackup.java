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
package com.untangle.tran.boxbackup;

import com.untangle.mvvm.tran.Transform;
import com.untangle.mvvm.logging.EventManager;

public interface BoxBackup extends Transform
{
    BoxBackupSettings getBoxBackupSettings();
    void setBoxBackupSettings(BoxBackupSettings settings);
    EventManager<BoxBackupEvent> getEventManager();
}
