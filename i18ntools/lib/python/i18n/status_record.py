"""
getmsg record
"""
import re
import datetime
import time

from i18n.languages import Languages


class StatusRecord:
    """
    Status record
    """
    status = {
        "code": None,
        "name": None,
        "translated_percent": 0.0,
        "url": "http://translations.edge.arista.com/engage/ngfw/fr/",
        "url_translate": "http://translations.edge.arista.com/projects/ngfw/official/fr/"
    }

    def __init__(self, source_id="official", po_file=None):
        """
        Init
        """
        self.source_id = source_id
        if po_file is not None:
            self.process_po_file(po_file)

    def process_po_file(self, po_file):
        """
        Using the po file object, build status record
        """
        languages = Languages()
        language = languages.get_by_id(po_file.language_id)
        self.status["code"] = po_file.language_id
        self.status["name"] = language["name"]

        self.status["last_change"] = po_file.get_last_modified()

        self.status["translated_percent"] = int(po_file.updated_record_count() / po_file.total_record_count() * 100)

        self.status["url"] = f"http://translations.edge.arista.com/engage/ngfw/{po_file.language_id}/"
        self.status["url_translate"] = f"http://translations.edge.arista.com/projects/ngfw/{self.source_id}/{po_file.language_id}/"

    def get_status(self):
        """
        Return the status record
        """
        return self.status