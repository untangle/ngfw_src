/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoConfigJPanel.java,v 1.13 2005/03/22 03:19:22 inieves Exp $
 */
package com.metavize.tran.nat.gui;

import com.metavize.mvvm.tran.Transform;
import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

public class NatConfigJPanel extends MEditTableJPanel{

    public NatConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setFillJButtonEnabled( false );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("protocols");
        super.setDetailsTitle("protocol details");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        NatTableModel protoTableModel = new NatTableModel(transformContext);
        this.setTableModel( protoTableModel );

    }
}


class NatTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 80;  /* category */
    private static final int C3_MW = 100; /* protocol */
    private static final int C4_MW = 100; /* description */
    private static final int C5_MW = 55;  /* block */
    private static final int C6_MW = 55;  /* log */
    private static final int C7_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW), 120); /* signature */


    private static final StringConstants sc = StringConstants.getInstance();
    
    //    private NatFilterPattern tempElem;
    //    private Alerts tempAlerts;
    
    NatTableModel(TransformContext transformContext){
        super(transformContext);
                
        //        tempElem = (NatFilterPattern) NodeType.type(NatFilterPattern.class).instantiate();
        //        tempAlerts = tempElem.alerts();
        
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min    rsz    edit   remv   desc   typ            def
        /*
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.EMPTY_CATEGORY, sc.TITLE_CATEGORY );
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  sc.empty( "no protocol" ), "protocol");
        addTableColumn( tableColumnModel,  4, C4_MW, true,  true,  false, true,  String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION );
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, Boolean.class, "false", sc.bold( sc.TITLE_BLOCK ));
        addTableColumn( tableColumnModel,  6, C6_MW, false, true,  false, false, Boolean.class, "false", sc.bold( sc.TITLE_LOG ));
        addTableColumn( tableColumnModel,  7, C7_MW, true,  true,  false, false, String.class,  sc.empty("no signature"), "signature");
        addTableColumn( tableColumnModel,  8, 0, false, false, true,  false, String.class,  tempPattern.getQuality(), "quality");
        */
        return tableColumnModel;
    }
    
    public Object generateSettings(Vector dataVector){
        Vector rowVector;
        /*
        Nat transform = (Nat)transformContext.transform();
        NatSettings transformSettings = transform.getProtoFilterSettings();
        */

        List elemList = new ArrayList();
        /**
        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            newElem = new ProtoFilterPattern();
            newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setProtocol( (String) rowVector.elementAt(3) );
            newElem.setDescription( (String) rowVector.elementAt(4) );
            newElem.setBlocked( ((Boolean) rowVector.elementAt(5)).booleanValue());
            newElem.setLog(((Boolean) rowVector.elementAt(6)).booleanValue());
            newElem.setDefinition( (String) rowVector.elementAt(7) );
	    newElem.setQuality( (String) rowVector.elementAt(8) );

            elemList.add(newElem);
        }
        **/
        
        return null;
        // transformSettings.setPatterns(elemList); 
        // return transformSettings;
    }
    
    public Vector generateRows(Object transformSettings){
        Vector allRows = new Vector();
        /* 
        Vector row;
        RedirectRule newElem;
        List elemList = ((ProtoFilterSettings)transformSettings).getPatterns();
        int count = 0;
        for (Iterator i = elemList.iterator() ; i.hasNext() ; count++) {
            newElem = (ProtoFilterPattern) i.next();
            row = new Vector();
            row.add(new Integer(count+1));
            row.add(super.ROW_SAVED);
            row.add(newElem.getCategory());
            row.add(newElem.getProtocol());
            row.add(newElem.getDescription());
            row.add(Boolean.valueOf(newElem.isBlocked()));
            row.add(Boolean.valueOf(newElem.getLog()));
            row.add(newElem.getDefinition());
	    row.add(newElem.getQuality());
            allRows.add(row);
        }
        */
        return allRows;
    }
}
