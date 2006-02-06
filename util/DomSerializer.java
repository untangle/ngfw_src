/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DomSerializer
{
    public static final void main(String[] args)
        throws Exception
    {
        for (String arg : args) {
            File f = new File(arg);
            SAXReader r = new SAXReader(false);
            r.setEntityResolver(DtdResolver.RESOLVER);
            Document d = r.read(f);
            File cache = new File(arg + ".bin");
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(cache));
            os.writeObject(d);
            os.close();
            System.out.println("serialized: " + arg + ".bin");
        }
    }


    private static class DtdResolver implements EntityResolver
    {
        public static final EntityResolver RESOLVER = new DtdResolver();

        private static final String URL = "http://hibernate.sourceforge.net/";

        private DtdResolver() { }

        public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException
        {
            if (null != publicId && systemId.startsWith(URL)) {
                String s = "org/hibernate/" + systemId.substring(URL.length());
                InputStream is = getClass().getClassLoader()
                    .getResourceAsStream(s);
                if (null != is) {
                    InputSource src = new InputSource(is);
                    src.setPublicId(publicId);
                    src.setPublicId(systemId);
                    return src;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
    }
}
