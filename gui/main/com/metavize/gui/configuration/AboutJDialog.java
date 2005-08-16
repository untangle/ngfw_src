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
    
    private JScrollPane contentJScrollPane;
    private JEditorPane contentJEditorPane;

    private static String buildString;
    private static String aboutString = "<br><br><b>Readme:</b> http://www.metavize.com/egquickstart<br><br><b>Website: </b>http://www.metavize.com</html>";

    public AboutJDialog( ) {
        MIN_SIZE = new Dimension(640, 480);
        MAX_SIZE = new Dimension(640, 1200);
    }

    public void generateGui(){
        this.setTitle("About EdgeGuard");
        this.removeActionButtons();
        
        // ABOUT /////////////
        try{
            buildString = "<html><b>Build:</b> " + Util.getMvvmContext().toolboxManager().mackageDesc("mvvm").getInstalledVersion();
        }
        catch(Exception e){
            buildString = "<html><b>Build:</b> unknown";
        }
	contentJEditorPane = new JEditorPane("text/html", buildString + aboutString);
	contentJEditorPane.setEditable(false);
	contentJEditorPane.setFont(new java.awt.Font("Arial", 11, 0) );
	contentJScrollPane = new JScrollPane( contentJEditorPane );
	contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
	contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
	this.contentJTabbedPane.addTab("About EdgeGuard", null, contentJScrollPane);
        
        // LISCENSE ////////////
        try{
	    URL licenseURL = Util.getClassLoader().getResource("EvalLicense.txt");
	    contentJEditorPane = new JEditorPane();
	    contentJEditorPane.setEditable(false);
	    contentJEditorPane.setPage(licenseURL);
	}
	catch(Exception e){
	    e.printStackTrace();
	}
	contentJScrollPane = new JScrollPane( contentJEditorPane );
	contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
	contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
	contentJScrollPane.getVerticalScrollBar().setValue(0);
	this.contentJTabbedPane.addTab("License Agreement", null, contentJScrollPane);
      
    }
    
    public void sendSettings(Object settings){}
    public void refreshSettings(){}

}
