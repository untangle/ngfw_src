/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.portal.gui;

import com.metavize.mvvm.tran.Transform;
import com.metavize.gui.transform.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.mvvm.portal.*;
import com.metavize.tran.portal.rdp.RdpBookmark;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class BookmarksJPanel extends MEditTableJPanel{    

    public BookmarksJPanel(Object obj, List<String> applicationNames, String mode) {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        BookmarksTableModel bookmarksTableModel = new BookmarksTableModel(obj, applicationNames, mode);
        this.setTableModel( bookmarksTableModel );
    }
}


class BookmarksTableModel extends MSortedTableModel<Object>{ 

    private static final int OT_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int OC0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int OC1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int OC2_MW = 150; /* name */
    private static final int OC3_MW = 120; /* app */
    private static final int OC4_MW = Util.chooseMax(OT_TW - (OC0_MW + OC2_MW + OC3_MW ), 120); /* target */

    private static final int RT_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int RC0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int RC1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int RC2_MW = 150; /* name */
    private static final int RC3_MW = 150; /* target */
    private static final int RC4_MW = 100; /* screen size */
    private static final int RC5_MW = 75;  /* create new console */
    private static final int RC6_MW = 100; /* hostname */
    private static final int RC7_MW = Util.chooseMax(RT_TW - (RC0_MW + RC2_MW + RC3_MW + RC4_MW + RC5_MW + RC6_MW), 120); /* optional command */

    private Object portalObject;
    private List<String> applicationNames;
    private String mode;

    public BookmarksTableModel(Object obj, List<String> applicationNames, String mode){
	portalObject = obj;
	this.applicationNames = applicationNames;
	this.mode = mode;
	updateScreenModel();
	updateAppModel();
    }

    private DefaultComboBoxModel appModel = new DefaultComboBoxModel();
    private void updateAppModel(){
	appModel.removeAllElements();
	for( String applicationName : applicationNames )
	    if( !applicationName.equals("RDP") )
		appModel.addElement(applicationName);
    }

    private DefaultComboBoxModel screenModel = new DefaultComboBoxModel();
    private void updateScreenModel(){
	screenModel.removeAllElements();
	for( String screenSize : RdpBookmark.SIZE_ENUMERATION )
	    screenModel.addElement(screenSize);
	screenModel.setSelectedItem(RdpBookmark.SIZE_DEFAULT);
    }

    
    public TableColumnModel getTableColumnModel(){

        if( mode.equals("OTHER") ){ // VNC, HTTP, CIFS
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #  min    rsz    edit   remv   desc   typ            def
	    addTableColumn( tableColumnModel,  0, OC0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
	    addTableColumn( tableColumnModel,  1, OC1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
	    addTableColumn( tableColumnModel,  2, OC2_MW, true,  true,  false, false, String.class, "[no name]", "name");
	    addTableColumn( tableColumnModel,  3, OC3_MW, true,  true,  false, false, ComboBoxModel.class, appModel, "application");
	    addTableColumn( tableColumnModel,  4, OC4_MW, true,  true,  false, false, String.class, "[no target]", "target");
	    addTableColumn( tableColumnModel,  5, 10,     false, false, true,  false, Bookmark.class, null, "");
	    return tableColumnModel;
	}
	else{ // RDP
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
	    //                                 #  min    rsz    edit   remv   desc   typ            def
	    addTableColumn( tableColumnModel,  0, RC0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
	    addTableColumn( tableColumnModel,  1, RC1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
	    addTableColumn( tableColumnModel,  2, RC2_MW, true,  true,  false, false, String.class, "[no name]", "name");
	    addTableColumn( tableColumnModel,  3, RC3_MW, true,  true,  false, false, String.class, "[no target]", "target");
	    addTableColumn( tableColumnModel,  4, RC4_MW, false, true,  false, false, ComboBoxModel.class, screenModel, "screen size");
	    addTableColumn( tableColumnModel,  5, RC5_MW, false, true,  false, false, Boolean.class, RdpBookmark.CONSOLE_DEFAULT, sc.html("create new<br>console"));
	    addTableColumn( tableColumnModel,  6, RC6_MW, true,  true,  false, false, String.class, "[no host]", sc.html("hostname"));
	    addTableColumn( tableColumnModel,  7, RC7_MW, true,  true,  false, false, String.class, "[no command]", sc.html("optional<br>command"));
	    addTableColumn( tableColumnModel,  8, 10,     false, false, true,  false, RdpBookmark.class, null, "");
	    return tableColumnModel;
	}
    }

        
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());

        if( mode.equals("OTHER") ){ // VNC, HTTP, CIFS
	    Bookmark newElem = null;
	    for( Vector rowVector : tableVector ){
		newElem = (Bookmark) rowVector.elementAt(5);
		newElem.setName( (String) rowVector.elementAt(2) );
		newElem.setApplicationName( (String) ((ComboBoxModel) rowVector.elementAt(3)).getSelectedItem() );
		newElem.setTarget( (String) rowVector.elementAt(4) );
		elemList.add(newElem);
	    }
	}
	else{ // RDP
	    RdpBookmark newElem = null;
	    for( Vector rowVector : tableVector ){
		newElem = (RdpBookmark) rowVector.elementAt(8);
		newElem.setName( (String) rowVector.elementAt(2) );
		newElem.setApplicationName( "RDP" );
		newElem.setTarget( (String) rowVector.elementAt(3) );
		newElem.setSize( (String) ((ComboBoxModel) rowVector.elementAt(4)).getSelectedItem() );
		newElem.setConsole( (Boolean) rowVector.elementAt(5) );
		newElem.setHost( (String) rowVector.elementAt(6) );
		newElem.setCommand( (String) rowVector.elementAt(7) );
		elemList.add(newElem);
	    }
	}

	// SAVE SETTINGS ////////
	if( !validateOnly ){

	    // FILTER OUT WHAT IS ABOUT TO BE SAVED OVER
	    List<Bookmark> oldBookmarkList = null;
	    if( portalObject instanceof PortalUser )
		oldBookmarkList = (List<Bookmark>) ((PortalUser)portalObject).getBookmarks();
	    else if( portalObject instanceof PortalGroup )
		oldBookmarkList = (List<Bookmark>) ((PortalGroup)portalObject).getBookmarks();
	    else if( portalObject == null )
		oldBookmarkList = (List<Bookmark>) ((PortalSettings)settings).getGlobal().getBookmarks();
	    if( mode.equals("OTHER") ){ // VNC, HTTP, CIFS
		for( Bookmark bookmark : oldBookmarkList )
		    if( !bookmark.getApplicationName().equals("RDP") )
			oldBookmarkList.remove(bookmark);
	    }
	    else{ // RDP
		for( Bookmark bookmark : oldBookmarkList )
		    if( bookmark.getApplicationName().equals("RDP") )
			oldBookmarkList.remove(bookmark);
	    }
	    // SAVE OVER
	    oldBookmarkList.addAll(elemList);
	    List newBookmarkList = oldBookmarkList;

	    // SAVE NEW LIST
	    if( portalObject instanceof PortalUser )
		((PortalUser)portalObject).setBookmarks(newBookmarkList);
	    else if( portalObject instanceof PortalGroup )
		((PortalGroup)portalObject).setBookmarks(newBookmarkList);
	    else if( portalObject == null )
		((PortalSettings)settings).getGlobal().setBookmarks(newBookmarkList);
	}
    }
    
    public Vector<Vector> generateRows(Object settings){
	List<Bookmark> bookmarks;
	if( portalObject instanceof PortalUser )
	    bookmarks = ((PortalUser)portalObject).getBookmarks();
	else if( portalObject instanceof PortalGroup )
	    bookmarks = ((PortalGroup)portalObject).getBookmarks();
	else if( portalObject == null )
	    bookmarks = ((PortalSettings)settings).getGlobal().getBookmarks();
	else
	    bookmarks = null;

	// FILTER OUT WHAT IS NOT TO BE DISPLAYED
	if( mode.equals("OTHER") ){ // VNC, HTTP, CIFS
	    for( Bookmark bookmark : bookmarks )
		if( bookmark.getApplicationName().equals("RDP") )
		    bookmarks.remove(bookmark);
	}
	else{ // RDP
	    for( Bookmark bookmark : bookmarks )
		if( !bookmark.getApplicationName().equals("RDP") )
		    bookmarks.remove(bookmark);
	}
	
        Vector<Vector> allRows = new Vector<Vector>(bookmarks.size());
	Vector tempRow = null;
	int rowIndex = 0;

	if( mode.equals("OTHER") ){ // VNC, HTTP, CIFS
	    for( Bookmark newElem : bookmarks ){
		rowIndex++;
		tempRow = new Vector(6);
		tempRow.add( super.ROW_SAVED );
		tempRow.add( rowIndex );
		tempRow.add( newElem.getName() );
		ComboBoxModel comboBoxModel = copyComboBoxModel(appModel);
		comboBoxModel.setSelectedItem(newElem.getApplicationName());
		tempRow.add( comboBoxModel );
		tempRow.add( newElem.getTarget() );
		tempRow.add( newElem );
		allRows.add( tempRow );
	    }
	}
	else{ /// RDP
	    for( Bookmark newBookmark : bookmarks ){
		rowIndex++;
		RdpBookmark newElem = (RdpBookmark) newBookmark;
		tempRow = new Vector(9);
		tempRow.add( super.ROW_SAVED );
		tempRow.add( rowIndex );
		tempRow.add( newElem.getName() );
		tempRow.add( newElem.getTarget() );
		ComboBoxModel comboBoxModel = copyComboBoxModel(screenModel);
		comboBoxModel.setSelectedItem(newElem.getSize());
		tempRow.add( comboBoxModel );
		tempRow.add( newElem.getConsole() );
		tempRow.add( newElem.getHost() );
		tempRow.add( newElem.getCommand() );
		tempRow.add( newElem );
		allRows.add( tempRow );
	    }
	}

        return allRows;
    }

}
