/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
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
    
    public ActiveXConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("ActiveX sources");
        super.setDetailsTitle("source details");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        ActiveXTableModel activeXTableModel = new ActiveXTableModel(transformContext);
        this.setTableModel( activeXTableModel );
    }
}


class ActiveXTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 120; /* identification */
    private static final int C3_MW = 55; /* block */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */
    private static final StringConstants sc = StringConstants.getInstance();

    ActiveXTableModel(TransformContext transformContext){
        super(transformContext);
        refresh();
    }

    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.empty( "no identification" ), "identification" );
        addTableColumn( tableColumnModel,  3, C3_MW, false, true,  false, false, Boolean.class, "true", sc.bold( sc.TITLE_BLOCK ));
        // addTableColumn( tableColumnModel,  6,  55, false, true,  false, false, Boolean.class, "false", "alert");
        // addTableColumn( tableColumnModel,  7,  55, false, true,  false, false, Boolean.class, "true", "log");
        addTableColumn( tableColumnModel,  4, C4_MW, true, true, false, true, String.class, sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    public Object generateSettings(Vector dataVector){
        Vector rowVector;
        SpywareSettings transformSettings = ((Spyware)transformContext.transform()).getSpywareSettings();
        StringRule newElem;
        List elemList = new ArrayList();
        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            newElem = new StringRule();
            // newElem.alerts( new Alerts());
            newElem.setString( (String) rowVector.elementAt(2) );
            newElem.setLive( ((Boolean) rowVector.elementAt(3)).booleanValue() );
            // newElem.alerts().generateCriticalAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            // newElem.alerts().generateSummaryAlerts( ((Boolean) rowVector.elementAt(7)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(4) );
            elemList.add(newElem);
        }
        transformSettings.setActiveXRules(elemList);
        return transformSettings;
    }
    
    public Vector generateRows(Object transformSettings){
        Vector allRows = new Vector();
        Vector row;
        StringRule newElem;
        Vector elemVector = new Vector( ((SpywareSettings)transformSettings).getActiveXRules() );
        for(int i=0; i<elemVector.size(); i++){
            row = new Vector();
            newElem = (StringRule) elemVector.elementAt(i);
            row.add(new Integer(i+1));
            row.add(super.ROW_SAVED);
            row.add(newElem.getString());
            row.add(Boolean.valueOf( newElem.isLive()));
            // if(newElem.alerts() == null)
            // newElem.alerts( new Alerts());
            // row.add(Boolean.valueOf( newElem.alerts().generateCriticalAlerts()));
            // row.add(Boolean.valueOf( newElem.alerts().generateSummaryAlerts()));
            row.add(newElem.getDescription());
            allRows.add(row);
        }
        return allRows;
    }  
}
