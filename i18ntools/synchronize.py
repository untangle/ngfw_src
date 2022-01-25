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

from subprocess import call
import importlib

UNTANGLE_DIR = '%s/lib/python' % (os.path.dirname(os.path.realpath(__file__)))
sys.path.insert(0, UNTANGLE_DIR)

import i18n

importlib.reload(sys)  
sys.setdefaultencoding('utf8')

ngfw = i18n.Ngfw()
languages = i18n.Languages()

pot_file_name = "pot/en/untangle-en.pot"
pot = i18n.PotFile(language="en", file_name=pot_file_name)

def main(argv):
    """
    Main entry for generate
    """
    global pot
    language_ids = languages.get_enabled_ids()

    try:
        opts, args = getopt.getopt(argv, "hpl:d", ["help", "language=", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-l", "--language"):
            language_ids = arg.split(",")

    if os.path.isfile(pot_file_name) is False:
        print("Missing template file %s" % (pot_file_name))
        sys.exit(1)

    pot.load()

    print("Synchronizing po languages...")
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
        print("  Synchronizing: %s, %s," % (language["name"], po.file_name),)

        for diff_record in diff["remove"]:
            po.remove_record(diff_record)

        ## Add new and synchronize comments for existing
        for record in pot.records:
            po.add_record(record, replace_comments=True)

        print("%d added, %d removed" % (len(diff["add"]), len(diff["remove"])),)

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
        print(", %d/%d chars/words to translate" % (character_count, word_count))
        total_character_count = total_character_count + character_count
        total_word_count = total_word_count + word_count

        po.save()

    print("%d/%d chars/words total to translate" % (total_character_count, total_word_count))

if __name__ == "__main__":
    main(sys.argv[1:])
