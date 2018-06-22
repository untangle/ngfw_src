/**
 * $Id$
 */
package com.untangle.uvm.admin.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;

/**
 * A servlet for import / export grid settings 
 */
@SuppressWarnings({ "serial", "unchecked" })
public class GridSettingsServlet extends HttpServlet
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd_HH-mm-ss";
    
    private static final String CHARACTER_ENCODING = "utf-8";

    /**
     * doPost - handle POST 
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        boolean isExport = "export".equals(req.getParameter("type"));
        if (isExport) {
            processExport(req, resp);
        } else {
            processImport(req, resp);
        }

    }

    /**
     * processImport - process an import
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private void processImport(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {

        // Create a factory for disk-based file items
        DiskFileItemFactory factory = new DiskFileItemFactory();
        // Create a new file upload handler
        ServletFileUpload upload = new ServletFileUpload(factory);

        // Parse the request
        List<FileItem> items = null;
        JSONArray gridSettings = null;
        try {
            items = upload.parseRequest(req);
            for (FileItem item : items) {
                if (!item.isFormField()) {
                    String fileString = item.getString();
                    if (fileString != null && fileString.trim().length() > 0) {
                        gridSettings = new JSONArray(fileString.trim());
                        createImportRespose(resp, true, gridSettings);
                        return;
                    } else {
                        createImportRespose(resp, false, importFailedMessage());
                        return;
                    }
                }
            }
        } catch (JSONException e) {
            logger.debug("Import grid settings failed. Settings must be formatted as a JSON Array.", e);
            createImportRespose(resp, false, importFailedMessage());
            return;
        } catch (Exception exn) {
            logger.warn("could not upload", exn);
            createImportRespose(resp, false, exn.getMessage());
            return;
        }

    }
    
    /**
     * createImportRespose - create a response to import
     * @param resp
     * @param success
     * @param msg
     * @throws IOException
     */
    private void createImportRespose(HttpServletResponse resp, boolean success, Object msg) throws IOException
    {
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        try {
            JSONObject obj = new JSONObject();
            obj.put("success", new Boolean(success));
            if (msg != null) {
                obj.put("msg", msg);
            }
            out.print(encodeHtml(obj.toString()));
        } catch (JSONException e) {
            logger.warn( "Import failed.", e );
        }
        out.flush();
        out.close();
    }
    
    /**
     * importFailedMessage - craft an import failed message
     * @return message
     */
    private String importFailedMessage()
    {
        UvmContext uvm = UvmContextFactory.context();
        Map<String,String> i18n_map = uvm.languageManager().getTranslations("untangle");
        return I18nUtil.tr("Import failed. Settings must be formatted as a JSON Array.", i18n_map);
    }
    
    /**
     * processExport process an export
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    private void processExport(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        String gridData = req.getParameter("gridData");
        String oemName = UvmContextFactory.context().oemManager().getOemName();
        String version = UvmContextFactory.context().version().replace(".","_");
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName().replace(".","_");
        String dateStr = (new SimpleDateFormat(DATE_FORMAT_NOW)).format((Calendar.getInstance()).getTime());
        String gridName = req.getParameter("gridName");

        String filename = oemName + "-" + version + "-" + gridName + "-" + hostName + "-" + dateStr + ".json";

        // Write content type and also length (determined via byte array).
        resp.setCharacterEncoding(CHARACTER_ENCODING);
        resp.setHeader("Content-Disposition","attachment; filename="+filename);

        try {
            JSONArray json = new JSONArray(gridData);
            json.write(resp.getWriter());
        } catch (JSONException e) {
            logger.debug("Export grid settings failed. Settings must be formatted as a JSON Array.", e);
            throw new ServletException("Export failed. Settings must be formatted as a JSON Array.");
        }
    }

    /**
     * encodeHtml - encoding text to an HTML
     * @param aText
     * @return HTML string
     */
    private static String encodeHtml(String aText)
    {
        final StringBuilder result = new StringBuilder();
        final StringCharacterIterator iterator = new StringCharacterIterator(aText);
        char character =  iterator.current();
        while (character != CharacterIterator.DONE ){
            if (character == '<') {
                result.append("&lt;");
            }
            else if (character == '>') {
                result.append("&gt;");
            }
            else if (character == '&') {
                result.append("&amp;");
            }
            else {
                //the char is not a special one
                //add it to the result as is
                result.append(character);
            }
            character = iterator.next();
        }
        return result.toString();
    }
    
}
