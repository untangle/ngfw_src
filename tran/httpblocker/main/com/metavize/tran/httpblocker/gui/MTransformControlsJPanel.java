/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MTransformControlsJPanel.java,v 1.8 2005/02/10 05:34:33 dmorris Exp $
 */
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private JTabbedPane blockJTabbedPane, passJTabbedPane;
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel){
        super(mTransformJPanel);
	
        blockJTabbedPane = new JTabbedPane();
        passJTabbedPane = new JTabbedPane();
        blockJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        blockJTabbedPane.setFocusable(false);
        blockJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        blockJTabbedPane.setRequestFocusEnabled(false);
        passJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        passJTabbedPane.setFocusable(false);
        passJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        passJTabbedPane.setRequestFocusEnabled(false);
        
        
	
        // this.mTabbedPane.insertTab("General Settings", null, new GeneralConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
	
        passJTabbedPane.insertTab("Clients", null, new PassedClientsConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        passJTabbedPane.insertTab("URLs", null, new PassedURLsConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        passJTabbedPane.setSelectedIndex(0);
	
	
        blockJTabbedPane.insertTab("File Extensions", null, new BlockedExtensionsConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        blockJTabbedPane.insertTab("MIME Types", null, new BlockedMIMETypesConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        blockJTabbedPane.insertTab("URLs", null, new BlockedURLsConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        blockJTabbedPane.insertTab("Categories", null, new BlockedCategoriesConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        blockJTabbedPane.setSelectedIndex(0);

        this.mTabbedPane.insertTab("Pass Lists", null, passJTabbedPane, null, 0);
        this.mTabbedPane.insertTab("Block Lists", null, blockJTabbedPane, null, 0);
        // XXX  this.eventTabbedPane.insertTab("Block + Pass Combined", null, new CombinedEventJPanel(mTransformJPanel.getTransformContext()), null, 0);
    }
    
}
