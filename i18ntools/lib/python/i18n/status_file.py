"""
Status file management
"""
import copy
import json
import os

from i18n.utility import Utility

class StatusFile:
    """
    Output file for server.
    Downloaded and consumed by NGFW to determine available languages
    """
    url_base = None
    url_translate_base = None
    last_author = "translations@translations.edge.arista.com"
    default_last_author = "FULL NAME <EMAIL@ADDRESS>"

    status = {
        "stats": {
        }
    }
    path = "/status"
    extension = "json"

    def __init__(self, source_id="official", path=None):
        """
        Init
        """
        if path != None:
            self.file_name = f"{path}{source_id}.{self.extension}"
        else:
            self.build_file_name(source_id)

    def build_file_name(self, source="official"):
        """
        Create filename from source
        """
        self.file_name = f"{Utility.get_base_path()}{self.path}/{source}.{self.extension}"

    def exists(self):
        return os.path.isfile(self.file_name)

    def add_record(self, language_id, status_record):
        """
        """
        self.status["stats"][language_id] = copy.deepcopy(status_record.get_status())

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

        print(self.file_name)
        with open(self.file_name, 'w', encoding='utf-8') as f:
            json.dump(self.status, f, ensure_ascii=False, indent=4)

