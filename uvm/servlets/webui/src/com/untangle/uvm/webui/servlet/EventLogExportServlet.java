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
import java.util.Arrays;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
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
    private final int MAX_RESULTS = 10000000;
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
        String name = req.getParameter("name");
		String query = req.getParameter("query");
		String policyIdStr = req.getParameter("policyId");
		String columnListStr = req.getParameter("columnList");

        if (name == null || query == null || policyIdStr == null || columnListStr == null) {
            logger.warn("Invalid parameters: " + name + " , " + query + " , " + policyIdStr + " , " + columnListStr);
            return;
        }

        Long policyId = Long.parseLong(policyIdStr);
        logger.info("Export CSV( name:" + name + " query: " + query + " policyId: " + policyId + " columnList: " + columnListStr + ")");

        ResultSet resultSet = UvmContextFactory.context().getEventsResultSet( query, policyId, MAX_RESULTS );
        
        // Write content type and also length (determined via byte array).
        resp.setCharacterEncoding(CHARACTER_ENCODING);
        resp.setHeader("Content-Type","text/csv");
        resp.setHeader("Content-Disposition","attachment; filename="+name+".csv");
        // Write the header
        resp.getWriter().write(columnListStr + "\n");

        if (resultSet == null)
            return;

        try {
            ResultSetMetaData metadata = resultSet.getMetaData();
            int numColumns = metadata.getColumnCount();
            String[] columnList = columnListStr.split(",");

            // Write each row 
            while (resultSet.next()) {
                // build JSON object from columns
                int writtenColumnCount = 0;
                
                for ( String columnName : columnList ) {
                    Object o = resultSet.getObject( columnName );
                    String oStr = "";
                    if (o != null)
                        oStr = o.toString().replaceAll(",","");
                    
                    if (writtenColumnCount != 0)
                        resp.getWriter().write(",");
                    resp.getWriter().write(oStr);
                    writtenColumnCount++;
                }
                resp.getWriter().write("\n");
            }
        } catch (Exception e) {
            logger.warn("Failed to export CSV.",e);
        }
        
    }
}
