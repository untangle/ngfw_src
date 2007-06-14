/*
 * $HeadURL:$
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

import com.untangle.gui.node.Refreshable;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.uvm.security.*;

public class AboutJDialog extends MConfigJDialog {

    private static final String NAME_ABOUT_CONFIG      = "Setup Info";
    private static final String NAME_ABOUT_INFO        = "Version";
    private static final String NAME_LICENSE_INFO      = "License Agreement";
    private static final String NAME_REGISTRATION_INFO = "Registration";
    private static final String NAME_TIMEZONE_PANEL    = "Timezone";
    private static final String NAME_BRANDING_PANEL    = "Branding";


    public AboutJDialog(Frame parentFrame) {
        super(parentFrame);
        setTitle(NAME_ABOUT_CONFIG);
        setHelpSource("setup_info_config");
        compoundSettings = new AboutCompoundSettings();
    }

    public void generateGui(){
        // ABOUT /////////////
        AboutAboutJEditorPane aboutAboutJEditorPane = new AboutAboutJEditorPane();
        JScrollPane aboutAboutJScrollPane = addScrollableTab(null, NAME_ABOUT_INFO, null, aboutAboutJEditorPane, false, true);
        aboutAboutJEditorPane.setContainingJScrollPane(aboutAboutJScrollPane);
        addRefreshable(NAME_ABOUT_INFO, aboutAboutJEditorPane);

        // LICENSE ////////////
        AboutLicenseJEditorPane aboutLicenseJEditorPane = new AboutLicenseJEditorPane();
        JScrollPane aboutLicenseJScrollPane = addScrollableTab(null, NAME_LICENSE_INFO, null, aboutLicenseJEditorPane, false, true);
        aboutLicenseJEditorPane.setContainingJScrollPane(aboutLicenseJScrollPane);
        addRefreshable(NAME_LICENSE_INFO, aboutLicenseJEditorPane);
                
        // REGISTRATION //////////
        AboutRegistrationJPanel aboutRegistrationJPanel = new AboutRegistrationJPanel();
        addScrollableTab(null, NAME_REGISTRATION_INFO, null, aboutRegistrationJPanel, false, true);
        addSavable(NAME_REGISTRATION_INFO, aboutRegistrationJPanel);
        addRefreshable(NAME_REGISTRATION_INFO, aboutRegistrationJPanel);
        aboutRegistrationJPanel.setSettingsChangedListener(this);

        // TIME ZONE //////
        AboutTimezoneJPanel timezoneJPanel = new AboutTimezoneJPanel();
        addScrollableTab(null, NAME_TIMEZONE_PANEL, null, timezoneJPanel, false, true);
        addSavable(NAME_TIMEZONE_PANEL, timezoneJPanel);
        addRefreshable(NAME_TIMEZONE_PANEL, timezoneJPanel);
        timezoneJPanel.setSettingsChangedListener(this);
                
        // BRANDING //////
        AboutBrandingJPanel brandingJPanel = new AboutBrandingJPanel();
        addScrollableTab(null, NAME_BRANDING_PANEL, null, brandingJPanel, false, true);
        addSavable(NAME_BRANDING_PANEL, brandingJPanel);
        addRefreshable(NAME_BRANDING_PANEL, brandingJPanel);
        brandingJPanel.setSettingsChangedListener(this);        
    }


    private class AboutLicenseJEditorPane extends JEditorPane
        implements Refreshable<AboutCompoundSettings> {
        private JScrollPane containingJScrollPane;
        public AboutLicenseJEditorPane(){
            setContentType("text/plain");
            setEditable(false);
            setFont(new java.awt.Font("Courier", 0, 11));
        }
        public void setContainingJScrollPane(JScrollPane jScrollPane){
            containingJScrollPane = jScrollPane;
        }
        public void doRefresh(AboutCompoundSettings aboutCompoundSettings){
            try{
                setPage(aboutCompoundSettings.getLicenseURL());
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error setting license", e);
            }
            containingJScrollPane.getVerticalScrollBar().setValue(0);
        }
    }

    
    private class AboutAboutJEditorPane extends JEditorPane
        implements Refreshable<AboutCompoundSettings> {
        private JScrollPane containingJScrollPane;
        public AboutAboutJEditorPane(){
            setContentType("text/html");
            setEditable(false);
            setFont(new java.awt.Font("Arial", 0, 11));
        }
        public void setContainingJScrollPane(JScrollPane jScrollPane){
            containingJScrollPane = jScrollPane;
        }
        public void doRefresh(AboutCompoundSettings aboutCompoundSettings){
            String versionString = "<html><b>Build: </b> " + aboutCompoundSettings.getInstalledVersion();
            versionString += "<br><b>Java: </b>" + System.getProperty("java.version");
            if(!Util.getIsDemo())
                versionString += "<br><b>Activation Key: </b>" + aboutCompoundSettings.getActivationKey();
            versionString += "</html>";

            try{
                setText(versionString);
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error setting about info", e);
            }
            containingJScrollPane.getVerticalScrollBar().setValue(0);
        }
    }

}
