#!/usr/bin/python
"""
Collect existing localization into single files by languages and perform
error checking for non-matching strings for the the same key.
It's only meant to be run once but kept here for informational purposes.
"""
import getopt
import os
import sys
import glob

UNTANGLE_DIR = '%s/lib/python' % (os.path.dirname(os.path.realpath(__file__)))
sys.path.insert(0, UNTANGLE_DIR)

import i18n

reload(sys)  
sys.setdefaultencoding('utf8')

languages = i18n.Languages()

def main(argv):
    """
    Main entry point for collate
    """
    base_path = None

    try:
        opts, args = getopt.getopt(argv, "hb:d", ["help", "base_path=", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)
    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        elif opt in ( "-b", "--base_path"):
            base_path = arg
        elif opt in ( "-l", "--language"):
            language = arg

    ngfw = i18n.Ngfw(base_path=base_path)

    #
    # Don't rely on modules; just pull all po files.
    #
    po_base_paths = []
    for search_path in ngfw.search_paths:
        for po_path in glob.glob(search_path + "/*/po"):
            po_base_paths.append(po_path)

    po_paths = []
    for po_base_path in po_base_paths:
        for root, dirs, files in os.walk(po_base_path):
            if "/i18ntools/" in root:
            	# Ignore ourselves just in case we happen to have directory
                continue
            if '.svn' in dirs:
                dirs.remove(".svn")
            if 'xx' in dirs:
                dirs.remove("xx")
            if len(dirs) == 0:
                continue
            for path in dirs:
                po_paths.append(po_base_path + "/" + path)

    collated_po_files = {}
    for language in languages.get_enabled():
        collated_po_files[language["id"]] = i18n.PoFile(language["id"])

    #
    # Read individual po files and then add records into collated
    #
    language_po_total_record_counts = {}
    for po_path in po_paths:
        language = po_path[po_path.rfind("/")+1:]
        if language == "ga":
            continue
        for po_file_name in glob.glob(po_path+"/*.po"):
            print(po_file_name,)
            po_file = i18n.PoFile(language, po_file_name)
            po_file.load()
            print("records=%d" % (len(po_file.records)))
            if not language in language_po_total_record_counts:
                language_po_total_record_counts[language] = 0
            language_po_total_record_counts[language] += len(po_file.records)
            for record in po_file.records:
                collated_po_files[language].add_record(record)

    print("Results:")
    for language in language_po_total_record_counts:
        print("language=%s, total_records=%d" % (language, language_po_total_record_counts[language]))

    #
    # Save collated files
    #
    for collated_po_file in collated_po_files.values():
        if collated_po_file.language == "en":
            continue
        if collated_po_file.language == "test":
            continue
        print("file=%s, non-duplicate records=%d" % (collated_po_file.file_name, len(collated_po_file.records)))
        collated_po_file.save()

if __name__ == "__main__":
    main(sys.argv[1:])
