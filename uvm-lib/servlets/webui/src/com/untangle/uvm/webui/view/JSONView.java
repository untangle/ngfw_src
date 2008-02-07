package com.untangle.uvm.webui.view;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.servlet.view.AbstractView;

/**
 * A View that renders its model as a JSON object.
 *
 * @author Catalin Matei <cmatei@untangle.com>
 */
public class JSONView extends AbstractView {

	   /** Default content type. Overridable as bean property. */
	   private static final String DEFAULT_JSON_CONTENT_TYPE = "application/json";
	   
	   public JSONView() {
	      super();
	      setContentType( DEFAULT_JSON_CONTENT_TYPE );
	   }

	   /**
	    * Creates a JSONObject [JSONObject,JSONArray,JSONNUll] from the model values.
	    */
	   protected JSONObject createJSON( Map model, HttpServletRequest request, HttpServletResponse response )throws JSONException {
	      return defaultCreateJSON( model );
	   }

	   /**
	    * Creates a JSONObject [JSONObject,JSONArray,JSONNUll] from the model values.
	    */
	   protected final JSONObject defaultCreateJSON( Map model ) {
	      return new JSONObject(model);
	   }

	   protected void renderMergedOutputModel( Map model, HttpServletRequest request,
	         HttpServletResponse response ) throws Exception {
	      response.setContentType( getContentType() );
	      JSONObject json = createJSON( model, request, response );
	      
	      json.write( response.getWriter() );
	   }
}