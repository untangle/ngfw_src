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

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

public class SaveUpload extends TagSupport
{
    private static final Logger logger = Logger.getLogger(SaveUpload.class);

    // TagSupport methods -----------------------------------------------------

    @Override
    public int doStartTag() throws JspException
    {
        HttpServletRequest req = (HttpServletRequest)pageContext.getRequest();
        String ct = req.getContentType();
        System.out.println("CT: " + ct);
        if (null != ct && ct.startsWith("multipart/form-data")) {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            try {
                List<FileItem> items = (List<FileItem>)upload.parseRequest(req);
                for (FileItem fi : items) {
                    System.out.println("FN: " + fi.getFieldName());
                }
            } catch (FileUploadException exn) {
                logger.warn("could not get upload", exn);
            }
        }

        return SKIP_BODY;
    }
}
