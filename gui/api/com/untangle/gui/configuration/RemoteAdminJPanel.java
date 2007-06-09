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

package com.untangle.gui.configuration;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.util.*;
import com.untangle.gui.widgets.MPasswordField;
import com.untangle.gui.widgets.dialogs.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.uvm.security.*;



public class RemoteAdminJPanel extends MEditTableJPanel{

    public RemoteAdminJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        RemoteAdminTableModel remoteAdminTableModel = new RemoteAdminTableModel();
        this.setTableModel( remoteAdminTableModel );
    }

}




class RemoteAdminTableModel extends MSortedTableModel<RemoteCompoundSettings> {

    private static final int MIN_PASSWD_LENGTH = 3;

    private static final StringConstants sc = StringConstants.getInstance();


    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #   min  rsz    edit   remv   desc   typ               def
        addTableColumn( tableColumnModel,  0,  Util.STATUS_MIN_WIDTH, false, false, true, false, String.class,     null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  Util.LINENO_MIN_WIDTH, false, false, true, false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  85, true,  true,  false, false, String.class,     sc.EMPTY_NAME,  sc.TITLE_NAME );
        addTableColumn( tableColumnModel,  3,  85, true,  true,  false, false, String.class,     "[no login]", "login");
        addTableColumn( tableColumnModel,  4,  85, false, true,  false, false, Boolean.class,     "false", sc.html("read-only<br>access"));
        addTableColumn( tableColumnModel,  5,  85, true,  true,  false, false, String.class,     "[no email]", sc.html("email<br>address"));
        addTableColumn( tableColumnModel,  6,  85, true,  false, true,  false, MPasswordField.class, PasswordUtil.encrypt("12345678"), sc.html("original<br>password"));
        addTableColumn( tableColumnModel,  7, 260, true,  true,  false, false, MPasswordField.class, "",     sc.html("set new password<br>(for new accounts or to change password)"));
        addTableColumn( tableColumnModel,  8, 260, true,  true,  false, false, MPasswordField.class, "",     sc.html("confirm new password<br>(for new accounts or to change password)"));
        addTableColumn( tableColumnModel,  9,  10, false, false, true,  false, User.class,    null, "");
        return tableColumnModel;
    }


    public void prevalidate(RemoteCompoundSettings remoteCompoundSettings, Vector<Vector> tableVector) throws Exception {
        Hashtable loginHashtable = new Hashtable();
        boolean oneValidAccount = false;
        boolean oneWritableAccount = false;
        int rowIndex = 0;

        // go through all the rows and perform some tests
        for( Vector tempUser : tableVector ){
            rowIndex++;
            byte[] origPasswd = ((MPasswordField)tempUser.elementAt(6)).getByteArray();
            char[] newPasswd = ((MPasswordField)tempUser.elementAt(7)).getPassword();
            char[] newConfPasswd  = ((MPasswordField)tempUser.elementAt(8)).getPassword();
            boolean readOnly = (Boolean) tempUser.elementAt(4);
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
                throw new Exception("The \"new\" and/or \"new confirm\" passwords for: \"" + loginName + "\" in row: " + rowIndex + " are shorter than the minimum " + MIN_PASSWD_LENGTH + " characters.");
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
                if( !readOnly )
                    oneWritableAccount = true;
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

        // verify that there was at least one non-read-only account
        if(!oneWritableAccount){
            throw new Exception("There must always be at least one non-read-only (writable) account.");
        }
    }

    public void generateSettings(RemoteCompoundSettings remoteCompoundSettings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        Set allRows = new LinkedHashSet(tableVector.size());
        User newElem = null;
        int rowIndex = 0;
        for( Vector rowVector : tableVector ){
            rowIndex++;
            newElem = (User) rowVector.elementAt(9);
            byte[] origPasswd = ((MPasswordField)rowVector.elementAt(6)).getByteArray();
            char[] newPasswd = ((MPasswordField)rowVector.elementAt(7)).getPassword();

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
            newElem.setReadOnly( (Boolean) rowVector.elementAt(4) );
            String email = (String) rowVector.elementAt(5);
            if( (email!=null) && (email.length()==0) )
                email = null;
            newElem.setEmail(email);
            allRows.add(newElem);
        }

        // SAVE SETTINGS /////////////
        if( !validateOnly ){
            AdminSettings adminSettings = remoteCompoundSettings.getAdminSettings();
            adminSettings.setUsers(allRows);
        }

    }

    public Vector<Vector> generateRows(RemoteCompoundSettings remoteCompoundSettings) {
        AdminSettings adminSettings = remoteCompoundSettings.getAdminSettings();
        Set<User> users = (Set<User>) adminSettings.getUsers();
        Vector<Vector> allRows = new Vector<Vector>(users.size());
        Vector tempRow = null;
        int rowIndex = 0;

        for( User user : users ){
            rowIndex++;
            tempRow = new Vector(9);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( user.getName() );
            tempRow.add( user.getLogin() );
            tempRow.add( user.isReadOnly() );
            tempRow.add( user.getEmail()==null?"":user.getEmail() );
            MPasswordField tempMPasswordField = new MPasswordField( user.getPassword() );
            tempRow.add( tempMPasswordField );
            tempRow.add( new MPasswordField() );
            tempRow.add( new MPasswordField() );
            MPasswordField originalMPasswordField = new MPasswordField();
            originalMPasswordField.setGeneratesChangeEvent(false);
            tempRow.add( user );
            allRows.add( tempRow );
        }

        return allRows;
    }





}
