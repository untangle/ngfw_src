/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.buildutil;

import static org.apache.bcel.Constants.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantInterfaceMethodref;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ConstantValue;
import org.apache.bcel.classfile.Deprecated;
import org.apache.bcel.classfile.ExceptionTable;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Signature;
import org.apache.bcel.classfile.SourceFile;
import org.apache.bcel.classfile.StackMap;
import org.apache.bcel.classfile.StackMapEntry;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.classfile.Unknown;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ClassLoaderRepository;

/**
 * Visits class file elements, generating the closure of referenced
 * classes.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class ClassVisitor
    implements org.apache.bcel.classfile.Visitor
{
    private final ClassLoaderRepository clr;
    private final Set<String> toVisit = new HashSet<String>();
    private final Set<String> unavailable = new HashSet<String>();
    private final Set<String> visited = new HashSet<String>();
    private final List<ConstantPool> constantPoolStack
        = new LinkedList<ConstantPool>();

    private ConstantPool constantPool = null;

    // constructors -----------------------------------------------------------

    public ClassVisitor(ClassLoader classLoader, List<String> toVisit)
    {
        this.clr = new ClassLoaderRepository(classLoader);

        visited.add("Z");
        visited.add("B");
        visited.add("C");
        visited.add("D");
        visited.add("F");
        visited.add("I");
        visited.add("J");
        visited.add("S");
        visited.add("V");

        for (String s : toVisit) {
            scheduleVisit(s);
        }
    }

    // public methods ---------------------------------------------------------

    public void visitAll()
    {
        while (!toVisit.isEmpty()) {
            Iterator<String> i = toVisit.iterator();
            String s = i.next();
            i.remove();

            try {
                JavaClass jc = clr.loadClass(s);
                jc.accept(this);
            } catch (ClassNotFoundException exn) {
                unavailable.add(s);
            }
        }
    }

    public Set<String> getVisited()
    {
        return visited;
    }

    public Set<String> getUnavailable()
    {
        return unavailable;
    }

    // org.apache.bcel.classfile.Visitor methods ------------------------------

    public void visitJavaClass(JavaClass obj)
    {
        pushConstantPool(obj.getConstantPool());

        String className = obj.getClassName();
        if (visited.contains(className)) {
            return;
        } else {
            visited.add(className);
        }

        for (Attribute at : obj.getAttributes()) {
            at.accept(this);
        }

        for (Field f : obj.getFields()) {
            f.accept(this);
        }

        for (String s : obj.getInterfaceNames()) {
            scheduleVisit(s);
        }

        for (Method m : obj.getMethods()) {
            m.accept(this);
        }

        scheduleVisit(obj.getSuperclassName());

        popConstantPool();
    }

    public void visitInnerClasses(InnerClasses obj)
    {
        for (InnerClass ic : obj.getInnerClasses()) {
            ic.accept(this);
        }
    }

    public void visitInnerClass(InnerClass obj)
    {
        String n = constantPool.getConstantString(obj.getInnerClassIndex(),
                                                  CONSTANT_Class);
        scheduleVisit(convertPath(n));
    }

    public void visitField(Field field)
    {
        Type t = field.getType();
        scheduleVisit(convertPath(t.getSignature()));
        ConstantValue cv = field.getConstantValue();
        if (null != cv) {
            cv.accept(this);
        }
    }

    public void visitMethod(Method obj)
    {
        pushConstantPool(obj.getConstantPool());

        for (Type t : obj.getArgumentTypes()) {
            scheduleVisit(convertPath(t.getSignature()));
        }

        Code c = obj.getCode();
        if (null != c) {
            c.accept(this);
        }

        ExceptionTable et = obj.getExceptionTable();
        if (null != et) {
            et.accept(this);
        }

        LineNumberTable lnt = obj.getLineNumberTable();
        if (null != lnt) {
            lnt.accept(this);
        }

        LocalVariableTable lvt = obj.getLocalVariableTable();
        if (null != lvt) {
            lvt.accept(this);
        }

        scheduleVisit(convertPath(obj.getReturnType().getSignature()));

        popConstantPool();
    }

    public void visitCode(Code obj)
    {
        pushConstantPool(obj.getConstantPool());

        for (Attribute attr : obj.getAttributes()) {
            attr.accept(this);
        }

        for (CodeException ce : obj.getExceptionTable()) {
            ce.accept(this);
        }

        LineNumberTable lnt = obj.getLineNumberTable();
        if (null != lnt) {
            lnt.accept(this);
        }

        LocalVariableTable lvt = obj.getLocalVariableTable();
        if (null != lvt) {
            lvt.accept(this);
        }

        popConstantPool();
    }

    public void visitLocalVariableTable(LocalVariableTable obj)
    {
        pushConstantPool(obj.getConstantPool());

        for (LocalVariable lv : obj.getLocalVariableTable()) {
            lv.accept(this);
        }

        popConstantPool();
    }

    public void visitLocalVariable(LocalVariable obj)
    {
        pushConstantPool(obj.getConstantPool());

        scheduleVisit(convertPath(obj.getSignature()));

        popConstantPool();
    }

    public void visitExceptionTable(ExceptionTable obj)
    {
        pushConstantPool(obj.getConstantPool());

        for (String en : obj.getExceptionNames()) {
            scheduleVisit(convertPath(en));
        }

        popConstantPool();
    }

    public void visitCodeException(CodeException obj)
    {
        int ct = obj.getCatchType();

        if (0 != ct) {
            String t = constantPool.getConstantString(ct, CONSTANT_Class);
            scheduleVisit(convertPath(t));
        }
    }

    public void visitConstantClass(ConstantClass obj)
    {
        scheduleVisit(convertPath(obj.getConstantValue(constantPool).toString()));
    }

    public void visitConstantFieldref(ConstantFieldref obj)
    {
        scheduleVisit(obj.getClass(constantPool));
    }

    public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj)
    {
        scheduleVisit(obj.getClass(constantPool));
    }

    public void visitConstantLong(ConstantLong obj)
    {
    }

    public void visitConstantMethodref(ConstantMethodref obj)
    {
        scheduleVisit(obj.getClass(constantPool));
    }

    public void visitConstantNameAndType(ConstantNameAndType obj)
    {
        for (String s : splitSignature(obj.getSignature(constantPool))) {
            scheduleVisit(convertPath(s));
        }
    }

    public void visitConstantString(ConstantString obj)
    {
    }

    public void visitConstantUtf8(ConstantUtf8 obj)
    {
    }

    public void visitConstantValue(ConstantValue obj)
    {
        pushConstantPool(obj.getConstantPool());
        popConstantPool();
    }

    public void visitStackMap(StackMap obj)
    {
    }

    public void visitStackMapEntry(StackMapEntry obj)
    {
    }

    public void visitSynthetic(Synthetic obj)
    {
        System.out.println("SYNTH");
        pushConstantPool(obj.getConstantPool());
        popConstantPool();
    }

    public void visitConstantDouble(ConstantDouble obj)
    {
    }

    public void visitConstantFloat(ConstantFloat obj)
    {
    }

    public void visitConstantInteger(ConstantInteger obj)
    {
    }

    public void visitSourceFile(SourceFile obj) { }

    public void visitConstantPool(ConstantPool obj)
    {
        for (Constant c : obj.getConstantPool()) {
            if (null != c) {
                c.accept(this);
            }
        }
    }

    public void visitLineNumberTable(LineNumberTable obj) { }
    public void visitLineNumber(LineNumber obj) { }
    public void visitUnknown(Unknown obj) { }
    public void visitDeprecated(Deprecated obj) { }

    public void visitSignature(Signature obj)
    {
        System.err.println("SIG: " + obj);
    }

    // private methods --------------------------------------------------------

    private void pushConstantPool(ConstantPool constantPool)
    {
        constantPoolStack.add(0, constantPool);
        this.constantPool = constantPool;
        constantPool.accept(this);
    }

    private void popConstantPool()
    {
        this.constantPool = constantPoolStack.remove(0);
    }

    private String convertPath(String path)
    {
        return path.replace('/', '.')
            .replace(";", "")
            .replaceFirst("^\\[+", "")
            .replaceFirst("^L+", "");
    }

    private List<String> splitSignature(String sig)
    {
        String[] a = sig.split("[;()]");
        List<String> l = new ArrayList<String>(a.length);
        for (String s : a) {
            if (!s.equals("")) {
                l.add(s);
            }
        }

        return l;
    }

    public void scheduleVisit(String c)
    {
        if (c.startsWith("java") || c.startsWith("javax")) { // make configurable
            return;
        } else if (!visited.contains(c) && !unavailable.contains(c)) {
            toVisit.add(c);
        }
    }
}

