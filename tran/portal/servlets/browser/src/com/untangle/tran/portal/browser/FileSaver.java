/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.portal.browser;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.mvvm.portal.PortalLogin;
import jcifs.smb.SmbFile;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

public class FileSaver extends HttpServlet
{
    private Logger logger;

    private FileItemFactory fileItemFactory;

    // HttpServlet methods ----------------------------------------------------

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        PortalLogin pl = (PortalLogin)req.getUserPrincipal();

        ServletFileUpload upload = new ServletFileUpload(fileItemFactory);
        String dest = null;

        List<FileItem> items;
        try {
            items = (List<FileItem>)upload.parseRequest(req);
        } catch (FileUploadException exn) {
            logger.warn("could not get file upload", exn);
            sendUploadFailure(resp);
            return;
        }


        for (Iterator<FileItem> i = items.iterator(); i.hasNext(); ) {
            FileItem fi = i.next();

            if (fi.isFormField()) {
                if (fi.getFieldName().equals("dest")) {
                    dest = fi.getString();
                }

                i.remove();
            }
        }

        byte[] buf = new byte[4092];
        for (FileItem fi : items) {
            try {
                copyFile(fi, dest, pl, buf);
            } catch (IOException exn) {
                logger.warn("could not save: " + fi.getName(), exn);
            }
        }

        sendUploadComplete(resp);
    }

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());
        fileItemFactory = new DiskFileItemFactory();
    }

    // private methods --------------------------------------------------------


    private void sendUploadFailure(HttpServletResponse resp)
        throws ServletException
    {
        try {
            resp.getWriter().println("<html><body onload=\"window.frameElement.uploadFailure();\"></body></html>");
        } catch (IOException exn) {
            throw new ServletException("could not send error response", exn);
        }
    }

    private void sendUploadComplete(HttpServletResponse resp)
        throws ServletException
    {
        try {
            resp.getWriter().println("<html><body onload=\"window.frameElement.uploadComplete();\"></body></html>");
        } catch (IOException exn) {
            throw new ServletException("could not send error response", exn);
        }
    }

    private void copyFile(FileItem fi, String dest, PortalLogin pl, byte[] buf)
        throws IOException
    {
        OutputStream os = null;
        InputStream is = null;

        try {
            SmbFile f = Util.getSmbFile(dest + basename(fi.getName()), pl);
            is = fi.getInputStream();
            os = f.getOutputStream();

            int i;
            while (0 <= (i = is.read(buf))) {
                os.write(buf, 0, i);
            }
        } catch (MalformedURLException exn) {
            logger.warn("bad url", exn);
        } finally {
            if (null != os) {
                try {
                    os.close();
                } catch (IOException exn) {
                    logger.warn("could not close OutputStream", exn);
                }
            }

            if (null != is) {
                try {
                    is.close();
                } catch (IOException exn) {
                    logger.warn("could not close InputStream", exn);
                }
            }
        }
    }

    private String basename(String s)
    {
        while (s.endsWith("/") || s.endsWith("\\")) {
            s = s.substring(0, s.length() - 1);
        }

        int i = s.lastIndexOf('/');
        if (0 <= i) {
            s = s.substring(i + 1, s.length());
        }

        i = s.lastIndexOf('\\');
        if (0 <= i) {
            s = s.substring(i + 1, s.length());
        }

        return s;
    }
}
