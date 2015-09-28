"""
Parse jspx file
"""
import re
import os
import xml.etree.ElementTree as ET

from i18n.po_record import PoRecord

class JspxFile:
    """
    Parse jsp file for messages and store as po records
    """
    regex_tag = re.compile(r'^.*\/uvm\}i18n$')

    def __init__(self, file_name=None):
        """
        Init
        """
        self.file_name = file_name
        self.records = []

    def load(self):
        """
        Load and begin processing of children
        """
        if os.path.isfile(self.file_name) == False:
            return

        tree = ET.parse(self.file_name)
        root = tree.getroot()

        self.load_children(root)

    def load_children(self, root):
        """
        Recursively look for i18n tags
        """
        for child in root:
            tag_match = re.search(JspxFile.regex_tag, child.tag)
            if tag_match:
                record = PoRecord(source_file_name=self.file_name)
                record.add_comment(": " + self.file_name)
                record.add_msg_id(child.text)
                self.records.append(record)

            self.load_children(child)

