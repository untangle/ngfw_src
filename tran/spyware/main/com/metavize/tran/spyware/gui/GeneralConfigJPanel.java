/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.spyware.gui;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.mvvm.tran.TransformContext;

import com.metavize.tran.spyware.*;

import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Vector;
import javax.swing.event.*;

public class GeneralConfigJPanel extends MEditTableJPanel {
    
    
    public GeneralConfigJPanel(TransformContext transformContext) {
        super(true, true);
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("General Settings");
        super.setDetailsTitle("rule notes");
        super.setAddRemoveEnabled(false);
        
        // create actual table model
        GeneralTableModel tableModel = new GeneralTableModel(transformContext);
        this.setTableModel( tableModel );
    }
}



class GeneralTableModel extends MSortedTableModel{ 

    private static final int T_TW = Util.TABLE_TOTAL_WIDTH;
    private static final int C0_MW = Util.LINENO_MIN_WIDTH; /* # */
    private static final int C1_MW = Util.STATUS_MIN_WIDTH; /* status */
    private static final int C2_MW = 200; /* setting name */
    private static final int C3_MW = 200; /* setting value */
    private static final int C4_MW = Util.chooseMax(T_TW - (C0_MW + C1_MW + C2_MW + C3_MW), 120); /* description */

    private static final StringConstants sc = StringConstants.getInstance();

    GeneralTableModel(TransformContext transformContext){
        super(transformContext);
        
        refresh();
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0, C0_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX);
        addTableColumn( tableColumnModel,  1, C1_MW, false, false, false, false, String.class,  null, sc.TITLE_STATUS);
        addTableColumn( tableColumnModel,  2, C2_MW, true,  false, false, false, String.class,  null, "setting name");
        addTableColumn( tableColumnModel,  3, C3_MW, true,  true,  false, false, Object.class,  null, "setting value");
        addTableColumn( tableColumnModel,  4, C4_MW, true, true, false, true, String.class,  sc.EMPTY_DESCRIPTION, sc.TITLE_DESCRIPTION);
        return tableColumnModel;
    }
    
    
    public Object generateSettings(Vector dataVector){
        Vector tempRowVector;
        
        SpywareSettings transformSettings = ((Spyware)transformContext.transform()).getSpywareSettings();
        
        /*
        // activeXEnabled
        tempRowVector = (Vector) dataVector.elementAt(0);
        transformSettings.activeXEnabled( ((Boolean)tempRowVector.elementAt(3)).booleanValue() );
        transformSettings.activeXDetails( (String) tempRowVector.elementAt(4) );
        
        // cookieBlockerEnabled
        tempRowVector = (Vector) dataVector.elementAt(1);
        transformSettings.cookieBlockerEnabled( ((Boolean)tempRowVector.elementAt(3)).booleanValue() );
        transformSettings.cookieBlockerDetails( (String) tempRowVector.elementAt(4) );
        
        // spywareEnabled
        tempRowVector = (Vector) dataVector.elementAt(2);
        transformSettings.spywareEnabled( ((Boolean)tempRowVector.elementAt(3)).booleanValue() );
        transformSettings.spywareDetails( (String) tempRowVector.elementAt(4) );
        */
        
        // blockAllActiveX
        tempRowVector = (Vector) dataVector.elementAt(0);
        transformSettings.setBlockAllActiveX( ((Boolean)tempRowVector.elementAt(3)).booleanValue() );
        transformSettings.setBlockAllActiveXDetails( (String) tempRowVector.elementAt(4) );
        
        return transformSettings;
    }
    
    public Vector generateRows(Object transformDescNode){
        Vector allRows = new Vector(1);
        Vector tempRowVector;
        
        SpywareSettings transformSettings = (SpywareSettings)transformDescNode;
       
        /*
        // activeXEnabled
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(1));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("enable ActiveX filtering");
        tempRowVector.add(Boolean.valueOf(((SpywareSettings)transformDescNode).activeXEnabled()) );
        tempRowVector.add( ((SpywareSettings)transformDescNode).activeXDetails() );
        allRows.add( tempRowVector );
        
        // cookieBlockerEnabled
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(2));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("enable Cookie filtering");
        tempRowVector.add(Boolean.valueOf(((SpywareSettings)transformDescNode).cookieBlockerEnabled()) );
        tempRowVector.add( ((SpywareSettings)transformDescNode).cookieBlockerDetails() );
        allRows.add( tempRowVector );
        
        // spywareEnabled
        tempRowVector = new Vector(4);
        tempRowVector.add(new Integer(3));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("enable Spyware filtering");
        tempRowVector.add(Boolean.valueOf(((SpywareSettings)transformDescNode).spywareEnabled()) );
        tempRowVector.add( ((SpywareSettings)transformDescNode).spywareDetails() );
        allRows.add( tempRowVector );
        */
        
        // blockAllActiveX
        tempRowVector = new Vector(5);
        tempRowVector.add(new Integer(1));
        tempRowVector.add(super.ROW_SAVED);
        tempRowVector.add("block all ActiveX (known & unknown)");
        tempRowVector.add( Boolean.valueOf( transformSettings.getBlockAllActiveX()));
        tempRowVector.add( transformSettings.getBlockAllActiveXDetails());
        allRows.add( tempRowVector );
        
        return allRows;
    }
}
