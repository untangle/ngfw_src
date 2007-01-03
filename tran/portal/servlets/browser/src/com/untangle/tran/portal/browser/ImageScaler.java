/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.portal.browser;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;
import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.untangle.mvvm.portal.PortalLogin;
import jcifs.smb.SmbFile;
import org.apache.log4j.Logger;


public class ImageScaler extends HttpServlet
{
    private static final String MIME_TYPES_PATH = "/etc/mime.types";
    private static final String OUTPUT_CONTENT_TYPE = "image/png";

    private static final int MAX_HEIGHT = 150;
    private static final int MAX_WIDTH = 150;

    private Logger logger;
    private MimetypesFileTypeMap mimeMap;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException
    {
        PortalLogin pl = (PortalLogin)req.getUserPrincipal();

        String url = req.getParameter("url");
        try {
            BufferedImage bi = readImage(url, pl);
            bi = scaleImage(bi);
            writeImage(bi, resp);
        } catch (MalformedURLException exn) {
            logger.warn("bad url: " + url, exn);
            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } catch (IllegalArgumentException exn) {
            logger.warn("could not read: " + url, exn);
            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        } catch (IOException exn) {
            logger.warn("could not scale image: " + url, exn);
            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        }
    }

    @Override
    public void init() throws ServletException
    {
        logger = Logger.getLogger(getClass());

        try {
            mimeMap = new MimetypesFileTypeMap(MIME_TYPES_PATH);
        } catch (IOException exn) {
            logger.error("could not setup mimemap", exn);
            mimeMap = new MimetypesFileTypeMap();
        }
    }

    // private methods --------------------------------------------------------

    private BufferedImage readImage(String url, PortalLogin pl)
        throws MalformedURLException, IOException
    {
        SmbFile f = Util.getSmbFile(url, pl);
        String contentType = mimeMap.getContentType(f.getName());
        Iterator<ImageReader> i = ImageIO.getImageReadersByMIMEType(contentType);
        if (!i.hasNext()) {
            throw new IllegalArgumentException("cannot write: " + contentType);
        }

        ImageReader ir = i.next();
        ImageInputStream iis = ImageIO.createImageInputStream(f.getInputStream());
        ir.setInput(iis);

        return ir.read(0);
    }

    private BufferedImage scaleImage(BufferedImage bi)
    {
        int h = bi.getHeight();
        int w = bi.getWidth();

        if (MAX_HEIGHT > h && MAX_WIDTH > w) {
            return bi;
        } else {
            int dw = w - MAX_WIDTH;
            int dh = h - MAX_HEIGHT;

            double scale = dw > dh ? MAX_WIDTH / (double)w
                : MAX_HEIGHT / (double)h;

            AffineTransform tx = new AffineTransform();
            tx.scale(scale, scale);
            return new AffineTransformOp(tx, null).filter(bi, null);
        }
    }

    private void writeImage(BufferedImage bi, HttpServletResponse resp)
        throws IOException
    {
        Iterator<ImageWriter> i = ImageIO.getImageWritersByMIMEType(OUTPUT_CONTENT_TYPE);
        if (!i.hasNext()) {
            logger.error("No ImageWriter for: " + OUTPUT_CONTENT_TYPE);
            return;
        }
        ImageWriter iw = i.next();

        ImageOutputStream ios = ImageIO.createImageOutputStream(resp.getOutputStream());

        resp.setContentType(OUTPUT_CONTENT_TYPE);

        iw.setOutput(ios);

        iw.write(bi);
    }
}
