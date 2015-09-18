"""
Supported languages
"""

import json
import sys


from i18n.utility import Utility

class Languages:
    """
    Languages
    """
    file_name = "languages.js"

    def __init__(self):
        """
        Init
        """
        self.load()

    def load(self):
        """
        Load configuration
        """
        full_file_name = Utility.get_base_path() + "/" + self.file_name
        ngfw_file = open(full_file_name)
        try:
            settings = json.load(ngfw_file)
        except:
            print "\n".join(str(v) for v in sys.exc_info())
        for key in settings:
            self.__dict__[key] = settings[key]
        ngfw_file.close()

    def get_enabled(self):
        """
        Return enabled languae records
        """
        enabled = []
        for language in self.languages:
            if language["enabled"] == True:
                enabled.append(language)
        return enabled
