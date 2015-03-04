"""
IDPS event log management
"""
import json

class IdpsEventMap:
    """
    IDPS event log management
    """
    #
    # NGFW event map management
    #
    file_name = "/etc/snort/idps.event.map.conf"
    
    def __init__(self, rules):
        self.settings = {}
        self.rules = rules
        self.create()

    def create(self):
        """
        Create a new settings file based on the processed
        rule set and default variables from snort configuration.
        """
        self.settings = { 
            "javaClass": "com.untangle.node.idps.IdpsEventMap",
            "rules": {
                "javaClass": "java.util.LinkedList",
                "list": []
            }
        }
        
        for rule in self.rules.get_rules().values():
            if rule.options["sid"] == "":
                continue
            msg = rule.options["msg"]
            if msg.startswith('"') and msg.endswith('"'):
                msg = msg[1:-1]
            self.settings["rules"]["list"].append( { 
                "javaClass" : "com.untangle.node.idps.IdpsEventMapRule",
                "sid": int(rule.options["sid"]),
                "gid": int(rule.options["gid"]),
                "category": rule.category,
                "msg": msg,
                "classtype": rule.options["classtype"],
            } )
        
    def save(self):
        """
        Save event map
        """
        settings_file = open( self.file_name, "w" )
        json.dump( 
            self.settings, settings_file, 
            False, True, True, True, None, 0 )
        settings_file.close()
