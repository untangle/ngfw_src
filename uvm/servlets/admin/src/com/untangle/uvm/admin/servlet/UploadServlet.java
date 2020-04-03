/**
 * $Id$
 */
package com.untangle.uvm.admin.servlet;

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
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.servlet.UploadHandler;

/**
 * A servlet for uploading a file
 */
@SuppressWarnings({"serial","unchecked"})
public class UploadServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * doPost - handle POST
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException
    {

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();

        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List<FileItem> items = null;
        Object result=null;
        try {
            items = upload.parseRequest(req);

            String uploadType = getUploadType(items);
            String arg = getArgument(items);

            logger.info("Handling Upload: " + uploadType + " (" + arg + ")");
            
            for ( FileItem item : items ) {
                if (!item.isFormField()) {
                    UploadHandler handler = UvmContextFactory.context().servletFileManager().getUploadHandler(uploadType);
                    if ( handler == null ) {
                        result = "Do not know how to handler the type '" + uploadType + "'";
                        logger.error("Unable to handle an upload of type: " + uploadType );
                        createResponse(resp, false, "Unable to handle an upload of type: " + uploadType);
                    } else {
                        result = handler.handleFile(item, arg);
                    }                    
                }
            }
        } catch (Exception exn) {
            logger.warn("could not upload", exn);
            createResponse(resp, false, exn.getMessage());
            return;
        }
        createResponse(resp, true, result);
    }

    /**
     * getUploadType - returns the "type" of the upload
     * This is used to determine which upload handler we send the upload to
     * @param items
     * @return string 
     */
    private String getUploadType(List<FileItem> items)
    {
        for ( FileItem fileItem : items ) {
            if (fileItem.isFormField() && "type".equals(fileItem.getFieldName())) {
                return fileItem.getString();
            }
        }
        return null;
    }

    /**
     * getArgument - returns any "argument" specified in the upload form
     * @param items
     * @return string argument or null
     */
    private String getArgument(List<FileItem> items)
    {
        for ( FileItem fileItem : items ) {
            if (fileItem.isFormField() && "argument".equals(fileItem.getFieldName())) {
                return fileItem.getString();
            }
        }
        return null;
    }
    
    /**
     * createResponse
     * @param resp
     * @param success
     * @param result
     * @throws IOException
     */
    private void createResponse(HttpServletResponse resp, boolean success, Object result)
        throws IOException
    {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        try {
            JSONObject obj=new JSONObject();

            if ( result != null && result instanceof ExecManagerResult ) {
                // obj.put("success",new Boolean(((ExecManagerResult)result).getResult() == 0));
                obj.put("success",((ExecManagerResult)result).getResult() == 0);
                obj.put("msg",((ExecManagerResult)result).getOutput());
            }
            else {
                obj.put("success",success);
                if (result != null){
                    obj.put("msg",result);
                }
            }
            
            out.print(obj);
        } catch (JSONException e) {
            logger.warn("Error generating response: ", e);
        }
        out.flush();
        out.close();
    }
}
