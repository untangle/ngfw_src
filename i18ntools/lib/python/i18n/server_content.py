import os
import shutil
from i18n.utility import Utility

class ServerContent:
    path = None
    name = None
    """
    """
    def __init__(self, output_path):
        """
        Init
        """
        self.path = f"{Utility.get_base_path()}/{output_path}/"
        self.name = output_path.split('/')[-1]
        if os.path.exists(self.path):
            shutil.rmtree(self.path)
        os.makedirs(self.path)

    def get_path(self):
        """
        """
        return self.path

    def package(self):
        """
        """
        Utility.run_command(f"tar -czf {self.name}.tgz {self.path}*")
