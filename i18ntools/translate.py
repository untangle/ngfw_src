#!/usr/bin/python3
import os
import getopt
import json
import sys
import glob
import re
import string
import http.client
import urllib.request, urllib.parse, urllib.error
import html.parser

UNTANGLE_DIR = '%s/lib/python' % ( os.path.dirname(os.path.realpath(__file__) ) )
sys.path.insert(0, UNTANGLE_DIR)

import i18n

Languages = i18n.Languages()

# !! add comment entry about source

def process_test(po):
    for (record_index, record) in enumerate(po.records):
        if record.msg_id == "":
            continue
        if record.msg_id == "date_fmt":
            value = 'Y|m|d'
        elif record.msg_id == "thousand_sep":
            value = "."
        elif record.msg_id == "decimal_sep":
            value = ","
        elif record.msg_id == "timestamp_fmt":
            value = "Y|m|d g:i:s a"
        else:
            value = "X" + record.msg_id + "X"
        po.records[record_index].msg_str = [value]

def main(argv):
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
        po.load()
        print("\tProcessing: %s, %s," % (language["name"], po.file_name),)
        print("")
        if language["id"] == "xx":
            process_test(po)
        po.save()

    # otherwise, look at languages source and process


if __name__ == "__main__":
    main(sys.argv[1:])
