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
import com.metavize.gui.widgets.MPasswordField;
import com.metavize.gui.util.*;

import com.metavize.tran.portal.*;
import com.metavize.mvvm.portal.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class GroupConfigJPanel extends MEditTableJPanel{

    public GroupConfigJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        GroupConfigTableModel groupConfigTableModel = new GroupConfigTableModel();
        this.setTableModel( groupConfigTableModel );
	groupConfigTableModel.setSortingStatus(2, GroupConfigTableModel.ASCENDING);
    }

}


class GroupConfigTableModel extends MSortedTableModel<Object>{ 

    private static final int T_TW  = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 150; /* name */
    private static final int C3_MW = 150; /* edit settings */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */


    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "[no name]", "name");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, SettingsButtonRunnable.class, "[no name]", "home page settings");
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, PortalGroup.class, null, "");
        return tableColumnModel;
    }

    protected void wireUpNewRow(Vector rowVector){
	PortalGroup portalGroup = (PortalGroup) rowVector.elementAt(5);
	SettingsButtonRunnable settingsButtonRunnable = (SettingsButtonRunnable) rowVector.elementAt(3);
	settingsButtonRunnable.setPortalGroup(portalGroup);
	settingsButtonRunnable.setUserType(false);	
    }

    public void prevalidate(Object settings, Vector<Vector> tableVector) throws Exception {
        Hashtable<String,String> nameHashtable = new Hashtable<String,String>();
        int rowIndex = 0;
        // go through all the rows and perform some tests
        for( Vector tempUser : tableVector ){
	    String name = (String) tempUser.elementAt(2);
	    // all names are unique
	    if( nameHashtable.contains( name ) )
		throw new Exception("The name in row: " + rowIndex + " has already been taken.");
	    else
		nameHashtable.put(name,name);
	    rowIndex++;
	}
    }
        
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());
	PortalGroup newElem = null;

	for( Vector rowVector : tableVector ){
	    newElem = (PortalGroup) rowVector.elementAt(5);
            newElem.setName( (String) rowVector.elementAt(2) );
            newElem.setDescription( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }

	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    PortalSettings portalSettings = (PortalSettings) settings;
	    portalSettings.setGroups(elemList);
	}

    }
    
    public Vector<Vector> generateRows(Object settings){
	PortalSettings portalSettings = (PortalSettings) settings;
	List<PortalGroup> groups = (List<PortalGroup>) portalSettings.getGroups();
        Vector<Vector> allRows = new Vector<Vector>(groups.size());
	Vector tempRow = null;
	int rowIndex = 0;

	for( PortalGroup newElem : groups ){
	    rowIndex++;
            tempRow = new Vector(7);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getName() );
	    SettingsButtonRunnable settingsButtonRunnable = new SettingsButtonRunnable("true");
	    settingsButtonRunnable.setPortalGroup(newElem);
	    settingsButtonRunnable.setUserType(false);
	    tempRow.add( settingsButtonRunnable );
            tempRow.add( newElem.getDescription() );
	    tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }

}
