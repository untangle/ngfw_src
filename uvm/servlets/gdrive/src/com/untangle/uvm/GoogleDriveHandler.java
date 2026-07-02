/**
 * $Id: GoogleDriveHandler.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.uvm;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.UvmContextFactory;

/**
 * GoogleDrive http servelet class
 */
@SuppressWarnings("serial")
public class GoogleDriveHandler extends HttpServlet
{
    /**
     * Shape of a valid OAuth state nonce. NonceFactory produces nonces via
     * {@code Long.toHexString(random.nextLong())} -- 1-16 lowercase hex chars.
     * Anything outside this pattern cannot be one of our nonces, so reject
     * upfront before touching the synchronized nonce map (early DoS bound).
     */
    private static final Pattern NONCE_PATTERN = Pattern.compile("^[0-9a-f]{1,16}$");

    private final Logger logger = LogManager.getLogger( this.getClass() );

    /**
     * Perform HTTP GET operation
     *
     * @param request HTTP request
     * @param response HTTP response
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet( HttpServletRequest request,  HttpServletResponse response ) throws ServletException, IOException
    {
        // nonce is delivered as the trailing path segment of /gdrive/gdrive/<nonce>
        String pathInfo = request.getPathInfo();
        String nonce = (pathInfo != null && pathInfo.startsWith("/")) ? pathInfo.substring(1) : null;
        String code = request.getParameter("code");

        if (nonce == null || !NONCE_PATTERN.matcher(nonce).matches()) {
            logger.warn("gdrive callback rejected: missing/malformed nonce, remoteAddr={}", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (code == null || code.isEmpty()) {
            logger.warn("gdrive callback rejected: missing code argument, remoteAddr={}", request.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        UvmContext uvmContext = UvmContextFactory.context();
        String error = uvmContext.googleManager().provideDriveCode(nonce, code);

        // Any non-null error means the token exchange did NOT happen (bad nonce,
        // failed Google exchange, etc.). Reflect that in the HTTP status so
        // callers/tests can distinguish success from failure without parsing
        // the HTML body. Body still renders the message for admin visibility.
        if (error != null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Google Drive Configuration</title>");

        if ( error == null ) {
            writer.println("<script type=\"text/javascript\">");
            writer.println("window.onload = function(){ close() };");
            writer.println("</script>");
        }
        
        writer.println("</head>");
        writer.println("<body bgcolor=white>");

        if ( error == null ) {
            writer.println("Google Drive Configuration successful!");
        } else {
            writer.println("Google Drive Configuration Failed: " + htmlEscape(error));
        }

        writer.println("</body>");
        writer.println("</html>");

        return;
    }

    /**
     * Escape a string for safe inclusion in an HTML element body or attribute.
     * Bootstrap's StringEscaperUtil is not on the servlet classpath, so this
     * minimal inline escape is used for the single error-message sink below.
     *
     * @param s the string to escape; may be null
     * @return the escaped string, or empty string if {@code s} is null
     */
    private static String htmlEscape(String s)
    {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
