#!/usr/bin/python3
"""
Synchronize template (.pot) with existing .po files
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

ngfw = i18n.Ngfw()
languages = i18n.Languages()

pot_file_name = "pot/en/untangle-en.pot"
pot = i18n.PotFile(language="en", file_name=pot_file_name)

def main(argv):
    """
    Main entry for generate
    """
    global pot
    source_ids = languages.get_source_ids()
    language_ids = None

    try:
        opts, args = getopt.getopt(argv, "hsl:s:d", ["help", "language=", "source=", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-l", "--language"):
            language_ids = arg.split(",")
        elif opt in ( "-s", "--source"):
            source_ids = arg.split(",")

    if os.path.isfile(pot_file_name) is False:
        print("Missing template file %s" % (pot_file_name))
        sys.exit(1)

    pot.load()

    print("Synchronizing po languages...")
    total_character_count = 0
    total_word_count = 0
    for source_id in source_ids:
        for language_id in languages.get_language_ids(source_id):
            if language_ids is not None and language_id not in language_ids:
                continue
            if language_id == "en":
                continue
        
            # print(f"{source_id}{language_id}")
            language = languages.get_by_id(language_id)

            po = i18n.PoFile(source=source_id, language=language_id)
            po.load()
            print(f"{source_id:9} {language['name']:20} {po.get_abbreviated_file_name():24}", end='')

            language = languages.get_by_id(language_id)
            diff = {
                "add": [x for x in pot.records if not po.get_record_by_msgid(x.msg_id)],
                "remove": [x for x in po.records if not pot.get_record_by_msgid(x.msg_id)]
            }

            for diff_record in diff["remove"]:
                po.remove_record(diff_record)

            ## Add new and synchronize comments for existing
            for record in pot.records:
                po.add_record(record, replace_comments=True)

            print(f" added={len(diff['add']):<5} removed={len(diff['remove']):<5}", flush=True)

            po.save()

if __name__ == "__main__":
    main(sys.argv[1:])
