"""
NGFW
"""
import json
import os
import re

import sys

from i18n.utility import Utility

class Ngfw:
    """
    NGFW
    """
    file_name = "ngfw.js"
    path = "ngfw"

    directories = []

    def __init__(self):
        """
        Initialize
        """
        self.base_path = Utility.get_base_path(self.path)
        self.load()

        self.search_paths = []
        for directory in self.directories:
            self.search_paths.append(self.base_path + "/" + directory + "/src")
            self.search_paths.append(self.base_path + "/" + directory + "/pkgs")

        self.__dict__["regex_comment_prefix"] = re.compile(r'' + self.base_path)

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
            if key == "json_parse_directories":
                self.__dict__["regex_json_parse"] = re.compile(r"/(" + "|".join(settings[key]) + ")/([^/]+).js$")
        ngfw_file.close()

    def get_module_directory(self, module):
        """
        Determine module directory from name
        """
        module_names = module.split("-", 3)
        if module_names[1] == "libuvm" or module_names[1] == "vm":
            source_directory = "uvm"
        elif module_names[1] == "base" or module_names[1] == "casing":
            source_directory = "-".join(["-".join(module_names[2:]), module_names[1]])
        else:
            source_directory = "-".join(module_names[2:])

        for path in self.search_paths:
            module_directory = path + "/" + source_directory
            if os.path.isdir(module_directory) == True:
                return module_directory

            # Fallback to module name (pkgs)
            module_directory = path + "/" + module
            if os.path.isdir(module_directory) == True:
                return module_directory

        return None
