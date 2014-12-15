import re

class SnortRule:
    #
    # Process rules from the snort format.
    #
    text_regex = re.compile(r'^([#\s]+|)([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+([^\s]+)\s+\((.+)\)')

    def __init__( self, regex_match, category ):
        self.category = category
        self.enabled = True
        if len( regex_match.group(1) ) > 0 and regex_match.group(1)[0] == "#":
            self.enabled = False
        self.action = regex_match.group(2)
        self.protocol = regex_match.group(3)
        self.lnet = regex_match.group(4)
        self.lport = regex_match.group(5)
        self.dir = regex_match.group(6)
        self.rnet = regex_match.group(7)
        self.rport = regex_match.group(8)
        self.options_raw = regex_match.group(9);
        self.options = { 
            "sid": -1,
            "classtype": "uncategoried",
            "msg": ""
        }
        self.content_modifiers = {}
        for option in regex_match.group(9).split(';'):
            option = option.strip()
            if option == "":
                continue
            key = option
            value = None
            if option.find(':') > -1:
                key,value = option.split( ':', 1 );
            
            self.options[key] = value
                
    def dump(self):
        print "rule dump"
        for property, value in vars(self).iteritems():
            print property, ": ", value

    def set_description( self, description ):
        if description.startswith('"') and description.endswith('"'):
            description = description[1:-1]
        self.description = description
        
    def set_action( self, log, block  ):
        action = "alert"
        if log == True and block == True:
            action = "drop"
        if log == False and block == True:
            action = "sdrop"
        if log == False and block == False:
            self.enabled = False
        self.action = action
        
    def set_name( self, name ):
        self.name = name
        
    def set_sid( self, sid ):
        self.sid = sid

    def get_enabled( self ):
        return self.enabled
    
    def build( self ):
        if self.enabled == True:
            enabled = ""
        else:
            enabled = "#"
        return enabled + self.action + " " + self.protocol + " " + self.lnet + " " + " " + self.lport + " " + self.dir + " " + self.rnet + " " + self.rport + " ( " + self.options_raw + " )"
