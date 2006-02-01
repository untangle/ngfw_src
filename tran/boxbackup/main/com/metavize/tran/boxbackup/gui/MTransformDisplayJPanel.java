/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.boxbackup.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;


public class MTransformDisplayJPanel extends com.metavize.gui.transform.MTransformDisplayJPanel{
        
    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);       
    }
    
    protected boolean getUpdateActivity(){ return false; }
    protected boolean getUpdateSessions(){ return false; }
    protected boolean getUpdateThroughput(){ return false; }
}
