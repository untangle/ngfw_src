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
Languages = i18n.Languages()

def main(argv):
    """
    Main entry for generate
    """
    language_ids = Languages.get_enabled_ids()

    try:
        opts, args = getopt.getopt(argv, "hl:d", ["help", "languages=", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-l", "--languages"):
            language_ids = arg.split(",")

    for id in language_ids:
        language = Languages.get_by_id(id)
        po = i18n.PoFile(language=language["id"])
        print("\tProcessing: %s, %s," % (language["name"], po.file_name))
        po.load()
        print("")
        print("record count=%d" % (len(po.records)),)
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

if __name__ == "__main__":
    main(sys.argv[1:])
