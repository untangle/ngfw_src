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

import com.untangle.gui.configuration.DirectoryCompoundSettings;
import com.untangle.gui.configuration.DirectoryJDialog;

import com.untangle.mvvm.tran.Transform;
import com.untangle.gui.transform.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.gui.widgets.MPasswordField;
import com.untangle.gui.util.*;

import com.untangle.tran.portal.*;
import com.untangle.mvvm.portal.*;
import com.untangle.mvvm.addrbook.UserEntry;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class KickUserJPanel extends MEditTableJPanel{

    public KickUserJPanel(MTransformControlsJPanel mTransformControlsJPanel) {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        KickUserTableModel kickUserTableModel = new KickUserTableModel(mTransformControlsJPanel);
        this.setTableModel( kickUserTableModel );
	kickUserTableModel.setSortingStatus(2, UserConfigTableModel.ASCENDING);
    }

}


class KickUserTableModel extends MSortedTableModel<Object>{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 150; /* UID */
    private static final int C3_MW = 150; /* group */
    private static final int C4_MW = 150; /* action */
    private static final int C5_MW = 150; /* client addr */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW + C4_MW + C4_MW + C5_MW), 140); /* date */

    private MTransformControlsJPanel mTransformControlsJPanel;

    public KickUserTableModel(MTransformControlsJPanel mTransformControlsJPanel){
	this.mTransformControlsJPanel = mTransformControlsJPanel;
    }

    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, sc.html("user id/login"));
        addTableColumn( tableColumnModel,  3, C3_MW, true,  false, false, false, String.class,  null, "group");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, KickButtonRunnable.class,  null, "logout");
        addTableColumn( tableColumnModel,  5, C5_MW, true,  false, false, false, IPaddrString.class, null, "client address");
        addTableColumn( tableColumnModel,  6, C6_MW, true,  false, false, false, Date.class,  "true", sc.html("login time" ));
        addTableColumn( tableColumnModel,  7, 10,    false, false, true,  false, PortalLogin.class, null, "");
        return tableColumnModel;
    }


        
    public void generateSettings(Object settings, Vector<Vector> tableVector, boolean validateOnly) throws Exception{
    }
    
    public Vector<Vector> generateRows(Object settings){
	List<PortalLogin> loginList = mTransformControlsJPanel.getLoginList();
        Vector<Vector> allRows = new Vector<Vector>(loginList.size());
	Vector tempRow = null;
	int rowIndex = 0;


	for( PortalLogin newElem : loginList ){
	    rowIndex++;
            tempRow = new Vector(8);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.getUser() );
            tempRow.add( (newElem.getGroup()==null?"no group":newElem.getGroup()) );
	    tempRow.add( new KickButtonRunnable(newElem, "true") );
            tempRow.add( new IPaddrString(newElem.getClientAddr()) );
            tempRow.add( newElem.getLoginDate() );
            tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }

}
