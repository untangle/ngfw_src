/**
 * $Id: AdvancedSupportHandler.java 1 2021-08-07 15:51:00Z mahotz $
 */
package com.untangle.uvm;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;
import org.json.JSONString;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.UvmContextFactory;

/**
 * AdvancedSupport http servelet class
 */
@SuppressWarnings("serial")
public class AdvancedSupportHandler extends HttpServlet
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    private static final String ADVANCED_SUPPORT_HANDLER_SCRIPT = System.getProperty("uvm.conf.dir") + "/advanced-support-handler";

    /**
     * Perform HTTP GET operation
     *
     * @param request
     *        HTTP request
     * @param response
     *        HTTP response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        requestHandler("GET", request, response);
    }

    /**
     * Perform HTTP POST operation
     *
     * @param request
     *        HTTP request
     * @param response
     *        HTTP response
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        requestHandler("POST", request, response);
    }

    /**
     * Main request handler for GET and POST methods. Extracts the parameters
     * from the request and stores them in a JSONObject. The object is then
     * converted to a String and formatted with Base64 and then passed along
     * with the request method to the external handler script. The script output
     * should be a raw HTML page which is used as the response.
     *
     * @param method
     *        The request method (GET or POST)
     * @param request
     *        HTTP request
     * @param response
     *        HTTP response
     * @throws IOException
     */
    private void requestHandler(String method, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        File script = new File(ADVANCED_SUPPORT_HANDLER_SCRIPT);

        // first make sure the advanced support handler script exists
        if (!script.exists() || !script.canExecute()) {
            response.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
        }

        // put all of the request parameter names and values into a JSON object
        JSONObject object = new JSONObject();
        Enumeration<String> params = request.getParameterNames();
        while (params.hasMoreElements()) {
            String name = params.nextElement();
            try {
                object.put(name, request.getParameter(name));
            } catch (Exception exn) {
            }
        }

        // convert the JSON to a string in Base64 format that can be passed to the script
        String parmstr = Base64.getEncoder().encodeToString(object.toString().getBytes());

        // call the handler script with the method and parameters
        UvmContext uvmContext = UvmContextFactory.context();
        String output = uvmContext.execManager().execOutput(ADVANCED_SUPPORT_HANDLER_SCRIPT + " " + method + " " + parmstr);

        // write the script output as the response
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.print(output);
    }
}
