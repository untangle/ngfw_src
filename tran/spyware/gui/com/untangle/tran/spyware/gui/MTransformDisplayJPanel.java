/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */



package com.untangle.tran.spyware.gui;

import com.untangle.gui.transform.*;

public class MTransformDisplayJPanel extends com.untangle.gui.transform.MTransformDisplayJPanel{


    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);

        super.activity0JLabel.setText("BLOCK");
        super.activity1JLabel.setText("ADDRESS");
        super.activity2JLabel.setText("ACTIVEX");
        super.activity3JLabel.setText("COOKIE");

    }

}
