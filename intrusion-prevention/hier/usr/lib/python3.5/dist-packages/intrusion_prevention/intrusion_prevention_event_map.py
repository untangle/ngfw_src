"""
IntrusionPrevention event log management
"""
import json

class IntrusionPreventionEventMap:
    """
    IntrusionPrevention event log management
    """
    #
    # NGFW event map management
    #
    file_name = "/etc/suricata/intrusion-prevention.event.map.conf"

    def __init__(self, signatures):
        self.settings = {}
        self.signatures = signatures
        self.create()

    def create(self):
        """
        Create a new settings file based on the processed
        signature set and default variables from suricata configuration.
        """
        self.settings = {
            "javaClass": "com.untangle.app.intrusion_prevention.IntrusionPreventionEventMap",
            "signatures": {
                "javaClass": "java.util.LinkedList",
                "list": []
            }
        }

        for signature in self.signatures.get_signatures().values():
            if signature.options["sid"] == "":
                continue
            msg = signature.options["msg"]
            if msg.startswith('"') and msg.endswith('"'):
                msg = msg[1:-1]

            rid = signature.get_rule()
            if rid is None:
                rid = ""
            else:
                rid = rid.get_id()

            self.settings["signatures"]["list"].append({
                "javaClass" : "com.untangle.app.intrusion_prevention.IntrusionPreventionEventMapSignature",
                "sid": int(signature.options["sid"]),
                "gid": int(signature.options["gid"]),
                "rid": rid,
                "category": signature.category,
                "msg": msg,
                "classtype": signature.options["classtype"],
            })

    def save(self):
        """
        Save event map
        """
        settings_file = open(self.file_name, "w")
        json.dump(self.settings, settings_file, False, True, True, True, None, 0)
        settings_file.close()
