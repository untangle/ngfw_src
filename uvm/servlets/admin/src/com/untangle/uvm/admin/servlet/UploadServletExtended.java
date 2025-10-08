/**
 * $Id$
 */
package com.untangle.uvm.admin.servlet;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.servlet.UploadHandler;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.untangle.uvm.util.Constants.COMMA;

/**
 * An extended servlet for uploading a base64 encoded file
 * Expects a 'file' parameter in form data
 */
public class UploadServletExtended extends UploadServlet {
    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * doPost - handle POST
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        ServletFileUpload upload = getServletFileUpload();

        // Parse the request
        List<FileItem> items = null;
        Object result=null;
        try {
            items = upload.parseRequest(req);

            String uploadType = getUploadType(items);
            Map<String, String> args = getArgumentsMap(items);

            logger.info("Handling Upload: {}", uploadType);

            for ( FileItem item : items ) {
                // Expects 'file' item containing the actual file contents
                if (item.getFieldName().equals("file")) {
                    UploadHandler handler = UvmContextFactory.context().servletFileManager().getUploadHandler(uploadType);
                    if ( handler == null ) {
                        handleNoUploadHandler(resp, uploadType);
                        return;
                    } else {
                        byte[] decoded = new byte[0];
                        // Try to extract base64 encoded file contents
                        String value = item.getString(String.valueOf(StandardCharsets.UTF_8));
                        if (value.startsWith("data:")) {
                            String[] parts = value.split(COMMA);
                            if (parts.length > 1) {
                                decoded = Base64.getDecoder().decode(parts[1]);
                            }
                        }
                        result = handler.handleV2File(decoded, args);
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
     * getArgumentsMap - returns extra arguments specified in the upload form
     * This map is forwarded to upload handlers
     * @param items
     * @return string argument or null
     */
    private Map<String, String> getArgumentsMap(List<FileItem> items)
    {
        Map<String, String> args = new HashMap<>();
        for ( FileItem fileItem : items ) {
            // skip the file item
            if (!fileItem.getFieldName().equals("file"))
                args.put(fileItem.getFieldName(), fileItem.getString());
        }
        return args;
    }
}
