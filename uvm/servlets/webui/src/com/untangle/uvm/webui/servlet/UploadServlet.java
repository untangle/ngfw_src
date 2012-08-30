/**
 * $Id: UploadServlet.java,v 1.00 2012/08/30 12:21:52 dmorris Exp $
 */
package com.untangle.uvm.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.servlet.UploadManager;

/**
 * A servlet for uploading a skin
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
@SuppressWarnings({"serial","unchecked"})
public class UploadServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List<FileItem> items = null;
        String msg=null;
        try {
            items = upload.parseRequest(req);

            String uploadType = getUploadType(items);

            // Process the uploaded items
            UploadManager uploadManager = UvmContextFactory.context().uploadManager();

            for ( FileItem item : items ) {
                if (!item.isFormField()) {
                    UploadHandler handler = uploadManager.getUploadHandler(uploadType);
                    if ( handler == null ) {
                        msg = "Do not know how to handler the type '" + uploadType + "'";
                        logger.info("Unable to handle an upload of type: " + uploadType );
                    } else {
                        msg = handler.handleFile(item);
                    }                    
                }
            }
        } catch (Exception exn) {
            logger.warn("could not upload", exn);
            createResponse(resp, false, exn.getMessage());
            return;
        }
        createResponse(resp, true, msg);
    }

    private String getUploadType(List<FileItem> items)
    {
        for ( FileItem fileItem : items ) {
            if (fileItem.isFormField() && "type".equals(fileItem.getFieldName())) {
                return fileItem.getString();
            }
        }
        return null;
    }

    private void createResponse(HttpServletResponse resp, boolean success,
                               String msg)
        throws IOException
    {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        try {
            JSONObject obj=new JSONObject();
            obj.put("success",new Boolean(success));
            if (msg != null){
                obj.put("msg",msg);
            }
            out.print(obj);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        out.flush();
        out.close();
    }
}
