/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.configuration;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;

public class RemoteJDialog extends MConfigJDialog {

    private static final String NAME_ADMINISTRATION_CONFIG  = "Remote Admin Config";
    private static final String NAME_ADMIN_ACCOUNTS         = "Admin Accounts";
    private static final String NAME_ACCESS_TAB             = "Access";
    private static final String NAME_ACCESS_RESTRICTIONS    = "Restrictions";
    private static final String NAME_ACCESS_PUBLIC          = "Public Address";
    private static final String NAME_CERTIFICATE_TAB        = "Certificates";
    private static final String NAME_CERTIFICATE_STATUS     = "Status";
    private static final String NAME_CERTIFICATE_GENERATION = "Generation";
    private static final String NAME_MONITORING_TAB         = "Monitoring";
    private static final String NAME_MONITORING_SNMP        = "SNMP";
    private static final String NAME_MONITORING_SYSLOG      = "Syslog";
    private static final String NAME_MANUAL_REBOOT          = "Manual Reboot";
    
    public RemoteJDialog( Frame parentFrame ) {
	super(parentFrame);
	setTitle(NAME_ADMINISTRATION_CONFIG);
    setHelpSource("remote_admin_config");
	compoundSettings = new RemoteCompoundSettings();
    }

    protected void generateGui(){
	// ADMIN ACCOUNTS ////////
	RemoteAdminJPanel remoteAdminJPanel = new RemoteAdminJPanel();
	addTab(NAME_ADMIN_ACCOUNTS, null, remoteAdminJPanel);
	addSavable(NAME_ADMIN_ACCOUNTS, remoteAdminJPanel);
	addRefreshable(NAME_ADMIN_ACCOUNTS, remoteAdminJPanel);
	
	// ACCESS //
        JTabbedPane accessJTabbedPane = addTabbedPane(NAME_ACCESS_TAB, null);

        // ACCESS RESTRICTIONS //////
        RemoteRestrictionJPanel remoteRestrictionJPanel = new RemoteRestrictionJPanel();
        addScrollableTab(accessJTabbedPane, NAME_ACCESS_RESTRICTIONS, null, remoteRestrictionJPanel, false, true);
        addSavable(NAME_ACCESS_RESTRICTIONS, remoteRestrictionJPanel);
        addRefreshable(NAME_ACCESS_RESTRICTIONS, remoteRestrictionJPanel);

	// ACCESS PUBLIC ADDRESS //
	RemotePublicAddressJPanel remotePublicAddressJPanel = new RemotePublicAddressJPanel();
	addScrollableTab(accessJTabbedPane, NAME_ACCESS_PUBLIC, null, remotePublicAddressJPanel, false, true);
	addSavable(NAME_ACCESS_PUBLIC, remotePublicAddressJPanel);
	addRefreshable(NAME_ACCESS_PUBLIC, remotePublicAddressJPanel);
	
        // CERTIFICATES /////////////
        JTabbedPane certificateJTabbedPane = addTabbedPane(NAME_CERTIFICATE_TAB, null);
	
        // CERTIFICATE STATUS //////
        RemoteCertStatusJPanel remoteCertStatusJPanel = new RemoteCertStatusJPanel();
	addScrollableTab(certificateJTabbedPane, NAME_CERTIFICATE_STATUS, null, remoteCertStatusJPanel, false, true);
        addRefreshable(NAME_CERTIFICATE_STATUS, remoteCertStatusJPanel);

	// CERTIFICATE GENERATION /////
	RemoteCertGenJPanel remoteCertGenJPanel = new RemoteCertGenJPanel(this);
	addScrollableTab(certificateJTabbedPane, NAME_CERTIFICATE_GENERATION, null, remoteCertGenJPanel, false, true);

        // MONITORING /////////////
        JTabbedPane monitoringJTabbedPane = addTabbedPane(NAME_MONITORING_TAB, null);
	
        // SNMP MONITORING //////
        RemoteSnmpJPanel remoteSnmpJPanel = new RemoteSnmpJPanel();
	addScrollableTab(monitoringJTabbedPane, NAME_MONITORING_SNMP, null, remoteSnmpJPanel, false, true);
        addSavable(NAME_MONITORING_SNMP, remoteSnmpJPanel);
        addRefreshable(NAME_MONITORING_SNMP, remoteSnmpJPanel);
	
        // SYSLOG MONITORING //////
        RemoteSyslogJPanel remoteSyslogJPanel = new RemoteSyslogJPanel();
	addScrollableTab(monitoringJTabbedPane, NAME_MONITORING_SYSLOG, null, remoteSyslogJPanel, false, true);
        addSavable(NAME_MONITORING_SYSLOG, remoteSyslogJPanel);
        addRefreshable(NAME_MONITORING_SYSLOG, remoteSyslogJPanel);
	
        // MANUAL REBOOT //////
        RemoteRebootJPanel remoteRebootJPanel = new RemoteRebootJPanel();
        addTab(NAME_MANUAL_REBOOT, null, remoteRebootJPanel);
    }

    protected void saveAll() throws Exception {
        // ASK THE USER IF HE REALLY WANTS TO SAVE SETTINGS ////////
        NetworkSaveSettingsProceedJDialog saveSettingsProceedJDialog = new NetworkSaveSettingsProceedJDialog(this);
        boolean isProceeding = saveSettingsProceedJDialog.isProceeding();
        if( isProceeding ){
            super.saveAll();
        }
    }

}
