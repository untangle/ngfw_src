"""
Suricata configuration file management
"""
import os
import re
import ruamel.yaml
from ruamel.yaml.compat import text_type

class SuricataConf:
    """
    Suricata configuration file management.
    """
    file_name = "/etc/suricata/suricata.yaml"
    var_address_regex = re.compile(r'(.+|)\d+\.\d+\.\d+\.\d+')

    def __init__(self, _debug=False):
        """

        Initialize configuration.

        Keyword Arguments:
            _debug {bool} -- [description] (default: {False})
        """
        self.conf = []
        self.variables = []
        self.load()

        self.setup_yaml()

    def setup_yaml(self):
        """

        [description]

        Returns:
            [type] -- [description]
        """
        def represent_bool(yaml_self, data):
            if data:
                value = u'yes'
            else:
                value = u'no'
            return yaml_self.represent_scalar(u'tag:yaml.org,2002:bool', text_type(value))

        ruamel.yaml.emitter.Emitter.write_comment_orig = ruamel.yaml.emitter.Emitter.write_comment
        def strip_empty_lines_write_comment(self, comment):
            comment.value = comment.value.replace('\n', '')
            if comment.value:
                self.write_comment_orig(comment)

        ruamel.yaml.representer.RoundTripRepresenter.add_representer(bool, represent_bool)
        ruamel.yaml.emitter.Emitter.write_comment = strip_empty_lines_write_comment

    def load(self):
        """
        Load suricata configuration
        """
        with open(SuricataConf.file_name, 'r') as stream:
            try:
                # self.conf = yaml.load(stream)
                self.conf = ruamel.yaml.load(stream, ruamel.yaml.RoundTripLoader, preserve_quotes=True)
            except ruamel.yaml.YAMLError as yaml_error:
                print(yaml_error)

    def save(self):
        """
        Save suricata configuration
        """
        temp_file_name = SuricataConf.file_name + ".tmp"
        with open(temp_file_name, 'w') as stream:
            try:
                #yaml.dump(self.conf, stream, default_flow_style=False)
                ruamel.yaml.dump(self.conf, stream=stream, Dumper=ruamel.yaml.RoundTripDumper, version=(1, 1), explicit_start=True)
                #, Dumper=ruamel.yaml.RoundTripDumper)
            except ruamel.yaml.YAMLError as yaml_error:
                print(yaml_error)

        backup_file_name = SuricataConf.file_name + ".bak"
        if os.path.isfile(backup_file_name):
            os.remove(backup_file_name)
        os.rename(SuricataConf.file_name, backup_file_name)

        if os.path.getsize(temp_file_name) != 0:
            os.rename(temp_file_name, SuricataConf.file_name)
            os.remove(backup_file_name)
        else:
            os.rename(backup_file_name, SuricataConf.file_name)

    def get_variables(self):
        """
        Return variable name tree.
        """
        return self.conf["vars"]

    def get_variable(self, key):
        """
        Get a variable by name.

        Suricata maintains variables in groups which seems slightly excessive for our purposes.
        """
        for group in self.conf["vars"]:
            for variable in self.conf["vars"][group]:
                if variable == key:
                    return self.conf["vars"][group][variable]
        return None

    def set_variable(self, key, value):
        """
        Set a variable's value.

        Suricata maintains variables in groups which seems slightly excessive for our purposes.
        """
        found = False
        for group in self.conf["vars"]:
            for variable in self.conf["vars"][group]:
                if variable == key:
                    self.conf["vars"][group][variable] = value
                    found = True
        if found is False:
            group = None

            # 1 look if variable referenced from existing
            if value[0] == '$':
                for lookup_group in self.conf["vars"]:
                    for variable in self.conf["vars"][lookup_group]:
                        if variable == value[1:]:
                            group = lookup_group

            # 2. address-groups: ip, ip range, or any,
            #    port-groups: port, !port, port range/list.  not any.
            if group is None:
                match_address = re.match(self.var_address_regex, value)
                if match_address:
                    group = 'address-groups'
                else:
                    group = "port-groups"

            self.conf["vars"][group][key] = value

    def set_config_from_path(self, path, value, config=None):
        """Set configuration from path

        From a list path, walk down config to set the value.

        Arguments:
            path {[list]} -- Path to walk
            value {[dict]} -- value to set last element

        Keyword Arguments:
            config {[type]} -- Current position in configuration (default: {None})
        """
        if config is None:
            config = self.conf

        for key in path:
            if issubclass(type(config), list):
                for item in config:
                    self.set_config_from_path(path, value, item)
            elif issubclass(type(config), dict):
                if key in config:
                    if len(path) == 1:
                        config[key] = value
                    else:
                        if config[key] is None and len(path[1:]) == 1:
                            config[key] = {}
                            # Bizzare hack for retaining nfq comments.
                            # The first two items refer to the comment prefix.
                            # The last two are comments after the key.  When adding a dict,
                            # the last two are duplicated.
                            if key in self.conf.ca.items and len(self.conf.ca.items[key]) == 4:
                                self.conf.ca.items[key] = self.conf.ca.items[key][:2]
                        self.set_config_from_path(path[1:], value, config[key])
                else:
                    if len(path) == 1:
                        config[key] = value

    def set(self, config_path, read_path=None):
        """Set configuration from path

        Build path and value used by set_config_from_path.

        Arguments:
            config_path {[dict]} -- Recursive dict where entry is a key pair.

        Keyword Arguments:
            read_path {[list]} -- Currend read_path list (default: {None})
        """
        if read_path is None:
            read_path = []

        for key in config_path:
            read_path.append(key)
            if type(config_path[key]) is dict:
                self.set(config_path[key], read_path)
            else:
                self.set_config_from_path(read_path, config_path[key])
            read_path.remove(key)
