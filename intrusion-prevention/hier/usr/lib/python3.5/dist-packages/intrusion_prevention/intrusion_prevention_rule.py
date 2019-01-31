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

        if not IntrusionPreventionRule.global_values:
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

    def get_id(self):
        """

        Return rule id

        Returns:
            String -- rule identifier.
        """
        return self.rule["id"]

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
            condition_value = condition["value"]

            if condition["type"] == "SID":
                match = self.matches_numeric(int(signature.options["sid"]), comparator, condition_value)
            elif condition["type"] == "GID":
                ## stop parsing condition_value
                match = self.matches_numeric(int(signature.options["gid"]), comparator, condition_value)
            elif condition["type"] == "ID":
                match = self.matches_text(signature.signature_id, comparator, condition_value)
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
                match = self.matches_text(signature.options["msg"].lower(), comparator, condition_value.lower())
            elif condition["type"] == "PROTOCOL":
                if not isinstance(condition["value"], list):
                    condition["value"] = condition["value"].split(',')
                match = self.matches_in(signature.protocol, comparator, condition["value"])
            elif condition["type"] == "SRC_ADDR":
                match = self.matches_network(signature.lnet.lower(), comparator, condition_value.lower())
            elif condition["type"] == "SRC_PORT":
                match = self.matches_port(signature.lport.lower(), comparator, condition_value.lower())
            elif condition["type"] == "DST_ADDR":
                match = self.matches_network(signature.rnet.lower(), comparator, condition_value.lower())
            elif condition["type"] == "DST_PORT":
                match = self.matches_port(signature.rport.lower(), comparator, condition_value.lower())
            elif condition["type"] == "SYSTEM_MEMORY":
                ## stop parsing condition_value
                match = self.matches_numeric(IntrusionPreventionRule.global_values["SYSTEM_MEMORY"], comparator, condition_value)
            elif condition["type"] == "SIGNATURE":
                match = self.matches_text(signature.build().lower(), comparator, condition_value.lower())
            elif condition["type"] == "CUSTOM":
                match = self.matches_in(signature.custom, comparator, [True if condition_value.lower() == "true" else False])
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

    def matches_numeric(self, source_value, comparator, condition_value):
        """
        Perform numeric comparison

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            condition_value {[type]} -- [description]

        Returns:
            boolean -- True if matches, otherwise False
        """
        result = False

        if ',' in condition_value:
            condition_values = [int(x) for x in condition_value.split(",")]
            result = self.matches_in(source_value, comparator, condition_values)
        elif '-' in condition_value:
            [condition_start_value, condition_stop_value] = [int(x) for x in condition_value.split("-")]
            result = self.matches_in(source_value, comparator, condition_start_value, condition_stop_value)
        else:
            condition_value = int(condition_value)
            if comparator == "=":
                result = source_value == condition_value
            elif comparator == "!=":
                result = source_value != condition_value
            elif comparator == "<=":
                result = source_value <= condition_value
            elif comparator == "<":
                result = source_value < condition_value
            elif comparator == ">":
                result = source_value > condition_value
            elif comparator == ">=":
                result = source_value >= condition_value
            elif comparator == "substr":
                result = str(condition_value) in str(source_value)
            elif comparator == "!substr":
                result = str(condition_value) not in str(source_value)

        return result

    def matches_in(self, source_value, comparator, condition_start_value, condition_stop_value=None):
        """
        Perform match in list.

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            condition_start_value {[type]} -- [description]
            condition_stop_value {[type]} -- [description]

        Returns:
            boolean -- True if source_value is in condition_value, otherwise False
        """
        is_in = False
        if condition_stop_value is not None:
            is_in = int(condition_start_value) <= source_value <= int(condition_stop_value)
        else:
            is_in = source_value in ((value.lower() if hasattr(value, "lower") else value) for value in condition_start_value)

        result = False
        if comparator == "=":
            result = is_in
        elif comparator == "!=":
            result = not is_in

        return result

    def matches_text(self, source_value, comparator, condition_value):
        """
        Perform text match

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            condition_value {[type]} -- [description]

        Returns:
            boolean -- True if match, otherwise False
        """
        result = False
        if comparator == "=":
            result = source_value == condition_value
        elif comparator == "!=":
            result = source_value != condition_value
        elif comparator == "substr":
            result = condition_value in source_value
        elif comparator == "!substr":
            result = not condition_value in source_value

        return result

    def address_to_bits(self, address):
        """
        Get IP address as string of bits

        Arguments:
            address {String} -- String of IP address
        """
        return ''.join('{:08b}'.format(int(x)) for x in address.split('.'))

    def matches_network(self, source_value, comparator, condition_value):
        """
        Perform network match

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            condition_value {[type]} -- [description]

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
        match_signature = re.search(IntrusionPreventionRule.ipv4NetworkRegex, condition_value)
        if equal_comparator and match_signature:
            if match_signature.group(4):
                target_prefix = int(match_signature.group(4))
            condition_value = self.address_to_bits(match_signature.group(1))[:target_prefix]

        record = None
        for value in source_values:
            match_value = value

            if equal_comparator:
                match_signature = re.search(IntrusionPreventionRule.ipv4NetworkRegex, value)
                if match_signature:
                    match_value = self.address_to_bits(match_signature.group(1))[:target_prefix]

                if match_value == condition_value:
                    record = value
                    break
            else:
                if condition_value in value:
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

    def matches_port(self, source_value, comparator, condition_value):
        """
        Perform port match

        Arguments:
            source_value {[type]} -- [description]
            comparator {[type]} -- [description]
            condition_value {[type]} -- [description]

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
                if match_value == condition_value:
                    record = value
                    break
            else:
                if condition_value in value:
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
        signature.set_rule(self)

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

    def add_signature_network(self, source_type, signature, negate=False):
        """

        Modify networks on target signature with rules.

        Arguments:
            source_type -- Network target source or destinatiion
            signature -- signature to modify
        """
        if self.rule[source_type + "Networks"] != "recommended":
            network = None
            if source_type == "source":
                network = signature.get_lnet()
            else:
                network = signature.get_rnet()

            if network.find('[') > -1:
                network = network[network.find('[')+1:network.rfind(']')]

            add_network = self.rule[source_type+"Networks"]
            if negate is True:
                add_network = '!'+add_network

            if network == "any":
                network = add_network
            else:
                network = "[{0},{1}]".format(network, add_network)

            if source_type == "source":
                signature.set_lnet(network)
            else:
                signature.set_rnet(network)
