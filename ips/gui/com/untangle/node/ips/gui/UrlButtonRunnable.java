/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */


package com.untangle.node.ips.gui;
import java.awt.Window;
import java.awt.event.*;
import java.net.URL;
import javax.jnlp.BasicService;
import javax.jnlp.ServiceManager;
import javax.swing.CellEditor;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.node.ips.*;

public class UrlButtonRunnable implements ButtonRunnable, Comparable<UrlButtonRunnable> {
    private String url;
    private Window topLevelWindow;
    private boolean isEnabled;
    public UrlButtonRunnable(String isEnabled){
    }
    public String getButtonText(){
        if( (url==null) || (!url.startsWith("http")))
            return "No URL";
        else
            return "Show URL";
    }

    public boolean valueChanged(){ return false; }
    public void setEnabled(boolean enabled){ this.isEnabled = enabled; }
    public boolean isEnabled(){
        if( (url==null) || (!url.startsWith("http")))
            return false;
        else
            return true;
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
