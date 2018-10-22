"""
Intrusion Prevention Rule
"""
import re

Rule_globals = None
#Rule_system_memory=None
class IntrusionPreventionRule:

    global_values = None

    def __init__(self, settingsRule):
        self.rule = settingsRule

        if IntrusionPreventionRule.global_values is None:
            IntrusionPreventionRule.build_global_values()

    @staticmethod
    def build_global_values():
        IntrusionPreventionRule.global_values = {}
        meminfo = open( "/proc/meminfo" )
        for line in meminfo:
            if "MemTotal:" in line:
                value = re.split(' +', line)[1]
                IntrusionPreventionRule.global_values["SYSTEM_MEMORY"] = int(value) * 1024

    def get_enabled(self):
        return self.rule["enabled"]

    def get_action(self):
        return self.rule["action"]

    # def get_id(self):
    #     return self.rule["id"]

    def matches(self, signature):
        if self.rule["enabled"] == False:
            return False

        # print(self.rule["conditions"])
        match = True
        for condition in self.rule["conditions"]["list"]:
            # print("condition")
            # print(condition)

            comparator = condition['comparator']
            targetValue = condition["value"]

            if condition["type"] == "SID":
                match = self.matches_numeric(int(signature.options["sid"]), comparator, int(targetValue))
            elif condition["type"] == "GID":
                match = self.matches_numeric(int(signature.options["gid"]), comparator, int(targetValue))
            elif condition["type"] == "ID":
                conditionArgs["actualValue"] = signature.signature_id
                match = self.matches_text(signature.signature_id, comparator, targetValue)
                #comparator: 'numeric'
            elif condition["type"] == "CATEGORY":
                if not isinstance(condition["value"],list):
                    condition["value"] = condition["value"].split(',')
                match = self.matches_in(signature.category, comparator, condition["value"])
            elif condition["type"] == "CLASSTYPE":
                if not isinstance(condition["value"],list):
                    condition["value"] = condition["value"].split(',')
                match = self.matches_in(signature.options["classtype"], comparator, condition["value"])
            elif condition["type"] == "MSG":
                match = self.matches_text(signature.options["msg"].lower(), comparator, targetValue.lower())
            elif condition["type"] == "PROTOCOL":
                if not isinstance(condition["value"],list):
                    condition["value"] = condition["value"].split(',')
                match = self.matches_in(signature.protocol, comparator, condition["value"])
            elif condition["type"] == "SRC_ADDR":
                conditionArgs["actualValue"] = signature.lnet
                conditionArgs["comparatorType"] = "network"
            elif condition["type"] == "SRC_PORT":
                match = self.matches_numeric(int(signature.lport), comparator, int(targetValue))
            elif condition["type"] == "DST_ADDR":
                conditionArgs["actualValue"] = signature.rnet
                conditionArgs["comparatorType"] = "network"
            elif condition["type"] == "DST_PORT":
                conditionArgs["actualValue"] = signature.rport
                match = self.matches_numeric(int(signature.rport), comparator, int(targetValue))
            elif condition["type"] == "RULE":
                conditionArgs["actualValue"] = signature.options_raw
                conditionArgs["comparatorType"] = "text"
            elif condition["type"] == "SOURCE":
                conditionArgs["actualValue"] = signature.path
                conditionArgs["comparatorType"] = "text"
            elif condition["type"] == "SYSTEM_MEMORY":
                match = self.matches_numeric(int(IntrusionPreventionRule.global_values["SYSTEM_MEMORY"]), comparator, int(targetValue))
            elif condition["type"] == "SIGNATURE":
                match = self.matches_text(signature.build().lower(), comparator, targetValue.lower())
            elif condition["type"] == "CUSTOM":
                match = self.matches_in(signature.custom, comparator, [True if targetValue.lower() == "true" else False])
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

    def matches_numeric(self, sourceValue, comparator, targetValue):
        if comparator == "=":
            return sourceValue == targetValue
        elif comparator == "!=":
            return sourceValue != targetValue
        elif comparator == "<=":
            return sourceValue <= targetValue
        elif comparator == "<":
            return sourceValue < targetValue
        elif comparator == ">":
            return sourceValue > targetValue
        elif comparator == ">=":
            return sourceValue >= targetValue

        return False

    def matches_text(self, sourceValue, comparator, targetValue):
        if comparator == "=":
            return sourceValue == targetValue
        elif comparator == "!=":
            return sourceValue != targetValue
        elif comparator == "substr":
            return targetValue in sourceValue
        elif comparator == "!substr":
            return not targetValue in sourceValue

        return False

    def matches_in(self, sourceValue, comparator, targetValue):
        is_in = sourceValue in ( ( value.lower() if hasattr(value, "lower") else value ) for value in targetValue)

        if comparator == "=":
            return is_in
        elif comparator == "!=":
            return not is_in

        return False

    def set_signature_action(self, signature):
        current_action = signature.get_action()
        if self.rule["action"] == "default":
            signature.set_action(current_action["log"], current_action["block"])
        elif self.rule["action"] == "log":
            signature.set_action(True, False)
        elif self.rule["action"] == "blocklog":
            if signature.get_action()["log"] is True:
                signature.set_action(True, True)
        elif self.rule["action"] == "block":
            signature.set_action(True, True)
        elif self.rule["action"] == "disable":
            signature.set_action(False, False)
