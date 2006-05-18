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
        addTableColumn( tableColumnModel,  2,  100, true,  true,  false, false, String.class,     "[no ID/login]", "user/login ID");
        addTableColumn( tableColumnModel,  3,  100, true,  true,  false, false, String.class,     sc.EMPTY_NAME, "first name");
        addTableColumn( tableColumnModel,  4,  100, true,  true,  false, false, String.class,     sc.EMPTY_NAME, "last name");
        addTableColumn( tableColumnModel,  5,  150, true,  true,  false, false, String.class,     "[no email]", "email address");
        addTableColumn( tableColumnModel,  6,  150, true,  true,  false, false, MPasswordField.class, "aabbccdd", "password");
        //addTableColumn( tableColumnModel,  7,  150, true,  true,  false, false, DirectoryBookmarksButtonRunnable.class, "true", "bookmarks");
        addTableColumn( tableColumnModel,  7,  150, true,  true,  false, true,  String.class,     sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  8,  10,  false, false, true,  false, UserEntry.class,  null, "");
        return tableColumnModel;
    }

    public void prevalidate(DirectoryCompoundSettings directoryCompoundSettings, Vector<Vector> tableVector) throws Exception {
        Hashtable<String,String> uidHashtable = new Hashtable<String,String>();

        int rowIndex = 1;

        // go through all the rows and perform some tests
        for( Vector tempUser : tableVector ){
	    String uid = (String) tempUser.elementAt(2);
	    String password = new String(((MPasswordField) tempUser.elementAt(6)).getPassword());
	    // all uid's are unique
	    if( uidHashtable.contains( uid ) )
		throw new Exception("The user/login ID at row: " + rowIndex + " has already been taken.");
	    else
		uidHashtable.put(uid,uid);
	    // the password meets certain criteria
	    if( password.length() == 0 )
		throw new Exception("The password at row: " + rowIndex + " must be at least 1 character long.");

	    rowIndex++;
	}

    }
    
    public void generateSettings(DirectoryCompoundSettings directoryCompoundSettings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
	List<UserEntry> allRows = new ArrayList(tableVector.size());
	UserEntry newElem = null;
	
        for( Vector rowVector : tableVector ){	    
	    newElem = (UserEntry) rowVector.elementAt(8);
	    newElem.setUID( (String) rowVector.elementAt(2) );
	    newElem.setFirstName( (String) rowVector.elementAt(3) );
	    newElem.setLastName( (String) rowVector.elementAt(4) );
	    newElem.setEmail( (String) rowVector.elementAt(5) );
	    newElem.setPassword( new String(((MPasswordField)rowVector.elementAt(6)).getPassword()) );
	    newElem.setComment( (String) rowVector.elementAt(7) );
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
            tempRow = new Vector(9);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( userEntry.getUID() );
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
