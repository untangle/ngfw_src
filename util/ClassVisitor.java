/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

import static org.apache.bcel.Constants.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.CodeException;
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
import org.apache.bcel.classfile.Visitor;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ClassLoaderRepository;

class ClassVisitor implements Visitor
{
    private final ClassLoaderRepository clr;
    private final List<String> toVisit = new LinkedList<String>();
    private final Set<String> unavailable = new HashSet<String>();
    private final Set<String> visited = new HashSet<String>();
    private final List<ConstantPool> constantPoolStack
        = new LinkedList<ConstantPool>();

    private ConstantPool constantPool = null;

    // constructors -----------------------------------------------------------

    public ClassVisitor(ClassLoader classLoader, List<String> toVisit)
    {
        this.clr = new ClassLoaderRepository(classLoader);
        this.toVisit.addAll(toVisit);

        visited.add("Z");
        visited.add("B");
        visited.add("C");
        visited.add("D");
        visited.add("F");
        visited.add("I");
        visited.add("J");
        visited.add("S");
    }

    // public methods ---------------------------------------------------------

    public void visitAll()
    {
        while (!toVisit.isEmpty()) {
            String s = toVisit.remove(0);

            if (!visited.contains(s) && !unavailable.contains(s)) {
                try {
                    JavaClass jc = clr.loadClass(s);
                    jc.accept(this);
                } catch (ClassNotFoundException exn) {
                    unavailable.add(s);
                }
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

    // Visitor methods --------------------------------------------------------

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
            toVisit.add(s);
        }

        for (Method m : obj.getMethods()) {
            m.accept(this);
        }

        toVisit.add(obj.getSuperclassName());

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
        toVisit.add(convertPath(n));
    }

    public void visitField(Field field)
    {
        Type t = field.getType();
        toVisit.add(convertPath(t.getSignature()));
    }

    public void visitMethod(Method obj)
    {
        for (Type t : obj.getArgumentTypes()) {
            toVisit.add(convertPath(t.getSignature()));
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

        toVisit.add(convertPath(obj.getReturnType().getSignature()));
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

        toVisit.add(convertPath(obj.getSignature()));

        popConstantPool();
    }

    public void visitExceptionTable(ExceptionTable obj)
    {
        pushConstantPool(obj.getConstantPool());

        for (String en : obj.getExceptionNames()) {
            toVisit.add(convertPath(en));
        }

        popConstantPool();
    }

    public void visitCodeException(CodeException obj)
    {
        int ct = obj.getCatchType();

        if (0 != ct) {
            String t = constantPool.getConstantString(ct, CONSTANT_Class);
            toVisit.add(convertPath(t));
        }
    }

    public void visitConstantClass(ConstantClass obj)
    {
        System.err.println("visitConstantClass: " + obj);
    }

    public void visitConstantFieldref(ConstantFieldref obj)
    {
        System.err.println("visitConstantFieldref: " + obj);
    }

    public void visitConstantInterfaceMethodref(ConstantInterfaceMethodref obj)
    {
        System.err.println("visitConstantInterfaceMethodref: " + obj);
    }

    public void visitConstantLong(ConstantLong obj)
    {
        System.err.println("visitConstantLong: " + obj);
    }

    public void visitConstantMethodref(ConstantMethodref obj)
    {
        System.err.println("visitConstantMethodref: " + obj);
    }

    public void visitConstantNameAndType(ConstantNameAndType obj)
    {
        System.err.println("visitConstantNameAndType: " + obj);
    }

    public void visitConstantString(ConstantString obj)
    {
        System.err.println("visitConstantString: " + obj);
    }

    public void visitConstantUtf8(ConstantUtf8 obj)
    {
        System.err.println("visitConstantUtf8: " + obj);
    }

    public void visitConstantValue(ConstantValue obj)
    {
        System.err.println("visitConstantValue: " + obj);
    }

    public void visitStackMap(StackMap obj)
    {
        System.err.println("visitStackMap: " + obj);
    }

    public void visitStackMapEntry(StackMapEntry obj)
    {
        System.err.println("visitStackMapEntry: " + obj);
    }

    public void visitSynthetic(Synthetic obj)
    {
        System.err.println("visitSynthetic: " + obj);
    }

    public void visitConstantDouble(ConstantDouble obj)
    {
        System.err.println("visitConstantDouble: " + obj);
    }

    public void visitConstantFloat(ConstantFloat obj)
    {
        System.err.println("visitConstantFloat: " + obj);
    }

    public void visitConstantInteger(ConstantInteger obj)
    {
        System.err.println("visitConstantInteger: " + obj);
    }

    // no-ops -----------------------------------------------------------------

    public void visitSourceFile(SourceFile obj) { }
    public void visitConstantPool(ConstantPool obj) { }
    public void visitLineNumberTable(LineNumberTable obj) { }
    public void visitLineNumber(LineNumber obj) { }
    public void visitUnknown(Unknown obj) { }
    public void visitDeprecated(Deprecated obj) { }
    public void visitSignature(Signature obj) { }


    // private methods --------------------------------------------------------

    private void pushConstantPool(ConstantPool constantPool)
    {
        constantPoolStack.add(0, constantPool);
        this.constantPool = constantPool;
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
}
