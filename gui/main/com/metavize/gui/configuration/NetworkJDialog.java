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

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.NetworkingConfiguration;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class NetworkJDialog extends MConfigJDialog {

    private static final String NAME_NETWORK_SETTINGS = "Network Settings";
    /* XXX Better name */
    private static final String NAME_ALIAS_PANEL      = "Address Aliases";

    public NetworkJDialog( ) {
    }

    protected Dimension getMinSize(){
	return new Dimension(640, 550);
    }
    
    protected void generateGui(){
        this.setTitle(NAME_NETWORK_SETTINGS);
        
        // GENERAL SETTINGS //////
        NetworkJPanel networkJPanel = new NetworkJPanel();
        JScrollPane contentJScrollPane = new JScrollPane( networkJPanel );
        contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.addTab(NAME_NETWORK_SETTINGS, null, contentJScrollPane);
	super.savableMap.put(NAME_NETWORK_SETTINGS, networkJPanel);
	super.refreshableMap.put(NAME_NETWORK_SETTINGS, networkJPanel);

        // ALIASES Panel /////
        InterfaceAliasJPanel aliasJPanel = new InterfaceAliasJPanel();
        super.contentJTabbedPane.addTab(NAME_ALIAS_PANEL, null, aliasJPanel );
	super.savableMap.put(NAME_ALIAS_PANEL, aliasJPanel );
	super.refreshableMap.put(NAME_ALIAS_PANEL, aliasJPanel );
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
        }
    }

}
