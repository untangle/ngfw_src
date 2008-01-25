package com.untangle.uvm.webui.web;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

import com.untangle.uvm.node.Node;
import com.untangle.uvm.webui.service.MainRackService;

public class NodeController extends AbstractController {

	protected final Log logger = LogFactory.getLog(getClass());
	
	private MainRackService mainRackService = null;
    
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) 
			throws Exception {
		
		// TODO error handling 
		
		String nodeName = request.getParameter("nodeName");
		Long nodeId = Long.parseLong(request.getParameter("nodeId"));
		String action = request.getParameter("action");
		Node node = getMainRackService().getNode(nodeName, nodeId);
		
		//TODO we should get the controller name from API
		String nodeControllerName = "com.untangle.uvm.webui.web.ProtoFilterController";
		
//		Class cl = Class.forName(nodeControllerName);
		Thread t = Thread.currentThread();
		ClassLoader loader = t.getContextClassLoader();
		Class cl = loader.loadClass(nodeControllerName);
		Constructor c = cl.getConstructor(Node.class);
		Object controllerInstance = c.newInstance(node);
		
        Method m = cl.getMethod(action, HttpServletRequest.class, HttpServletResponse.class);
        return (ModelAndView)m.invoke(controllerInstance, request, response);
	}
	
	public MainRackService getMainRackService() {
		return mainRackService;
	}

	public void setMainRackService(MainRackService mainRackService) {
		this.mainRackService = mainRackService;
	}
	
}