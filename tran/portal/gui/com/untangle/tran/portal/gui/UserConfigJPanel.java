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

import java.awt.Insets;
import java.awt.Window;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.portal.*;
import com.untangle.tran.portal.*;

public class UserConfigJPanel extends MEditTableJPanel{

    public UserConfigJPanel(MTransformControlsJPanel mTransformControlsJPanel) {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);

        // create actual table model
        UserConfigTableModel userConfigTableModel = new UserConfigTableModel(mTransformControlsJPanel);
        this.setTableModel( userConfigTableModel );
        //userConfigTableModel.setSortingStatus(3, UserConfigTableModel.ASCENDING);
    }

}


class UserConfigTableModel extends MSortedTableModel<Object>{

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 55;  /* live */
    private static final int C3_MW = 150; /* UID */
    private static final int C4_MW = 150; /* group */
    private static final int C5_MW = 150; /* edit (settings) */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */

    private MTransformControlsJPanel mTransformControlsJPanel;
    private JPanel panel;

    protected boolean getSortable(){ return false; }

    public UserConfigTableModel(MTransformControlsJPanel mTransformControlsJPanel){
        this.mTransformControlsJPanel = mTransformControlsJPanel;
    }

    private static final String PLEASE_SELECT_USER = "Please select a user";
    private DefaultComboBoxModel groupModel = new DefaultComboBoxModel();


    public void updateGroupModel(List<PortalGroup> portalGroups){
        groupModel.removeAllElements();
        PortalGroupWrapper defaultPortalGroupWrapper = new PortalGroupWrapper(null);
        groupModel.addElement(defaultPortalGroupWrapper); // for the "default" group
        for( PortalGroup portalGroup : portalGroups ){
            groupModel.addElement(new PortalGroupWrapper(portalGroup));
        }
        groupModel.setSelectedItem(defaultPortalGroupWrapper);
    }

    public TableColumnModel getTableColumnModel(){

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, false, true,  false, false, Boolean.class, "true", sc.bold("live"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, UidButtonRunnable.class, "true", "user ID/login");
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, false, ComboBoxModel.class, groupModel, "group");
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, SettingsButtonRunnable.class,  "false", sc.html("bookmarks and<br>page settings" ));
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  true, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  7, 10,    false, false, true,  false, PortalUser.class, null, "");
        return tableColumnModel;
    }

    protected void wireUpNewRow(Vector rowVector){
        PortalUser portalUser = (PortalUser) rowVector.elementAt(7);
        SettingsButtonRunnable settingsButtonRunnable = (SettingsButtonRunnable) rowVector.elementAt(5);
        settingsButtonRunnable.setPortalUser(portalUser);
        settingsButtonRunnable.setUserType(true);
        settingsButtonRunnable.setMTransformControlsJPanel(mTransformControlsJPanel);
        UidButtonRunnable uidButtonRunnable = (UidButtonRunnable) rowVector.elementAt(3);
        uidButtonRunnable.setTopLevelWindow((Window)mTransformControlsJPanel.getTopLevelAncestor());
    }


    public void prevalidate(Object settings, Vector<Vector> tableVector) throws Exception {
        Hashtable<String,String> uidHashtable = new Hashtable<String,String>();
        int rowIndex = 1;
        // go through all the rows and perform some tests
        for( Vector tempUser : tableVector ){
            String state = (String) tempUser.elementAt(0);
            if( !ROW_REMOVE.equals(state) ){
                String uid = ((UidButtonRunnable) tempUser.elementAt(3)).getUid();
                // no uid is unselected
                if( uid == null ){
                    throw new Exception("No user id/login has been selected in row: " + rowIndex);
                }
                // all uid's are unique
                if( uidHashtable.contains( uid ) )
                    throw new Exception("The user/login ID in row: " + rowIndex + " already exists.");
                else
                    uidHashtable.put(uid,uid);
            }
            rowIndex++;
        }
    }

    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
        List elemList = new ArrayList(tableVector.size());
        PortalUser newElem = null;

        for( Vector rowVector : tableVector ){
            newElem = (PortalUser) rowVector.elementAt(7);
            newElem.setLive( (Boolean) rowVector.elementAt(2) );
            newElem.setUid( ((UidButtonRunnable) rowVector.elementAt(3)).getUid() );
            PortalGroupWrapper portalGroupWrapper = (PortalGroupWrapper) ((ComboBoxModel) rowVector.elementAt(4)).getSelectedItem();
            newElem.setPortalGroup( portalGroupWrapper.getPortalGroup() );
            newElem.setDescription( (String) rowVector.elementAt(6) );
            elemList.add(newElem);
        }

        // SAVE SETTINGS ////////
        if( !validateOnly ){
            PortalSettings portalSettings = (PortalSettings) settings;
            portalSettings.setUsers(elemList);
        }
    }

    public Vector<Vector> generateRows(Object settings){
        PortalSettings portalSettings = (PortalSettings) settings;
        List<PortalUser> users = (List<PortalUser>) portalSettings.getUsers();
        Vector<Vector> allRows = new Vector<Vector>(users.size());
        Vector tempRow = null;
        int rowIndex = 0;

        updateGroupModel((List<PortalGroup>)portalSettings.getGroups());
        //updateUidModel(mTransformControlsJPanel.getLocalUserEntries());

        for( PortalUser newElem : users ){
            rowIndex++;
            tempRow = new Vector(8);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.isLive() );
            UidButtonRunnable uidButtonRunnable = new UidButtonRunnable("true");
            uidButtonRunnable.setUid( newElem.getUid() );
            tempRow.add( uidButtonRunnable );
            ComboBoxModel comboBoxModel = copyComboBoxModel(groupModel);
            comboBoxModel.setSelectedItem(new PortalGroupWrapper(newElem.getPortalGroup()));
            tempRow.add( comboBoxModel );
            SettingsButtonRunnable settingsButtonRunnable = new SettingsButtonRunnable("true");
            settingsButtonRunnable.setPortalUser(newElem);
            settingsButtonRunnable.setUserType(true);
            settingsButtonRunnable.setMTransformControlsJPanel(mTransformControlsJPanel);
            tempRow.add( settingsButtonRunnable );
            tempRow.add( newElem.getDescription() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }

    class PortalGroupWrapper {
        private PortalGroup portalGroup;
        public PortalGroupWrapper(PortalGroup portalGroup){
            this.portalGroup = portalGroup;
        }
        public String toString(){
            if( portalGroup == null )
                return "no group";
            else
                return portalGroup.getName();
        }
        public PortalGroup getPortalGroup(){
            return portalGroup;
        }
        public boolean equals(Object obj){
            if( ! (obj instanceof PortalGroupWrapper) )
                return false;
            PortalGroupWrapper other = (PortalGroupWrapper) obj;
            if( (getPortalGroup() == null) && (other.getPortalGroup() == null) )
                return true;
            else if( (getPortalGroup() != null) && (other.getPortalGroup() == null) )
                return false;
            else if( (getPortalGroup() == null) && (other.getPortalGroup() != null) )
                return false;
            else
                return getPortalGroup().equals(other.getPortalGroup());
        }
    }
}
