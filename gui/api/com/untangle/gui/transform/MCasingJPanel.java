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

package com.untangle.gui.transform;

import com.untangle.mvvm.*;
import com.untangle.mvvm.toolbox.MackageDesc;
import com.untangle.mvvm.tran.*;

public abstract class MCasingJPanel<T extends CompoundSettings> extends javax.swing.JPanel
    implements Savable<T>, Refreshable<T> {

    public MCasingJPanel(){
    }

    public abstract void setSettingsChangedListener(SettingsChangedListener settingsChangedListener);
    public abstract String getDisplayName();
    public abstract void doSave(T compoundSettings, boolean validateOnly) throws Exception;
    public abstract void doRefresh(T compoundSettings);
}
