/**
 * $Id: ChicletServlet.java,v 1.00 2012/06/11 14:54:05 dmorris Exp $
 */
package com.untangle.uvm.servlet;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;


/**
 * A servlet that renders an image
 */
@SuppressWarnings("serial")
public class ChicletServlet extends HttpServlet
{
    /** image content type */
    private static final String IMAGE_CONTENT_TYPE = "image/png";

    private final Logger logger = Logger.getLogger( this.getClass());
    
    // This is the 'atom' icon that we use for all untangle-* packages that don't have an icon.
    private static final String unknownIconString = "iVBORw0KGgoAAAANSUhEUgAAACoAAAAqCAIAAABKoV4MAAAABGdBTUEAANbY1E9YMgAAABl0RVh0U29mdHdhcmUAQWRvYmUgSW1hZ2VSZWFkeXHJZTwAAATkSURBVHja1JhrKG1bFMftg1semyghESVKSClSSilSinyQV5S8SoqUV5RIKSJFREJuiigRpZQSUconIZ9IKVIOpWzv+zt7nrvuPGuvxd77nu12x6e15lxrjjn+4/8fY65liI6ODg4O9vf39/HxcXd3d3FxcXKMOTs7/2E2Ll5fX+vr6xk0eHt7Ly8vM+rq6sqEwWBwcph9+9ve39/ZQWxsrMvd3Z3JZKqtrQ0MDPTy8mIHjnBMVODq5ubm6elJqG9vb/n5+Yz/gPrl5eXs7Ozp6cnR7j08PIxGIynm9vn5+ad77Pb29vHxEfcOzT2hwzABADH/4x57eHhgB9a7j4iIaG1tBbOmpqabm5tPn2dlXJByXiELXPziHiMlzFnjmwjq6uoqKiqCgoJqamra2to+fQV/sJsIcQESsO8HGe1DsqGhobOz8/7+/uTkZHV1NS8vz/p33832Uwt2+CZzyPX8/Fzc7u7upqSk2ClFO97JyMhYX1+XR9hKeHj4F7nPzMzc2NiQR8CfwS9yD/JwWB45ODiIi4uzYyltmYEkouICcokSDdpra2uQFt8MaspaEUVaWlpCQgKPUVIDAgKQWW9v7/X1tbXuJycnc3NzLy8vFa6xYl9fHxekmVgtX8FZWVlZfHw8LtloS0uLjNbo6GhRUZG17i/NptwS9KrZCLGrq4s0s+LQ0JCoNsRH+yLc7e3t6upqy9XuzGYD+EdHR5GRkWjasnSQ9dLSUl9f34GBARYV9au/v39/f19FCMWQJbM2UI9UdXR0aLafkJCQi4sLUoAz0gwMYABUDDKlWR8LCwvHxsZsiJ5EEtDg4KAlmIRLlU1KSqLQChJAi6Wlpc3NTcuCzcMsQn20WXh7e3sLCwvDw8MyBmQEgZ2enmZnZysERBTp6emkgHLk5+cn+x4fHycMnv9IfHBKb4q0zczMACDXlZWVkH9lZUXhc09PT3NzM3IQt0zNzs5mZWVxSyuan5+PiYmRlckzoaGhHHISExO3trZ0wVcMSEkqOyAdc3NzpJBrMYUEyIIg9sjIiLhAXWyIHQBDVVWVLB/7q96r2VRMVG5F55YfVj1gc9WTwQfzkpISoueC6oHkxBTUg/zUsqmpKSUdgA9ICAcVwBuKDxr+ZAt6uU9OTsafHERYWNjh4aHIrgoJivTOzg4lSKYemVI6oWbudcGHNcXFxQhPxhYOE01UVBS0Utal3kE6liYMOdlgU15ezrZkOVgFPmTmNTBX5VUhGs6gGLOIAm7COCoB2KgeZgecybq7uzWLsW70LE2t0Dz3ER8ICymKniY+XBhUzj+q7S4uLubk5NjWcDUpgydRyOhpjY2NovOKlkPN0aus1CUqBN9S1roHRjwp0RMcq9PoGARqqqHsCTxguDgHTE9PowU8UTDksq+Xfm33JIyFBLaEywjhMsiGEB4FRxOYP82GJ/bKiUFUCzaHCNvb221wT38U32CWRn8jUL2jDkb0Yh8fPPOvznpAoloLKep19N9/1ORUg8xUZ98P+tZvdm/5WQNVNVXnEPeADytpqeI2NTVV5rnD3YvTGFJGFIie2kybsf7rTv7Gs/NrHv4jv4mJCcoq9ceyNmt2bXSr+sK1/2fC8fFxQUGBTa+YTKarqyt2YDQa1b8XvsYI+rvZxM+Vb07/kf0SvX2q/d/bXwIMAJ+e3tKQEpgPAAAAAElFTkSuQmCC";
    private static byte[] unknownIcon = null;
    static {
        try {
            unknownIcon = Base64.decodeBase64(unknownIconString.getBytes());
        } catch (Exception x) {
            
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        byte[] bytes = getImageData(req);
        if (null == bytes) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // Write content type and also length (determined via byte array).
        resp.setContentType(IMAGE_CONTENT_TYPE);
        resp.setContentLength(bytes.length);

        // Flush byte array to servlet output stream.
        ServletOutputStream out = resp.getOutputStream();
        out.write(bytes);
        out.flush();
    }

    protected byte[] getImageData(HttpServletRequest request)
    {
        String name = request.getParameter("name");

        String fileName = System.getProperty("uvm.home") + "/web/library/images/" + name + ".png";
        File iconFile = new File(fileName);
        InputStream is = null;
        byte[] result = null;
        
        /**
         * Look on the local filesystem
         */
        if (iconFile.exists()) {
            try {
                is = new FileInputStream(iconFile);
                int length = (int)iconFile.length();
                result = new byte[length];

                // Read in the bytes
                int offset = 0;
                int numRead = 0;
                while (offset < result.length && (numRead=is.read(result, offset, result.length-offset)) >= 0) {
                    offset += numRead;
                }
    
                is.close();

                if (offset < result.length) {
                    throw new IOException("Could not completely read file: ");
                }
            } catch (IOException e) {
                result = null;
                logger.warn("Exception geting Image data",e);
            } 
        }

        /**
         * Look in the apt cache
         */
        if (result == null) {
            result = UvmContextFactory.context().aptManager().packageDesc(name).descIcon();
        }

        /**
         * If still not found just use default generic icon
         */
        if (result == null) {
            result = unknownIcon;
        }
        
        return result;
    }
}
