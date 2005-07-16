/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: RemoteJDialog.java 194 2005-04-06 19:13:55Z inieves $
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


/**
 *
 * @author  inieves
 */
public class RemoteJDialog extends MConfigJDialog {

    private static final String NAME_REMOTE_SETTINGS = "Remote Administration";

    public RemoteJDialog( ) {
        MIN_SIZE = new Dimension(640, 480);
        MAX_SIZE = new Dimension(640, 1200);
    }

    protected void generateGui(){
        this.setTitle(NAME_REMOTE_SETTINGS);
        
        // GENERAL SETTINGS //////
        RemoteJPanel remoteJPanel = new RemoteJPanel();
        JScrollPane contentJScrollPane = new JScrollPane( remoteJPanel );
        contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.addTab(NAME_REMOTE_SETTINGS, null, contentJScrollPane);
	super.savableMap.put(NAME_REMOTE_SETTINGS, remoteJPanel);
	super.refreshableMap.put(NAME_REMOTE_SETTINGS, remoteJPanel);
    }
    
    protected void sendSettings(Object settings) throws Exception {
	Util.getNetworkingManager().set( (NetworkingConfiguration) settings);
    }
    protected void refreshSettings(){
	settings = Util.getNetworkingManager().get();
    }

    protected void saveAll(){
	// ASK THE USER IF HE REALLY WANTS TO SAVE SETTINGS ////////
        SaveSettingsProceedJDialog saveSettingsProceedJDialog = new SaveSettingsProceedJDialog();
        boolean isProceeding = saveSettingsProceedJDialog.isProceeding();
        if( isProceeding ){ 
            super.saveAll();
            new RestartDialog();
        }
    }

}
