
/*
 * AdminConfigJDialog.java
 *
 * Created on December 12, 2004, 1:06 AM
 */

package com.metavize.gui.configuration;

import com.metavize.gui.widgets.configWindow.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;
import java.net.URL;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;

import com.metavize.gui.util.StringConstants;


/**
 *
 * @author  inieves
 */
public class LicenseJDialog extends ConfigJDialog {
    
    public LicenseJDialog( ) {
        super(Util.getMMainJFrame());

        JScrollPane contentJScrollPane;
	JEditorPane contentJEditorPane = null;

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
	contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED );
	contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );
	contentJScrollPane.getVerticalScrollBar().setValue(0);

	this.contentJTabbedPane.setTitleAt(0, "License Agreement");
	this.contentJPanel.add(contentJScrollPane);
        this.setTitle("License Agreement");
        this.removeActionButtons();
    }





}
