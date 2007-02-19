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

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.MPasswordField;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.portal.*;
import com.untangle.mvvm.tran.Transform;
import com.untangle.tran.portal.*;

public class GroupConfigJPanel extends MEditTableJPanel{

    public GroupConfigJPanel(MTransformControlsJPanel mTransformControlsJPanel) {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        GroupConfigTableModel groupConfigTableModel = new GroupConfigTableModel(mTransformControlsJPanel);
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

    private MTransformControlsJPanel mTransformControlsJPanel;

    public GroupConfigTableModel(MTransformControlsJPanel mTransformControlsJPanel){
        this.mTransformControlsJPanel = mTransformControlsJPanel;
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  "[no name]", "name");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, SettingsButtonRunnable.class, "false", sc.html("bookmarks and<br>page settings"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, PortalGroup.class, null, "");
        return tableColumnModel;
    }

    protected void wireUpNewRow(Vector rowVector){
        PortalGroup portalGroup = (PortalGroup) rowVector.elementAt(5);
        SettingsButtonRunnable settingsButtonRunnable = (SettingsButtonRunnable) rowVector.elementAt(3);
        settingsButtonRunnable.setPortalGroup(portalGroup);
        settingsButtonRunnable.setUserType(false);
        settingsButtonRunnable.setMTransformControlsJPanel(mTransformControlsJPanel);
    }


    Hashtable<String,String> userHashtable = new Hashtable<String,String>();

    public void prevalidate(Object settings, Vector<Vector> tableVector) throws Exception {

        PortalSettings portalSettings = (PortalSettings) settings;

        // BUILD THE LIST OF REFERENCED GROUPS (ASSUMES NAMES ARE UNIQUE)
        userHashtable.clear();
        List<PortalUser> portalUsers = (List<PortalUser>) portalSettings.getUsers();
        for( PortalUser user : portalUsers ){
            if( user.getPortalGroup() != null ){
                if( !userHashtable.containsKey(user.getPortalGroup().getName()) )
                    userHashtable.put(user.getPortalGroup().getName(), user.getUid());
            }
        }

        Hashtable<String,String> nameHashtable = new Hashtable<String,String>();
        int rowIndex = 1;
        // go through all the rows and perform some tests on groups
        for( Vector tempUser : tableVector ){
            String state = (String) tempUser.elementAt(0);
            String name = (String) tempUser.elementAt(2);
            if( !ROW_REMOVE.equals(state) ){
                // all names are unique
                if( nameHashtable.contains( name ) )
                    throw new Exception("The name in row: " + rowIndex + " has already been taken.");
                else
                    nameHashtable.put(name,name);
            }
            else{
                // removed rows should not be referenced
                if( userHashtable.containsKey(name) ){
                    throw new Exception("The group \""
                                        + name
                                        + "\" cannot be deleted because it is being used by the user: "
                                        + userHashtable.get(name)
                                        + " in the Users list." );
                }
            }
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
            settingsButtonRunnable.setMTransformControlsJPanel(mTransformControlsJPanel);
            tempRow.add( settingsButtonRunnable );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }

}
