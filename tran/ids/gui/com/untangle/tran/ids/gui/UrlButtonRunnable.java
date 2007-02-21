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


package com.untangle.tran.ids.gui;
import com.untangle.tran.ids.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.util.*;
import java.awt.Window;
import java.awt.Component;
import java.awt.event.*;
import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.CellEditor;

public class UrlButtonRunnable implements ButtonRunnable, Comparable<UrlButtonRunnable> {
    private String url;
    private Window topLevelWindow;
    private boolean isEnabled;
    public UrlButtonRunnable(String isEnabled){
    }
    public String getButtonText(){
        if( url.startsWith("http") )
            return "Show " + url.substring(7,url.length());
        else
            return "No URL";
    }

    public boolean valueChanged(){ return false; }
    public void setEnabled(boolean enabled){ this.isEnabled = enabled; }
    public boolean isEnabled(){
        if( url.startsWith("http") )
            return true;
        else
            return false;
    }

    public void setUrl(String url){ this.url = url; }
    public void setCellEditor(CellEditor cellEditor){}
    public void setTopLevelWindow(Window topLevelWindow){ this.topLevelWindow = topLevelWindow; }

    public void actionPerformed(ActionEvent evt){ run(); }
    public void run(){
	if(url!=null){
	    try{
		URL newURL = new URL(url);
		((BasicService) ServiceManager.lookup("javax.jnlp.BasicService")).showDocument(newURL);
	    }
	    catch(Exception e){
		Util.handleExceptionNoRestart("Error showing URL", e);
		MOneButtonJDialog.factory(topLevelWindow, "Intrusion Prevention", "Unable to show URL", "Intrusion Prevention Warning", "Warning");
	    }
	}
    }
    public int compareTo(UrlButtonRunnable o){
        return url.compareTo(o.url);
    }
}
