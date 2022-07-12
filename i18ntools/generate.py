#!/usr/bin/python3
"""
Rebuild gettext template file (.pot) and
synchronize with existing po files
"""
import datetime
import fnmatch
import getopt
import json
import os
import sys
import re
import shutil
import subprocess

from subprocess import call
import importlib

UNTANGLE_DIR = '%s/lib/python' % (os.path.dirname(os.path.realpath(__file__)))
sys.path.insert(0, UNTANGLE_DIR)

import i18n

Debug=False

importlib.reload(sys)
sys.setdefaultencoding('utf8')

ngfw = i18n.Ngfw()
languages = i18n.Languages()

pot = i18n.PotFile(language="en", file_name="/tmp/generated.pot")
# pot = i18n.PotFile(language="en", file_name="pot/en/untangle-en.pot")
pot_file_name = "pot/en/untangle-en.pot"

def get_keys(module):
    """
	Walk source trees and pull text appropriately from
	different types of files
    """
    global pot
    module_source_directory = ngfw.get_module_directory(module)

    full_file_name = None
    for root, dir_names, file_names in os.walk(module_source_directory):

        for file_name in fnmatch.filter(file_names, '*.json'):
            full_file_name = root + "/" + file_name
            report_file = open(full_file_name)
            try:
                settings = json.load(report_file)
                report_file.close()

                pot.load()

                for report_field in ngfw.report_fields:
                    if report_field in settings:
                        record = i18n.PoRecord()
                        record.add_msg_id(settings[report_field])
                        record.add_comment(": " + full_file_name)
                        pot.add_record(record)

                pot.save()
            except Exception as e:
                #print(sys.exc_info()[0])
                print("Ignoring file: " + full_file_name)
                pass

        for file_name in fnmatch.filter(file_names, '*.js'):
            full_file_name = root + "/" + file_name

            ## xgettext does not support suffixes like "foo".t() only prefixes like _("foo")
            ## Instead we extract all strings with -a but we don't actually want ALL strings
            ## So we grep for t() to only process lines with t() on it.
            ## We also remove empty strings so xgettext wont complain
            ## and also remove all \r and \n from inside strings

            command = '''/bin/cat %s | sed 's/\\\\r//g' | sed 's/\\\\n//g' | perl -pe 's/"([^"]+?)"\.t\(\)/_("\\1")/g' |  perl -pe "s/'((?:[^'\\\\\\]++|\\\\\\.)*)'\.t\(\)/_('\\1')/g" | xgettext -j --copyright-holder="%s" -LJavascript -o %s -''' %(full_file_name, ngfw.copyright, pot.file_name)
            try:
                pipes = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, text=True)
                std_out, std_err = pipes.communicate()
                if pipes.returncode != 0:
                    print("error!")
                elif len(std_err):
                    print(full_file_name)
                    print(std_err)
                    sys.exit(1)
            except Exception as e:
                print(Exception)
                print(e)
                print(sys.exc_info()[0])
                sys.exit(1)

            ##
            ## Replace location of "standard input" with actual path .
            ##
            command = '''cat %s | sed 's@#: standard input:@#: %s:@g' > /tmp/generated-locations.pot''' %(pot.file_name, full_file_name)
            try:
                pipes = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE, shell=True, text=True)
                std_out, std_err = pipes.communicate()
                if pipes.returncode != 0:
                    print("error!")
                elif len(std_err):
                    print(full_file_name)
                    print(std_err)
                    sys.exit(1)
            except Exception as e:
                print(Exception)
                print(e)
                print(sys.exc_info()[0])
                sys.exit(1)

            shutil.move("/tmp/generated-locations.pot", pot.file_name)

        for file_name in fnmatch.filter(file_names, '*.py'):
            full_file_name = root + "/" + file_name
            call([
                "xgettext",
                "-j",
                "--copyright-holder=''" + ngfw.copyright + '"',
                "-L", "Python",
                "-k_",
                "-o", pot.file_name,
                full_file_name
            ])
        for file_name in fnmatch.filter(file_names, '*.java'):
            full_file_name = root + "/" + file_name
            call([
                "xgettext",
                "-j",
                "--copyright-holder=''" + ngfw.copyright + '"',
                "-L", "Java",
                "-ktr",
                "-kmarktr",
                "-o", pot.file_name,
                full_file_name
            ])
        for file_name in fnmatch.filter(file_names, '*.jspx'):
            full_file_name = root + "/" + file_name
            jspfile = i18n.JspxFile(full_file_name)
            jspfile.load()
            pot.load()

            for record in jspfile.records:
                pot.add_record(record)

            pot.save()

def main(argv):
    """
    Main entry for generate
    """
    global pot
    global Debug

    try:
        opts, args = getopt.getopt(argv, "hpl:d", ["help", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        if opt in ( "-d", "--debug"):
            Debug=True

    #
    # Process source files
    #
    print("Processing source files...")
    pot.reset()
    modules = ngfw.modules
    for module in modules:
        print("\t" + module)
        get_keys(module)

    # Change comments to not have path leading to "ngfw"
    print("Converting comments...")
    pot.load()
    invalid_msg_id_count = 0
    for record in pot.records:
        if record.is_valid_msg_id() == False:
            invalid_msg_id_count = invalid_msg_id_count + 1
            print("Invalid string found:")
            print(record)
            print("")

        for (comment_index, comment) in enumerate(record.comment):
            record.comment[comment_index] = re.sub(ngfw.regex_comment_prefix, "/" + ngfw.path, comment)
    if invalid_msg_id_count > 0:
        print("Invalid string count: %d" % invalid_msg_id_count)
    pot.save()

    pot.rename(pot_file_name)

if __name__ == "__main__":
    main(sys.argv[1:])
