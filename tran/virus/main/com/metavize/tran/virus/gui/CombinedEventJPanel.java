/*
 * 
 *
 * Created on March 25, 2004, 6:11 PM
 */

package com.metavize.tran.virus.gui;


import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.logging.*;

import com.metavize.gui.transform.*;
import com.metavize.gui.pipeline.MPipelineJPanel;
import com.metavize.gui.widgets.editTable.*;
import com.metavize.gui.util.*;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;
import java.util.Set;
import javax.swing.event.*;

import com.metavize.tran.virus.*;

public class CombinedEventJPanel extends MEditTableJPanel {
    
    /*
    public CombinedEventJPanel(TransformContext transformContext) {
        super();
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("HTTP MIME Type/FTP/HTTP Combined log");
        super.setDetailsTitle("rule notes");
        
        // create actual table model
        CombinedEventTableModel eventTableModel = new CombinedEventTableModel(transformContext);
        this.setTableModel( eventTableModel );
        
    }
    */
}

/*
class CombinedEventTableModel extends MSortedTableModel{ 
    
    
    CombinedEventTableModel(TransformContext transformContext){
        super(transformContext);
    }
    
    public TableColumnModel getTableColumnModel(){
        
        DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
        //                                 #  min  rsz    edit   remv   desc   typ            def
        addTableColumn( tableColumnModel,  0,  30, false, false, false, false, Integer.class, null, "#");
        addTableColumn( tableColumnModel,  1, 100, true,  false, false, false, String.class,  null, "time");
        addTableColumn( tableColumnModel,  2, 150, true,  false, false, false, String.class,  null, "client");
        addTableColumn( tableColumnModel,  3, 150, true,  false, false, false, String.class,  null, "server");
        addTableColumn( tableColumnModel,  4, 150, true,  false, false, false, String.class,  null, "scan result");
        addTableColumn( tableColumnModel,  5,  85, true,  false, false, false, String.class,  null, "action");
        return tableColumnModel;  
    }


    
    public Set generateSettings(Vector dataVector){
        return null;
    }
    
    public void flushLog(){
        try{
            Util.getMvvmContext().loggingManager().clearLogs(super.transformContext.getTid());
        }
        catch(Exception e){
            try{
                Util.handleExceptionWithRestart("error resetting log", e);
            }
            catch(Exception f){
                Util.handleExceptionNoRestart("error resetting log", f);
            }
        }
    }
    
    public Vector generateRows(Object transformSettings){
        LogEvent newEvent;
        VirusLogEvent tempEvent;
        Vector allRows = new Vector();
        Vector row;
        int counter = 1;
        LogEvent logEvents[] = null;
        
        logEvents = Util.getMvvmContext().loggingManager().transformLogEvents(super.transformContext.getTid(), "com.metavize.tran.util.virus.VirusLogEvent");
        
        for(int i=0; i<logEvents.length; i++){
            newEvent = logEvents[i];
            
            if(newEvent instanceof VirusLogEvent){
                tempEvent = (VirusLogEvent) newEvent;
                row = new Vector(6);

                row.add(new Integer(counter));
                row.add(tempEvent.timestamp());
                row.add(tempEvent.clientIP());
                row.add(tempEvent.serverIP());
                if( (new Boolean(tempEvent.clean() )).booleanValue() == true )
                    row.add("clean");
                else
                    row.add("infected");
                if( (new Boolean(tempEvent.block())).booleanValue() == true )
                    row.add("blocked");
                else
                    row.add("passed");
            }
            else{
                System.err.println("ERROR: unknown type of log event");
                continue;
            }
            
            allRows.add(row);
            counter++;
        }
        return allRows;
    }   
    
}
*/
