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

package com.untangle.tran.portal.gui;

import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.util.*;
import com.untangle.gui.transform.MTransformControlsJPanel;
import com.untangle.gui.transform.SettingsChangedListener;
import com.untangle.mvvm.portal.*;


import java.awt.Dimension;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;



public class GroupSettingsJDialog extends MConfigJDialog implements SettingsChangedListener {

    private static final String NAME_TITLE     = "Group Settings";
    private static final String NAME_HOME               = "Page Setup";
    private static final String NAME_BOOKMARKS          = "Bookmarks";
    private static final String NAME_RDP_BOOKMARKS      = "Remote Desktop";
    private static final String NAME_OTHER_BOOKMARKS    = "Web, File Browser, VNC";

    private List<Application> applications;
    private PortalGroup portalGroup;
    private MTransformControlsJPanel mTransformControlsJPanel;
        
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
        this.mTransformControlsJPanel = mTransformControlsJPanel;
        //saveJButton.setText("<html><b>Change</b> Settings</html>");
        applications = new Vector<Application>();
    }
    
    protected Dimension getMinSize(){
        return new Dimension(640, 610);
    }

    protected void saveAll() throws Exception {
        super.saveAll();
        setVisible(false);
        mTransformControlsJPanel.saveGui();  // XXX a little dangerous because refresh could come before the save... but refresh wouldnt do anything bad.
        //if( settingsChanged )
        //    mTransformControlsJPanel.setSaveSettingsHintVisible(true);	    
    }

    private boolean firstRefresh = true;
    protected void refreshAll() throws Exception {
        super.refreshAll();
        if(firstRefresh){
            applications.clear();
            applications.addAll(Util.getMvvmContext().portalManager().applicationManager().getApplications());
        }
        firstRefresh = false;
        //settingsChanged = false;
    }

    public void settingsChanged(Object source){
        settingsChanged = true;
    }

    protected void generateGui(){
        this.setTitle(NAME_TITLE + " for " + portalGroup.getName() );

	// GLOBAL BOOKMARKS //
	JTabbedPane bookmarksJTabbedPane = addTabbedPane(NAME_BOOKMARKS, null);

	// OTHER BOOKMARKS //
	BookmarksJPanel otherBookmarksJPanel = new BookmarksJPanel(portalGroup, applications, "OTHER");
	bookmarksJTabbedPane.addTab(NAME_OTHER_BOOKMARKS, null, otherBookmarksJPanel);
	addSavable(NAME_OTHER_BOOKMARKS, otherBookmarksJPanel);
	addRefreshable(NAME_OTHER_BOOKMARKS, otherBookmarksJPanel);
	otherBookmarksJPanel.setSettingsChangedListener(this);

	// RDP BOOKMARKS //
	BookmarksJPanel rdpBookmarksJPanel = new BookmarksJPanel(portalGroup, applications, "RDP");
	bookmarksJTabbedPane.addTab(NAME_RDP_BOOKMARKS, null, rdpBookmarksJPanel);
	addSavable(NAME_RDP_BOOKMARKS, rdpBookmarksJPanel);
	addRefreshable(NAME_RDP_BOOKMARKS, rdpBookmarksJPanel);
	rdpBookmarksJPanel.setSettingsChangedListener(this);

	// HOME //
	GroupHomeSettingsJPanel groupHomeSettingsJPanel = new GroupHomeSettingsJPanel(portalGroup);
	addRefreshable(NAME_HOME, groupHomeSettingsJPanel);
	addSavable(NAME_HOME, groupHomeSettingsJPanel);
	addScrollableTab( getMTabbedPane(), NAME_HOME, null, groupHomeSettingsJPanel, false, true);
	groupHomeSettingsJPanel.setSettingsChangedListener(this);
    }

}
