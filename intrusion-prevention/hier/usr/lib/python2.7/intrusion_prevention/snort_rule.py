"""
Snort rule
"""
import re

class SnortRule:
    """
    Process rule from the snort format.
    """
    text_regex = re.compile(r'^(?i)([#\s]+|)(alert|log|pass|activate|dynamic|drop|reject|sdrop)\s+((tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+(\-\>|\<\>)\s+([^\s]+)\s+([^\s]+)\s+|)\((.+)\)')
    var_regex = re.compile(r'^\$(.+)')

    options_key_regexes = {}
    
    # For the very rare circumstances where we need to override a rule's enabled value.
    # Currently being used for Snort version 2.9.2.2 compiled for Jessie.
    # Once we have a newer version and confirm the rule no longer has a problem,
    # remove this particular rule.
    rule_enabled_overrides = [{
        "sid": "403",
        "gid": "116",
        "enabled": False
    }]

    def __init__(self, match, category, path="rules"):
        self.category = category
        self.path = path
        if len(match.group(1)) > 0 and (match.group(1)[0] == "#"):
            self.enabled = False
        else:
            self.enabled = True
        self.action = match.group(2).lower()
        if match.group(3) != "":
            [self.protocol, self.lnet, self.lport, self.direction, self.rnet, self.rport,self.options_raw] = match.group(4,5,6,7,8,9,10)
            self.protocol = self.protocol.lower()
        else:
            [self.protocol, self.lnet, self.lport, self.direction, self.rnet, self.rport] = [None,None,None,None,None,None]
            self.options_raw = match.group(10)
        
        # Process raw options only for specified keypairs
        self.options = { 
            "sid": "-1",
            "gid": "1",
            "classtype": "uncategoried",
            "msg": ""
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
                key, value = option.strip().split( ':', 1 )
                if key in self.options:
                    if value.count('"') == 1:
                        in_quote = True
                    self.options[key] = value.strip()

        self.rule_id = self.options["sid"] + "_" + self.options["gid"] 
            
    def dump(self):
        """
        Print snort rule
        """
        print "rule dump"
        for prop, value in vars(self).iteritems():
            print prop, ": ", value

    def set_action(self, log, block):
        """
        Set rule action based on log, block
        """
        if log == True and block == True:
            self.action = "drop"
        elif log == False and block == True:
            self.action = "sdrop"
        else:
            self.action = "alert"

        if log == False and block == False:
            self.enabled = False
        else:
            self.enabled = True

    def set_options(self, key, value):
        """
        Set options on key with value
        """
        if not key in self.options:
            return

        find = key+":"+self.options[key]+";"
        self.options[key] = value

        new_options_raw = self.options_raw.replace(find, key + ":" + value + ";")
        if self.options_raw != new_options_raw:
            print self.options_raw
            print new_options_raw
            self.options_raw = new_options_raw
        
    def set_msg(self, msg):
        """
        Set msg
        """
        if msg.startswith('"') and msg.endswith('"'):
            msg = msg[1:-1]
        self.set_options( "msg", '"' + msg + '"' )
        
    def get_msg(self):
        """
        Get msg
        """
        return self.options["msg"]
        
    def set_sid(self, sid):
        """
        Set sid
        """
        self.set_options( "sid", sid )

    def set_classtype(self, classtype):
        """
        Set classtype
        """
        if classtype.startswith('"') and classtype.endswith('"'):
            classtype = classtype[1:-1]
        self.set_options( "classtype", classtype )

    def get_category(self):
        return self.category

    def set_category(self, category):
        self.category = category

    def get_enabled(self):
        """
        Get enabled
        """
        enabled = self.enabled
        if len(SnortRule.rule_enabled_overrides):
            for override in SnortRule.rule_enabled_overrides:
                match = True
                for rule_key in override.keys():
                    if rule_key == "enabled":
                        continue
                    if override[rule_key] != self.options[rule_key]:
                        match = False
                        break
                if match:
                    enabled = override["enabled"]
                    break
    
        return enabled
    
    def get_category(self):
        """
        Get category
        """
        return self.category

    def match(self, classtypes, categories, rule_ids):
        """
        See if the specified filtering match this rule appropriately.
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

        if "+" + self.rule_id in rule_ids or self.rule_id in rule_ids:
            rule_id_match = True
        elif "+" + self.rule_id in rule_ids:
            return False
        else:
            rule_id_match = False

        return classtype_match or category_match or rule_id_match
    
    def build(self):
        """
        Build for snort.conf usage
        """
        if self.get_enabled() == True:
            enabled = ""
        else:
            enabled = "#"
        if self.protocol != None:
            protocol = self.protocol + " "
        else:
            protocol = ""
        if self.lnet != None:
            lnet = self.lnet + " "
        else:
            lnet = ""
        if self.lport != None:
            lport = self.lport + " "
        else:
            lport = ""
        if self.direction != None:
            direction = self.direction + " "
        else:
            direction = ""
        if self.rnet != None:
            rnet = self.rnet + " "
        else:
            rnet = ""
        if self.rport != None:
            rport = self.rport + " "
        else:
            rport = ""
        return enabled + self.action + " " + protocol + lnet + lport + direction + rnet + rport + "( " + self.options_raw + " )"

    def get_variables(self):
        variables = []
        for prop, value in vars(self).iteritems():
            if isinstance( value, str ) == False:
                continue
            match_variable = re.search( SnortRule.var_regex, value )
            if match_variable:
                if variables.count( match_variable.group( 1 ) ) == 0:
                    variables.append( match_variable.group( 1 ) )

        for option in self.options_raw.split(';'):
            option = option.strip()
            if option == "":
                continue
            key = option
            value = None
            if option.find(':') > -1:
                key, value = option.split( ':', 1 )
                key = key.strip()
                value = value.strip()
                match_variable = re.search( SnortRule.var_regex, value )
                if match_variable:
                    if variables.count( match_variable.group( 1 ) ) == 0:
                        variables.append( match_variable.group( 1 ) )

        return variables
