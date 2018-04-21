#!/usr/bin/python
import os
import getopt
import sys
import glob
import re
import string

UNTANGLE_DIR = '%s/lib/python' % ( os.path.dirname(os.path.realpath(__file__) ) )
sys.path.insert(0, UNTANGLE_DIR)

import i18n

reload(sys)  
sys.setdefaultencoding('utf8')

languages = i18n.Languages()

def usage():
	print("Usage!")

def main(argv):
    _debug = False
    source_po_file_name = None
    language_ids = languages.get_enabled_ids()

    try:
        opts, args = getopt.getopt(argv, "hsl:d", ["help", "language=", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-d", "--debug"):
            _debug = True
        elif opt in ( "-l", "--language"):
            language_id = arg.split(",")

    for language_id in language_ids:
        po = i18n.PoFile(language=language_id, file_name=source_po_file_name)
        po.load()

        language = languages.get_by_id(language_id)
        print("Status: %s" % (language["name"]))
        print(" file=%s," % (po.file_name))

        print(" total records=%d, updated_records=%d, completed=%2.2f%%" % ((po.total_record_count(), po.updated_record_count(), (float(po.updated_record_count()) / po.total_record_count()) * 100)))

if __name__ == "__main__":
    main(sys.argv[1:])
