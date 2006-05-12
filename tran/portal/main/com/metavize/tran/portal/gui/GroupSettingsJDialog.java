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

package com.metavize.tran.portal.gui;

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;
import com.metavize.gui.transform.MTransformControlsJPanel;
import com.metavize.gui.transform.SettingsChangedListener;
import com.metavize.mvvm.portal.*;


import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class GroupSettingsJDialog extends MConfigJDialog implements SettingsChangedListener {

    private static final String NAME_TITLE     = "Group Settings";
    private static final String NAME_BOOKMARKS = "Links";
    private static final String NAME_HOME      = "Page Setup";

    private List<String> applicationNames;
    private PortalGroup portalGroup;
    private MTransformControlsJPanel mTransformControlsJPanel;
    private boolean settingsChanged;
        
    public static GroupSettingsJDialog factory(Window topLevelWindow, PortalGroup portalGroup, MTransformControlsJPanel mTransformControlsJPanel){
	if( topLevelWindow instanceof Frame )
	    return new GroupSettingsJDialog((Frame)topLevelWindow, portalGroup, mTransformControlsJPanel);
	else if( topLevelWindow instanceof Dialog )
	    return new GroupSettingsJDialog((Dialog)topLevelWindow, portalGroup, mTransformControlsJPanel);
	else
	    return null;
    }

    public GroupSettingsJDialog(Dialog topLevelDialog, PortalGroup portalGroup, MTransformControlsJPanel mTransformControlsJPanel){
	super(topLevelDialog);
	init(portalGroup, mTransformControlsJPanel);
    }

    public GroupSettingsJDialog(Frame topLevelFrame, PortalGroup portalGroup, MTransformControlsJPanel mTransformControlsJPanel){
	super(topLevelFrame);
	init(portalGroup, mTransformControlsJPanel);
    }

    private void init(PortalGroup portalGroup, MTransformControlsJPanel mTransformControlsJPanel){
	this.portalGroup = portalGroup;
	saveJButton.setText("<html><b>Change</b> Settings</html>");
	this.mTransformControlsJPanel = mTransformControlsJPanel;
    }
    
    protected Dimension getMinSize(){
	return new Dimension(640, 610);
    }

    protected void saveAll() throws Exception {
	super.saveAll();
	if( settingsChanged )
	    mTransformControlsJPanel.setSaveSettingsHintVisible(true);	    
    }

    protected void refreshAll() throws Exception {
	applicationNames = Util.getMvvmContext().portalManager().applicationManager().getApplicationNames();
	super.refreshAll();
	settingsChanged = false;
    }

    public void settingsChanged(Object source){
	settingsChanged = true;
    }

    protected void generateGui(){
        this.setTitle(NAME_TITLE + " for " + portalGroup.getName() );

        // BOOKMARKS //
	BookmarksJPanel bookmarksJPanel = new BookmarksJPanel(portalGroup, applicationNames);
	addRefreshable(NAME_BOOKMARKS, bookmarksJPanel);
	addSavable(NAME_BOOKMARKS, bookmarksJPanel);
        addTab(NAME_BOOKMARKS, null, bookmarksJPanel);
	bookmarksJPanel.setSettingsChangedListener(this);

	// HOME //
	GroupHomeSettingsJPanel groupHomeSettingsJPanel = new GroupHomeSettingsJPanel(portalGroup);
	addRefreshable(NAME_HOME, groupHomeSettingsJPanel);
	addSavable(NAME_HOME, groupHomeSettingsJPanel);
	addScrollableTab( getMTabbedPane(), NAME_HOME, null, groupHomeSettingsJPanel, false, true);
	groupHomeSettingsJPanel.setSettingsChangedListener(this);
    }

}
