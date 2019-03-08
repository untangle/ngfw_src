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
Type_name = None
Validators = {}
Show_valid = False
Start_time = time()

class JavaParser:
    def __init__(self, file_path):
        file = open(file_path)
        source = file.read()
        file.close()

        try:
            self.tree = javalang.parse.parse(source)
        except:
            print(file_path)

    def get_node(self, tree):
        result = {}

        if tree is None:
            return result

        if type(tree) == tuple:
            for path, node in tree:
                print("(tuple) node=%s" % ( str(node)))
                print("(tuple) node type=%s" % ( str( type( node) ) ))
                if type(node) is list or type(node) is tuple:
                    print("(tuple) do list")
                else:
                    print("(tuple) do other")
                    if "attr" in node:
                        print("(tuple) do attr")
                    else:
                        print(self.get_node(node))
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
            result['_type'] = 'package'
            return JavaDocValidator( node_name, result )
        return None

    def get_classes(self):
        result = []
        for path, node in self.tree:
            node_name = node.__class__.__name__
            if node_name != "ClassDeclaration":
                continue
            class_result = {"_optional": False}

            for key in node.attrs:
                class_result[key] = getattr(node, key) 
                if key == "implements":
                    serializable = False
                    jsonstring = False
                    for implement in self.get_node(getattr(node,key)):
                        # remove all prepended class paths.. "java.io.Serializable" -> "Serializable"
                        while implement.get('sub_type') != None:
                            implement = implement.get('sub_type')
                        if implement["name"] == "Serializable":
                            serializable = True
                        elif implement["name"] == "JSONString":
                            jsonstring = True
                    # ignore classes that include serializable and jsonstring
                    if serializable is True and jsonstring is True:
                        class_result["_optional"] = True
                if key == "extends":
                    extend = self.get_node(getattr(node,key))
                    logEvent = False
                    if extend.get('name') == "LogEvent":
                        logEvent = True
                    if logEvent is True:
                        class_result["_optional"] = True

            class_result['_type'] = 'class'
            result.append( JavaDocValidator( node_name, class_result) )
        return result

    def get_methods(self, classes):
        result = []
        current_class = None
        optional_method = False

        in_interface = False
        for path, node in self.tree:
            node_name = node.__class__.__name__

            if node_name == "InterfaceDeclaration":
                in_interface = True

            if node_name == "ClassDeclaration":
                in_interface = False
                current_class = getattr(node,"name")

                class_found = False
                for c in classes:
                    if c.tree["name"] == current_class and c.tree["_optional"] is True:
                        optional_method = True

            if in_interface is True:
                continue

            # Other method identifiers to look for?
            if node_name != "MethodDeclaration" and node_name != "ConstructorDeclaration":
                continue

            method_result = {}
            for key in node.attrs:
                value = getattr(node, key)
                method_result[key] = value
                if key == "return_type":
                    method_result[key] = self.get_node(value)
                if key == "parameters":
                    method_result[key] = self.get_node(value)

            method_result['_type'] = 'method'
            method_validator = JavaDocValidator( node_name, method_result, optional_method)
            if optional_method is False or method_validator.missing is False:
                result.append( method_validator )
        return result

class JavaDocValidator:

    def __init__(self, type, tree, optional=False):
        self.type = type
        self.tree = tree
        self.optional = optional

        self.missing = False
        self.parameters_missing = False
        self.parameter_mismatch = False
        self.return_mismatch = False
        self.return_missing = False
        self.throws_mismatch = False
        self.throws_missing = False

        self.valid = False

        if Type_name is None or Type_name == tree["name"]:
            self.validate()

    def validate(self):

        if Debug:
            print("\tvalidating %s" % (self.get_definition()))
            print(self.tree)
        if "documentation" not in self.tree or self.tree["documentation"] is None:
            self.missing = True
            return

        javadoc = javalang.javadoc.parse(self.tree["documentation"])
        if javadoc.deprecated:
            # Don't process futher if deprecated.
            return

        if "parameters" in self.tree and len(self.tree["parameters"]) > 0:
            if len(javadoc.params) == 0:
                if self.optional is False:
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
        if "return_type" in self.tree and len(self.tree["return_type"])> 0:
            if return_type == "void":
                self.return_mismatch = True
            elif javadoc.return_doc is None:
                if self.optional is False:
                    self.return_missing = True
        else:
            if return_type != "void":
                if self.optional is False:
                    self.return_missing = True

        if "throws" in self.tree and self.tree["throws"] is not None:
            if len(javadoc.throws) == 0:
                if self.optional is False:
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

        if self.tree['_type'] == 'package':
            return self.tree['_type'] + ' ' + self.tree['name'] + ';'
        elif self.tree['_type'] == 'class':
            return self.tree['_type'] + ' ' + self.tree['name'] + ';'
        else:
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

            return " ".join(definition) + "(" + ", ".join(arguments) + ")" + throws

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

def print_summary_report( file_count, required_count, missing_count, invalid_count, valid_count):
    print("")
    print("Total files %d, required javadocs %d" % ( file_count, required_count ))
    if required_count > 0:
        if missing_count > 0:
            print("Missing \t %4d \t %4.2f%%" % (missing_count, ( float( missing_count ) / required_count * 100) ))
        if invalid_count > 0:
            print("Invalid \t %4d \t %4.2f%%" % (invalid_count, ( float( invalid_count ) / required_count * 100) ))
        print("Valid   \t %4d \t %4.2f%%" % (valid_count, ( float(valid_count) / required_count * 100) ))
    print("")

def print_report(total_only=False, detail_only=False):
    total_file_count = 0
    total_required_count = 0
    total_missing_count = 0
    total_valid_count = 0
    total_invalid_count = 0

    current_file_count = 0
    current_required_count = 0
    current_missing_count = 0
    current_valid_count = 0
    current_invalid_count = 0

    current_directory = None
    for file_path in sorted(Validators):
        directory = file_path[:file_path.find('/')]
        if current_directory is None or current_directory != directory:
            if total_only is False and current_directory is not None and detail_only is False:
                print_summary_report( current_file_count, current_required_count, current_missing_count, current_invalid_count, current_valid_count)
            current_file_count = 0
            current_required_count = 0
            current_missing_count = 0
            current_valid_count = 0
            current_invalid_count = 0
            current_directory = directory
            if total_only is False and detail_only is False:
                print("\n" + current_directory + "\n" + '=' * len(current_directory) + "\n")
        total_file_count += 1
        current_file_count += 1
        total_required_count += len(Validators[file_path])
        current_required_count += len(Validators[file_path])
        show_path = False
        for validator in Validators[file_path]:
            if Type_name is not None and validator.tree["name"] != Type_name:
                continue
            report = validator.get_report()
            if total_only is False and validator.valid is not True and show_path is False:
                print(file_path)
                show_path = True
            if total_only is False and validator.valid is not True or Show_valid is True:
                print("\t" +validator.get_definition())
                print("\t\t" + "\n\t".join(report))
            if validator.valid is True:
                total_valid_count += 1
                current_valid_count += 1
            elif validator.missing is True:
                total_missing_count += 1
                current_missing_count += 1
            else:
                total_invalid_count +=1
                current_invalid_count += 1

    if total_only is False and detail_only is False:
        print_summary_report( current_file_count, current_required_count, current_missing_count, current_invalid_count, current_valid_count)

    if detail_only is False:
        print("Total Repository")
        print_summary_report( total_file_count, total_required_count, total_missing_count,total_invalid_count, total_valid_count)
        print("")
        print("Elapsed %4.2f s" % (time() - Start_time))

    return total_valid_count == total_required_count

def usage():
    """
    Show usage
    """
    print("usage")

def main(argv):
    global Debug
    global Type_name

    path = "."
    ignore_paths = []
    filename = None
    show_detail_only = False

    try:
        opts, args = getopt.getopt(argv, "hs:d", ["help", "screen=", "resolution=", "path=", "ignore_path=", "filename=", "debug", "type_name=", "show_valid", "detail_only"] )
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
            Type_name = arg
        if opt in ( "--show_valid"):
            Show_valid = True
        if opt in ( "--detail_only"):
            show_detail_only = True

    if Debug is True:
        print("path=" + path)
        print("ignore_path=" + ",".join(ignore_paths))
        if filename != None:
            print("filename=" + filename)

    file_reports = {}

    if show_detail_only is False:
        print("Finding and validating...")
    file_paths = get_files(path)
    for file_path in file_paths:
        ignore = False
        for ignore_path in ignore_paths:
            if ignore_path in file_path:
                ignore = True
        if ignore is True:
            if Debug:
                print("\tIgnored: " + file_path)
            continue

        if ( filename is not None ) and ( file_path.endswith("/" + filename) is False):
            continue

        Validators[file_path] = []

        if Debug:
            print(file_path)

        parser = JavaParser(file_path)
        package = parser.get_package()
        classes = []
        if package is not None:
            Validators[file_path].append( package )
        for cd in parser.get_classes():
            classes.append(cd)
            Validators[file_path].append( cd )
        for md in parser.get_methods(classes):
            Validators[file_path].append( md )

    if show_detail_only is False:
        print("Report")
    # Show total only as a header
    print_report(True, show_detail_only)
    # Show full report with total as footer
    if print_report(False, show_detail_only) is True:
        sys.exit(0)
    else:
        sys.exit(1)

if __name__ == "__main__":
    main( sys.argv[1:] )
