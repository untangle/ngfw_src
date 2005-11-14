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


public class RemoteJDialog extends MConfigJDialog {

    private static final String NAME_ADMINISTRATION_SETTINGS = "Remote Administration";
    private static final String NAME_SNMP_SETTINGS = "SNMP Monitoring";
    private static final String NAME_SYSLOG_SETTINGS = "Syslog Monitoring";

    public RemoteJDialog( ) {
    }

    protected void generateGui(){
        this.setTitle(NAME_ADMINISTRATION_SETTINGS);
        
        // REMOTE ADMINISTRATION //////
        RemoteAdministrationJPanel remoteAdministrationJPanel = new RemoteAdministrationJPanel();
        JScrollPane remoteAdministrationJScrollPane = new JScrollPane( remoteAdministrationJPanel );
        remoteAdministrationJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        remoteAdministrationJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.addTab(NAME_ADMINISTRATION_SETTINGS, null, remoteAdministrationJScrollPane);
	super.savableMap.put(NAME_ADMINISTRATION_SETTINGS, remoteAdministrationJPanel);
	super.refreshableMap.put(NAME_ADMINISTRATION_SETTINGS, remoteAdministrationJPanel);

        // SNMP MONITORING //////
        RemoteSnmpJPanel remoteSnmpJPanel = new RemoteSnmpJPanel();
        JScrollPane remoteSnmpJScrollPane = new JScrollPane( remoteSnmpJPanel );
        remoteSnmpJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        remoteSnmpJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.addTab(NAME_SNMP_SETTINGS, null, remoteSnmpJScrollPane);
	super.savableMap.put(NAME_SNMP_SETTINGS, remoteSnmpJPanel);
	super.refreshableMap.put(NAME_SNMP_SETTINGS, remoteSnmpJPanel);
        
        // SYSLOG MONITORING //////
        RemoteSyslogJPanel remoteSyslogJPanel = new RemoteSyslogJPanel();
        JScrollPane remoteSyslogJScrollPane = new JScrollPane( remoteSyslogJPanel );
        remoteSyslogJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        remoteSyslogJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
        this.contentJTabbedPane.addTab(NAME_SYSLOG_SETTINGS, null, remoteSyslogJScrollPane);
	super.savableMap.put(NAME_SYSLOG_SETTINGS, remoteSyslogJPanel);
	super.refreshableMap.put(NAME_SYSLOG_SETTINGS, remoteSyslogJPanel);
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
