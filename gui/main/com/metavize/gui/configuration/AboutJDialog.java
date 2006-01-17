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

import java.net.URL;
import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;

import com.metavize.gui.util.StringConstants;


public class AboutJDialog extends MConfigJDialog {

    private static final String NAME_ABOUT_CONFIG = "EdgeGuard Info";
    private static final String NAME_VERSION_INFO = "Version/Revision";
    private static final String NAME_LISCENSE_INFO = "Liscense Agreement";
    
    private static final String aboutString = "<br><br><b>Readme:</b> http://www.metavize.com/egquickstart<br><br><b>Website: </b>http://www.metavize.com</html>";

    public AboutJDialog( ) {
    }

    public void generateGui(){
        this.setTitle(NAME_ABOUT_CONFIG);
        this.removeActionButtons();
        
        // ABOUT /////////////
	String buildString = null;
        try{
            buildString = "<html><b>Build:</b> " + Util.getMvvmContext().toolboxManager().mackageDesc("mvvm").getInstalledVersion();
        }
        catch(Exception e){
            buildString = "<html><b>Build:</b> unknown";
        }
	JEditorPane contentJEditorPane = new JEditorPane("text/html", buildString + aboutString);
	contentJEditorPane.setEditable(false);
	contentJEditorPane.setFont(new java.awt.Font("Arial", 0, 11) );
	addScrollableTab(null, NAME_VERSION_INFO, null, contentJEditorPane, false, true);
        
        // LISCENSE ////////////
        try{
	    URL licenseURL = Util.getClassLoader().getResource("License.txt");
	    contentJEditorPane = new JEditorPane();
	    contentJEditorPane.setEditable(false);
	    contentJEditorPane.setPage(licenseURL);
	}
	catch(Exception e){
	    e.printStackTrace();
	}
	JScrollPane contentJScrollPane = addScrollableTab(null, NAME_LISCENSE_INFO, null, contentJEditorPane, false, true);
	contentJScrollPane.getVerticalScrollBar().setValue(0);
      
    }
    
    public void sendSettings(Object settings){}
    public void refreshSettings(){}

}
