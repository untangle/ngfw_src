package com.untangle.uvm.webui.web;

import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.multiaction.MultiActionController;

import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.toolbox.MackageDesc;
import com.untangle.uvm.webui.WebuiUvmException;
import com.untangle.uvm.webui.domain.ConfigItem;
import com.untangle.uvm.webui.service.MainRackService;

public class MainRackController extends MultiActionController {

	protected final Log logger = LogFactory.getLog(getClass());
	
	private MainRackService mainRackService = null;
	
	public ModelAndView getStoreItems(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		List<MackageDesc> items = null;
		
		try {
			items = getMainRackService().getStoreItems();
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
		
		return new ModelAndView("json/store", getResult(success, msg, items));
	}

	public ModelAndView getToolboxItems(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		List<MackageDesc> items = null;
		try {
			items = getMainRackService().getToolboxItems();
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/toolbox", getResult(success, msg, items));
	}
	
	public ModelAndView getConfigItems(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		List<ConfigItem> items = null;
		try {
			items = getMainRackService().getConfigItems();
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
		
		return new ModelAndView("json/config", getResult(success, msg, items));
	}
	
	public ModelAndView getVirtualRacks(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		List<Policy> virtualRacks = null;
		try {
			virtualRacks = getMainRackService().getVirtualRacks();
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
		
		return new ModelAndView("json/virtualRacks", getResult(success, msg, virtualRacks));
	}
	
	public ModelAndView getNodes(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		List<NodeContext> nodes = null;
		String policyName = request.getParameter("rackName");
		try {
			nodes = getMainRackService().getNodes(policyName);
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/nodes", getResult(success, msg, nodes));
	}
	
	public ModelAndView addToRack(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		NodeContext node = null;
		String policyName = request.getParameter("rackName");
		String installName = request.getParameter("installName");
		try {
			node = getMainRackService().addToRack(installName, policyName);
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/node", getResult(success, msg, node));
	}
	
	public ModelAndView removeFromRack(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		Long nodeId = Long.parseLong(request.getParameter("nodeId"));
		String installName = request.getParameter("installName");
		try {
			getMainRackService().removeFromRack(installName, nodeId);
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/response", getResult(success, msg, null));
	}
		
	public ModelAndView purchase(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		String installName = request.getParameter("installName");
		try {
			getMainRackService().purchase(installName);
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/response", getResult(success, msg, null));
	}
	
	public ModelAndView returnToStore(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		String installName = request.getParameter("installName");
		try {
			getMainRackService().returnToStore(installName);
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/response", getResult(success, msg, null));
	}

	public ModelAndView startNode(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		String nodeName = request.getParameter("nodeName");
		Long nodeId = Long.parseLong(request.getParameter("nodeId"));
		try {
			getMainRackService().startNode(nodeName, nodeId);
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/response", getResult(success, msg, null));
	}
	
	public ModelAndView stopNode(HttpServletRequest request, HttpServletResponse response) {
		boolean success = true;		
		String msg = null;
		String nodeName = request.getParameter("nodeName");
		Long nodeId = Long.parseLong(request.getParameter("nodeId"));
		try {
			getMainRackService().stopNode(nodeName, nodeId);
		} catch (WebuiUvmException e) {
			msg = e.getMessage();
			success = false;
		}
			
		return new ModelAndView("json/response", getResult(success, msg, null));
	}	
	
	public ModelAndView getImage(HttpServletRequest request, HttpServletResponse response) {
		logger.debug("Displaying Image");
		String name = request.getParameter("name");
		byte[] imageData = getMainRackService().getMackageDesc(name).getDescIcon();
		
	    return new ModelAndView("image", "imageData", imageData);
	}
	
	private HashMap<String, Object> getResult(boolean success, String message, Object data) {
		HashMap<String, Object> result = new HashMap<String, Object>();
		result.put("success", new Boolean(success));
		result.put("msg", message);
		result.put("data", data);
		return result;
	}

	public MainRackService getMainRackService() {
		return mainRackService;
	}

	public void setMainRackService(MainRackService mainRackService) {
		this.mainRackService = mainRackService;
	}

}