/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: ConnectivityTester.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.mvvm.engine;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.untangle.mvvm.BrandingManager;

class BrandingManagerImpl implements BrandingManager
{
    private static final File BRANDING_DIR;
    private static final File IMAGE_DIR;

    public Set<String> getImageNames()
    {
        String[] files = IMAGE_DIR.list();

        Set<String> s = new TreeSet<String>();
        for (int i = 0; i < files.length; i++) {
            s.add(files[i]);
        }

        return s;
    }

    public byte[] getImage(String name)
        throws IOException
    {
        File imageFile = new File(IMAGE_DIR, name);
        if (!imageFile.exists()) {
            return null;
        } else {
            RandomAccessFile f = null;
            try {
                f = new RandomAccessFile(imageFile, "r");
                byte[] r = new byte[(int)f.length()];

                int i = 0;
                for (int c = 0; 0 <= (c = f.read(r, i, r.length - i)); i += c);

                return r;
            } finally {
                if (null != f) {
                    f.close();
                }
            }
        }
    }

    public Map<String, byte[]> getImages()
        throws IOException
    {
        String[] files = IMAGE_DIR.list();

        Map<String, byte[]> m = new TreeMap<String, byte[]>();

        for (int i = 0; i < files.length; i++) {
            m.put(files[i], getImage(files[i]));
        }

        return m;
    }

    public void addImage(String name, byte[] image)
        throws IOException
    {
        File f = new File(IMAGE_DIR, name);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(image);
        } finally {
            if (null != fos) {
                fos.close();
            }
        }
    }

    // static initialization --------------------------------------------------

    static {
        String wd = System.getProperty("bunnicula.web.dir");
        BRANDING_DIR = new File(wd, "branding");
        IMAGE_DIR = new File(BRANDING_DIR, "images");
    }
}
