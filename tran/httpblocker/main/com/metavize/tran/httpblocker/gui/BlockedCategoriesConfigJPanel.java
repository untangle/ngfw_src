/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: BlockedCategoriesConfigJPanel.java,v 1.7 2005/02/09 20:38:31 jdi Exp $
 */
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.tran.httpblocker.*;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.List;
import javax.swing.event.*;

public class BlockedCategoriesConfigJPanel extends MEditTableJPanel {
    
    
    public BlockedCategoriesConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("Blocked Categories");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        CategoryTableModel categoryTableModel = new CategoryTableModel(transformContext);
        super.setTableModel( categoryTableModel );
    }
}


class CategoryTableModel extends MSortedTableModel
{ 
    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 150; /* category */
    private static final int C3_MW = 100; /* block domains */
    private static final int C4_MW = 100; /* block URLs */
    private static final int C5_MW = 100; /* block expressions */
    private static final int C6_MW = 120; /* description */
    private static final int C7_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW + C6_MW), 55); /* original name */

    CategoryTableModel(TransformContext transformContext){
        super(transformContext);
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true, false, Integer.class, null, "#");
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, "status");
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "category");
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, null, "<html><b><center>block<br>domains</center></b></html>");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, null, "<html><b><center>block<br>URLs</center></b></html>");
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, Boolean.class, null, "<html><b><center>block<br>expressions</center></b></html>");
        //addTableColumn( tableColumnModel,  6,  55, false, true,  false, false, Boolean.class, null, "alert");
        //addTableColumn( tableColumnModel,  7,  55, false, true,  false, false, Boolean.class, null, "log");
        addTableColumn( tableColumnModel,  6, C6_MW, true, true, false,  true,  String.class,  null, "description");
        addTableColumn( tableColumnModel,  7, C7_MW, false, false, true,  false, String.class,  null, "original name");
        return tableColumnModel;
    }
    
    
    public Object generateSettings(Vector dataVector){
        Vector rowVector;
        BlacklistCategory newElem;
        List elemList = new ArrayList();

        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            
            newElem = new BlacklistCategory();
            newElem.setDisplayName( (String) rowVector.elementAt(2) );
            newElem.setBlockDomains( ((Boolean) rowVector.elementAt(3)).booleanValue() );
            newElem.setBlockUrls( ((Boolean) rowVector.elementAt(4)).booleanValue() );
            newElem.setBlockExpressions( ((Boolean) rowVector.elementAt(5)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(6) );
            newElem.setName( (String) rowVector.elementAt(7) );
            
            //Alerts newAlerts= (Alerts)NodeType.type(Alerts.class).instantiate();
            //newAlerts.generateCriticalAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            //newAlerts.generateSummaryAlerts( ((Boolean) rowVector.elementAt(7)).booleanValue() );
            //newElem.alerts(newAlerts);
            
            elemList.add(newElem);  
        }
        
        HttpBlockerSettings transformSettings = ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings();
        transformSettings.setBlacklistCategories( elemList );
        return transformSettings;
    }
    
    public Vector generateRows(Object transformDescNode){
        Vector allRows = new Vector();
        Vector row;
        int counter = 1;
        BlacklistCategory newElem;
        List elemList = ((HttpBlocker)transformContext.transform()).getHttpBlockerSettings().getBlacklistCategories();

        for (Iterator i = elemList.iterator() ; i.hasNext() ; ){
            newElem = (BlacklistCategory) i.next();           

            row = new Vector();
            row.add(new Integer(counter));
            row.add(super.ROW_SAVED);
            row.add(newElem.getDisplayName());
            row.add(Boolean.valueOf(newElem.getBlockDomains()) );
            row.add(Boolean.valueOf(newElem.getBlockUrls()) );
            row.add(Boolean.valueOf(newElem.getBlockExpressions()) );
            row.add(newElem.getDescription());
            row.add(newElem.getName());

            // alerts
            //if(newElem.alerts() == null)
            //newElem.alerts( (Alerts) NodeType.type(Alerts.class).instantiate()  );
            //Alerts newAlerts = newElem.alerts();
            //row.add(new Boolean(newElem.alerts().generateCriticalAlerts()));
            //row.add(new Boolean(newElem.alerts().generateSummaryAlerts()));

            allRows.add(row);
            counter++;
        }
        return allRows;
    }
}
