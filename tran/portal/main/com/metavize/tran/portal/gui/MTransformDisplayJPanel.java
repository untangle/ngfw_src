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



package com.metavize.tran.portal.gui;

import com.metavize.gui.transform.*;

public class MTransformDisplayJPanel extends com.metavize.gui.transform.MTransformDisplayJPanel{


    public MTransformDisplayJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);

        super.activity0JLabel.setText("LOGIN");
        super.activity1JLabel.setText("LOGOUT");
        super.activity2JLabel.setText(" ");
        super.activity3JLabel.setText(" ");

    }

}
