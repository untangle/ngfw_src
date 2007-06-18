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

import com.untangle.gui.node.CompoundSettings;
import com.untangle.gui.node.MCasingJPanel;
import com.untangle.gui.util.Util;
import com.untangle.uvm.networking.AccessSettings;
import com.untangle.uvm.networking.MiscSettings;
import com.untangle.uvm.networking.NetworkSpacesSettings;


public class MaintenanceCompoundSettings implements CompoundSettings {

    // ACCESS SETTINGS //
    private AccessSettings accessSettings;
    public AccessSettings getAccessSettings(){ return accessSettings; }

    // MISC SETTINGS //
    private MiscSettings miscSettings;
    public MiscSettings getMiscSettings() { return miscSettings; }

    // NETWORK SETTINGS //
    private NetworkSpacesSettings networkSettings;
    public NetworkSpacesSettings getNetworkSettings(){ return networkSettings; }

    // MAIL NODE SETTINGS //
    private CompoundSettings mailNodeCompoundSettings;
    public CompoundSettings getMailNodeCompoundSettings(){ return mailNodeCompoundSettings; }

    // HTTP NODE SETTINGS //
    private CompoundSettings httpNodeCompoundSettings;
    public CompoundSettings getHttpNodeCompoundSettings(){ return httpNodeCompoundSettings; }

    // FTP NODE SETTINGS //
    private CompoundSettings ftpNodeCompoundSettings;
    public CompoundSettings getFtpNodeCompoundSettings(){ return ftpNodeCompoundSettings; }

    private MCasingJPanel[] casingJPanels;
    public MCasingJPanel[] getCasingJPanels(){ return casingJPanels; }

    public void save() throws Exception {
        Util.getNetworkManager().setSettings(accessSettings,miscSettings,networkSettings);

        if(mailNodeCompoundSettings != null){
            mailNodeCompoundSettings.save();
        }
        if(httpNodeCompoundSettings != null){
            httpNodeCompoundSettings.save();
        }
        if(ftpNodeCompoundSettings != null){
            ftpNodeCompoundSettings.save();
        }
    }

    public void refresh() throws Exception {
        Util.getNetworkManager().updateLinkStatus();
        accessSettings = Util.getNetworkManager().getAccessSettings();
        miscSettings = Util.getNetworkManager().getMiscSettings();
        networkSettings = Util.getNetworkManager().getNetworkSettings();

        casingJPanels = Util.getPolicyStateMachine().loadAllCasings(true);

        if(mailNodeCompoundSettings == null){
            mailNodeCompoundSettings = Util.getCompoundSettings("com.untangle.node.mail.gui.MailNodeCompoundSettings", "mail-casing");
        }
        if(mailNodeCompoundSettings != null)
            mailNodeCompoundSettings.refresh();

        if(httpNodeCompoundSettings == null){
            httpNodeCompoundSettings = Util.getCompoundSettings("com.untangle.node.http.gui.HttpNodeCompoundSettings", "http-casing");
        }
        if(httpNodeCompoundSettings != null)
            httpNodeCompoundSettings.refresh();

        if(ftpNodeCompoundSettings == null){
            ftpNodeCompoundSettings = Util.getCompoundSettings("com.untangle.node.ftp.gui.FtpNodeCompoundSettings", "ftp-casing");
        }
        if(ftpNodeCompoundSettings != null)
            ftpNodeCompoundSettings.refresh();
    }

    public void validate() throws Exception {
        accessSettings.validate();
        miscSettings.validate();
        System.err.println( "need validation for network settings" );
        //networkSettings.validate();
    }

}
