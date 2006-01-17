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

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.metavize.gui.util.*;
import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.NetworkingConfiguration;

public class RemoteJDialog extends MConfigJDialog {

    private static final String NAME_ADMINISTRATION_CONFIG = "Remote Admin Config";
    private static final String NAME_ADMIN_ACCOUNTS = "Admin Accounts";
    private static final String NAME_ADMIN_RESTRICTIONS = "Restrictions";
    private static final String NAME_MONITORING_SETTINGS = "Monitoring";
    private static final String NAME_SNMP_SETTINGS = "SNMP";
    private static final String NAME_SYSLOG_SETTINGS = "Syslog";
    private static final String NAME_MANUAL_REBOOT = "Manual Reboot";

    public RemoteJDialog( ) {
    }

    protected void generateGui(){
        this.setTitle(NAME_ADMINISTRATION_CONFIG);

	// ADMIN ACCOUNTS ////////
	RemoteAdminJPanel remoteAdminJPanel = new RemoteAdminJPanel();
	addTab(NAME_ADMIN_ACCOUNTS, null, remoteAdminJPanel);
	addSavable(NAME_ADMIN_ACCOUNTS, remoteAdminJPanel);
	addRefreshable(NAME_ADMIN_ACCOUNTS, remoteAdminJPanel);

        // REMOTE RESTRICTION //////
        RemoteRestrictionJPanel remoteRestrictionJPanel = new RemoteRestrictionJPanel();
	addScrollableTab(null, NAME_ADMIN_RESTRICTIONS, null, remoteRestrictionJPanel, false, true);
        addSavable(NAME_ADMIN_RESTRICTIONS, remoteRestrictionJPanel);
        addRefreshable(NAME_ADMIN_RESTRICTIONS, remoteRestrictionJPanel);

        // MONITORING /////////////
        JTabbedPane monitoringJTabbedPane = addTabbedPane(NAME_MONITORING_SETTINGS, null);

        // SNMP MONITORING //////
        RemoteSnmpJPanel remoteSnmpJPanel = new RemoteSnmpJPanel();
	addScrollableTab(monitoringJTabbedPane, NAME_SNMP_SETTINGS, null, remoteSnmpJPanel, false, true);
        addSavable(NAME_SNMP_SETTINGS, remoteSnmpJPanel);
        addRefreshable(NAME_SNMP_SETTINGS, remoteSnmpJPanel);

        // SYSLOG MONITORING //////
        RemoteSyslogJPanel remoteSyslogJPanel = new RemoteSyslogJPanel();
	addScrollableTab(monitoringJTabbedPane, NAME_SYSLOG_SETTINGS, null, remoteSyslogJPanel, false, true);
        addSavable(NAME_SYSLOG_SETTINGS, remoteSyslogJPanel);
        addRefreshable(NAME_SYSLOG_SETTINGS, remoteSyslogJPanel);

        // MANUAL REBOOT //////
        RemoteRebootJPanel remoteRebootJPanel = new RemoteRebootJPanel();
        addTab(NAME_MANUAL_REBOOT, null, remoteRebootJPanel);
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
