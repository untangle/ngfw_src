package com.untangle.uvm.webui.view;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.gui.util.IPPortString;
import com.untangle.node.protofilter.ProtoFilterLogEvent;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.node.PipelineEndpoints;

public class ProtoFilterEventsJSONView extends JSONView {

	@Override
	protected JSONObject createJSON(Map model, HttpServletRequest request, HttpServletResponse response) throws JSONException {
		JSONObject jsonResponse = new JSONObject();
		//TODO extract success & msg in a super class as default functionality
		jsonResponse.put("success", model.get("success"));
		jsonResponse.put("msg", model.get("msg")); // TODO I18N
		
		List<ProtoFilterLogEvent> events = (List<ProtoFilterLogEvent>)model.get("data");
		JSONArray jsonEvents = createJSONEvents(events);
    	jsonResponse.put("data", jsonEvents);
		
		return jsonResponse;
	}

	private JSONArray createJSONEvents(List<ProtoFilterLogEvent> events)
			throws JSONException {
		JSONArray jsonEvents = new JSONArray();		
		for( ProtoFilterLogEvent event : events ) {
			JSONObject jsonEvent = createJSONEvent(event);
			jsonEvents.put(jsonEvent);
		}
		return jsonEvents;
	}

	private JSONObject createJSONEvent(ProtoFilterLogEvent event) throws JSONException {
		JSONObject jsonEvent = new JSONObject();
		PipelineEndpoints pe = event.getPipelineEndpoints();
		jsonEvent.put( "timestamp", event.getTimeStamp() );
		jsonEvent.put( "action", event.isBlocked() ? "blocked" : "passed" );
		jsonEvent.put( "client", null == pe ? new IPPortString() : new IPPortString(pe.getCClientAddr(), pe.getCClientPort()) );
		jsonEvent.put( "request", event.getProtocol() );
		jsonEvent.put( "reason", event.isBlocked() ? "blocked in block list" : "not blocked in block list" );
		jsonEvent.put( "server", null == pe ? new IPPortString() : new IPPortString(pe.getSServerAddr(), pe.getSServerPort()) );
		return jsonEvent;
	}
}

