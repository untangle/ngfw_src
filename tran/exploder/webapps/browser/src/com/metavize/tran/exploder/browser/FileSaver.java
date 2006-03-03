/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.exploder.browser;

import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

public class FileSaver extends HttpServlet
{
    private Logger logger;

    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        try {
            List<FileItem> items = (List<FileItem>)upload.parseRequest(req);
            for (FileItem fi : items) {
                if (!fi.isFormField()) {
                    String name = fi.getName();
                    System.out.println("FN: " + name);
                }
            }
        } catch (FileUploadException exn) {
            logger.warn("could not get upload", exn);
        }

        try {
            resp.getWriter().println("<html><body onload=\"window.frameElement.uploadComplete();\"></body></html>");
        } catch (IOException exn) {
            throw new ServletException("could not write response", exn);
        }
    }

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
    }
}
