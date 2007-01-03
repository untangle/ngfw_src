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

package com.untangle.buildutil;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Closure
{
    public static void main(String[] args)
    {
        if (0 == args.length) {
            System.err.println("usage: ...");
            System.exit(1);
        }

        List<String> rest = new LinkedList<String>();
        URL[] urls = new URL[0];
        ClassLoader classLoader = null;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-cp") || arg.equals("-classpath")) {
                String[] classpath = args[++i].split(":");
                urls = new URL[classpath.length];
                for (int j = 0; j < classpath.length; j++) {
                    try {
                        urls[j] = new File(classpath[j]).toURL();
                    } catch (MalformedURLException exn) {
                        System.err.println("ignoring bad url: " + classpath[j]);
                    }
                }
                classLoader = new URLClassLoader(urls);
            } else {
                rest.add(arg);
            }
        }

        if (null == classLoader) {
            System.err.println("classpath not specified");
            System.exit(1);
        } else if (0 == rest.size()) {
            System.err.println("expected command");
            System.exit(1);
        }

        String cmd = rest.remove(0);

        if (cmd.equalsIgnoreCase("compute-closure")) {
            computeClosure(classLoader, rest);
        } else if (cmd.equalsIgnoreCase("unused")) {
            unused(classLoader, rest, urls);
        }
    }

    private static void computeClosure(ClassLoader cl, List<String> classes)
    {
        ClassVisitor cv = new ClassVisitor(cl, classes);
        cv.visitAll();

        Set<String> s = cv.getVisited();
        for (String str : s) {
            System.out.println(str);
        }
    }

    private static void unused(ClassLoader cl, List<String> classes, URL[] urls)
    {
        ClassVisitor cv = new ClassVisitor(cl, classes);
        cv.visitAll();
        Set<String> visited = cv.getVisited();

        Set<String> jarClasses = getClasses(urls);


        jarClasses.removeAll(visited);

        System.out.println(jarClasses);
    }

    private static Set<String> getClasses(URL[] urls)
    {
        Set<String> s = new HashSet<String>();

        for (URL url : urls) {
            try {
                File f = new File(url.toURI());
                JarFile jf = new JarFile(f);
                Enumeration<JarEntry> es = jf.entries();
                while (es.hasMoreElements()) {
                    JarEntry je = es.nextElement();
                    String name = je.getName();
                    if (name.matches(".*\\.class$")) {
                        String c = name
                            .replace('/', '.')
                            .replaceFirst("\\.class$", "");
                        s.add(c);
                    }
                }
            } catch (IOException exn) {
                System.err.println(exn);
            } catch (URISyntaxException exn) {
                System.err.println(exn);
            }
        }

        return s;
    }
}
