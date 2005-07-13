/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.protofilter.gui;

import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.protofilter.*;

import javax.swing.table.*;
import java.util.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform){
        super(transform);
	setTableModel(new LogTableModel());
    }

    public Vector generateRows(Object settings){

        List<ProtoFilterLog> logList = (List<ProtoFilterLog>) ((ProtoFilter)super.logTransform).getLogs(depthJSlider.getValue());
        Vector allEvents = new Vector();

        Vector test = new Vector();
        Vector event;

        for( ProtoFilterLog log : logList ){
            event = new Vector();
            event.add( log.getCreateDate().toString() );
	    event.add( log.getAction() );
	    event.add( log.getProtocol() );
	    event.add( log.getReason() );
            event.add( log.getDirection().getDirectionName() );
            event.add( log.getServerAddr() + ":" + ((Integer)log.getSServerPort()).toString() );
            event.add( log.getClientAddr() + ":" + ((Integer)log.getCClientPort()).toString() );
            allEvents.insertElementAt(event,0);
        }
	
        return allEvents;
    }
    

    
    class LogTableModel extends MSortedTableModel{                                                                                       
	
	public TableColumnModel getTableColumnModel(){                                                                                   
	    DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();                                                    
	    //                                 #   min  rsz    edit   remv   desc   typ               def                                
	    addTableColumn( tableColumnModel,  0,  125, true,  false, false, false, String.class, null, "timestamp" );                   
	    addTableColumn( tableColumnModel,  1,  55,  true,  false, false, false, String.class, null, "action" );                      
	    addTableColumn( tableColumnModel,  2,  100, true,  false, false, false, String.class, null, "request" );                     
	    addTableColumn( tableColumnModel,  3,  100, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
	    addTableColumn( tableColumnModel,  4,  100, true,  false, false, false, String.class, null, sc.html("request<br>direction") );                   
	    addTableColumn( tableColumnModel,  5,  155, true,  false, false, false, String.class, null, "server" );                      
	    addTableColumn( tableColumnModel,  6,  155, true,  false, false, false, String.class, null, sc.html("client<br>(requestor)") );
	    return tableColumnModel;                                                                                                     
	}                                                                                                                                
	
	public void generateSettings(Object settings, boolean validateOnly) throws Exception {}                                          
	
	public Vector generateRows(Object settings) {                                                                                    
	    return LogJPanel.this.generateRows(null);                                                                              
	}                                                                                                                                
	
    }       

}
