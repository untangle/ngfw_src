/**
 * $Id$
 */
package com.untangle.uvm.admin.servlet;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.util.SafeCheckValidationException;
import com.untangle.uvm.util.SafeCheckValidator;
import com.untangle.uvm.util.SafeType;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.untangle.uvm.util.Constants.COMMA;

/**
 * An extended servlet for uploading a base64 encoded file
 * Expects a 'file' parameter in form data
 */
@SuppressWarnings({"serial","unchecked"})
public class UploadServletExtended extends UploadServlet {
    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * Form field names that are part of the servlet dispatch protocol and
     * must be filtered out before the per-handler schema check. The "type"
     * field carries the upload type that selects the handler; without
     * filtering, every legitimate request would be strict-rejected because
     * no handler schema declares "type".
     */
    private static final Set<String> RESERVED_KEYS = Set.of("type");

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

            // Resolve handler once before iterating files; lookup is the
            // same for every file item in this request.
            UploadHandler handler = UvmContextFactory.context().servletFileManager().getUploadHandler(uploadType);
            if (handler == null) {
                handleNoUploadHandler(resp, uploadType);
                return;
            }

            // Filter dispatch metadata before the schema check (otherwise
            // every legitimate request would be rejected for carrying "type").
            Map<String, String> filteredArgs = new HashMap<>(args);
            filteredArgs.keySet().removeAll(RESERVED_KEYS);

            // Strict-rejection: every remaining key MUST be declared in the
            // handler's schema. Defensively coerce a null schema to empty
            // so a buggy handler fails closed (rejects everything) instead
            // of NPE.
            Map<String, SafeType[]> schema = handler.getArgumentTypes();
            if (schema == null) schema = Collections.emptyMap();
            for (Map.Entry<String, String> entry : filteredArgs.entrySet()) {
                String key = entry.getKey();
                SafeType[] types = schema.get(key);
                if (types == null) {
                    // Never log the value - it may be attacker-controlled
                    // and may carry credentials.
                    logger.warn("v2 upload arg rejected (unknown key): handler={} key={}",
                                handler.getClass().getName(), key);
                    createResponse(resp, false, "Unknown argument");
                    return;
                }
                try {
                    SafeCheckValidator.validate(entry.getValue(), types,
                        handler.getClass().getName() + ".args[" + key + "]");
                } catch (SafeCheckValidationException ex) {
                    logger.warn("v2 upload arg rejected (validation): handler={} key={} reason={}",
                                handler.getClass().getName(), key, ex.getMessage());
                    createResponse(resp, false, "Invalid argument");
                    return;
                }
            }

            for ( FileItem item : items ) {
                // Expects 'file' item containing the actual file contents
                if (item.getFieldName().equals("file")) {
                    byte[] decoded = new byte[0];
                    // Try to extract base64 encoded file contents
                    String value = item.getString(String.valueOf(StandardCharsets.UTF_8));
                    if (value.startsWith("data:")) {
                        String[] parts = value.split(COMMA);
                        if (parts.length > 1) {
                            decoded = Base64.getDecoder().decode(parts[1]);
                        }
                    }
                    result = handler.handleV2File(decoded, filteredArgs);
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
