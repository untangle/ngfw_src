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


package com.untangle.tran.reporting.gui;

import com.untangle.gui.transform.*;
import com.untangle.gui.pipeline.MPipelineJPanel;
import com.untangle.mvvm.tran.TransformContext;


public class MTransformDisplayJPanel extends com.untangle.gui.transform.MTransformDisplayJPanel{
    
    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel) throws Exception {
        super(mTransformJPanel);
        

    }

    final protected boolean getUpdateActivity(){ return false; }
    final protected boolean getUpdateSessions(){ return false; }
    final protected boolean getUpdateThroughput(){ return false; }
}
