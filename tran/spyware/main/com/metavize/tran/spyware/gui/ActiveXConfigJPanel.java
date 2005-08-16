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


package com.metavize.tran.spyware.gui;

import com.metavize.mvvm.tran.TransformContext;

import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.tran.spyware.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import java.util.List;
import java.util.ArrayList;
import javax.swing.event.*;

public class ActiveXConfigJPanel extends MEditTableJPanel {
    
    public ActiveXConfigJPanel() {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("ActiveX sources");
        super.setDetailsTitle("source details");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        ActiveXTableModel activeXTableModel = new ActiveXTableModel();
        this.setTableModel( activeXTableModel );
    }
}


class ActiveXTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C1_MW = Util.LINENO_MIN_WIDTH; /* # - invisible */
    private static final int C2_MW = 240; /* identification */
    private static final int C3_MW = 55; /* block */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C2_MW + C3_MW), 120); /* description */

    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.empty( "no identification" ), "identification" );
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.bold("block"));
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        addTableColumn( tableColumnModel,  5, 10,    false, false, true,  false, StringRule.class, null, "");
        return tableColumnModel;
    }
    
    public void generateSettings(Object settings, boolean validateOnly) throws Exception{
        List elemList = new ArrayList();
	for( Vector rowVector : (Vector<Vector>) this.getDataVector() ){

            StringRule newElem = (StringRule) rowVector.elementAt(5);
            newElem.setString( (String) rowVector.elementAt(2) );
            newElem.setLive( ((Boolean) rowVector.elementAt(3)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }

	// SAVE SETTINGS ///////
	if( !validateOnly ){
	    SpywareSettings spywareSettings = (SpywareSettings) settings;
	    spywareSettings.setActiveXRules(elemList);
	}

    }
    
    public Vector generateRows(Object settings){
	SpywareSettings spywareSettings = (SpywareSettings) settings;
        Vector allRows = new Vector();
	int count = 1;
	for( StringRule newElem : (List<StringRule>) spywareSettings.getActiveXRules() ){

            Vector row = new Vector();
            row.add(super.ROW_SAVED);
            row.add(new Integer(count));
            row.add(newElem.getString());
            row.add(Boolean.valueOf( newElem.isLive()));
            row.add(newElem.getDescription());
	    row.add(newElem);

            allRows.add(row);
	    count++;
        }
        return allRows;
    }  
}
