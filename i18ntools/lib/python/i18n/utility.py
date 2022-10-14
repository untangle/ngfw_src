"""
i18n related utilty
"""
import os
import subprocess
import sys

from os.path import expanduser

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
            
        if base_path is None:
            base_path = home = expanduser("~")
            
        return base_path

    @staticmethod
    def run_command(command):
        """
        Execuute a shell command
        """
        try:
            subprocess_command = subprocess.Popen(command, stderr=subprocess.PIPE, stdout=subprocess.PIPE, shell=True, text=True)
            subprocess_command_output = subprocess_command.communicate()[0]
        except:
            if subprocess_command_output != None:
                for output in subprocess_command_output.decode("ascii").split("\n"):
                    if len(output) > 0:
                        print(f"shell_command: command={command} result={output}".format(output=output))
            return False

        return True