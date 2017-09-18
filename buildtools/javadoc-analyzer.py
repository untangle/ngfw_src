#!/usr/bin/python

import copy
import getopt
import javalang
import os
import re
import sys
import time

from timeit import default_timer as time

Debug = False

class JavaParser:
    def __init__(self, file_path):
        file = open(file_path)
        source = file.read()
        file.close()

        self.tree = javalang.parse.parse(source)

    def get_node(self, tree):
        result = {}

        if tree is None:
            return result

        if type(tree) == tuple:
            for path, node in tree:
                print "(tuple) node=%s" % ( str(node))
                print "(tuple) node type=%s" % ( str( type( node) ) )
                if type(node) is list or type(node) is tuple:
                    print "(tuple) do list"
                else:
                    print "(tuple) do other"
                    if "attr" in node:
                        print "(tuple) do attr"
                    else:
                        print self.get_node(node)
        elif type(tree) == list:
            result = []
            for node in tree:
                result.append(self.get_node(node))
        else:
            for key in tree.attrs:
                value = getattr(tree, key)
                if value.__class__.__module__ == "javalang.tree":
                    result[key] = self.get_node(value)
                elif type(value) is list:
                    result[key] = self.get_node(value)
                else:
                    result[key] = value

        return result

    def get_package(self):
        result = {}
        for path, node in self.tree:
            node_name = node.__class__.__name__
            if node_name != "PackageDeclaration":
                continue
            for key in node.attrs:
                result[key] = getattr(node, key)
            break
        if result:
            return JavaDocValidator( node_name, result )
        return None

    def get_classes(self):
        result = []
        for path, node in self.tree:
            node_name = node.__class__.__name__
            if node_name != "ClassDeclaration":
                continue
            class_result = {}
            for key in node.attrs:
                class_result[key] = getattr(node, key) 
            result.append( JavaDocValidator( node_name, class_result) )
        return result

    def get_methods(self):
        result = []
        for path, node in self.tree:
            node_name = node.__class__.__name__
            if node_name != "MethodDeclaration":
                continue
            method_result = {}
            for key in node.attrs:
                value = getattr(node, key)
                method_result[key] = value
                if key == "return_type":
                    method_result[key] = self.get_node(value)
                if key == "parameters":
                    method_result[key] = self.get_node(value)

            result.append( JavaDocValidator( node_name, method_result) )
        return result

class JavaDocValidator:

    def __init__(self, type, tree):
        self.type = type
        self.tree = tree

        self.missing = False
        self.parameters_missing = False
        self.parameter_mismatch = False
        self.return_mismatch = False
        self.return_missing = False
        self.throws_mismatch = False
        self.throws_missing = False

        self.valid = False

        self.validate()

    def validate(self):
        if Debug:
            print "\tvalidating %s" % (self.get_definition())
        if "documentation" not in self.tree or self.tree["documentation"] is None:
            self.missing = True
            return

        javadoc = javalang.javadoc.parse(self.tree["documentation"])
        if "parameters" in self.tree:
            if len(javadoc.params) == 0:
                self.parameters_missing = True
            elif len(javadoc.params) != len(self.tree["parameters"]):
                self.parameter_mismatch = True
            else:
                for index, parameter in enumerate(self.tree["parameters"]):
                    if javadoc.params[index][0] != parameter["name"]:
                        self.parameter_mismatch = True
        else:
            if len(javadoc.params) > 0:
                self.parameter_mismatch = True

        return_type = self.get_return_type()
        if "return" in self.tree:
            if return_type == "void":
                self.return_mismatch = True
        else:
            if return_type != "void":
                self.return_missing = True

        if "throws" in self.tree and self.tree["throws"] is not None:
            if len(javadoc.throws) == 0:
                self.throws_missing = True
            elif len(javadoc.throws) != len(self.tree["throws"]):
                self.throws_mismatch = True
            else:
                for index, throw in enumerate(self.tree["throws"]):
                    if throw not in javadoc.throws:
                            self.throw_mismatch = True
        else:
            if len(javadoc.throws) > 0:
                self.throws_mismatch = True

    def get_return_type(self):
        return_type = "void"
        if "return_type" in self.tree:
            if "name" in self.tree["return_type"]:
                return_type = self.tree["return_type"]["name"]
                if "arguments" in self.tree["return_type"] and self.tree["return_type"]["arguments"] is not None:
                    arguments = []
                    for argument in self.tree["return_type"]["arguments"]:
                        arguments.append(argument["type"]["name"])
                    return_type += "<" + ",".join(arguments) + ">"

        return return_type

    def get_throws(self):
        throws = []
        if "throws" in self.tree and self.tree["throws"] is not None:
            for throw in self.tree["throws"]:
                throws.append(throw)
        if len(throws) > 0:
            throws_string = " throws " + ",".join(throws)
        else:
            throws_string = ""

        return throws_string


    def get_definition(self):
        definition = []

        if "modifiers" in self.tree and self.tree["modifiers"] is not None:
            for modifier in self.tree["modifiers"]:
                definition.append(modifier)

        definition.append(self.get_return_type())

        definition.append(self.tree["name"])

        arguments = []
        if "parameters" in self.tree:
            for parameter in self.tree["parameters"]:
                if "type" in parameter and "name" in parameter["type"]:
                    arguments.append(parameter["type"]["name"] + " " + parameter["name"])

        throws = self.get_throws()

        return " ".join(definition) + "(" + " ".join(arguments) + ")" + throws

    def get_report(self):
        report = []
        if self.missing is True:
            report.append("Missing documentation")

        if self.parameter_mismatch is True:
            report.append("Parameter mismatch")
        if self.parameters_missing is True:
            report.append("Parameters missing")

        if self.return_mismatch is True:
            report.append("Return mismatch")
        if self.return_missing is True:
            report.append("Return missing")

        if self.throws_mismatch is True:
            report.append("Throws mismatch")
        if self.throws_missing is True:
            report.append("Throws missing")

        if(len(report) == 0):
            report.append("Valid")
            self.valid = True

        return report

def get_files(paths):
    file_paths = []
    for path in paths.split(","):
        for root, dirs, files in os.walk(path):
            for file in files:
                if file.endswith(".java"):
                    file_paths.append( os.path.join(root, file) )

    return file_paths

def usage():
    """
    Show usage
    """
    print "usage"

def main(argv):
    global Debug

    path = "."
    ignore_paths = []
    filename = None
    type_name = None

    try:
        opts, args = getopt.getopt(argv, "hs:d", ["help", "screen=", "resolution=", "path=", "ignore_path=", "filename=", "debug", "type_name="] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        if opt in ( "-d", "--debug"):
            Debug = True
        if opt in ( "--path"):
            path = arg
        if opt in ( "--ignore_path"):
            ignore_paths = arg.split(",")
        if opt in ( "--filename"):
            filename = arg
        if opt in ( "--type_name"):
            type_name = arg

    if Debug is True:
        print "path=" + path
        print "ignore_path=" + ",".join(ignore_paths)
        if filename != None:
            print "filename=" + filename

    file_reports = {}
    validators = {}

    start_time = time()

    print "Finding and validating..."
    file_paths = get_files(path)
    for file_path in file_paths:
        if Debug:
            print file_path

        ignore = False
        for ignore_path in ignore_paths:
            if ignore_path in file_path:
                ignore = True
        if ignore is True:
            if Debug:
                print "\tIgnored"
            continue

        if ( filename is not None ) and ( file_path.endswith("/" + filename) is False):
            continue

        validators[file_path] = []

        parser = JavaParser(file_path)
        package = parser.get_package()
        if package is not None:
            validators[file_path].append( parser.get_package() )
        for cd in parser.get_classes():
            validators[file_path].append( cd )
        for md in parser.get_methods():
            validators[file_path].append( md )

    file_count = 0
    required_count = 0
    missing_count = 0
    valid_count = 0
    invalid_count = 0

    print "Report"
    for file_path in validators:
        print file_path
        file_count += 1
        required_count += len(validators[file_path])
        for validator in validators[file_path]:
            if type_name is not None and validator.tree["name"] != type_name:
                continue
            print validator.get_definition()
            report = validator.get_report()
            print "\t" + "\n\t".join(report)
            if validator.valid is True:
                valid_count += 1
            elif validator.missing is True:
                missing_count += 1
            else:
                invalid_count +=1

    print 
    print "Total files %d, required javadocs %d" % ( file_count, required_count )
    print "Missing \t %4d \t %4.2f%%" % (missing_count, ( float( missing_count ) / required_count * 100) )
    print "Invalid \t %4d \t %4.2f%%" % (invalid_count, ( float( invalid_count ) / required_count * 100) )
    print "Valid   \t %4d \t %4.2f%%" % (valid_count, ( float(valid_count) / required_count * 100) )
    print 
    print "Elapsed %4.2f s" % (time() - start_time)

if __name__ == "__main__":
    main( sys.argv[1:] )
