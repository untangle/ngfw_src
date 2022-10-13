#!/usr/bin/python3
import os
import getopt
import sys
import glob
import re
import string
import importlib

UNTANGLE_DIR = '%s/lib/python' % ( os.path.dirname(os.path.realpath(__file__) ) )
sys.path.insert(0, UNTANGLE_DIR)

import i18n

importlib.reload(sys)

languages = i18n.Languages()

def usage():
    print("Usage!")

def main(argv):
    _debug = False
    source_po_file_name = None
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
        elif opt in ( "-d", "--debug"):
            _debug = True
        elif opt in ( "-l", "--language"):
            language_ids = arg.split(",")
        elif opt in ( "-s", "--source"):
            source_ids = arg.split(",")

    for source_id in source_ids:
        for language_id in languages.get_language_ids(source_id):
            if language_ids is not None and language_id not in language_ids:
                continue

            po = i18n.PoFile(language=language_id, source=source_id, file_name=source_po_file_name)
            po.load()

            language = languages.get_by_id(language_id)
            print(f"{source_id:9} {language['name']:20} {po.get_abbreviated_file_name():24}", end='')

            print(f" total={po.total_record_count():<5} updated={po.updated_record_count():<5} completed={float((po.updated_record_count()) / po.total_record_count()) * 100:05.2f}%", flush=True)

if __name__ == "__main__":
    main(sys.argv[1:])
