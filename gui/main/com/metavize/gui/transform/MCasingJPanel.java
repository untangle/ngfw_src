/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.transform;

import com.metavize.mvvm.*;
import com.metavize.mvvm.toolbox.MackageDesc;
import com.metavize.mvvm.tran.*;

public abstract class MCasingJPanel<T extends CompoundSettings> extends javax.swing.JPanel
    implements Savable<T>, Refreshable<T> {

    public MCasingJPanel(){
    }

    public abstract String getDisplayName();
    public abstract void doSave(T compoundSettings, boolean validateOnly) throws Exception;
    public abstract void doRefresh(T compoundSettings);
}
