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

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;

import com.metavize.gui.util.StringConstants;


/**
 *
 * @author  inieves
 */
public class AboutJDialog extends ConfigJDialog {
    
    private JScrollPane contentJScrollPane;
    private JEditorPane contentJEditorPane;
    
    private static String aboutString = "<html><b>Build:</b> " 
	+ Util.getMvvmContext().toolboxManager().mackageDesc("mvvm").getInstalledVersion()
	+ "<br><br><b>Readme:</b> http://www.metavize.com/egquickstart<br><br><b>Website: </b>http://www.metavize.com</html>";

    public AboutJDialog( ) {
        super(Util.getMMainJFrame());

	contentJEditorPane = new JEditorPane("text/html", aboutString);
	contentJEditorPane.setEditable(false);
	contentJEditorPane.setFont(new java.awt.Font("Arial", 11, 0) );
	contentJScrollPane = new JScrollPane( contentJEditorPane );
	contentJScrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
	contentJScrollPane.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS );

	this.contentJTabbedPane.setTitleAt(0, "About EdgeGuard");
	this.contentJPanel.add(contentJScrollPane);
        this.setTitle("About EdgeGuard");
	this.removeActionButtons();
        
    }
    

}
