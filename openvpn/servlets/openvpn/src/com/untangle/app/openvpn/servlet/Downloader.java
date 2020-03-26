/**
 * $Id$
 */

package com.untangle.app.openvpn.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

/**
 * Implementation of the OpenVPN client download handler servlet
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class Downloader extends HttpServlet
{
    private static final String CONFIG_PAGE = "/config.zip";
    private static final String CONFIG_NAME_PREFIX = "config-";
    private static final String CONFIG_NAME_SUFFIX = ".zip";
    private static final String CONFIG_TYPE = "application/zip";

    private static final String CHROME_PAGE = "/chrome.onc";
    private static final String CHROME_NAME_PREFIX = "chrome-";
    private static final String CHROME_NAME_SUFFIX = ".onc";
    private static final String CHROME_TYPE = "application/download";

    private static final String INLINE_PAGE = "/inline.ovpn";
    private static final String INLINE_NAME_PREFIX = "inline-";
    private static final String INLINE_NAME_SUFFIX = ".ovpn";
    private static final String INLINE_TYPE = "application/download";

    private final Logger logger = Logger.getLogger(this.getClass());

    /**
     * The main service handler for client download requests
     * 
     * @param request
     *        The web request
     * @param response
     *        The web response
     * @throws ServletException
     * @throws IOException
     */
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String commonName = request.getParameter("client");
        String fileName = null;
        String downloadFilename = null;
        String pageName = request.getServletPath();
        String type = "";

        if (pageName.equalsIgnoreCase(CONFIG_PAGE)) {
            fileName = "/tmp/openvpn/client-packages/" + "config-" + commonName + ".zip";
            downloadFilename = "openvpn-" + commonName + "-config.zip";
            type = CONFIG_TYPE;
        } else if (pageName.equalsIgnoreCase(CHROME_PAGE)) {
            fileName = "/tmp/openvpn/client-packages/" + "chrome-" + commonName + ".onc";
            downloadFilename = "openvpn-" + commonName + "-chrome.onc";
            type = CHROME_TYPE;
        } else if (pageName.equalsIgnoreCase(INLINE_PAGE)) {
            fileName = "/tmp/openvpn/client-packages/" + "inline-" + commonName + ".ovpn";
            downloadFilename = "openvpn-" + commonName + "-inline.ovpn";
            type = INLINE_TYPE;
        } else {
            fileName = null;
            downloadFilename = null;
        }

        /*
         * File name shouldn't be null unless the web.xml is misconfigured to
         * force pages that are not supposed to reach here
         */
        if ((commonName == null) || (fileName == null) || (downloadFilename == null)) {
            request.setAttribute("com.untangle.app.openvpn.servlet.reason", "downloadFilename or fileName is null [" + pageName + "]");
            rejectFile(request, response);
        } else {
            streamFile(request, response, fileName, downloadFilename, type);
        }
    }

    /**
     * Streams a file to a user
     * 
     * @param request
     *        The web request
     * @param response
     *        The web response
     * @param fileName
     *        Full path of the file to download
     * @param downloadFileName
     *        Name that should be given to the file that is downloaded
     * @param type
     *        The content type
     * @throws ServletException
     * @throws IOException
     */
    void streamFile(HttpServletRequest request, HttpServletResponse response, String fileName, String downloadFileName, String type) throws ServletException, IOException
    {
        InputStream fileData = null;
        File fileItem = null;
        long length = 0;

        logger.debug("Streaming '" + fileName + "'");

        fileItem = new File(fileName);

        try {
            fileData = new FileInputStream(fileItem);
        } catch (FileNotFoundException e) {
            logger.info("The file '" + fileName + "' does not exist");
            request.setAttribute("com.untangle.app.openvpn.servlet.reason", "The file '" + fileName + "' does not exist");
            rejectFile(request, response);
            return;
        }

        length = fileItem.length();
        response.setContentType(type);

        if (downloadFileName != null) {
            response.setHeader("Content-Disposition", "attachment; filename=\"" + downloadFileName + "\"");
        }
        if (length > 0) {
            response.setHeader("Content-Length", "" + length);
        }

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        OutputStream out = null;

        try {
            out = response.getOutputStream();
            bis = new BufferedInputStream(fileData);
            bos = new BufferedOutputStream(out);
            byte[] buff = new byte[2048];
            int bytesRead;
            while (-1 != (bytesRead = bis.read(buff, 0, buff.length)))
                bos.write(buff, 0, bytesRead);
        } catch (Exception e) {
            logger.warn("Error streaming file.", e);
        } finally {
            try {
                if (bis != null) bis.close();
            } catch (Exception e) {
                logger.warn("Error closing input stream", e);
            }

            try {
                if (bos != null) bos.close();
            } catch (Exception e) {
                logger.warn("Error closing output stream", e);
            }

            try {
                if (out != null) out.close();
            } catch (Exception e) {
                logger.warn("Error closing output stream", e);
            }
        }

        // normally not needed because bis.close() will handle it but
        // it doesn't hurt and is fallback in case of exception above
        fileData.close();
    }

    /**
     * Called to reject a download request
     * 
     * @param request
     *        The web request
     * @param response
     *        The web response
     * @throws ServletException
     * @throws IOException
     */
    void rejectFile(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        request.setAttribute("com.untangle.app.openvpn.servlet.debugging", "");
        request.setAttribute("com.untangle.app.openvpn.servlet.valid", false);

        /* Indicate that the response was not rejected */
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        request.getRequestDispatcher("/Index.jsp").forward(request, response);
    }
}
