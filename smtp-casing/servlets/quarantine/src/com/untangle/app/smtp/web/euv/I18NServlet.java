/**
 *  $Id:$
 */
package com.untangle.app.smtp.web.euv;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContext;

/**
 * A servlet that when given a module name returns a javascript hash containing
 * all the key-value pairs for localization.
 * 
 */
@SuppressWarnings("serial")
public class I18NServlet extends HttpServlet
{
    /** json content type */
    private static final String JSON_CONTENT_TYPE = "application/json";
    /** character encoding */
    private static final String CHARACTER_ENCODING = "utf-8";

    /**
     * Handle the request for the desired language.
     * 
     * @param  req              HttpServletRequest object containing the "module" request.
     * @param  resp             HttpServletResponse object that will be written to with the requested localization.
     * @throws ServletException If there's an problem with the servlet.
     * @throws IOException      General input/ooutput error.
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        UvmContext uvm = UvmContextFactory.context();
        LanguageManager languageManager = uvm.languageManager();

        String module = req.getParameter("module");

        Map<String, String> map = languageManager.getTranslations(module);

        // Write content type and also length (determined via byte array).
        resp.setContentType(JSON_CONTENT_TYPE);
        resp.setCharacterEncoding(CHARACTER_ENCODING);

        try {
            JSONObject json = createJSON(map);
            json.write(resp.getWriter());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Creates a JSONObject [JSONObject,JSONArray,JSONNUll] from the map values.
     * @param  map           String (english) to String (target language) mapping.
     * @return               JSONObject of the mapping
     * @throws JSONException Any problem creating the json object.
     */
    protected JSONObject createJSON(Map<String,String> map) throws JSONException 
    {
        return new JSONObject(map);
    }

}