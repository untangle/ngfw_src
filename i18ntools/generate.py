#!/usr/bin/python
"""
Rebuild gettext template file (.pot) and 
synchronize with existing po files
"""
import datetime
import fnmatch
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
        for file_name in fnmatch.filter(file_names, '*.js'):
            full_file_name = root + "/" + file_name

            report_event_match = re.search(ngfw.regex_json_parse, full_file_name)
            if report_event_match:
                report_file = open(full_file_name)
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
            else:
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
            call(
                "ruby " + ngfw.base_path + "/work/src/i18ntools/xi18ntags.rb " +full_file_name + " >> " + pot.file_name,
                shell=True
            )

def main(argv):
    """
    Main entry for generate
    """
    global pot

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
    for record in pot.records:
        for (comment_index, comment) in enumerate(record.comment):
            record.comment[comment_index] = re.sub(ngfw.regex_comment_prefix, "/" + ngfw.path, comment)
    pot.save()

    print "Synchronizing po files..."
    total_character_count = 0
    total_word_count = 0
    for language in languages.get_enabled():
        if language["id"] == "en":
            continue

        po = i18n.PoFile(language=language["id"])
        po.load()

        diff = {
            "add": [x for x in pot.records if not po.get_record_by_msgid(x.msg_id)],
            "remove": [x for x in po.records if not pot.get_record_by_msgid(x.msg_id)]
        }
        print "\tSynchronizing: %s, %s," % (language["name"], po.file_name),

        for diff_record in diff["remove"]:
            po.remove_record(diff_record)

        ## Add new and synchronize comments for existing
        for record in pot.records:
            po.add_record(record, replace_comments=True)

        print "%d added, %d removed" % (len(diff["add"]), len(diff["remove"])),

        character_count = 0
        word_count = 0
        for record in po.records:
            if record.msg_id == "":
                now = datetime.datetime.now()
                record.replace_msg_str("PO-Revision-Date:", "PO-Revision-Date: " + now.strftime("%Y-%m-%d %H:%M%z") + '\\n')
            record.set_verified()
            if len("".join(record.msg_str)) == 0:
                character_count = character_count + len(record.msg_id)
                word_count = word_count + len(re.findall(r'\w+', record.msg_id))
        print ", %d/%d chars/words to translate" % (character_count, word_count)
        total_character_count = total_character_count + character_count
        total_word_count = total_word_count + word_count

        po.save()

    print "%d/%d chars/words total to translate" % (total_character_count, total_word_count)

    if os.path.isfile(pot_file_name):
        os.remove(pot_file_name)
    pot.rename(pot_file_name)

if __name__ == "__main__":
    main(sys.argv[1:])
