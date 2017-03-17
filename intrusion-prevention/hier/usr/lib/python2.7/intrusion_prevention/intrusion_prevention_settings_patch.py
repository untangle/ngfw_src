"""
IntrusionPrevention settings patch management
"""
import json

class IntrusionPreventionSettingsPatch:
    """
    Simple stub for managing patches
    """
    def __init__(self):
        self.settings = {}

    def load(self, file_name=None):
        """
        Load patch
        """
        settings_file = open(file_name)
        self.settings = json.load(settings_file)
        settings_file.close()