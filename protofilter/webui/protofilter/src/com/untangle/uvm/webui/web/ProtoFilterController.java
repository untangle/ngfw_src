package com.untangle.uvm.webui.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import com.untangle.node.protofilter.ProtoFilter;
import com.untangle.node.protofilter.ProtoFilterLogEvent;
import com.untangle.node.protofilter.ProtoFilterPattern;
import com.untangle.node.protofilter.ProtoFilterSettings;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.webui.WebuiUvmException;
import com.untangle.uvm.webui.service.NodeService;
import com.untangle.uvm.webui.view.ProtoFilterEventsJSONView;
import com.untangle.uvm.webui.view.ProtoFilterListJSONView;

public class ProtoFilterController extends MultiActionController {

	protected final Log logger = LogFactory.getLog(getClass());
	private NodeService nodeService = null;
	
	public ModelAndView loadProtocolList(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		String nodeName = request.getParameter("nodeName");
		Long nodeId = Long.parseLong(request.getParameter("nodeId"));
		Object data = null;
		try {
			// TODO we can cache the node object in the session
			ProtoFilter node = getProtoFilter(nodeName, nodeId);
			data = node.getProtoFilterSettings().getPatterns();
		} catch (Exception e) {
			msg = e.getMessage();
			success = false;
		}
			
		// use ProtoFilterListJSONView to render JSON objects
		return new ModelAndView( new ProtoFilterListJSONView(), getResult(success, msg, data));
	}

	public ModelAndView loadRepositoryDescs(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		String nodeName = request.getParameter("nodeName");
		Long nodeId = Long.parseLong(request.getParameter("nodeId"));
		List<RepositoryDesc> repositoryDescs = null;
		try {
			ProtoFilter node = getProtoFilter(nodeName, nodeId);
	        EventManager<ProtoFilterLogEvent> em = node.getEventManager();
	        repositoryDescs = em.getRepositoryDescs();
		} catch (Exception e) {
			msg = e.getMessage();
			success = false;
		}
			
		// use ProtoFilterEventLogJSONView to render JSON objects
		return new ModelAndView("json/repositoryDescs", getResult(success, msg, repositoryDescs));
	}
	
	public ModelAndView loadEvents(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		String nodeName = request.getParameter("nodeName");
		Long nodeId = Long.parseLong(request.getParameter("nodeId"));
		String repositoryName = request.getParameter("repositoryName");
		List<ProtoFilterLogEvent> events = null;
		try {
			ProtoFilter node = getProtoFilter(nodeName, nodeId);
	        EventManager<ProtoFilterLogEvent> em = node.getEventManager();
	        events = em.getRepository(repositoryName).getEvents();
		} catch (Exception e) {
			msg = e.getMessage();
			success = false;
		}
			
		// use ProtoFilterEventsJSONView to render JSON objects
		return new ModelAndView(new ProtoFilterEventsJSONView(), getResult(success, msg, events));
		
	}
		
	public ModelAndView saveProtocolList(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		String nodeName = request.getParameter("nodeName");
		Long nodeId = Long.parseLong(request.getParameter("nodeId"));
		String jsonData = request.getParameter("data");

		try {
			ProtoFilter node = getProtoFilter(nodeName, nodeId);
			ProtoFilterSettings settings = node.getProtoFilterSettings();
			
			JSONArray jsonPatterns = new JSONArray(jsonData);
			List<ProtoFilterPattern> patterns = createPatterns(jsonPatterns);
			
		    settings.setPatterns(patterns);
		    node.setProtoFilterSettings(settings);
			
		} catch (JSONException jsone) {
			logger.error("Ivalid data", jsone);
			//TODO I18N this
			msg = "Invalid data!";
			success = false;
		} catch (Exception e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/response", getResult(success, msg, null));
	}

	private List<ProtoFilterPattern> createPatterns(JSONArray jsonPatterns)
			throws JSONException {
		List<ProtoFilterPattern> patterns = new ArrayList<ProtoFilterPattern>();
		for (int i = 0; i < jsonPatterns.length(); i++) {
			JSONObject jsonPattern = jsonPatterns.getJSONObject(i);
			ProtoFilterPattern pattern = createPattern(jsonPattern);
			patterns.add(pattern);
		}
		return patterns;
	}

	private ProtoFilterPattern createPattern(JSONObject jsonPattern)
			throws JSONException {
		//TODO validation
		// validate_bool(block);
		// validate_bool(log);
		
		ProtoFilterPattern pattern = new ProtoFilterPattern();								
		pattern.setCategory(jsonPattern.getString("category"));
		pattern.setProtocol(jsonPattern.getString("protocol"));
		pattern.setBlocked(jsonPattern.getBoolean("blocked"));
		pattern.setLog(jsonPattern.getBoolean("log"));
		pattern.setDescription(jsonPattern.getString("description"));
		pattern.setDefinition(jsonPattern.getString("signature"));
		return pattern;
	}
	
	private HashMap<String, Object> getResult(boolean success, String message, Object data) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("success", new Boolean(success));
		result.put("msg", message);
		result.put("data", data);
		return result;
	}
	
	private ProtoFilter getProtoFilter(String nodeName, Long nodeId) throws WebuiUvmException{
		return (ProtoFilter) getNodeService().getNode(nodeName, nodeId);
	}

	public NodeService getNodeService() {
		return nodeService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
}