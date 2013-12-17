/*
 * $Id: CsvServlet.java,v 1.00 2011/12/17 10:13:49 dmorris Exp $
 */
package com.untangle.uvm.reports.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.untangle.node.reporting.ReportingNodeImpl;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.servlet.DownloadHandler;

/**
 * Gets CSV for a detail report.
 */
@SuppressWarnings("serial")
public class CsvServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        DownloadHandler handler = UvmContextFactory.context().servletFileManager()
                .getDownloadHandler(ReportingNodeImpl.REPORTS_EVENT_LOG_DOWNLOAD_HANDLER);

        if (handler == null) {
            logger.error("Unable to handle an download of type: " + ReportingNodeImpl.REPORTS_EVENT_LOG_DOWNLOAD_HANDLER);
            return;
        }

        logger.info("Serving Download: " + ReportingNodeImpl.REPORTS_EVENT_LOG_DOWNLOAD_HANDLER);
        handler.serveDownload(req, resp);
    }

}
