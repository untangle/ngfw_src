/**
 * $Id: UvmStatusHandler.java $
 */
package com.untangle.uvm.health;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmState;

/**
 * Minimal local UVM status endpoint used by keepalived.
 *
 * HTTP 200 means the UVM request path is responsive and the UVM lifecycle
 * state is RUNNING. All other responses are unhealthy.
 */
@SuppressWarnings("serial")
public class UvmStatusHandler extends HttpServlet
{
    private static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    /**
     * Perform HTTP GET operation
     * @param request HTTP request
     * @param response HTTP response
     * @throws ServletException Servlet exception
     * @throws IOException IO exception
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        UvmState state;
        try {
            // Do not call context(); the status check must not initialize UVM
            // or perform any other application work.
            state = UvmContextFactory.state();
        } catch (Throwable exn) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        boolean healthy = (state == UvmState.RUNNING);
        response.setStatus(healthy ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("Cache-Control", "no-store");

        PrintWriter writer = response.getWriter();
        writer.print("{\"state\":\"");
        writer.print(state == null ? "unknown" : state.toString());
        writer.print("\",\"healthy\":");
        writer.print(healthy ? "true" : "false");
        writer.println("}");
    }
}
