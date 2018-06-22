/**
 * $Id$
 */
package com.untangle.uvm.setup.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jabsorb.JSONSerializer;
import org.jabsorb.serializer.MarshallException;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;

import com.untangle.uvm.LanguageManager;

/**
 * A servlet which will display the start page
 */
@SuppressWarnings("serial")
public class Language extends HttpServlet
{
    /**
     * doGet - handle GET requests
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException 
    {
        UvmContext context = UvmContextFactory.context();
        request.setAttribute( "buildStamp", getServletConfig().getInitParameter("buildStamp") );
        request.setAttribute( "skinName", context.skinManager().getSettings().getSkinName());
        request.setAttribute( "extjsTheme", context.skinManager().getSkinInfo().getExtjsTheme());

        /* Retrieve the list of languages and serialize it for the setup wizard. */

        JSONSerializer js = new JSONSerializer();

        try {
            js.registerDefaultSerializers();
        } catch ( Exception e ) {
            throw new ServletException( "Unable to load the default serializer", e );
        }

        LanguageManager rlm = context.languageManager();
        
        try {
            request.setAttribute( "languageList", js.toJSON( rlm.getLanguagesList()));
            request.setAttribute( "language", rlm.getLanguageSettings().getLanguage());
            request.setAttribute( "languageSource", rlm.getLanguageSettings().getSource());
        } catch ( MarshallException e ) {
            throw new ServletException( "Unable to serializer JSON", e );
        }
            
        String url="/WEB-INF/jsp/language.jsp";
        ServletContext sc = getServletContext();
        RequestDispatcher rd = sc.getRequestDispatcher(url);
        rd.forward(request, response);
    }
}
