/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.mvvm.tran.TransformContext;

import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.IPMaddrRule;
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

public class SpywareConfigJPanel extends MEditTableJPanel{
    
    
    public SpywareConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("spyware sources");
        super.setDetailsTitle("source details");
        super.setAddRemoveEnabled(true);
        
        // create actual table model
        SpyTableModel spyTableModel = new SpyTableModel(transformContext);
        this.setTableModel( spyTableModel );
    }
}


class SpyTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 100; /* name */
    private static final int C3_MW = 150; /* subnet */
    private static final int C4_MW = 55; /* block */
    private static final int C5_MW = 55; /* log */
    private static final int C6_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW + C4_MW + C5_MW), 120); /* description */
    private static final StringConstants sc = StringConstants.getInstance();

    SpyTableModel(TransformContext transformContext){
        super(transformContext);      
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS );
        addTableColumn( tableColumnModel,  2, C2_MW, true,  true,  false, false, String.class,  sc.EMPTY_NAME, sc.TITLE_NAME );
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, String.class,  "1.2.3.4/5", "subnet");
        addTableColumn( tableColumnModel,  4, C4_MW, false, true,  false, false, Boolean.class, "true", sc.bold( sc.TITLE_BLOCK ));
        // addTableColumn( tableColumnModel,  6,  55, false, true,  false, false, Boolean.class, "false", "alert");
        addTableColumn( tableColumnModel,  5, C5_MW, false, true,  false, false, Boolean.class, "true", sc.bold( sc.TITLE_LOG ));
        addTableColumn( tableColumnModel,  6, C6_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    public Object generateSettings(Vector dataVector){
        Vector rowVector;
        IPMaddrRule newElem;
        List elemList = new ArrayList();
        for(int i=0; i<dataVector.size(); i++){
            rowVector = (Vector) dataVector.elementAt(i);
            newElem = new IPMaddrRule();
            // newElem.setCategory( (String) rowVector.elementAt(2) );
            newElem.setName( (String) rowVector.elementAt(2) );
	    IPMaddr newIPMaddr;
	    try{ newIPMaddr = IPMaddr.parse( (String) rowVector.elementAt(3) ); }
            catch(IllegalArgumentException e){ continue; }
            newElem.setIpMaddr( newIPMaddr );
            newElem.setLive( ((Boolean) rowVector.elementAt(4)).booleanValue() );
            newElem.setLog( ((Boolean) rowVector.elementAt(5)).booleanValue() );

            // newElem.alerts( new Alerts());
            // newElem.alerts().generateCriticalAlerts( ((Boolean) rowVector.elementAt(6)).booleanValue() );
            // newElem.alerts().generateSummaryAlerts( ((Boolean) rowVector.elementAt(7)).booleanValue() );
            newElem.setDescription( (String) rowVector.elementAt(6) );
            elemList.add(newElem);
        }
        SpywareSettings transformSettings = ((Spyware)transformContext.transform()).getSpywareSettings();
        transformSettings.setSubnetRules(elemList);
        return transformSettings;
    }
    
    public Vector generateRows(Object transformSettings){
        Vector allRows = new Vector();
        Vector row;
        IPMaddrRule newElem;
        Vector elemVector = new Vector( ((SpywareSettings)transformSettings).getSubnetRules() );
        for(int i=0; i<elemVector.size(); i++){
            row = new Vector();
            newElem = (IPMaddrRule) elemVector.elementAt(i);
            row.add(new Integer(i+1));
            row.add(super.ROW_SAVED);
            // row.add(newElem.getCategory());
            row.add(newElem.getName());
            row.add(newElem.getIpMaddr().toString());
            row.add(Boolean.valueOf( newElem.isLive()));
            row.add(Boolean.valueOf( newElem.getLog()));
            // if(newElem.alerts() == null)
            // newElem.alerts( new Alerts());
            // row.add(Boolean.valueOf(newElem.alerts().generateCriticalAlerts()));
            // row.add(Boolean.valueOf(newElem.alerts().generateSummaryAlerts()));    
            row.add(newElem.getDescription());
            
            allRows.add(row);
        }
        return allRows;
    } 
}
