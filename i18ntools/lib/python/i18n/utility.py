"""
i18n related utilty
"""
import os

class Utility:
    """
    Utility class for i81n
    """
    @staticmethod
    def get_base_path(find_path="i18ntools"):
        """
        Pull base path, regardless of current working directory
        """
        find_path = "/" + find_path +"/"
        base_path = None
        full_path = os.path.realpath(__file__)
        full_path_ngfw_index = full_path.find(find_path)
        if full_path_ngfw_index != -1:
            base_path = full_path[0:full_path_ngfw_index + len(find_path) - 1]
        return base_path
