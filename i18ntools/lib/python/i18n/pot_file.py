"""
Gettext template file
"""

from i18n.po_file import PoFile

class PotFile(PoFile):
    """
    Gettext template file
    """
    path = "/pot"
    extension = "pot"

#    def __init__(self, language="en", file_name=None):
#        """
#        Extend, moditfy to reflect our extension
#        """
#        PoFile.__init__(self, language, file_name)
#        if self.file_name.endswith(".po"):
#            self.file_name = self.file_name[:-3] + ".pot"
