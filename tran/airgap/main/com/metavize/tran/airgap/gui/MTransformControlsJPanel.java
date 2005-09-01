/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */


package com.metavize.tran.airgap.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.*;
import java.awt.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_GENERAL_SETTINGS = "General Settings";
    private static final String NAME_LOG = "Event Log";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
        
	super.contentJPanel.remove(super.saveJButton);
	super.contentJPanel.remove(super.reloadJButton);

	// GENERAL SETTINGS //////
	JPanel messageJPanel = new JPanel();
	messageJPanel.setLayout(new GridBagLayout());
	JLabel messageJLabel = new JLabel();
	messageJLabel.setText("<html>The Packet Attack Shield has no configurable settings.</html>");
	messageJLabel.setFont(new java.awt.Font("Arial", 0, 12));
	messageJLabel.setHorizontalAlignment(SwingConstants.CENTER);
	messageJLabel.setVerticalAlignment(SwingConstants.CENTER);
	messageJPanel.add(messageJLabel, new GridBagConstraints(0,0,1,1,0d,0d,GridBagConstraints.CENTER,GridBagConstraints.BOTH,new Insets(0,0,0,0),0,0));
	super.mTabbedPane.add(NAME_GENERAL_SETTINGS, messageJPanel);

	// EVENT LOG ///////////////
	LogJPanel logJPanel = new LogJPanel(mTransformJPanel.getTransformContext().transform(), this);
	super.mTabbedPane.addTab(NAME_LOG, null, logJPanel);
	super.shutdownableMap.put(NAME_LOG, logJPanel);
    }
     
    
}
