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

package com.metavize.tran.firewall.gui;

import com.metavize.gui.widgets.editTable.*;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.firewall.*;

import javax.swing.table.*;
import java.util.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform){
        super(transform);
	setTableModel(new LogTableModel());
    }

    public Vector generateRows(Object settings){

        List<FirewallLog> requestLogList = (List<FirewallLog>) ((Firewall)super.logTransform).getEventLogs(depthJSlider.getValue());
        Vector allEvents = new Vector();

        Vector test = new Vector();
        Vector event;

        for( FirewallLog requestLog : requestLogList ){
            event = new Vector();
            event.add( requestLog.getCreateDate().toString() );
	    event.add( requestLog.getAction() );
	    event.add( requestLog.getReason() );
            event.add( requestLog.getDirection().getDirectionName() );
            event.add( requestLog.getServerAddr() + ":" + ((Integer)requestLog.getServerPort()).toString() );
            event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getClientPort()).toString() );
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
	    addTableColumn( tableColumnModel,  2,  100, true,  false, false, false, String.class, null, sc.html("reason for<br>action") );
	    addTableColumn( tableColumnModel,  3,  100, true,  false, false, false, String.class, null, sc.html("request<br>direction") );
	    addTableColumn( tableColumnModel,  4,  155, true,  false, false, false, String.class, null, "server" );
	    addTableColumn( tableColumnModel,  5,  155, true,  false, false, false, String.class, null, sc.html("client<br>(requestor)") );
	    return tableColumnModel;                                                                                                     
	}                                                                                                      

	public void generateSettings(Object settings, boolean validateOnly) throws Exception {} 
	
	public Vector generateRows(Object settings) {
	    return LogJPanel.this.generateRows(null);
	}
	
    }       

}
