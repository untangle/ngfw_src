package com.untangle.uvm.webui.view;

import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.view.AbstractView;

/**
 * A View that renders an image
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
public class ImageView extends AbstractView {

	   /** Default content type. Overridable as bean property. */
	   private static final String DEFAULT_IMAGE_CONTENT_TYPE = "image/png";
	   
	   public ImageView() {
	      super();
	      setContentType( DEFAULT_IMAGE_CONTENT_TYPE );
	   }

	    protected byte[] getData(Map model, HttpServletRequest request) {
	        return (byte[]) model.get("imageData");
	    }
	    
	   protected void renderMergedOutputModel( Map model, HttpServletRequest request,
	         HttpServletResponse response ) throws Exception {
		   
	        byte[] bytes = getData(model, request);
	        
	        // Write content type and also length (determined via byte array).
	        response.setContentType(getContentType());
	        response.setContentLength(bytes.length);

	        // Flush byte array to servlet output stream.
	        ServletOutputStream out = response.getOutputStream();
	        out.write(bytes);
	        out.flush();
	   }
}
