/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
        remoteAdminJPanel.setSettingsChangedListener(this);

        // ACCESS //
        JTabbedPane accessJTabbedPane = addTabbedPane(NAME_ACCESS_TAB, null);

        // ACCESS RESTRICTIONS //////
        RemoteRestrictionJPanel remoteRestrictionJPanel = new RemoteRestrictionJPanel();
        addScrollableTab(accessJTabbedPane, NAME_ACCESS_RESTRICTIONS, null, remoteRestrictionJPanel, false, true);
        addSavable(NAME_ACCESS_RESTRICTIONS, remoteRestrictionJPanel);
        addRefreshable(NAME_ACCESS_RESTRICTIONS, remoteRestrictionJPanel);
        remoteRestrictionJPanel.setSettingsChangedListener(this);

        // ACCESS PUBLIC ADDRESS //
        RemotePublicAddressJPanel remotePublicAddressJPanel = new RemotePublicAddressJPanel();
        addScrollableTab(accessJTabbedPane, NAME_ACCESS_PUBLIC, null, remotePublicAddressJPanel, false, true);
        addSavable(NAME_ACCESS_PUBLIC, remotePublicAddressJPanel);
        addRefreshable(NAME_ACCESS_PUBLIC, remotePublicAddressJPanel);
        remotePublicAddressJPanel.setSettingsChangedListener(this);

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
        remoteSnmpJPanel.setSettingsChangedListener(this);

        // SYSLOG MONITORING //////
        RemoteSyslogJPanel remoteSyslogJPanel = new RemoteSyslogJPanel();
        addScrollableTab(monitoringJTabbedPane, NAME_MONITORING_SYSLOG, null, remoteSyslogJPanel, false, true);
        addSavable(NAME_MONITORING_SYSLOG, remoteSyslogJPanel);
        addRefreshable(NAME_MONITORING_SYSLOG, remoteSyslogJPanel);
        remoteSyslogJPanel.setSettingsChangedListener(this);

        // MANUAL REBOOT //////
        RemoteRestartJPanel remoteRestartJPanel = new RemoteRestartJPanel();
        addTab(NAME_MANUAL_REBOOT, null, remoteRestartJPanel);
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
