package com.untangle.uvm.webui.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;

public class StartPageController extends AbstractController {

	protected final Log logger = LogFactory.getLog(getClass());
    
	protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) 
			throws Exception {
		
    	logger.debug("Displaying Web2.0 Client startPage");
        return new ModelAndView("startPage");
	}

}