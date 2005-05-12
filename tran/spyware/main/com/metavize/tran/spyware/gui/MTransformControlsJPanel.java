/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.mvvm.tran.TransformContext;

import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.tran.spyware.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class MTransformControlsJPanel extends com.metavize.gui.transform.MTransformControlsJPanel{
    
    private static final String NAME_SETTINGS = "General Settings";
    private static final String NAME_ACTIVEX = "ActiveX Block List";
    private static final String NAME_SPYWARE = "Spyware Block List";
    private static final String NAME_COOKIE = "Cookie Block List";
    
    public MTransformControlsJPanel(MTransformJPanel mTransformJPanel) {
        super(mTransformJPanel);
    }

    protected void generateGui(){

        // GENERAL SETTINGS ////////
	GeneralConfigJPanel generalConfigJPanel = new GeneralConfigJPanel();
        this.mTabbedPane.insertTab(NAME_SETTINGS, null, generalConfigJPanel, null, 0);
	super.savableMap.put(NAME_SETTINGS, generalConfigJPanel);
	super.refreshableMap.put(NAME_SETTINGS, generalConfigJPanel);

	// ACTIVEX ///////////////
	ActiveXConfigJPanel activeXConfigJPanel = new ActiveXConfigJPanel();
        this.mTabbedPane.insertTab(NAME_ACTIVEX, null, activeXConfigJPanel, null, 0);
	super.savableMap.put(NAME_ACTIVEX, activeXConfigJPanel);
	super.refreshableMap.put(NAME_ACTIVEX, activeXConfigJPanel);

	// SPYWARE ///////////////
	SpywareConfigJPanel spywareConfigJPanel = new SpywareConfigJPanel();
        this.mTabbedPane.insertTab(NAME_SPYWARE, null, spywareConfigJPanel, null, 0);
	super.savableMap.put(NAME_SPYWARE, spywareConfigJPanel);
	super.refreshableMap.put(NAME_SPYWARE, spywareConfigJPanel);

	// COOKIES //////////////
	CookieConfigJPanel cookieConfigJPanel = new CookieConfigJPanel();
        this.mTabbedPane.insertTab(NAME_COOKIE, null, cookieConfigJPanel, null, 0);
	super.savableMap.put(NAME_COOKIE, cookieConfigJPanel);
	super.refreshableMap.put(NAME_COOKIE, cookieConfigJPanel);
        
    }
}


