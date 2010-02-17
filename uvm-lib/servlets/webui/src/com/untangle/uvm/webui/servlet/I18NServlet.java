/*
 * $HeadURL: svn://chef/work/src/uvm-lib/impl/com/untangle/uvm/engine/ReportingManagerImpl.java $
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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.RemoteLanguageManager;
import com.untangle.uvm.client.RemoteUvmContext;

/**
 * A servlet that when given a module name returns a javascript hash containing
 * all the key-value pairs in that module.
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
public class I18NServlet extends HttpServlet {

    /** json content type */
    private static final String JSON_CONTENT_TYPE = "application/json";
    /** character encoding */
    private static final String CHARACTER_ENCODING = "utf-8";

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        RemoteUvmContext uvm = LocalUvmContextFactory.context().remoteContext();
        RemoteLanguageManager languageManager = uvm.languageManager();

        String module = req.getParameter("module");

        Map<String, String> map = languageManager.getTranslations(module);

        // Write content type and also length (determined via byte array).
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(CHARACTER_ENCODING);

        try {
            JSONObject json = createJSON(map);
            json.write(resp.getWriter());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * Creates a JSONObject [JSONObject,JSONArray,JSONNUll] from the map values.
     */
    protected JSONObject createJSON(Map map) throws JSONException
    {
        return new JSONObject(map);
    }
}