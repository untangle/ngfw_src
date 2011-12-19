/**
 * $Id: EventLogExportServlet.java,v 1.00 2011/12/18 20:38:21 dmorris Exp $
 */
package com.untangle.uvm.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;

/**
 * A servlet for export event log data
 */
@SuppressWarnings({ "serial", "unchecked" })
public class EventLogExportServlet extends HttpServlet
{

	private final Logger logger = Logger.getLogger(getClass());
	
    /** character encoding */
    private static final String CHARACTER_ENCODING = "utf-8";

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
    {
        processExport(req, resp);
	}

	private void processExport(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
    {
		String gridData = req.getParameter("data");
        String gridName = req.getParameter("name");

        // Write content type and also length (determined via byte array).
        resp.setCharacterEncoding(CHARACTER_ENCODING);
        resp.setHeader("Content-Disposition","attachment; filename="+gridName+".json");

        logger.warn("Writer: " + resp.getWriter());
        logger.warn("data: " + resp.getWriter());
        
        resp.getWriter().write(gridData);
	}
}
