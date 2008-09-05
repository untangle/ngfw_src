package com.untangle.uvm.webui.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.RemoteBrandingManager;
import com.untangle.uvm.RemoteLanguageManager;
import com.untangle.uvm.RemoteSkinManager;
import com.untangle.uvm.client.RemoteUvmContext;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A servlet for uploading a skin
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
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
            RemoteUvmContext uvm = LocalUvmContextFactory.context().remoteContext();

            Iterator iter = items.iterator();
            while (iter.hasNext()) {
                FileItem item = (FileItem) iter.next();

                if (!item.isFormField()) {
                    if ("skin".equals(uploadType)) {
                        RemoteSkinManager skinManager = uvm.skinManager();
                        skinManager.uploadSkin(item);
                    } else if ("language".equals(uploadType)) {
                        RemoteLanguageManager languageManager = uvm.languageManager();
                        languageManager.uploadLanguagePack(item);
                    } else if ("logo".equals(uploadType)) {
                        byte[] logo=item.get();
                        RemoteBrandingManager brandingManager = uvm.brandingManager();
                        brandingManager.setLogo(logo);
                    } else if ("restore".equals(uploadType)) {
                        byte[] backupFileBytes=item.get();
                        uvm.restoreBackup(backupFileBytes);

                    }
                }
            }
        } catch (Exception exn) {
            logger.warn("could not upload", exn);
            createRespose(resp, false, exn.getMessage());
            return;
        }
        createRespose(resp, true, msg);
    }

    private String getUploadType(List<FileItem> items)
    {
        for (Iterator iterator = items.iterator(); iterator.hasNext();) {
            FileItem fileItem = (FileItem) iterator.next();
            if (fileItem.isFormField() && "type".equals(fileItem.getFieldName())) {
                return fileItem.getString();
            }
        }
        return null;
    }

    private void createRespose(HttpServletResponse resp, boolean success,
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
