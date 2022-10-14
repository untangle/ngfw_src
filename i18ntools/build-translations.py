#!/usr/bin/python3
"""
Build for translations:
    - Compile .po to .mo files.
    - Update source json files.
    - Package tarball for translations server.
"""
import getopt
import os
import sys

from subprocess import call
import importlib

UNTANGLE_DIR = '%s/lib/python' % (os.path.dirname(os.path.realpath(__file__)))
sys.path.insert(0, UNTANGLE_DIR)

import i18n

importlib.reload(sys)

ngfw = i18n.Ngfw()
languages = i18n.Languages()

pot_file_name = "pot/en/untangle-en.pot"
pot = i18n.PotFile(language_id="en", file_name=pot_file_name)

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

    # create target directories
    server_content = i18n.ServerContent("translations")

    print("Convert po to mo...")
    for source_id in source_ids:
        status_file = i18n.StatusFile(source_id, server_content.get_path())
        for language_id in languages.get_language_ids(source_id):
            if language_ids is not None and language_id not in language_ids:
                continue
            if language_id == "xx" or language_id == "en":
                continue
        
            language = languages.get_by_id(language_id)

            po = i18n.PoFile(source_id=source_id, language_id=language_id)
            print(f"{source_id:9} {language['name']:20} {po.get_abbreviated_file_name():24}", flush=True)
            po.load()
            po.package(server_content.get_path())

            status_record = i18n.StatusRecord(source_id, po)
            status_file.add_record(language_id, status_record)

        status_file.save()

    # Package for distribution
    server_content.package()

if __name__ == "__main__":
    main(sys.argv[1:])
