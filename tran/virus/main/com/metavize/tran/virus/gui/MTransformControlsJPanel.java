/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MTransformControlsJPanel.java,v 1.6 2005/03/19 02:16:52 inieves Exp $
 */
package com.metavize.tran.virus.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.util.*;

import com.metavize.mvvm.*;
import com.metavize.mvvm.tran.*;


import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private JTabbedPane httpJTabbedPane, ftpJTabbedPane;
    private EmailDetectionJPanel emailDetectionJPanel;
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);

        httpJTabbedPane = new JTabbedPane();
        ftpJTabbedPane = new JTabbedPane();
        httpJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        httpJTabbedPane.setFocusable(false);
        httpJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        httpJTabbedPane.setRequestFocusEnabled(false);
        ftpJTabbedPane.setBorder(new EmptyBorder(7, 13, 13, 13));
        ftpJTabbedPane.setFocusable(false);
        ftpJTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        ftpJTabbedPane.setRequestFocusEnabled(false);
        
        emailDetectionJPanel = new EmailDetectionJPanel();
        String transformName = mTransformJPanel.getTransformContext().getTransformDesc().getName();
        if( transformName.equals("sophos-transform") )
            Util.setEmailDetectionSophosJPanel( emailDetectionJPanel );
        else if( transformName.equals("fprot-transform") )
            Util.setEmailDetectionFprotJPanel( emailDetectionJPanel );
        else if( transformName.equals("hauri-transform") )
            Util.setEmailDetectionHauriJPanel( emailDetectionJPanel );

        Util.updateDependencies();
        
        this.mTabbedPane.insertTab("General Settings", null, new GeneralConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        this.mTabbedPane.insertTab("eMail", null, emailDetectionJPanel, null, 0);
        this.mTabbedPane.insertTab("FTP", null, ftpJTabbedPane, null, 0);
        this.mTabbedPane.insertTab("Web", null, httpJTabbedPane, null, 0);
        
        ftpJTabbedPane.insertTab("Sources", null, new FTPConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        httpJTabbedPane.insertTab("File Extension List (from Sources)", null, new ExtensionsConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        httpJTabbedPane.insertTab("MIME Type List (from Sources)", null, new MIMEConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        httpJTabbedPane.insertTab("Sources", null, new HTTPConfigJPanel(mTransformJPanel.getTransformContext()), null, 0);
        httpJTabbedPane.setSelectedIndex(0);
        
        //        this.eventTabbedPane.insertTab("HTTP + FTP Combined", null, new CombinedEventJPanel(mTransformJPanel.getTransformContext()), null, 0);
    }
    
}
