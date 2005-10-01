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

import com.metavize.gui.widgets.dialogs.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import java.util.*;
import javax.swing.table.*;
import javax.swing.*;

import com.metavize.mvvm.security.*;
import com.metavize.mvvm.*;

import com.metavize.gui.util.StringConstants;



public class AdminConfigJDialog extends MConfigJDialog {
    
    private static final String NAME_ADMIN_ACCOUNTS = "Accounts";

    public AdminConfigJDialog( ) {
    }

    protected void generateGui(){
        this.setTitle(NAME_ADMIN_ACCOUNTS);
        
        // create graphical context
        MEditTableJPanel mEditTableJPanel = new MEditTableJPanel(true, false);
        mEditTableJPanel.setInsets(new Insets(4, 4, 2, 2));
        mEditTableJPanel.setTableTitle(NAME_ADMIN_ACCOUNTS);
        mEditTableJPanel.setDetailsTitle("rule notes");
        mEditTableJPanel.setAddRemoveEnabled(true);

        // create actual table model
        AdminConfigTableModel configTableModel = new AdminConfigTableModel();
	super.savableMap.put(NAME_ADMIN_ACCOUNTS, configTableModel);
	super.refreshableMap.put(NAME_ADMIN_ACCOUNTS, configTableModel);

        mEditTableJPanel.setTableModel( configTableModel );
        this.contentJTabbedPane.addTab(NAME_ADMIN_ACCOUNTS, null, mEditTableJPanel);
    }
    
    public void sendSettings(Object settings) throws Exception {
	Util.getAdminManager().setAdminSettings( (AdminSettings) settings );
    }
    public void refreshSettings(){
	super.settings = Util.getAdminManager().getAdminSettings();
    }

}



    
class AdminConfigTableModel extends MSortedTableModel {

    private static final int MIN_PASSWD_LENGTH = 3;
    private Hashtable loginHashtable = new Hashtable();

    private static final StringConstants sc = StringConstants.getInstance();
    

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  Util.STATUS_MIN_WIDTH, false, false, false, false, String.class,     null, sc.TITLE_STATUS );
	addTableColumn( tableColumnModel,  1,  Util.LINENO_MIN_WIDTH, false, false, false, false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  85, true,  true,  false, false, String.class,     sc.EMPTY_NAME,  sc.TITLE_NAME );
        addTableColumn( tableColumnModel,  3,  85, true,  true,  false, false, String.class,     "[no login]", "login");
        addTableColumn( tableColumnModel,  4,  85, true,  false, true,  false, MPasswordField.class, PasswordUtil.encrypt("12345678"), sc.html("original<br>password"));
        addTableColumn( tableColumnModel,  5, 260, true,  true,  false, false, MPasswordField.class, "",     sc.html("set new password<br>(for new accounts or to change password)"));
        addTableColumn( tableColumnModel,  6, 260, true,  true,  false, false, MPasswordField.class, "",     sc.html("confirm new password<br>(for new accounts or to change password)"));
        addTableColumn( tableColumnModel,  7, 250, true,  true,  false, false, MPasswordField.class, "12345678",     sc.html("<b>original user's password</b><br>(required to change or remove account)"));
        addTableColumn( tableColumnModel,  8,  85, true,  true,  false, false, String.class,     "[no email]", sc.html("email<br>address"));
        addTableColumn( tableColumnModel,  9,  10, false, false, true,  false, User.class,    null, "");
        return tableColumnModel;
    }


    public void prevalidate(Object settings, Vector<Vector> tableVector) throws Exception {
        loginHashtable.clear();
	boolean oneValidAccount = false;
        int rowIndex = 0;

        // go through all the rows and perform some tests
        for( Vector tempUser : tableVector ){
            rowIndex++;
            byte[] origPasswd = ((MPasswordField)tempUser.elementAt(4)).getByteArray();
            char[] newPasswd = ((MPasswordField)tempUser.elementAt(5)).getPassword();
            char[] newConfPasswd  = ((MPasswordField)tempUser.elementAt(6)).getPassword();
            char[] proceedPasswd  = ((MPasswordField)tempUser.elementAt(7)).getPassword();
            String loginName = (String) tempUser.elementAt(3);
	    String state = (String) tempUser.elementAt(0);

	    // verify that the login name is at least one character long
            if( loginName.length() < 1 ){
		throw new Exception("The login name in row: " + rowIndex + "cannot be blank.");
	    }

            // verify that the login name is not duplicated
            if( loginHashtable.containsKey(loginName) ){
		throw new Exception("The login name: \"" + loginName + "\" in row: " + rowIndex + " already exists.");
            }
	    else{
		loginHashtable.put(loginName, loginName);
	    }
            
	    // verify that the new password is of the proper length
            if( (newPasswd.length > 0) && (newPasswd.length < MIN_PASSWD_LENGTH) ){
		throw new Exception("The \"new\" and//or \"new confirm\" passwords for: \"" + loginName + "\" in row: " + rowIndex + " are shorter than the minimum " + MIN_PASSWD_LENGTH + " characters.");
            }

            // verify that the new and new confirm passwords are the same
            if( !java.util.Arrays.equals(newPasswd, newConfPasswd) ){
		throw new Exception("The \"new\" and \"new confirm\" passwords are not the same for: \"" + loginName + "\" in row: " + rowIndex + ".");
            }

	    
	    if( ROW_REMOVE.equals(state) ){

	    }
	    else{
		// record if the row was not removed, that way we know we had at least one valid account
                oneValidAccount = true;

	    }

            // verify that all of the original and proceed passwords are the same for any non-saved (changed) row
            if( !((String)tempUser.elementAt(0)).equals(super.ROW_SAVED)
		&&  !PasswordUtil.check( String.valueOf(proceedPasswd), origPasswd )){
		throw new Exception("The \"original\" password is not correct for: \"" + loginName + "\" in row: " + rowIndex + ".");
            }

            // verify that if this is a new row, a new password has been chosen
            if( ((String)tempUser.elementAt(0)).equals(super.ROW_ADD) && (newPasswd.length == 0) ){
		throw new Exception("The \"new\" password has not been set for: \"" + loginName + "\" in row: " + rowIndex + ".");
            }



        }

        // verify that there is at least one valid entry after all operations
        if(!oneValidAccount){
	    throw new Exception("There must always be at least one valid account.");
        }

    }
    
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {

	Set allRows = new LinkedHashSet(tableVector.size());
	User newElem = null;
	Util.printMessage("STATUS: STARTING SAVING of accounts");
	int rowIndex = 0;
        for( Vector rowVector : tableVector ){
	    rowIndex++;
            newElem = (User) rowVector.elementAt(9);
            byte[] origPasswd = ((MPasswordField)rowVector.elementAt(4)).getByteArray();
            char[] newPasswd = ((MPasswordField)rowVector.elementAt(5)).getPassword();            

            if( ((String)rowVector.elementAt(0)).equals(super.ROW_REMOVE) ){
		// THIS IS HERE FOR SAFETY
                continue; // those removed and of a strange state are never even added            
            }
            else{
		newElem.setName( (String) rowVector.elementAt(2) );
		newElem.setLogin( (String) rowVector.elementAt(3) );
		if( newPasswd.length > 0 )
		    newElem.setClearPassword( new String(newPasswd) );
            }
            newElem.setEmail( (String) rowVector.elementAt(8) );
            allRows.add(newElem);
        }
        
	// SAVE SETTINGS /////////////
	if( !validateOnly ){
	    AdminSettings adminSettings = (AdminSettings) settings;
	    adminSettings.setUsers(allRows);
	}

    }

    public Vector<Vector> generateRows(Object settings) {
	AdminSettings adminSettings = (AdminSettings) settings;
	Set<User> users = (Set<User>) adminSettings.getUsers(); 
        Vector<Vector> allRows = new Vector<Vector>(users.size());
	Vector tempRow = null;
        int rowIndex = 0;

	for( User user : users ){
	    rowIndex++;
            tempRow = new Vector(10);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( user.getName() );
            tempRow.add( user.getLogin() );
            MPasswordField tempMPasswordField = new MPasswordField( user.getPassword() );
            tempRow.add( tempMPasswordField );
            tempRow.add( new MPasswordField() );
            tempRow.add( new MPasswordField() );
	    MPasswordField originalMPasswordField = new MPasswordField();
	    originalMPasswordField.setGeneratesChangeEvent(false);
            tempRow.add( originalMPasswordField );
            tempRow.add( user.getEmail() );
            tempRow.add( user );
            allRows.add( tempRow );
        }
        
        return allRows;
    }





}
