#!/usr/bin/python
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

from subprocess import call

UNTANGLE_DIR = '%s/lib/python' % (os.path.dirname(os.path.realpath(__file__)))
sys.path.insert(0, UNTANGLE_DIR)

import i18n

reload(sys)  
sys.setdefaultencoding('utf8')

ngfw = i18n.Ngfw()
languages = i18n.Languages()

pot = i18n.PotFile(language="en", file_name="generated.pot")
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
    process_json_file = None
    for root, dir_names, file_names in os.walk(module_source_directory):
        for file_name in fnmatch.filter(file_names, '*.js'):
            full_file_name = root + "/" + file_name

            report_event_match = re.search(ngfw.regex_json_parse, full_file_name)
            process_json_file = False
            if report_event_match:
                # Attempt to pull known fields  from a json file
                process_json_file = True
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
                except:
                    report_file.close()
                    process_json_file = False

            if process_json_file == False:
                call([
                    "xgettext",
                    "-j",
                    "--copyright-holder=''" + ngfw.copyright + '"',
                    "-L", "JavaScript",
                    "-ki18n._",
                    "-o", pot.file_name,
                    full_file_name
                ])

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

    try:
        opts, args = getopt.getopt(argv, "hpl:d", ["help", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()

    #
    # Process source files
    #
    print "Processing source files..."
    pot.reset()
    modules = ngfw.modules
    for module in modules:
        print "\t" + module
        get_keys(module)

    # Change comments to not have path leading to "ngfw"
    print "Converting comments..."
    pot.load()
    invalid_msg_id_count = 0
    for record in pot.records:
        if record.is_valid_msg_id() == False:
            invalid_msg_id_count = invalid_msg_id_count + 1
            print "Invalid string found:"
            print record
            print 

        for (comment_index, comment) in enumerate(record.comment):
            record.comment[comment_index] = re.sub(ngfw.regex_comment_prefix, "/" + ngfw.path, comment)
    if invalid_msg_id_count > 0:
        print "Invalid string count: %d" % invalid_msg_id_count
    pot.save()

    if os.path.isfile(pot_file_name):
        os.remove(pot_file_name)
    pot.rename(pot_file_name)

if __name__ == "__main__":
    main(sys.argv[1:])
