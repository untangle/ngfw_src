/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: DirectoryLocalJPanel.java 5043 2006-03-06 19:08:12Z inieves $
 */

package com.metavize.gui.configuration;

import com.metavize.mvvm.addrbook.*;

import com.metavize.gui.widgets.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.Util;

import java.awt.Insets;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;

import com.metavize.gui.util.StringConstants;


public class DirectoryLocalJPanel extends MEditTableJPanel{

    public DirectoryLocalJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);       

        // create actual table model
        DirectoryLocalTableModel directoryLocalTableModel = new DirectoryLocalTableModel();
        this.setTableModel( directoryLocalTableModel );
    }
    
}



    
class DirectoryLocalTableModel extends MSortedTableModel<DirectoryCompoundSettings> {

    private static final StringConstants sc = StringConstants.getInstance();
    
    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  Util.STATUS_MIN_WIDTH, false, false, false, false, String.class,     null, sc.TITLE_STATUS );
	addTableColumn( tableColumnModel,  1,  Util.LINENO_MIN_WIDTH, false, false, false, false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  100, true,  true,  false, false, String.class,     sc.EMPTY_NAME, "first name");
        addTableColumn( tableColumnModel,  3,  100, true,  true,  false, false, String.class,     sc.EMPTY_NAME, "last name");
        addTableColumn( tableColumnModel,  4,  150, true,  true,  false, false, String.class,     "[no email]", "email address");
        addTableColumn( tableColumnModel,  5,  150, true,  true,  false, false, MPasswordField.class, "aabbccdd", "password");
        addTableColumn( tableColumnModel,  6,  150, true,  true,  false, true,  String.class,     sc.EMPTY_COMMENT, "comment");
        addTableColumn( tableColumnModel,  7,  10,  false, false, true,  false, UserEntry.class,  null, "");
        return tableColumnModel;
    }

    
    public void generateSettings(DirectoryCompoundSettings directoryCompoundSettings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
	List<UserEntry> allRows = new ArrayList(tableVector.size());
	UserEntry newElem = null;
	
        for( Vector rowVector : tableVector ){
	    
	    newElem = (UserEntry) rowVector.elementAt(7);
	    newElem.setFirstName( (String) rowVector.elementAt(2) );
	    newElem.setLastName( (String) rowVector.elementAt(3) );
	    newElem.setEmail( (String) rowVector.elementAt(4) );
	    newElem.setUID( newElem.getFirstName() + newElem.getLastName() + newElem.getEmail() );
	    newElem.setPassword( new String(((MPasswordField)rowVector.elementAt(5)).getPassword()) );
	    newElem.setComment( (String) rowVector.elementAt(6) );
            allRows.add(newElem);
        }
        
	// SAVE SETTINGS /////////////
	if( !validateOnly ){
	    directoryCompoundSettings.setLocalUserList(allRows);
	}

    }

    public Vector<Vector> generateRows(DirectoryCompoundSettings directoryCompoundSettings) {
	List<UserEntry> userEntries = directoryCompoundSettings.getLocalUserList();
        Vector<Vector> allRows = new Vector<Vector>(userEntries.size());
	Vector tempRow = null;
        int rowIndex = 0;

	for( UserEntry userEntry : userEntries ){
	    rowIndex++;
            tempRow = new Vector(8);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( userEntry.getFirstName() );
            tempRow.add( userEntry.getLastName() );
            tempRow.add( userEntry.getEmail() );
            tempRow.add( new MPasswordField(userEntry.getPassword()) );
            tempRow.add( userEntry.getComment() );
            tempRow.add( userEntry );
            allRows.add( tempRow );
        }
        
        return allRows;
    }





}
