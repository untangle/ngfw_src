"""
Snort rule
"""
import re

class SnortRule:
    """
    Process rule from the snort format.
    """
    text_regex = re.compile(r'^(?i)([#\s]+|)(alert|log|pass|activate|dynamic|drop|reject|sdrop)\s+((tcp|udp|icmp|ip)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+|)\((.+)\)')

    def __init__(self, regex_match, category, path="rules"):
        self.category = str(category)
        self.path = str(path)
        self.enabled = True
        if len( regex_match.group(1) ) > 0 and regex_match.group(1)[0] == "#":
            self.enabled = False
        self.action = regex_match.group(2).lower()
        if regex_match.group(3) != "":
            self.protocol = str(regex_match.group(4).lower())
            self.lnet = str(regex_match.group(5))
            self.lport = str(regex_match.group(6))
            self.direction = str(regex_match.group(7))
            self.rnet = str(regex_match.group(8))
            self.rport = str(regex_match.group(9))
        else:
            self.protocol = None
            self.lnet = None
            self.lport = None
            self.direction = None
            self.rnet = None
            self.rport = None
        
        self.options_raw = regex_match.group(10)
        self.options = { 
            "sid": "-1",
            "gid": "1",
            "classtype": "uncategoried",
            "msg": ""
        }
        
        self.content_modifiers = {}
        for option in regex_match.group(10).split(';'):
            option = option.strip()
            if option == "":
                continue
            key = option
            value = None
            if option.find(':') > -1:
                key, value = option.split( ':', 1 )
                key = key.strip()
                value = value.strip()

            self.options[key] = str(value)

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
        action = "alert"
        enabled = True
        if log == True and block == True:
            action = "drop"
        if log == False and block == True:
            action = "sdrop"

        if log == False and block == False:
            enabled = False 

        self.action = action
        self.enabled = enabled

    def set_options(self, key, value):
        """
        Set options on key with value
        """
        self.options[key] = value
        options_raw_match_re = re.compile( r'\s+' + key + ":([^;]+);")
        match_rule = re.search( options_raw_match_re, self.options_raw )
        if match_rule:
            self.options_raw = options_raw_match_re.sub( " " + key + ":" + value + ";", self.options_raw )
        
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

    def get_enabled(self):
        """
        Get enabled
        """
        return self.enabled
    
    def get_category(self):
        """
        Get category
        """
        return self.category

    def match(self, classtypes, categories, rule_ids):
        """
        See if the specified filtering match this rule appropriately.
        If an item is prefixed by a + or just named, then match.
        """
        if len(classtypes) == 0:
            classtype_match = True
        elif "+" + self.options["classtype"] in classtypes or self.options["classtype"] in classtypes:
            classtype_match = True
        else:
            classtype_match = False

        if len(categories) == 0:
            category_match = True
        elif "+" + self.category in categories or self.category in categories:
            category_match = True
        else:
            category_match = False

        if len(rule_ids) == 0:
            rule_id_match = True
        elif "+" + self.rule_id in rule_ids or self.rule_id in rule_ids:
            rule_id_match = True
        else:
            rule_id_match = False

        return classtype_match and category_match and rule_id_match
    
    def build(self):
        """
        Build for snort.conf usage
        """
        if self.enabled == True:
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
