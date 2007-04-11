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

		import com.untangle.gui.widgets.dialogs.*;
		import com.untangle.gui.widgets.editTable.*;
		import com.untangle.gui.util.*;

		import java.net.URL;
		import java.awt.*;
		import java.util.*;
		import javax.swing.table.*;
		import javax.swing.*;

		import com.untangle.mvvm.security.*;
		import com.untangle.mvvm.*;

		import com.untangle.gui.util.StringConstants;
		import com.untangle.gui.transform.Refreshable;

		public class AboutJDialog extends MConfigJDialog {

			private static final String NAME_ABOUT_CONFIG      = "Setup Info";
			private static final String NAME_ABOUT_INFO        = "Version";
			private static final String NAME_LICENSE_INFO      = "License Agreement";
			private static final String NAME_REGISTRATION_INFO = "Registration";
			private static final String NAME_TIMEZONE_PANEL    = "Timezone";


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

				// LISCENSE ////////////
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
