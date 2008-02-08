package com.untangle.uvm.webui.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.client.RemoteUvmContext;

/**
 * A servlet that renders an image
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
public class ImageServlet extends HttpServlet {
   /** image content type */
   private static final String IMAGE_CONTENT_TYPE = "image/png";

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		
        byte[] bytes = getImageData(req);
        
        // Write content type and also length (determined via byte array).
        resp.setContentType(IMAGE_CONTENT_TYPE);
        resp.setContentLength(bytes.length);

        // Flush byte array to servlet output stream.
        ServletOutputStream out = resp.getOutputStream();
        out.write(bytes);
        out.flush();
	}
	
    protected byte[] getImageData(HttpServletRequest request) {
		String name = request.getParameter("name");
		
		// TODO cache uvm in session
        RemoteUvmContext uvm = LocalUvmContextFactory.context().remoteContext();
        
        return uvm.toolboxManager().mackageDesc(name).descIcon();
    }
	
}
