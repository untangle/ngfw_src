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

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class BookmarksJPanel extends MEditTableJPanel{    

    public BookmarksJPanel(Object obj) {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        BookmarksTableModel bookmarksTableModel = new BookmarksTableModel(obj);
        this.setTableModel( bookmarksTableModel );
    }
}


class BookmarksTableModel extends MSortedTableModel<Object>{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C2_MW = 150; /* name */
    private static final int C3_MW = 120; /* app */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW ), 120); /* target */

    private Object portalObject;

    public BookmarksTableModel(Object obj){
	portalObject = obj;
    }

    private DefaultComboBoxModel appModel = new DefaultComboBoxModel();
    private void updateAppModel(){
	appModel.removeAllElements();
	appModel.addElement("HTTP");
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class, "[no name]", "name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, ComboBoxModel.class, appModel, "application");
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, false, String.class, "[no target]", "target");
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, Bookmark.class, null, "");
        return tableColumnModel;
    }

        
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());
	Bookmark newElem = null;

	for( Vector rowVector : tableVector ){
	    newElem = (Bookmark) rowVector.elementAt(5);
            newElem.setName( (String) rowVector.elementAt(2) );
            newElem.setApplicationName( (String) ((ComboBoxModel) rowVector.elementAt(3)).getSelectedItem() );
            newElem.setTarget( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }

	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    if( portalObject instanceof PortalUser )
		((PortalUser)portalObject).setBookmarks(elemList);
	    else if( portalObject instanceof PortalGroup )
		((PortalGroup)portalObject).setBookmarks(elemList);
	    else if( portalObject == null )
		((PortalSettings)settings).getGlobal().setBookmarks(elemList);
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
        Vector<Vector> allRows = new Vector<Vector>(bookmarks.size());
	Vector tempRow = null;
	int rowIndex = 0;

	updateAppModel();

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
        return allRows;
    }

}
