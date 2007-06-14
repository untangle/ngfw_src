/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.gui.configuration;

import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;

import com.untangle.gui.util.StringConstants;
import com.untangle.gui.util.Util;
import com.untangle.gui.widgets.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.uvm.*;
import com.untangle.uvm.addrbook.*;
import com.untangle.uvm.security.*;


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
        addTableColumn( tableColumnModel,  0,  Util.STATUS_MIN_WIDTH, false, false, true, false, String.class,     null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1,  Util.LINENO_MIN_WIDTH, false, false, true, false, Integer.class,    null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2,  100, true,  true,  false, false, String.class,     "[no ID/login]", "user/login ID");
        addTableColumn( tableColumnModel,  3,  100, true,  true,  false, false, String.class,     sc.EMPTY_NAME, "first name");
        addTableColumn( tableColumnModel,  4,  100, true,  true,  false, false, String.class,     sc.EMPTY_NAME, "last name");
        addTableColumn( tableColumnModel,  5,  250, true,  true,  false, false, String.class,     "[no email]", "email address");
        addTableColumn( tableColumnModel,  6,  150, true,  true,  false, false, MPasswordField.class, "", "password");
        //addTableColumn( tableColumnModel,  7,  150, true,  true,  false, false, DirectoryBookmarksButtonRunnable.class, "true", "bookmarks");
        //addTableColumn( tableColumnModel,  7,  150, true,  true,  false, true,  String.class,     sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  7,  10,  false, false, true,  false, UserEntry.class,  null, "");
        return tableColumnModel;
    }

    public void prevalidate(DirectoryCompoundSettings directoryCompoundSettings, Vector<Vector> tableVector) throws Exception {
        Hashtable<String,String> uidHashtable = new Hashtable<String,String>();

        int rowIndex = 1;

        // go through all the rows and perform some tests
        for( Vector tempUser : tableVector ){
            String state = (String) tempUser.elementAt(2);
            if( ROW_REMOVE.equals(state) )
                continue;
            String uid = (String) tempUser.elementAt(2);
            String firstName = (String) tempUser.elementAt(3);
            String lastName = (String) tempUser.elementAt(4);

            String password = new String(((MPasswordField) tempUser.elementAt(6)).getPassword());

            if( !ROW_REMOVE.equals(state) ){
                // all uid's are unique
                if( uidHashtable.contains( uid ) )
                    throw new Exception("The user/login ID at row: " + rowIndex + " has already been taken.");
                else
                    uidHashtable.put(uid,uid);

                // first name contains no spaces
                if( firstName.contains(" ") )
                    throw new Exception("The first name at row: " + rowIndex + " must not contain any space characters.");

                // last name contains no spaces
                if( lastName.contains(" ") )
                    throw new Exception("The last name at row: " + rowIndex + " must not contain any space characters.");
            }

            // CHECK PASSWORDS FOR ONLY NEW ROWS
            if( ROW_ADD.equals(state) ) {
                // the password is at least one character
                if( password.length() == 0 )
                    throw new Exception("The password at row: " + rowIndex + " must be at least 1 character long.");

                // the password contains no spaces
                if( password.contains(" ") )
                    throw new Exception("The password at row: " + rowIndex + " must not contain any space characters.");
            }

            rowIndex++;
        }

    }

    public void generateSettings(DirectoryCompoundSettings directoryCompoundSettings, Vector<Vector> tableVector, boolean validateOnly) throws Exception {
        List<UserEntry> allRows = new ArrayList(tableVector.size());
        UserEntry newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (UserEntry) rowVector.elementAt(7);
            newElem.setUID( (String) rowVector.elementAt(2) );
            newElem.setFirstName( (String) rowVector.elementAt(3) );
            newElem.setLastName( (String) rowVector.elementAt(4) );
            newElem.setEmail( (String) rowVector.elementAt(5) );
            newElem.setPassword( new String(((MPasswordField)rowVector.elementAt(6)).getPassword()) );
            // newElem.setComment( (String) rowVector.elementAt(7) );
            // String asdf = new String(((MPasswordField)rowVector.elementAt(6)).getPassword()) ;
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
            // We can't get the password back.
            tempRow.add( new MPasswordField(UserEntry.UNCHANGED_PASSWORD) );
            // tempRow.add( userEntry.getComment() );
            tempRow.add( userEntry );
            allRows.add( tempRow );
        }

        return allRows;
    }





}
