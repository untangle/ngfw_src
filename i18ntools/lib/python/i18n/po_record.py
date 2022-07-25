"""
getmsg record
"""
import re
import datetime
import time

class PoRecord:
    """
    getmsg record
    """
    regex_arguments = re.compile(r'({\d+})')

    regex_starts_with_whitespace = re.compile(r'^\s+');
    regex_ends_with_whitespace = re.compile(r'\s+$');

    unverified = ": Status: UNVERIFIED"

    regex_last_revision_date = re.compile(r'PO-Revision-Date:\s+(.+)\\n')
    parse_date_formats = [
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%d %H:%M",
    ]
    regex_content_type = re.compile(r'Content-Type:\s+text/plain;\s+charset=([^\\]+)\\n')

    def __init__(self, source_file_name=None):
        """
        Init
        """
        self.msg_id = ""
        self.msg_str = []
        self.comment = []
        if source_file_name != None:
            self.source_file_name = source_file_name
        else:
            self.source_file_name = ""
        self.source_record = None

    def add_msg_id(self, msg_id):
        """
        Set message identifier
        """
        self.msg_id += msg_id

    def add_msg_str(self, msg_str):
        """
        Add to message str
        """
        self.msg_str.append(msg_str)

        if self.msg_id == "":
            for (msgstr_index, msgstr) in enumerate(self.msg_str):
                content_type_match = re.findall(PoRecord.regex_content_type, msgstr)
                if len(content_type_match) > 0 and content_type_match[0] == "CHARSET":
                    self.msg_str[msgstr_index] = re.sub("CHARSET", "UTF-8", msgstr)

    def replace_msg_str(self, prefix, replace):
        """
        Replace all of msg_str
        """
        for (msg_str_index, msg_str) in enumerate(self.msg_str):
            if msg_str.startswith(prefix) == True:
                self.msg_str[msg_str_index] = replace
                break

    def set_comment(self, comment):
        """
        Set the comment
        """
        self.comment = comment

    def add_comment(self, comment):
        """
        Add to comment
        """
        self.comment.append(comment)

    def is_valid_msg_id(self):
        valid = True
        if re.search(PoRecord.regex_starts_with_whitespace, self.msg_id):
            valid = False
        if re.search(PoRecord.regex_ends_with_whitespace, self.msg_id):
            valid = False

        return valid

    def set_verified(self):
        """
        Always want unverified comment at end of source comments
        """
        if len("".join(self.msg_str)) == 0:
            verified = False
        else:
            verified = True

        unverified_found = False
        for (comment_index, comment) in enumerate(self.comment):
            if comment == self.unverified:
                del self.comment[comment_index]
                unverified_found = True
                break

        if verified == False or unverified_found == True:
            self.comment.append(self.unverified)

    def arguments_match(self):
        """
        Verify that if msgid containts arguments, msgstr matches
        """
        if len("".join(self.msg_str)) == 0:
            return True
        msgid_arguments_match = re.search(PoRecord.regex_arguments, self.msg_id)
        if not msgid_arguments_match:
            return True

        if re.findall(PoRecord.regex_arguments, self.msg_id) == re.findall(PoRecord.regex_arguments, "".join(self.msg_str)):
            return True
        else:
            return False

    def set_source_record(self, record):
        """
        Set the source record
        """
        self.source_record = record

    def get_revision_date(self):
        default_revision_date = time.strptime("1970","%Y")

        if self.source_record != None:
            headers = self.source_record.msg_str
        elif self.msg_id != "":
            headers = record.msg_str
        else:
            return default_revision_date

        for header in headers:
            last_revision_match = re.findall(PoRecord.regex_last_revision_date, header)
            if last_revision_match:
                last_revision_date = last_revision_match[0]
                tz_strip_space_pos = last_revision_date.rindex(" ")
                for sep in ["-", "+"]:
                    tz_strip_char_pos = last_revision_date.rindex(sep)
                    if tz_strip_char_pos and (tz_strip_char_pos > tz_strip_space_pos):
                        last_revision_date = last_revision_date[0:tz_strip_char_pos]
                        break

                for date_format in self.parse_date_formats:
                    try:
                        revision_date = datetime.strptime(last_revision_date, date_format)
                        break
                    except:
                        revision_date = default_revision_date
                break;

        return revision_date

    def is_updated(self):

        if len("".join(self.msg_str)) == 0:
            return False

        if "".join(self.msg_str) == self.msg_id:
            return False

        return True

    def __str__(self):
        """
        String representation for output
        """
        string_record = ""
        if len(self.comment):
            string_record += "\n#" + "\n#".join(self.comment)

        string_record += "\nmsgid \"" + self.msg_id + "\""

        if len(self.msg_str) == 0:
            string_record += "\nmsgstr \"\""
        elif len(self.msg_str) == 1:
            string_record += "\nmsgstr \"" + self.msg_str[0] + "\""
        else:
            string_record += "\nmsgstr \"\"\n"
            for msg_str in self.msg_str:
                string_record += "\"" + msg_str + "\"" + "\n"
        return str(string_record)
