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

package com.metavize.tran.ids.gui;

import com.metavize.mvvm.tran.Transform;
import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import com.metavize.tran.ids.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class IDSConfigJPanel extends MEditTableJPanel{

    public IDSConfigJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("protocols");
        super.setDetailsTitle("protocol details");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        IDSTableModel idsTableModel = new IDSTableModel();
        this.setTableModel( idsTableModel );
	//protoTableModel.setSortingStatus(2, ProtoTableModel.ASCENDING);
    }
}


class IDSTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH_LARGE;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 55;  /* on */
    private static final int C3_MW = 55;  /* log */
    private static final int C4_MW = 150; /* category */
    private static final int C5_MW = 150; /* signature */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */

    
    public TableColumnModel getTableColumnModel(){        

        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  2, C2_MW, false, true,  false, false, Boolean.class, "false", sc.bold("on"));
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "false", sc.bold("log"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, false, String.class,  sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY );
        addTableColumn( tableColumnModel,  5, C5_MW, true,  true,  false, false, String.class,  sc.empty("no rule text"), "rule text");
        addTableColumn( tableColumnModel,  6, C6_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  7, 10,    false, false, true,  false, IDSRule.class, null, "");
        return tableColumnModel;
    }
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception{
	List elemList = new ArrayList();
	IDSRule newElem = null;

	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){            
	    newElem = (IDSRule) rowVector.elementAt(7);
            newElem.setLive( (Boolean) rowVector.elementAt(2) );
            newElem.setLog( (Boolean) rowVector.elementAt(3) );
            newElem.setCategory( (String) rowVector.elementAt(4) );
            newElem.setText( (String) rowVector.elementAt(5) );
            newElem.setDescription( (String) rowVector.elementAt(6) );
            elemList.add(newElem);
        }

	// SAVE SETTINGS ////////
	if( !validateOnly ){
	    IDSSettings transformSettings = (IDSSettings) settings;
	    transformSettings.setRules(elemList);
	}

    }
    
    public Vector generateRows(Object settings){
	IDSSettings idsSettings = (IDSSettings) settings;
	Vector allRows = new Vector();
	Vector tempRow = null;
        int rowIndex = 0;

	for( IDSRule newElem : (List<IDSRule>) idsSettings.getRules() ){
	    rowIndex++;
            tempRow = new Vector(8);
            tempRow.add( super.ROW_SAVED );
            tempRow.add( rowIndex );
            tempRow.add( newElem.isLive() );
            tempRow.add( newElem.getLog() );
            tempRow.add( newElem.getCategory() );
            tempRow.add( newElem.getText() );
            tempRow.add( newElem.getDescription() );
	    tempRow.add( newElem );
            allRows.add( tempRow );
        }
        return allRows;
    }
}
