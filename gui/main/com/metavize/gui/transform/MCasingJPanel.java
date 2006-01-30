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

public abstract class MCasingJPanel extends javax.swing.JPanel implements Savable, Refreshable {

    protected TransformContext transformContext;
    public TransformContext getTransformContext(){ return transformContext; }
    protected MackageDesc mackageDesc;
    public MackageDesc getMackageDesc(){ return mackageDesc; }

    public MCasingJPanel(TransformContext transformContext){
        this.transformContext = transformContext;
        this.mackageDesc = transformContext.getMackageDesc();
    }

    public abstract void doSave(Object settings, boolean validateOnly) throws Exception;
    public abstract void doRefresh(Object settings);
}
