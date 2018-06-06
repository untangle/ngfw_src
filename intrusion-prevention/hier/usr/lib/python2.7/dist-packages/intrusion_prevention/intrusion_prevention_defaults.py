"""
IntrusionPrevention defaults management
"""
import json

class IntrusionPreventionDefaults:
    """
    Profile defaults
    """
    file_name = "/usr/share/untangle-snort-config/current/templates/defaults.js"

    def __init__(self):
        self.settings = {}

    def load(self, file_name=None):
        """
        Load settings
        """
        if file_name == None:
            file_name = self.file_name
            
        settings_file = open(file_name)
        self.settings = json.load(settings_file)
        settings_file.close()

    def get_profile(self, profile_id):
        """
        Return the desired profile by identifier.
        """
        if ( "profiles" in self.settings.keys() ) == False:
            return None
        for profile in self.settings["profiles"]:
            if profile_id == profile["profileId"]:
                return profile

    def get_categories(self):
        """
        Pull categories tree
        """
        if ( "categories" in self.settings.keys() ) == False:
            return []
        return self.settings["categories"]

    def get_original_category(self, category):
        """
        Map category back to its original.
        """
        if "_" in category:
            return category.split("_")[0]
        else:
            return category
