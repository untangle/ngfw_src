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



public class UserSettingsJDialog extends MConfigJDialog implements SettingsChangedListener {

    private static final String NAME_TITLE              = "User Settings";
    private static final String NAME_HOME               = "Page Setup";
    private static final String NAME_BOOKMARKS          = "Bookmarks";
    private static final String NAME_RDP_BOOKMARKS      = "Remote Desktop";
    private static final String NAME_OTHER_BOOKMARKS    = "Web, File Browser, VNC";


    private List<Application> applications;
    private PortalUser portalUser;
    private MTransformControlsJPanel mTransformControlsJPanel;
        
    public static UserSettingsJDialog factory(Window topLevelWindow, PortalUser portalUser, MTransformControlsJPanel mTransformControlsJPanel){
	if( topLevelWindow instanceof Frame )
	    return new UserSettingsJDialog((Frame)topLevelWindow, portalUser, mTransformControlsJPanel);
	else if( topLevelWindow instanceof Dialog )
	    return new UserSettingsJDialog((Dialog)topLevelWindow, portalUser, mTransformControlsJPanel);
	else
	    return null;
    }

    public UserSettingsJDialog(Dialog topLevelDialog, PortalUser portalUser, MTransformControlsJPanel mTransformControlsJPanel){
        super(topLevelDialog);
        init(portalUser, mTransformControlsJPanel);
    }

    public UserSettingsJDialog(Frame topLevelFrame, PortalUser portalUser, MTransformControlsJPanel mTransformControlsJPanel){
        super(topLevelFrame);
        init(portalUser, mTransformControlsJPanel);
    }

    private void init(PortalUser portalUser, MTransformControlsJPanel mTransformControlsJPanel){
        this.portalUser = portalUser;
        this.mTransformControlsJPanel = mTransformControlsJPanel;
        applications = new Vector<Application>();
        //saveJButton.setText("<html><b>Change</b> Settings</html>");
    }
    
    protected Dimension getMinSize(){
        return new Dimension(640, 610);
    }

    protected void saveAll() throws Exception {
        super.saveAll();
        setVisible(false);
        mTransformControlsJPanel.saveGui();

        /*
        if( settingsChanged )
            mTransformControlsJPanel.setSaveSettingsHintVisible(true);	    
        */
    }

    private boolean firstRefresh = true;
    protected void refreshAll() throws Exception {
        applications.clear();
        applications.addAll(Util.getMvvmContext().portalManager().applicationManager().getApplications());
        super.refreshAll();
        if( !firstRefresh ){
            setVisible(false);
            mTransformControlsJPanel.refreshGui();
        }
        firstRefresh = false;
        //settingsChanged = false;
    }

    public void settingsChanged(Object source){
        settingsChanged = true;
    }

    protected void generateGui(){
	String groupName;
	if( portalUser.getPortalGroup() != null )
	    groupName = portalUser.getPortalGroup().getName();
	else
	    groupName = "no group";
        this.setTitle(NAME_TITLE + " for " + portalUser.getUid() + " (" + groupName + ")");

	// GLOBAL BOOKMARKS //
	JTabbedPane bookmarksJTabbedPane = addTabbedPane(NAME_BOOKMARKS, null);

	// OTHER BOOKMARKS //
	BookmarksJPanel otherBookmarksJPanel = new BookmarksJPanel(portalUser, applications, "OTHER");
	bookmarksJTabbedPane.addTab(NAME_OTHER_BOOKMARKS, null, otherBookmarksJPanel);
	addSavable(NAME_OTHER_BOOKMARKS, otherBookmarksJPanel);
	addRefreshable(NAME_OTHER_BOOKMARKS, otherBookmarksJPanel);
	otherBookmarksJPanel.setSettingsChangedListener(this);

	// RDP BOOKMARKS //
	BookmarksJPanel rdpBookmarksJPanel = new BookmarksJPanel(portalUser, applications, "RDP");
	bookmarksJTabbedPane.addTab(NAME_RDP_BOOKMARKS, null, rdpBookmarksJPanel);
	addSavable(NAME_RDP_BOOKMARKS, rdpBookmarksJPanel);
	addRefreshable(NAME_RDP_BOOKMARKS, rdpBookmarksJPanel);
	rdpBookmarksJPanel.setSettingsChangedListener(this);

	// HOME //
	UserHomeSettingsJPanel userHomeSettingsJPanel = new UserHomeSettingsJPanel(portalUser);
	addRefreshable(NAME_HOME, userHomeSettingsJPanel);
	addSavable(NAME_HOME, userHomeSettingsJPanel);
	addScrollableTab(getMTabbedPane(), NAME_HOME, null, userHomeSettingsJPanel, false, true);
	userHomeSettingsJPanel.setSettingsChangedListener(this);
    }

}
