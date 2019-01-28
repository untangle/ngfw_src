"""
Suricata signature
"""
import copy
import re

class SuricataSignature:
    """
    Process signature from the suricata format.
    """
    text_regex = re.compile(r'^(?i)([#\s]+|)(alert|log|pass|activate|dynamic|drop|reject|sdrop)\s+(([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+(\-\>|\<\>)\s+([^\s]+)\s+([^\s]+)\s+|)\((.+)\)')
    var_regex = re.compile(r'^\$(.+)')
    custom_gid = 2400

    options_key_regexes = {}

    # For the very rare circumstances where we need to override a signature's enabled value.
    # Currently being used for Suricata version 2.9.2.2 compiled for Jessie.
    # Once we have a newer version and confirm the signature no longer has a problem,
    # remove this particular signature.
    signature_enabled_overrides = [{
        "sid": "403",
        "gid": "116",
        "enabled": False
    }]

    action_changed = False
    network_changed = False

    block_action = "reject"

    def __init__(self, match, category, path=None):
        self.category = category
        self.path = path
        self.custom = path is None
        if match.group(1) and (match.group(1)[0] == "#"):
            self.enabled = False
        else:
            self.enabled = True
        self.action = match.group(2).lower()

        if match.group(3) != "":
            [self.protocol, self.lnet, self.lport, self.direction, self.rnet, self.rport, self.options_raw] = match.group(4, 5, 6, 7, 8, 9, 10)
            self.protocol = self.protocol.lower()
        else:
            [self.protocol, self.lnet, self.lport, self.direction, self.rnet, self.rport] = [None, None, None, None, None, None]
            self.options_raw = match.group(10)

        # Process raw options only for specified keypairs
        self.options = {
            "sid": "-1",
            "gid": "1",
            "classtype": "uncategoried",
            "msg": "",
            "metadata": None
        }

        in_quote = False
        key = None
        for option in self.options_raw.split(';'):
            if in_quote:
                if key in self.options:
                    self.options[key] += option.strip()
                if option.count('"') == 1:
                    in_quote = False
                in_quote = False
            if option.find(':') > -1:
                key, value = option.strip().split(':', 1)
                if key in self.options:
                    if value.count('"') == 1:
                        in_quote = True
                    self.options[key] = value.strip()

        # self.signature_id = self.options["sid"] + "_" + self.options["gid"]
        self.update_signature_id()

        self.initial_action = self.get_action()

    def copy(self):
        """

        Return a copy of this signature

        Returns:
            signature -- Copy of this signature
        """
        new_signature =  copy.deepcopy(self)

        gid = int(new_signature.get_gid())
        if gid < SuricataSignature.custom_gid:
            gid = SuricataSignature.custom_gid
        else:
            gid += 1
        new_signature.set_gid(str(gid))

        return new_signature

    def dump(self):
        """
        print(suricata signature)
        """
        print("signature dump")
        for prop, value in vars(self).iteritems():
            print(prop, ": ", value)

    def set_action(self, log, block):
        """
        Set signature action based on log, block
        """
        if block is True:
            self.action = self.block_action
        else:
            self.action = "alert"

        if log is False and block is False:
            self.enabled = False
        else:
            self.enabled = True

        self.action_changed = True

    def get_action(self):
        """

        Return signautre action

        Returns:
            [String] -- String of action
        """
        action = {
            # "enabled": self.enabled,
            "log": False,
            "block": False
        }

        if self.enabled is False:
            return action

        if self.action == 'drop':
            action["log"] = True
            action["block"] = True
        elif self.action == 'alert':
            action["log"] = True

        return action

    def get_action_changed(self):
        """

        Return if action changed

        Returns:
            boolean -- True of action changed, otherwise False.
        """
        return self.action_changed

    def set_lnet(self, lnet):
        self.lnet = lnet
        self.network_changed = True

    def get_lnet(self):
        return self.lnet

    def set_rnet(self, rnet):
        self.rnet = rnet
        self.network_changed = True

    def get_rnet(self):
        return self.rnet

    def get_network_changed(self):
        return self.network_changed

    def set_options(self, key, value):
        """
        Set options on key with value
        """
        if not key in self.options:
            return

        if self.options[key] is None:
            self.options[key] = value
            new_options_raw = self.options_raw + " " + key + ":" + value + ";"
        else:
            find = key+":"+self.options[key]+";"
            self.options[key] = value

            new_options_raw = self.options_raw.replace(find, key + ":" + value + ";")

        if self.options_raw != new_options_raw:
            self.options_raw = new_options_raw

    def get_options(self, key):
        """
        Set options on key with value
        """
        if not key in self.options:
            return None

        return self.options[key]

    def set_msg(self, msg):
        """
        Set msg
        """
        if msg.startswith('"') and msg.endswith('"'):
            msg = msg[1:-1]
        self.set_options("msg", '"' + msg + '"')

    def get_msg(self):
        """
        Get msg
        """
        return self.options["msg"]

    def set_sid(self, sid):
        """
        Get sid
        """
        self.set_options("sid", sid)
        self.update_signature_id()

    def get_sid(self):
        """
        Get sid
        """
        return self.get_options("sid")

    def set_gid(self, gid):
        """
        Set gid
        """
        self.set_options("gid", gid)
        self.update_signature_id()

    def get_gid(self):
        """
        Get gid
        """
        return self.get_options("gid")

    def get_signature_id(self):
        """
        Get signature id
        """
        return self.signature_id


    def update_signature_id(self):
        """
        Set signature id
        """
        self.signature_id = self.options["sid"] + "_" + self.options["gid"]

    def set_classtype(self, classtype):
        """
        Set classtype
        """
        if classtype.startswith('"') and classtype.endswith('"'):
            classtype = classtype[1:-1]
        self.set_options("classtype", classtype)

    def get_category(self):
        """

        Return signature category.

        Returns:
            string -- Category for this signauture.
        """
        return self.category

    def set_category(self, category):
        """

        Specify category for signature.

        Arguments:
            category {string} -- Category
        """
        self.category = category

    def get_enabled(self):
        """
        Get enabled
        """
        enabled = self.enabled
        if SuricataSignature.signature_enabled_overrides:
            for override in SuricataSignature.signature_enabled_overrides:
                match = True
                for signature_key in override.keys():
                    if signature_key == "enabled":
                        continue
                    if override[signature_key] != self.options[signature_key]:
                        match = False
                        break
                if match:
                    enabled = override["enabled"]
                    break

        return enabled

    def get_metadata(self):
        """
        Get metadata in associative array
        """
        metadata = {}
        if self.options["metadata"] is not None:
            for field in self.options["metadata"].split(','):
                (key, value) = field.strip().split(' ', 1)
                metadata[key] = value
        return metadata

    def set_metadata(self, metadata):
        """
        Set metadata from associative array
        """
        if metadata is not None or metadata:
            fields = []
            for key in metadata:
                fields.append(key + ' ' + metadata[key])
            metadata = ",".join(fields)
        self.set_options("metadata", metadata)

    def match(self, classtypes, categories, signature_ids):
        """
        See if the specified filtering match this signature appropriately.
        If an item is prefixed by a "+" or just named, then match.
        If an item is prefixed with a "-", then don't match.
        """
        if "+" + self.options["classtype"] in classtypes or self.options["classtype"] in classtypes:
            classtype_match = True
        elif "-" + self.options["classtype"] in classtypes:
            return False
        else:
            classtype_match = False

        if "+" + self.category in categories or self.category in categories:
            category_match = True
        elif "+" + self.category in categories:
            return False
        else:
            category_match = False

        if "+" + self.signature_id in signature_ids or self.signature_id in signature_ids:
            signature_id_match = True
        elif "+" + self.signature_id in signature_ids:
            return False
        else:
            signature_id_match = False

        return classtype_match or category_match or signature_id_match

    def build(self):
        """
        Build for suricata.conf usage
        """
        if self.get_enabled() is True:
            enabled = ""
        else:
            enabled = "#"
        if self.protocol is not None:
            protocol = self.protocol + " "
        else:
            protocol = ""
        if self.lnet is not None:
            lnet = self.lnet + " "
        else:
            lnet = ""
        if self.lport is not None:
            lport = self.lport + " "
        else:
            lport = ""
        if self.direction is not None:
            direction = self.direction + " "
        else:
            direction = ""
        if self.rnet is not None:
            rnet = self.rnet + " "
        else:
            rnet = ""
        if self.rport is not None:
            rport = self.rport + " "
        else:
            rport = ""
        return enabled + self.action + " " + protocol + lnet + lport + direction + rnet + rport + "( " + self.options_raw + " )"

    def get_variables(self):
        """

        Get list of variables

        Returns:
            variables -- key pair list of variables
        """
        variables = []
        for prop, value in vars(self).iteritems():
            if isinstance(value, str) is False:
                continue
            match_variable = re.search(SuricataSignature.var_regex, value)
            if match_variable:
                if variables.count(match_variable.group(1)) == 0:
                    variables.append(match_variable.group(1))

        for option in self.options_raw.split(';'):
            option = option.strip()
            if option == "":
                continue
            key = option
            value = None
            if option.find(':') > -1:
                key, value = option.split(':', 1)
                key = key.strip()
                value = value.strip()
                match_variable = re.search(SuricataSignature.var_regex, value)
                if match_variable:
                    if variables.count(match_variable.group(1)) == 0:
                        variables.append(match_variable.group(1))

        return variables
