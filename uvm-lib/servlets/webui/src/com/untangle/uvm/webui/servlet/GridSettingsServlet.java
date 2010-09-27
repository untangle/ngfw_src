/*
 * $HeadURL: svn://chef/work/src/uvm-lib/servlets/webui/src/com/untangle/uvm/webui/servlet/SetupSettingsServlet.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.I18nUtil;

/**
 * A servlet for import / export grid settings 
 * 
 * @author Vlad Dumitrescu <vdumitrescu@untangle.com>
 */
@SuppressWarnings({ "serial", "unchecked" })
public class GridSettingsServlet extends HttpServlet {

	private final Logger logger = Logger.getLogger(getClass());
	
    /** character encoding */
    private static final String CHARACTER_ENCODING = "utf-8";

	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		boolean isExport = "export".equals(req.getParameter("type"));
		if (isExport) {
			processExport(req, resp);
		} else {
			processImport(req, resp);
		}

	}

	private void processImport(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {

		// Create a factory for disk-based file items
		DiskFileItemFactory factory = new DiskFileItemFactory();
		// Create a new file upload handler
		ServletFileUpload upload = new ServletFileUpload(factory);

		// Parse the request
		List<FileItem> items = null;
		JSONArray gridSettings = null;
		try {
			items = upload.parseRequest(req);
			for (FileItem item : items) {
				if (!item.isFormField()) {
					String fileString = item.getString();
					if (fileString != null && fileString.trim().length() > 0) {
						gridSettings = new JSONArray(fileString.trim());
						createImportRespose(resp, true, gridSettings);
						return;
					} else {
						createImportRespose(resp, false, importFailedMessage());
						return;
					}
				}
			}
		} catch (JSONException e) {
			logger.debug("Import grid settings failed. Settings must be formatted as a JSON Array.", e);
			createImportRespose(resp, false, importFailedMessage());
			return;
		} catch (Exception exn) {
			logger.warn("could not upload", exn);
			createImportRespose(resp, false, exn.getMessage());
			return;
		}

	}
	
	private void createImportRespose(HttpServletResponse resp, boolean success,
			Object msg) throws IOException {
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		try {
			JSONObject obj = new JSONObject();
			obj.put("success", new Boolean(success));
			if (msg != null) {
				obj.put("msg", msg);
			}
			out.print(obj);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		out.flush();
		out.close();
	}
	
	private String importFailedMessage()
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle-libuvm");
        return I18nUtil.tr("Import failed. Settings must be formatted as a JSON Array.", i18n_map);
    }
	
	
	private void processExport(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String gridData = req.getParameter("gridData");
        String gridName = req.getParameter("gridName");

        // Write content type and also length (determined via byte array).
        resp.setCharacterEncoding(CHARACTER_ENCODING);
        resp.setHeader("Content-Disposition","attachment; filename="+gridName+".json");

        try {
            JSONArray json = new JSONArray(gridData);
            json.write(resp.getWriter());
        } catch (JSONException e) {
			logger.debug("Export grid settings failed. Settings must be formatted as a JSON Array.", e);
            throw new ServletException("Export failed. Settings must be formatted as a JSON Array.");
        }
	}
}
