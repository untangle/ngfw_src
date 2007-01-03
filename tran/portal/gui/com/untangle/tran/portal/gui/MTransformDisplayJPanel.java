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



package com.untangle.tran.portal.gui;

import com.untangle.gui.transform.*;

public class MTransformDisplayJPanel extends com.untangle.gui.transform.MTransformDisplayJPanel{


    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);

        super.activity0JLabel.setText("LOGIN");
        super.activity1JLabel.setText("WEB");
        super.activity2JLabel.setText("FILE");
        super.activity3JLabel.setText("DESKTOP");

    }

    final protected boolean getUpdateThroughput(){ return false; }
}
