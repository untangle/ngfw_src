/**
 * $Id$
 */
package com.untangle.uvm.jsp;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Map;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;

/**
 * I18nTag
 */
@SuppressWarnings("serial")
public class I18nTag extends BodyTagSupport
{
    public String p[] = new String[4];
    
    /**
     * getP0
     * @return
     */
    public String getP0()
    {
        return this.p[0];
    }

    /**
     * setP0
     * @param v
     */
    public void setP0(String v)
    {
        this.p[0] = v;
    }

    /**
     * getP1
     * @return
     */
    public String getP1()
    {
        return this.p[1];
    }

    /**
     * setP1
     * @param v
     */
    public void setP1(String v)
    {
        this.p[1] = v;
    }

    /**
     * getP2
     * @return
     */
    public String getP2()
    {
        return this.p[2];
    }

    /**
     * setP2
     * @param v
     */
    public void setP2(String v)
    {
        this.p[2] = v;
    }

    /**
     * getP3
     * @return
     */
    public String getP3()
    {
        return this.p[3];
    }

    /**
     * setP3
     * @param v
     */
    public void setP3(String v)
    {
        this.p[3] = v;
    }
    
    /**
     * doStartTag
     * @throws JspException
     * @return int
     */
    public final int doStartTag() throws JspException
    {
        return EVAL_BODY_BUFFERED;
    }

    /**
     * doEndTag
     * @throws JspException
     * @return
     */
    @SuppressWarnings("unchecked") //getAttribute
    public final int doEndTag() throws JspException
    {
        BodyContent body = getBodyContent();
        String bodyString = null;

        if ( body != null ) bodyString = body.getString().trim();

        JspWriter out = getPreviousOut();
                
        if ( bodyString == null ) {
            throw new JspException( "Translation string must be in the body." );
        }
        
        try {
            /* Actually translate the string */
            Map<String, String> i18n_map = (Map<String, String>)pageContext.getRequest().getAttribute( "i18n_map" );
            for ( int c = 0 ; c < this.p.length ; c++ ) if ( this.p[c] == null ) this.p[c] = "";

            out.print( tr(bodyString, this.p, i18n_map ));

            return EVAL_PAGE;
            
        } catch ( IOException e ) {
            throw new JspException( "Unable to write translation string.", e );
        } finally {
            for ( int c = 0 ; c < this.p.length ; c++ ) this.p[c] = null;
        }
    }
    
    /**
     * i18n - translate the page context
     * @param pageContext
     * @param value
     * @return the string
     */
    @SuppressWarnings("unchecked") //getAttribute
    public static String i18n( PageContext pageContext, String value )
    {
        /* Actually translate the string */
        Map<String, String> i18n_map = (Map<String, String>)pageContext.getRequest().getAttribute( "i18n_map" );
        return tr(value, i18n_map);
    }

    /**
     * release
     */
    public void release()
    {
        this.p = null;
    }
 
    /**
     * we should use the methods from I18nUtil but, the I18nTag can not load the utility class at runtime,
     * even if from jsp it can be used fine; do not know why.
     * @param value
     * @param objects
     * @param i18n_map
     * @return the translated string
     */
    private static String tr(String value, Object[] objects, Map<String, String> i18n_map)
    {
        return MessageFormat.format( tr(value,i18n_map), objects);
    }

    /**
     * tr - get the translation for value
     * @param value
     * @param i18n_map - the map
     * @return the translated string
     */
    private static String tr(String value, Map<String, String> i18n_map)
    {
        String tr = i18n_map.get(value);
        if (tr == null) {
            tr = value;
        }
        return tr;
    }    
}
