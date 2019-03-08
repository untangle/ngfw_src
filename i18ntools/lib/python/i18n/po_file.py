"""
PO file management
"""
import re
import os
import sys
import sre_parse

from i18n.po_record import PoRecord
from i18n.utility import Utility

class PoFile:
    """
    Getmsg file
    """
    regex_comment = re.compile(r'^\#(.*)')
    regex_msgid = re.compile(r'^msgid\s+\"(.*)\"')
    regex_msgstr = re.compile(r'^msgstr\s+\"(.*)\"')
    regex_continue = re.compile(r'^\"(.*)\"')

    path = "/po"
    extension = "po"

    def __init__(self, language, file_name=None):
        """
        Init
        """
        self.language = language
        self.records = []
        if file_name != None:
            self.file_name = file_name
        else:
            self.build_file_name()

        self.add_record_statistics = {}

    def build_file_name(self):
        """
        Create filename from language
        """
        self.file_name = Utility.get_base_path() + self.path + "/%s/untangle-%s.%s" % (self.language, self.language, self.extension)

    def set_file_name(self, file_name):
        self.file_name = file_name

    def load(self):
        """
        Read po file and parse into po records
        """
        if os.path.isfile(self.file_name) == False:
            return

        pofile = open(self.file_name)

        self.records = []

        source_record = None
        record_mode = "comment"
        record = PoRecord(source_file_name=self.file_name)
        for line in pofile.readlines():
            line = line.strip()
            if line == "":
                continue

            comment_match = re.search(PoFile.regex_comment, line)
            msgid_match = re.search(PoFile.regex_msgid, line)
            msgstr_match = re.search(PoFile.regex_msgstr, line)
            continue_match = re.search(PoFile.regex_continue, line)

            if (comment_match or msgid_match) and (record_mode == "msgstr"):
                if record.msg_id == "" and source_record == None:
                    source_record = record
                else:
                    record.set_source_record(source_record)

                self.add_record(record)

                record_mode = "msg_id"
                record = PoRecord(source_file_name=self.file_name)

            if comment_match:
                record.add_comment(comment_match.group(1))
                record_mode = "comment"

            if msgid_match:
                if msgid_match.group(1) != "":
                    record.add_msg_id(msgid_match.group(1))
                record_mode = "msgid"

            if msgstr_match:
                if msgstr_match.group(1) != "":
                    record.add_msg_str(msgstr_match.group(1))
                record_mode = "msgstr"

            if continue_match:
                if record_mode == "msgstr":
                    record.add_msg_str(continue_match.group(1))
                elif record_mode == "msgid":
                    record.add_msg_id(continue_match.group(1))
                else:
                    print("Unknown continue mode for match: " + continue_match.group(1))

        record.set_source_record(source_record)
        self.add_record(record)

        pofile.close()

    def add_record(self, new_record, replace_comments=False):
        """
        Append record with checking.
        This does the same as msgmerge but much more for our purpoeses.
        """

        # Perform argument check
        if new_record.arguments_match() == False:
            print("WARNING: Arguments do not match; clearing msgstr")
            print(new_record)
            new_record.msg_str = []

        add_record = True
        replace_index = False
        replace_comment_index = False
        for (record_index, record) in enumerate(self.records):
            if record.msg_id == new_record.msg_id:
                replace_comment_index = record_index
                add_record = False
                if record.msg_id == "":
                    # Ok with overwriting the identifier record
                    break
                if len(record.msg_str) == len(new_record.msg_str):
                    # If string is the same, we're fine
                    matches = True
                    for (index, record_msg_str) in enumerate(record.msg_str):
                        if new_record.msg_str[index] != record.msg_str[index]:
                            matches = False
                    if matches == True:
                        break

                # Match failed.  Can we resolve?

                # One record is empty and the other isnt.
                if len(record.msg_str) == 0 and len(new_record.msg_str) > 0:
                    # Existing message is empty but not this one.  Replace
                    replace_index = record_index
                    break
                elif len(record.msg_str) > 0 and len(new_record.msg_str) == 0:
                    # Existing message is non-empty but new record is.  Keep old.
                    break

                # Look at arguments.  Some translations have them and others do not and
                # those without can break.  If we find that one has argument replacement
                # and the other does not, use the one with argument replacements.
                msgid_arguments_match = re.findall(PoRecord.regex_arguments, record.msg_id)
                if msgid_arguments_match:
                    current_msg_str_arguments_match = re.findall(PoRecord.regex_arguments, record.msg_str[0])
                    new_msg_str_arguments_match = re.findall(PoRecord.regex_arguments, new_record.msg_str[0])
                    if len(current_msg_str_arguments_match) != len(new_msg_str_arguments_match):
                        if len(current_msg_str_arguments_match) == len(msgid_arguments_match):
                            break
                        else:
                            replace_index = record_index
                            break

                # Finally, use whomever has the most recent revision date
                if new_record.get_revision_date() > record.get_revision_date():
                    # Master record is newer; use new record
                    replace_index = record_index
                    break
                else:
                    # We have the latest.
                    break

                # Debug here foe more conflicts
                print("*msgid conflict")
                print("record=")
                print(record.source_file_name)
                print(record)
                print("source record=")
                print(record.source_record.source_file_name)
                print(record.source_record)
                print("")
                print("\nnew record=")
                print(new_record.source_file_name)
                print(new_record)
                print("source record=")
                print(new_record.source_record.source_file_name)
                print(new_record.source_record)
                sys.exit()

        if replace_index is not False:
            self.records[replace_index] = new_record
            if not "replace_count" in self.add_record_statistics:
                self.add_record_statistics["replace_count"] = 0
            self.add_record_statistics["replace_count"] =+ 1
            return new_record
        elif add_record == True:
            self.records.append(new_record)
            if not "add_count" in self.add_record_statistics:
                self.add_record_statistics["add_count"] = 0
            self.add_record_statistics["add_count"] =+ 1
            return new_record
        else:
            if not "ignored_count" in self.add_record_statistics:
                self.add_record_statistics["ignored_count"] = 0
            self.add_record_statistics["ignored_count"] =+ 1

        if replace_comments == True and replace_comment_index is not False:
            self.records[replace_comment_index].set_comment(new_record.comment)

        return None

    def total_record_count(self):
        return len(self.records)

    def updated_record_count(self):
        updated_count = 1
        for record in self.records:
            if record.is_updated() is True:
                updated_count += 1
        return updated_count

    def remove_record(self, remove_record):
        """
        Remove the current record
        """
        for record in self.records:
            if remove_record.msg_id == record.msg_id:
                self.records.remove(record)
                return remove_record
        return None

    def make_directories(self, file_name=None):
        """
        Create directory path
        """
        if file_name == None:
            file_name = self.file_name
        rindex = file_name.rfind("/")
        if rindex != -1:
            path = file_name[0:rindex]
            if os.path.isdir(path) == False:
                os.makedirs(path)

    def save(self):
        """
        Save file
        """
        self.make_directories()

        pofile = open(self.file_name, "w")
        for record in self.records:
            pofile.write(str(record))
            pofile.write("\n")
        pofile.close()

    def reset(self):
        """
        Reset file to zero length.  xgettext gets upset if file does not exist
        """
        if os.path.exists(self.file_name):
            os.remove(self.file_name)

        pofile = open(self.file_name, "w")
        pofile.close()

    def rename(self, new_file_name):
        """
        Rename to new filename
        """
        self.make_directories(new_file_name)
        os.rename(self.file_name, new_file_name)
        self.file_name = new_file_name

    def get_record_by_msgid(self, msg_id):
        """
        Return record by message identifier
        """
        for record in self.records:
            if record.msg_id == msg_id:
                return record
        return None
