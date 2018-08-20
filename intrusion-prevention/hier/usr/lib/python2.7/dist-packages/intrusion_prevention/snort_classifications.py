"""
IntrusionPrevention defaults management
"""
import re

class SnortClassifications:
    """
    Profile defaults
    """
    file_name = "/usr/share/untangle-snort-config/current/rules/classification.config"
    classification_regex = re.compile(r'^config classification: ([^,]+),([^,]+),(\d+)')
    reserved_id_regex = re.compile(r'^reserved_classification_')

    rules = {}

    defaults = {
        "1": {
            "description": "Critical",
            "action": "blocklog"
        },
        "2": {
            "description": "High",
            "action": "blocklog"
        },
        "3": {
            "description": "Medium",
            "action": "log"
        },
        "4": {
            "description": "Low",
            "action": "default"
        },
    }

    def __init__(self):
        self.settings = {}

    def load(self, file_name=None):
        """
        Load settings
        """
        if file_name == None:
            file_name = self.file_name
            
        classification_file = open(file_name)
        for line in classification_file:
            line = line.rstrip( "\n" )
            match_classification = re.search( SnortClassifications.classification_regex, line )
            if match_classification:
                # print match_classification
                # print match_classification.group(3,1)
                self.set_rule(match_classification.group(3), match_classification.group(1))
        classification_file.close()

    def set_rule(self, priority, classtype):
        if priority in self.rules:
            self.rules[priority]['conditions']['list'][0]['value'].append(classtype)
        else:
            self.rules[priority] = {
                "id": "reserved_classification_" + priority,
                "enabled": False,
                "description": "Severity {0}".format(priority),
                "action": "default",
                "conditions": {
                    "list": [{
                        "javaClass": "java.util.LinkedList",
                        "type": "CLASSTYPE",
                        "comparator": "=",
                        "value": [classtype]
                    }]
                }
            }
            if priority in self.defaults:
                self.rules[priority].update(self.defaults[priority])

    def get_rules(self):
        return self.rules.values()