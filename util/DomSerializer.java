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
import java.io.ObjectOutputStream;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;

public class DomSerializer
{
    public static final void main(String[] args)
        throws Exception
    {
        for (String arg : args) {
            File f = new File(arg);
            SAXReader r = new SAXReader(false);
            Document d = r.read(f);
            File cache = new File(arg + ".bin");
            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(cache));
            os.writeObject(d);
            os.close();
            System.out.println("serialized: " + arg + ".bin");
        }
    }
}
