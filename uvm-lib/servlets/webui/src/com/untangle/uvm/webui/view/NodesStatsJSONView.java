package com.untangle.uvm.webui.view;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeStats;

public class NodesStatsJSONView extends JSONView {

	@Override
	protected JSONObject createJSON(Map model, HttpServletRequest request, HttpServletResponse response) throws JSONException {
		JSONObject jsonResponse = new JSONObject();
		//TODO extract success & msg in a super class as default functionality
		jsonResponse.put("success", model.get("success"));
		jsonResponse.put("msg", model.get("msg")); // TODO I18N
		
		List<Node> nodes = (List<Node>)model.get("data");
		JSONArray jsonNodesStats = createJSONNodesStats(nodes);
		jsonResponse.put("data", jsonNodesStats);
		
		return jsonResponse;
	}

	private JSONArray createJSONNodesStats(List<Node> nodes)
			throws JSONException {
		JSONArray jsonNodesStats= new JSONArray();
		for(Node  node : nodes) {
			JSONObject jsonNodeStats = createJSONNodeStats(node);
			jsonNodesStats.put(jsonNodeStats);
		}
		return jsonNodesStats;
	}

	private JSONObject createJSONNodeStats(Node node)
			throws JSONException {
		JSONObject jsonNodeStats = new JSONObject();
		jsonNodeStats.put("nodeId", node.getTid().getId());
		
		NodeStats nodeStats = node.getStats();
		JSONObject jsonStats = new JSONObject();
        jsonStats.put("tcpSessionCount", nodeStats.tcpSessionCount());
        jsonStats.put("tcpSessionTotal", nodeStats.tcpSessionTotal());
        jsonStats.put("tcpSessionRequestTotal", nodeStats.tcpSessionRequestTotal());
        jsonStats.put("udpSessionCount", nodeStats.udpSessionCount());
        jsonStats.put("udpSessionTotal", nodeStats.udpSessionTotal());
        jsonStats.put("udpSessionRequestTotal", nodeStats.udpSessionRequestTotal());
        jsonStats.put("c2tBytes", nodeStats.c2tBytes());
        jsonStats.put("c2tChunks", nodeStats.c2tChunks());
        jsonStats.put("t2sBytes", nodeStats.t2sBytes());
        jsonStats.put("t2sChunks", nodeStats.t2sChunks());
        jsonStats.put("s2tBytes", nodeStats.s2tBytes());
        jsonStats.put("s2tChunks", nodeStats.s2tChunks());
        jsonStats.put("t2cBytes", nodeStats.t2cBytes());
        jsonStats.put("t2cChunks", nodeStats.t2cChunks());
        jsonStats.put("startDate", nodeStats.startDate());
        jsonStats.put("lastConfigureDate", nodeStats.lastConfigureDate());
        jsonStats.put("lastActivityDate", nodeStats.lastActivityDate());
        
        JSONArray jsonCounters = new JSONArray();
        for (int i = 0; i < 16; i++) {
			jsonCounters.put(nodeStats.getCount(i));
		}
        jsonStats.put("counters", jsonCounters);
		
		jsonNodeStats.put("stats", jsonStats);
		
		return jsonNodeStats;
	}
	
}
