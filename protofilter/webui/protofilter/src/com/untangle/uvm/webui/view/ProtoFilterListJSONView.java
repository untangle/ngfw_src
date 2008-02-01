package com.untangle.uvm.webui.view;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.node.protofilter.ProtoFilterPattern;

public class ProtoFilterListJSONView extends JSONView {

	@Override
	protected JSONObject createJSON(Map model, HttpServletRequest request, HttpServletResponse response) throws JSONException {
		JSONObject jsonResponse = new JSONObject();
		//TODO extract success & msg in a super class as default functionality
		jsonResponse.put("success", model.get("success"));
		jsonResponse.put("msg", model.get("msg")); // TODO I18N
		
		List<ProtoFilterPattern> patterns = (List<ProtoFilterPattern>)model.get("data");
		JSONArray jsonPatterns = createJSONPatterns(patterns);
		jsonResponse.put("data", jsonPatterns);
		
		return jsonResponse;
	}

	private JSONArray createJSONPatterns(List<ProtoFilterPattern> patterns)
			throws JSONException {
		JSONArray jsonPatterns = new JSONArray();
		for(ProtoFilterPattern  pattern : patterns) {
			JSONObject jsonPattern = createJSONPattern(pattern);
			jsonPatterns.put(jsonPattern);
		}
		return jsonPatterns;
	}

	private JSONObject createJSONPattern(ProtoFilterPattern pattern)
			throws JSONException {
		JSONObject jsonPattern = new JSONObject();
		// TODO I18N dynamic data
		jsonPattern.put("category", pattern.getCategory());
		jsonPattern.put("protocol", pattern.getProtocol());
		jsonPattern.put("blocked", pattern.isBlocked());
		jsonPattern.put("log", pattern.getLog());
		jsonPattern.put("description", pattern.getDescription());
		jsonPattern.put("signature", pattern.getDefinition());
		return jsonPattern;
	}
	
}
