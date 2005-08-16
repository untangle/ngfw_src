/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MaintenanceJDialog.java 194 2005-04-06 19:13:55Z inieves $
 */

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.NetworkingConfiguration;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class MaintenanceJDialog extends MConfigJDialog {

    private static final String NAME_REMOTE_SETTINGS = "Support";

    public MaintenanceJDialog( ) {
        MIN_SIZE = new Dimension(640, 480);
        MAX_SIZE = new Dimension(640, 1200);
    }

    protected void generateGui(){
        this.setTitle(NAME_REMOTE_SETTINGS);
        
        // GENERAL SETTINGS //////
        MaintenanceJPanel maintenanceJPanel = new MaintenanceJPanel();
        JScrollPane contentJScrollPane = new JScrollPane( maintenanceJPanel );
        contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.addTab(NAME_REMOTE_SETTINGS, null, contentJScrollPane);
	super.savableMap.put(NAME_REMOTE_SETTINGS, maintenanceJPanel);
	super.refreshableMap.put(NAME_REMOTE_SETTINGS, maintenanceJPanel);
    }
    
    protected void sendSettings(Object settings) throws Exception {}
    protected void refreshSettings(){}


}
