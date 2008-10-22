package com.untangle.uvm.webui.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.BrandingBaseSettings;
import com.untangle.uvm.LocalUvmContextFactory;

/**
 * A servlet which will display the start page
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
public class StartPageServlet extends HttpServlet
{
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {
        String url="/WEB-INF/jsp/startPage.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        BrandingBaseSettings bbs = LocalUvmContextFactory.context().remoteContext().
            brandingManager().getBaseSettings();
        req.setAttribute( "bbs", bbs );
        rd.forward(req, resp);
    }
}
