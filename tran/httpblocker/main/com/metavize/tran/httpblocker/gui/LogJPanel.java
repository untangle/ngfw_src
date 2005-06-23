
package com.metavize.tran.httpblocker.gui;

import com.metavize.gui.widgets.editTable.MLogTableJPanel;

import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.httpblocker.HttpBlocker;
import com.metavize.tran.httpblocker.RequestLog;
import com.metavize.tran.httpblocker.Reason;

import java.util.*;

public class LogJPanel extends MLogTableJPanel {

    public LogJPanel(Transform transform){
	super(transform);
    }

    public Vector generateRows(Object settings){

	List<RequestLog> requestLogList = (List<RequestLog>) ((HttpBlocker)super.logTransform).getEvents(null, depthJSlider.getValue());
	Vector allEvents = new Vector();

	Vector test = new Vector();
	Vector event;

	for( RequestLog requestLog : requestLogList ){

	    event = new Vector();
	    event.add( requestLog.timeStamp().toString() );

	    Reason reason = requestLog.getReason();
	    if( reason == null ){
		event.add( "pass" );
		event.add( requestLog.getUrl() );
		event.add( "unknown" );
	    }
	    else{
		event.add( "block" );
		event.add( requestLog.getUrl() );
		event.add( reason.toString() );
	    }
	    event.add( "unknown" );
	    event.add( requestLog.getServerAddr() + ":" + ((Integer)requestLog.getSServerPort()).toString() );
	    event.add( requestLog.getClientAddr() + ":" + ((Integer)requestLog.getCClientPort()).toString() );
	    allEvents.insertElementAt(event,0);
	}

	return allEvents;
    }

}
