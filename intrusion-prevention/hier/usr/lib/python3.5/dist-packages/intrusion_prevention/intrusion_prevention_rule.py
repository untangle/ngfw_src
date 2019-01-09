"""
Intrusion Prevention Rule
"""
import re

class IntrusionPreventionRule:
    """
    Instance of Intrusion Prevention rule

    Manage a rule

    Variables:
        global_values List -- Shared global values.
        ipv4NetworkRegex regex -- Match IPv4 address
    """

    global_values = {}

    ipv4NetworkRegex = re.compile(r'((\d{1,3}\.){3,3}\d{1,3})(\/(\d{1,2}|)|)')

    def __init__(self, settingsRule):
        self.rule = settingsRule

        if IntrusionPreventionRule.global_values is None:
            IntrusionPreventionRule.build_global_values()

    @staticmethod
    def build_global_values():
        """
        Define globally used values.
        """
        IntrusionPreventionRule.global_values = {}
        meminfo = open("/proc/meminfo")
        for line in meminfo:
            if "MemTotal:" in line:
                value = re.split(' +', line)[1]
                IntrusionPreventionRule.global_values["SYSTEM_MEMORY"] = int(value) * 1024

    def get_enabled(self):
        """
        Get rule's enabled status.

        Returns:
            Boolean -- True if enabled, otherwise False.
        """
        return self.rule["enabled"]

    def get_action(self):
        """
        Get rule's action.

        Returns:
            String of action.
        """
        return self.rule["action"]

    def matches(self, signature):
        """

        Determine of this rule matches the specified signature.

        Arguments:
            signature {SuricataSignature} -- Signature to test.

        Returns:
            bool -- True if rule matches, otherwise False.
        """
        if not self.rule["enabled"]:
            return False

        match = True
        for condition in self.rule["conditions"]["list"]:
            # print("condition")
            # print(condition)

            comparator = condition['comparator']
            target_value = condition["value"]

            if condition["type"] == "SID":
                match = self.matches_numeric(int(signature.options["sid"]), comparator, int(target_value))
            elif condition["type"] == "GID":
                match = self.matches_numeric(int(signature.options["gid"]), comparator, int(target_value))
            elif condition["type"] == "ID":
                match = self.matches_text(signature.signature_id, comparator, target_value)
            elif condition["type"] == "CATEGORY":
                if not isinstance(condition["value"], list):
                    condition["value"] = condition["value"].split(',')
                match = self.matches_in(signature.category, comparator, condition["value"])
            elif condition["type"] == "CLASSTYPE":
                if not isinstance(condition["value"], list):
                    condition["value"] = condition["value"].split(',')
                match = self.matches_in(signature.options["classtype"], comparator, condition["value"])
            elif condition["type"] == "ACTION":
                if not isinstance(condition["value"], list):
                    condition["value"] = condition["value"].split(',')
                action = signature.action
                if signature.enabled is False:
                    action = "disable"
                elif action == "alert":
                    action = "log"
                elif action in ["reject", "drop"]:
                    action = "block"
                match = self.matches_in(action, comparator, condition["value"])
            elif condition["type"] == "MSG":
                match = self.matches_text(signature.options["msg"].lower(), comparator, target_value.lower())
            elif condition["type"] == "PROTOCOL":
                if not isinstance(condition["value"], list):
                    condition["value"] = condition["value"].split(',')
                match = self.matches_in(signature.protocol, comparator, condition["value"])
            elif condition["type"] == "SRC_ADDR":
                match = self.matches_network(signature.lnet.lower(), comparator, target_value.lower())
            elif condition["type"] == "SRC_PORT":
                match = self.matches_port(signature.lport.lower(), comparator, target_value.lower())
            elif condition["type"] == "DST_ADDR":
                match = self.matches_network(signature.rnet.lower(), comparator, target_value.lower())
            elif condition["type"] == "DST_PORT":
                match = self.matches_port(signature.rport.lower(), comparator, target_value.lower())
            elif condition["type"] == "SYSTEM_MEMORY":
                match = self.matches_numeric(int(IntrusionPreventionRule.global_values["SYSTEM_MEMORY"]), comparator, int(target_value))
            elif condition["type"] == "SIGNATURE":
                match = self.matches_text(signature.build().lower(), comparator, target_value.lower())
            elif condition["type"] == "CUSTOM":
                match = self.matches_in(signature.custom, comparator, [True if target_value.lower() == "true" else False])
            else:
                ### exception
                print("UNKNOWN")
                match = False
                break

            # print("match result=")
            # print(match)

            if match is False:
                return False

        return match

    def matches_numeric(self, source_value, comparator, target_value):
        """
        Perform numeric comparison

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            target_value {[type]} -- [description]

        Returns:
            boolean -- True if matches, otherwise False
        """
        result = False
        if comparator == "=":
            result = source_value == target_value
        elif comparator == "!=":
            result = source_value != target_value
        elif comparator == "<=":
            result = source_value <= target_value
        elif comparator == "<":
            result = source_value < target_value
        elif comparator == ">":
            result = source_value > target_value
        elif comparator == ">=":
            result = source_value >= target_value
        elif comparator == "substr":
            result = str(target_value) in str(source_value)
        elif comparator == "!substr":
            result = str(target_value) not in str(source_value)

        return result

    def matches_in(self, source_value, comparator, target_value):
        """
        Perform match in list.

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            target_value {[type]} -- [description]

        Returns:
            boolean -- True if source_value is in target_value, otherwise False
        """
        is_in = source_value in (( value.lower() if hasattr(value, "lower") else value) for value in target_value)

        result = False
        if comparator == "=":
            result = is_in
        elif comparator == "!=":
            result = not is_in

        return result

    def matches_text(self, source_value, comparator, target_value):
        """
        Perform text match

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            target_value {[type]} -- [description]

        Returns:
            boolean -- True if match, otherwise False
        """
        result = False
        if comparator == "=":
            result = source_value == target_value
        elif comparator == "!=":
            result = source_value != target_value
        elif comparator == "substr":
            result = target_value in source_value
        elif comparator == "!substr":
            result = not target_value in source_value

        return result

    def address_to_bits(self, address):
        """
        Get IP address as string of bits

        Arguments:
            address {String} -- String of IP address
        """
        return ''.join('{:08b}'.format(int(x)) for x in address.split('.'))

    def matches_network(self, source_value, comparator, target_value):
        """
        Perform network match

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            target_value {[type]} -- [description]

        Returns:
            boolean -- True if match, otherwise False
        """
        equal_comparator = comparator[-1] == '='

        source_values = []
        if source_value[0] == '[':
            source_values = re.split(r'\s*,\s*', source_value[1:-1])
        else:
            source_values.append(source_value)

        target_prefix = 32
        match_signature = re.search(IntrusionPreventionRule.ipv4NetworkRegex, target_value)
        if equal_comparator and match_signature:
            if match_signature.group(4):
                target_prefix = int(match_signature.group(4))
            target_value = self.address_to_bits(match_signature.group(1))[:target_prefix]

        record = None
        for value in source_values:
            match_value = value

            if equal_comparator:
                match_signature = re.search(IntrusionPreventionRule.ipv4NetworkRegex, value)
                if match_signature:
                    match_value = self.address_to_bits(match_signature.group(1))[:target_prefix]

                if match_value == target_value:
                    record = value
                    break
            else:
                if target_value in value:
                    record = value

        result = False
        if comparator == "=":
            result = record is not None
        elif comparator == "!=":
            result = record is None
        elif comparator == "substr":
            result = record is not None
        elif comparator == "!substr":
            result = record is None

        return result

    def matches_port(self, source_value, comparator, target_value):
        """
        Perform port match

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            target_value {[type]} -- [description]

        Returns:
            boolean -- True if match, otherwise False
        """
        equal_comparator = comparator[-1] == '='

        source_values = []
        if source_value[0] == '[':
            source_values = re.split(r'\s*,\s*', source_value[1:-1])
        else:
            source_values.append(source_value)

        record = None
        for value in source_values:
            match_value = value

            if equal_comparator:
                if match_value == target_value:
                    record = value
                    break
            else:
                if target_value in value:
                    record = value

        result = False
        if comparator == "=":
            result = record is not None
        elif comparator == "!=":
            result = record is None
        elif comparator == "substr":
            result = record is not None
        elif comparator == "!substr":
            result = record is None

        return result

    def set_signature_action(self, signature):
        """
        Set action on signature.

        Arguments:
            signature {SuricataSignature} -- Signature to set
        """
        current_action = signature.get_action()
        if self.rule["action"] == "default":
            signature.set_action(current_action["log"], current_action["block"])
        elif self.rule["action"] == "log":
            signature.set_action(True, False)
        elif self.rule["action"] == "blocklog":
            if signature.get_action()["log"] is True or signature.get_action()["block"] is True:
                signature.set_action(True, True)
            else:
                signature.set_action(False, False)
        elif self.rule["action"] == "block":
            signature.set_action(True, True)
        elif self.rule["action"] == "disable":
            signature.set_action(False, False)
