#!/usr/bin/python

import sys
sys.path.insert(0,'@PREFIX@/usr/lib/python%d.%d/' % sys.version_info[:2])

import getopt

import curses
import curses.textpad
from curses import panel

import json

from time import sleep

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from jsonrpc import JSONDecodeException

import uvm

Debug = False
AppTitle = "Untangle Next Generation Firewall Configuration Console"

class UvmContext:
    """
    UVM managemnt
    """
    context = None
    def __init__(self):
        try:
            self.context = uvm.Uvm().getUvmContext( "localhost", None, None, 60 )
        except (JSONRPCException, JSONDecodeException) as e:
            if hasattr(e, 'error'):
                errorstr = repr(e.error)
            else:
                errorstr = str(e)
            if (not parser.quiet):
                print "Error: %s" % errorstr
                if (errorstr.find("password") != -1):
                    print "Are you root? Did you supply a username/password?"
                if (errorstr.find("unavailable") != -1):
                    print "Is the untangle-vm running?"
                sys.exit(1)

    def exec_and_get_output(self, command, screen):
        """
        Execute a command and send output to the output screen
        """
        screen.external_call_header()

        reader = self.context.execManager().execEvil(command)

        screen.external_call_output("", True)
        while True:
            results = reader.readFromOutput()
            if results is None:
                break
            for c in results:
                screen.external_call_output(c)
        screen.external_call_footer()

### !!! new class for interface management:
# get
#   Pull everything into object for reading
# set
#   Extract back out for proper network settings
#   save settings

class Screen(object):
    """
    Base class for screen management
    """
    title = None
    y_pos = 0
    x_pos = 0
    debug_x_pos = 0

    process_continue = True

    modes = ['default']
    current_mode = None

    def __init__(self, stdscreen):
        """
        Initialize screen
        """
        self.stdscreen = stdscreen
        self.window = stdscreen.subwin(0,0)
        self.window.keypad(1)
        self.panel = panel.new_panel(self.window)
        self.panel.hide()
        self.max_height, self.max_width = stdscreen.getmaxyx()
        self.current_mode = self.modes[0]

    def debug_message(self, msg, clear=False):
        """
        Print a message on the bottom of the screen
        """
        height, width = self.stdscreen.getmaxyx()
        if clear is True:
            self.debug_x_pos = 0

        str_msg = str(msg)
        if len(str_msg) + self.debug_x_pos > ( width - 1):
            self.debug_x_pos = 0

        self.window.addstr( height - 1, self.debug_x_pos, str_msg )

        self.debug_x_pos += len(str_msg) + 1

        if self.debug_x_pos >= width:
            self.debug_x_pos = 0

    def message(self, msg, cont=False):
        """
        Line based message
        """
        str_msg = str(msg)
        # Clear entire line
        self.stdscreen.hline(self.y_pos, 0, " " , 79)

        self.window.addstr( self.y_pos, self.x_pos, msg )

        if cont is True:
            self.x_pos += len(str_msg)
        else:
            self.y_pos += 1
            self.x_pos = 0

    def process(self):
        """
        Main loop that calls display and looks for keypresses
        """
        self.panel.top()
        self.panel.show()

        while True:
            curses.doupdate()

            self.key = None
            self.display()
            if self.process_continue == False:
                break

            if self.key == None:
                self.key = self.window.getch()

            self.process_continue = self.key_process()

            if self.process_continue is False:
                break

    def display(self):
        """
        Clear the screen and display titles
        """
        self.window.clear()
        self.window.refresh()
        self.y_pos = 0
        self.display_title(AppTitle)
        # self.y_pos += 1
        if self.title is not None:
            self.display_title(self.title)
            # self.y_pos += 1
        self.y_pos += 1

    def display_title(self, title):
        """
        Display title with appropriate attributes
        """
        self.window.addstr(self.y_pos, self.x_pos, title, curses.A_BOLD)
        self.y_pos += 1

    def key_process(self):
        """
        Handle keystrokes
        
        If Enter, advance the current mode (if multiple modes exist)
        If Esc, decriment current mode.  
            If this is the starting mode, return False to exit process loop
        """
        if self.key in [curses.KEY_ENTER, ord('\n')]:
            if self.modes.index(self.current_mode) < len(self.modes) -1:
                self.current_mode = self.modes[self.modes.index(self.current_mode) + 1]

        if self.key == 27:
            if self.modes.index(self.current_mode) == 0:
                return False
            else:
                self.current_mode = self.modes[self.modes.index(self.current_mode) - 1]

    def external_call_header(self):
        """
        For external calls, display header
        """
        self.window.addstr(self.y_pos, self.x_pos, "Starting operation...");

    def external_call_footer(self):
        """
        For external calls, display footer
        """
        self.y_pos += 1
        self.window.addstr(self.y_pos, self.x_pos, "Press any key to perform operation again");
        self.y_pos +=1
        self.window.addstr(self.y_pos, self.x_pos, "Press Esc to return to menu")
        self.window.refresh()

    def external_call_output(self, data, reset_column = False):
        """
        Run external call.
        """
        data_string = str(data)
        data_len = len(data_string)

        if reset_column is True:
            self.x_pos = 0

        if data_string == "\n":
            self.x_pos = 0
            self.y_pos += 1
            data_string = ""
            data_len = 0

        if self.x_pos > self.max_width:
            self.x_pos = 0

        if self.y_pos > self.max_height:
            self.y_pos = 1

        self.window.addstr(self.y_pos, self.x_pos, data_string)
        self.x_pos += data_len
        self.window.refresh()

    def textbox(self, y, x, value=""):
        win = curses.newwin( 1, 40, y, x)
        textbox = curses.textpad.Textbox( win )
        self.stdscreen.hline(y, x, "_" , 40)
        self.stdscreen.addstr(y, x, value)

        win.addstr(0,0, value)
        win.refresh()
#       curses.setsyx(y, x + len(value))
        self.window.move(y, x + len(value) )
        curses.curs_set(1)
        self.stdscreen.refresh()
        self.window.refresh()
        return textbox

class Menu(Screen):
    """
    Menu class
    """
    menu_pos = 0

    def __init__(self, items, stdscreen):
        super(Menu, self).__init__(stdscreen)
        self.items = items
        self.menu_pos = 0

        self.networkSettings = Uvm.context.networkManager().getNetworkSettings()
        self.interfaceStatus = Uvm.context.networkManager().getInterfaceStatus()
        self.deviceStatus = Uvm.context.networkManager().getDeviceStatus()
        self.interfaces = filter( lambda i: i['isVlanInterface'] is False, self.networkSettings["interfaces"]["list"] )

        """
        Get IP address of first interface that is not WAN to display recommended URL
        """
        for interface in self.interfaces:
            for device in self.deviceStatus["list"]:
                if interface["physicalDev"] == device["deviceName"]:
                    for k,v in device.iteritems():
                        interface[k] = v
            for interfaceStatus in self.interfaceStatus["list"]:
                if interface["interfaceId"] == interfaceStatus["interfaceId"]:
                    for k,v in interfaceStatus.iteritems():
                        interface[k] = v

        self.internal_ip_address = None
        for interface in self.interfaces:
            if interface["isWan"] is True:
                continue
            if interface["configType"] == "DISABLED":
                continue

            if interface["v4ConfigType"] == "STATIC":
                self.internal_ip_address = interface["v4StaticAddress"]
            elif interface["v4ConfigType"] == "DHCP":
                if "v4AutoAddressOverride" in interface:
                    self.internal_ip_address = interface["v4AutoAddressOverride"]
                else:
                    self.internal_ip_address = interface["v4Address"]
            elif interface["v4ConfigType"] == "PPPOE":
                self.internal_ip_address = interface["v4Address"]

            if self.internal_ip_address is not None:
                break

    def display(self):
        """
        Show menu
        """
        super(Menu, self).display()

        for index, item in enumerate(self.items):
            if index == self.menu_pos:
                mode = curses.A_REVERSE
            else:
                mode = curses.A_NORMAL

            msg = '%s' % (item[0])
            self.window.addstr( self.y_pos + index, self.x_pos, msg, mode)

        self.y_pos = self.y_pos + len(self.items) + 1
        self.window.addstr( self.y_pos, self.x_pos, "Use [Up] and [Down] cursor keys to select menu and press [Enter]")

        # Footer
        height, width = self.stdscreen.getmaxyx()
        self.y_pos = height - 3
        self.window.addstr( self.y_pos, self.x_pos, "Console is only for initial network configuration")
        self.y_pos += 1
        self.window.addstr( self.y_pos, self.x_pos, "For full configuration browse to https://%s" % (self.internal_ip_address) )

    def navigate(self, n):
        """
        Record current index position
        """
        self.menu_pos += n
        if self.menu_pos < 0:
            self.menu_pos = 0
        elif self.menu_pos >= len(self.items):
            self.menu_pos = len(self.items)-1

    def key_process(self):
        """
        Handle key presses
        """
        if super(Menu, self).key_process() is False:
            return False

        if self.key in [curses.KEY_ENTER, ord('\n')]:
            """
            Enter key.  Select current menu item
            """
            if type(self.items[self.menu_pos][1]) is type:
                obj = self.items[self.menu_pos][1](self.stdscreen)
                obj.process()
            else:
                self.items[self.menu_pos][1]()

        elif self.key == curses.KEY_UP:
            self.navigate(-1)

        elif self.key == curses.KEY_DOWN:
            self.navigate(1)

        elif self.key in range( ord("1"), ord("1") + len(self.items) ):
            self.menu_pos = self.key - ord("1")

        return True


"""
Form
"""
class Form(Screen):
    """
    Form class
    """
    modes = ["confirm"]

    mode_menu_pos = {}
    mode_items = {}
    mode_selected_item = {}

    confirm_selections = [{
        "text": "Save"
    },{
        "text": "Cancel",
        "action": False,
        "default": True
    }]
    confirm_message = "Use [Up] and [Down] cursor keys to save or cancel changes and press [Enter]"

    def display(self):
        """ 
        Display
        """
        super(Form, self).display()

        self.mode_items[self.current_mode] = getattr(self, self.current_mode + '_selections')

        """
        Select default option. If not specified, use first
        """
        if self.current_mode not in self.mode_menu_pos:
            for index, item in enumerate(self.mode_items[self.current_mode]):
                if "default" in item and item["default"] is True:
                    self.mode_menu_pos[self.current_mode] = index
        if self.current_mode not in self.mode_menu_pos:
            self.mode_menu_pos[self.current_mode] = 0

        if hasattr(self,'display_form'):
            self.display_form()

        getattr(self,'display_' + self.current_mode)()

    def display_confirm(self):
        """
        Display confirm form
        """
        self.y_pos += 2
        self.display_title( "Confirm" )
        # self.y_pos += 1

        for index, item in enumerate(self.confirm_selections):
            if index == self.mode_menu_pos[self.current_mode]:
                mode = curses.A_REVERSE
            else:
                mode = curses.A_NORMAL

            msg = '%-12s' % (item["text"]) 
            self.window.addstr( self.y_pos + index, self.x_pos, item["text"], mode)

        self.y_pos = self.y_pos + len(self.confirm_selections) + 1
        self.message(self.confirm_message)

    def navigate(self, n):
        """
        Navigate menu positions in mode
        """
        self.mode_menu_pos[self.current_mode] += n
        if self.mode_menu_pos[self.current_mode] < 0:
            self.mode_menu_pos[self.current_mode] = 0
            return
        elif self.mode_menu_pos[self.current_mode] >= len(self.mode_items[self.current_mode]):
            self.mode_menu_pos[self.current_mode] = len(self.mode_items[self.current_mode])-1
            return

    def key_process(self):
        """
        Proces key presses
        """
        if self.key in [curses.KEY_ENTER, ord('\n')]:
            for index, item in enumerate(self.mode_items[self.current_mode]):
                if index == self.mode_menu_pos[self.current_mode]:
                    self.mode_selected_item[self.current_mode] = item

                    if "action" in self.mode_selected_item[self.current_mode]:
                        if self.mode_selected_item[self.current_mode]["action"] is False:
                            return False
                    method = getattr(self, self.current_mode + '_action')
                    if method:
                        method()

        if self.key == 27:
            self.mode_items[self.current_mode] = None

        if super(Form, self).key_process() is False:
            return False

        if self.key == curses.KEY_UP:
            self.navigate(-1)

        elif self.key == curses.KEY_DOWN:
            self.navigate(1)

        return True

class RemapInterfaces(Screen):
    title = "Remap Interfaces"

    menu_pos = 0

    switch_values = [
        "deviceName",
        "mbit",
        "duplex",
        "vendor",
        "macAddress",
    ]

    def __init__(self, stdscreen):
        super(RemapInterfaces, self).__init__(stdscreen)
        self.selected_device = None

        self.networkSettings = Uvm.context.networkManager().getNetworkSettings()
        self.interfaceStatus = Uvm.context.networkManager().getInterfaceStatus()
        self.deviceStatus = Uvm.context.networkManager().getDeviceStatus()
        self.interfaces = filter( lambda i: i['isVlanInterface'] is False, self.networkSettings["interfaces"]["list"] )

        for interface in self.interfaces:
            for device in self.deviceStatus["list"]:
                if interface["physicalDev"] == device["deviceName"]:
                    for k,v in device.iteritems():
                        interface[k] = v

    def display(self):
        super(RemapInterfaces, self).display()

        msg = '%-12s %-15s %-4s %-5s  %-6s %-10s %-10s' % ("Status", "Name", "Dev", "Speed", "Duplex", "Vendor", "MAC Address" )
        self.display_title( msg )
        # self.y_pos += 1

        for index, interface in enumerate(self.interfaces):
            if self.selected_device is not None:
                if ( interface["deviceName"] == self.selected_device ):
                    mode = curses.A_REVERSE
                else:    
                    mode = curses.A_NORMAL
            else:
                if index == self.menu_pos:
                    mode = curses.A_REVERSE
                else:
                    mode = curses.A_NORMAL

            duplex = interface["duplex"]
            if duplex == 'FULL_DUPLEX':
                duplex = 'Full'
            elif duplex == 'HALF_DUPLEX':
                duplex = 'Half'
            else:
                duplex = 'Unk'

            vendor = interface["vendor"]
            if len(vendor) > 7:
                vendor = vendor[:7] + "..."

            msg = '%-12s %-15s %-4s %-5s  %-6s %-10s %-10s' % (str(interface["connected"]), interface["name"], interface["deviceName"], str(interface["mbit"]), duplex, vendor, interface["macAddress"] )
            self.window.addstr( self.y_pos + index, self.x_pos, msg)
            self.window.addstr( self.y_pos + index, self.x_pos + 29, msg[29:], mode)

        self.y_pos = self.y_pos + len(self.interfaces) + 1
        if self.selected_device is not None:
            self.window.addstr( self.y_pos, self.x_pos, "Use [Up] and [Down] cursor keys to move selected device to new location and press [Enter]")
        else:
            self.window.addstr( self.y_pos, self.x_pos, "Use [Up] and [Down] cursor keys to select device to move and press [Enter]")
        self.y_pos += 1

    def navigate(self, n):
        self.menu_pos += n
        if self.menu_pos < 0:
            self.menu_pos = 0
            return
        elif self.menu_pos >= len(self.interfaces):
            self.menu_pos = len(self.interfaces)-1
            return

        # reorder in two steps
        # 1. Walk up to new index.  Anything before selected is appended.
        # 2. Append selected
        # 3. Anything after selected is appended.
        if self.selected_device is not None:
            other_index = None
            selected_index = None
            for index, interface in enumerate(self.interfaces):
                if self.selected_device == interface["deviceName"]:
                    selected_index = index
                    continue

                if selected_index is not None:
                    if n < 0 and other_index is not None:
                        break

                other_index = index

                if selected_index is not None:
                    if n > 0:
                        if other_index is not None:
                            break
                        if selected_index == len(self.interfaces):
                            other_index = None
                            break

            if ( other_index is not None ) and ( selected_index is not None ):
                for key in self.switch_values:
                    old_value = self.interfaces[other_index][key]
                    self.interfaces[other_index][key] = self.interfaces[selected_index][key]
                    self.interfaces[selected_index][key] = old_value

    def key_process(self):
        if super(RemapInterfaces, self).key_process() is False:
            return False

        if self.key in [curses.KEY_ENTER, ord('\n')]:
            if self.selected_device is None:
                for index, interface in enumerate(self.interfaces):
                    if index == self.menu_pos:
                        self.selected_device = interface['deviceName']
            else:
                ## confirm here
                self.selected_device = None

        elif self.key == curses.KEY_UP:
            self.navigate(-1)

        elif self.key == curses.KEY_DOWN:
            self.navigate(1)

        return True

class AssignInterfaces(Screen):
    title = "Assign Interface Addresses"

    modes = ['interface', 'config', 'addressed', 'edit', 'edit_field', 'confirm']
    mode_menu_pos = {}
    mode_items = {}
    mode_selected_item = {}

    selected = {}

    config_selections = [
        ('Addressed', 'ADDRESSED'),
        ('Bridged', 'BRIDGED'),
        ('Disabled', 'DISABLED')
    ]

    # addressed_edits = [
    addressed_selections = [
        ('DHCP', 'DHCP'),
        ('Static', 'STATIC'),
        ('PPPoE', 'PPPOE')
    ]

    # primary/secondary only for WAN
    # need to get/show current
    dhcp_field_selections = [
        ('Address Override', 'v4AutoAddressOverride'),
        ('Netmask Override', 'v4AutoPrefixOverride'),
        ('Gateway Override', 'v4AutoGatewayOverride'),
        ('Primary DNS Override', 'v4AutoDns1Override'),
        ('Secondary DNS Override', 'v4AutoDns2Override')
    ]

    # primary/secondary only for WAN
    static_field_selections = [
        ('Address', 'v4StaticAddress'),
        ('Netmask', 'v4StaticPrefix'),
        ('Gateway', 'v4StaticGateway'),
        ('Primary DNS', 'v4StaticDns1'),
        ('Secondary DNS', 'v4StaticDns2')
    ]

    # primary/secondary only if use peer dns =false
    pppoe_field_selections = [
        ('Username', 'v4PPPoEUsername'),
        ('Password', 'v4PPPoEPassword'),
        ('Use Peer DNS', 'v4PPPoEUsePeerDns'),
        ('Primary DNS', 'v4PPPoEDns1'),
        ('Secondary DNS', 'v4PPPoEDns1')
    ]

    # confirms_selection = [
    #     ('Save', 'save'),
    #     ('Cancel', 'cancel')
    # ]

    # modes = enum( 'SELECT', 'CONFIG', 'ADDRESSED', 'EDIT', 'CONFIRM')
    # mode = modes()

    def __init__(self, stdscreen):
        super(AssignInterfaces, self).__init__(stdscreen)

        self.networkSettings = Uvm.context.networkManager().getNetworkSettings()
        self.interfaceStatus = Uvm.context.networkManager().getInterfaceStatus()
        self.deviceStatus = Uvm.context.networkManager().getDeviceStatus()
        self.interface_selections = filter( lambda i: i['isVlanInterface'] is False, self.networkSettings["interfaces"]["list"] )
        
        for interface in self.interface_selections:
            for device in self.deviceStatus["list"]:
                if interface["physicalDev"] == device["deviceName"]:
                    for k,v in device.iteritems():
                        interface[k] = v
            for interfaceStatus in self.interfaceStatus["list"]:
                if interface["interfaceId"] == interfaceStatus["interfaceId"]:
                    for k,v in interfaceStatus.iteritems():
                        interface[k] = v

        self.selected_device = None

    def display(self):
        super(AssignInterfaces, self).display()

        # self.debug_message()

        if self.current_mode not in self.mode_menu_pos:
            self.mode_menu_pos[self.current_mode] = 0

        # self.debug_message("current mode=" + self.current_mode)
        if ( self.current_mode not in self.mode_items ) or ( self.mode_items[self.current_mode] == None ):
            ## ?? maybe _list instead
            if self.current_mode == "edit_field":
                self.mode_items[self.current_mode] = self.mode_items[self.modes[self.modes.index(self.current_mode) -1]]
            elif self.current_mode == "edit":
                if self.mode_selected_item["addressed"][1] == "DHCP":
                    self.mode_items[self.current_mode] = self.dhcp_field_selections
                elif self.mode_selected_item["addressed"][1] == "STATIC":
                    self.mode_items[self.current_mode] = self.static_field_selections
                elif self.mode_selected_item["addressed"][1] == "PPPOE":
                    self.mode_items[self.current_mode] = self.pppoe_field_selections
            else:
                self.mode_items[self.current_mode] = getattr(self, self.current_mode + '_selections')

        if self.current_mode == "edit_field":
            getattr(self,'display_edit')()
        else:
            getattr(self,'display_' + self.current_mode)()

        self.y_pos += 1

    def display_interface(self, show_selected_only = False):
        msg = '%-12s %-15s %-4s %-5s  %-10s %-18s %-5s' % ("Status", "Name", "Dev", "Speed", "Config", "Address", "is Wan" )
        self.display_title( msg )
        # self.y_pos += 1

        for index, interface in enumerate(self.mode_items["interface"]):
            if show_selected_only:
                select_mode = curses.A_NORMAL
                # if interface["deviceName"] != self.selected_device:
                if interface != self.mode_selected_item['interface']:
                    continue
            else:
                ## !!! highlight current value
                # if index == self.menu_pos:
                if index == self.mode_menu_pos[self.current_mode]:
                    select_mode = curses.A_REVERSE
                else:
                    select_mode = curses.A_NORMAL

            config = interface["configType"]
            for c in self.config_selections:
                if c[1] == config:
                    config = c[0] 

            address = ''
            if 'v4Address' in interface:
                address = interface['v4Address'] + '/' + str(interface['v4PrefixLength'])

            msg = '%-12s %-15s %-4s %-5s  %-10s %-18s %-5s' % (str(interface["connected"]), interface["name"], interface["deviceName"], str(interface["mbit"]), config, address, interface["isWan"] )
            if show_selected_only is True:
                index = 0            
            self.window.addstr( self.y_pos + index, self.x_pos, msg, select_mode)

        self.y_pos = self.y_pos + 1
        if show_selected_only is False:
            self.y_pos = self.y_pos + len(self.mode_items["interface"])
            self.window.addstr( self.y_pos, self.x_pos, "Use [Up] and [Down] cursor keys to select interface to edit and press [Enter]")

    def display_config(self, show_selected_only=False):

        self.display_interface(show_selected_only=True)

        self.y_pos += 1
        msg = '%-12s' % ("Config")
        self.display_title( msg )
        # self.y_pos += 1

        for index, item in enumerate(self.mode_items["config"]):
            if show_selected_only is True:
                # if self.config != config:
                if item != self.mode_selected_item['config']:
                    continue
                mode = curses.A_NORMAL
            else: 
                if index == self.mode_menu_pos[self.current_mode]:
                    mode = curses.A_REVERSE
                else:
                    mode = curses.A_NORMAL

            msg = '%-12s' % (item[0]) 
            self.window.addstr( self.y_pos + index, self.x_pos, msg, mode)

        self.y_pos = self.y_pos + 1
        if show_selected_only is False:
            self.y_pos = self.y_pos + len(self.mode_items["config"])
            self.window.addstr( self.y_pos, self.x_pos, "Use [Up] and [Down] cursor keys to select interface config mode and press [Enter]")

    def display_addressed(self, show_selected_only=False):

        self.display_config(show_selected_only=True)

        self.y_pos += 1
        msg = '%-12s' % ("Addressed")
        self.display_title( msg )
        # self.y_pos += 1
        index_adjust = 0

        for index, item in enumerate(self.mode_items["addressed"]):
            if show_selected_only is True:
                # if self.config != config:
                if item != self.mode_selected_item['addressed']:
                    continue
                mode = curses.A_NORMAL
            else: 
                ## !!! highlight current value
                if self.mode_selected_item['interface']['isWan'] is False and item[1] != 'STATIC':
                    index_adjust -= 1
                    continue

                # self.debug_message(index)
                if index == self.mode_menu_pos[self.current_mode]:
                    mode = curses.A_REVERSE
                else:
                    mode = curses.A_NORMAL

            if show_selected_only is True:
                index = 0

            msg = '%-20s' % (item[0]) 
            self.window.addstr( self.y_pos + index + index_adjust, self.x_pos, msg, mode)

        self.y_pos = self.y_pos + 1
        if show_selected_only is False:
            self.y_pos = self.y_pos + len(self.mode_items["addressed"]) + index_adjust
            self.window.addstr( self.y_pos, self.x_pos, "Use [Up] and [Down] cursor keys to select address mode and press [Enter]")

    def display_edit(self):

        self.display_addressed(show_selected_only=True)
    
        self.debug_message("current mode="+self.current_mode)

        self.y_pos += 1
        msg = '%-12s' % ("Edit")
        self.display_title( msg )
        # self.y_pos += 1

        # if modified, add edit_commit_fields

        edit_mode = self.current_mode == "edit_field"
        edit_index = None
        edit_item = None
        # if(edit_mode):
        #     self.debug_message( self.mode_selected_item["edit_field"] );
        for index, item in enumerate(self.mode_items["edit"]):
            if edit_mode:
                # "edit_field" in self.mode_selected_item and item == self.mode_selected_item["edit_field"]:
                # self.debug_message("edit field")
                # self.debug_message("do edit")
                # mode = curses.A_REVERSE
                if self.mode_selected_item["edit"][1] == item[1]:
                    mode = curses.A_REVERSE
                    edit_index = index
                    edit_item = item
                else:
                    mode = curses.A_NORMAL
            else:
                if index == self.mode_menu_pos[self.current_mode]:
                    mode = curses.A_REVERSE
                else:
                    mode = curses.A_NORMAL

            msg = '%-30s %-40s' % (item[0], self.mode_selected_item['interface'][item[1]]) 
            self.window.addstr( self.y_pos + index, self.x_pos, msg, mode)

        if edit_mode:
            self.window.addstr( self.y_pos + len(self.mode_items["edit"]) + 2, self.x_pos, "Modify the value and press [Enter]")

            # ??? may only need index?
            self.edit_field(edit_index, edit_item)
        else:
            self.y_pos = self.y_pos + len(self.mode_items["edit"]) + 1
            self.window.addstr( self.y_pos, self.x_pos, "Use [Up] and [Down] cursor keys to select field to edit and press [Enter]")

        ## track modifiications

    def edit_field(self, edit_index, edit_item):
        # note value type:
        # integer
        # none
        textbox = self.textbox( self.y_pos + edit_index, 31, str(self.mode_selected_item['interface'][edit_item[1]]) )
        # self.y_pos = self.y_pos + len(self.mode_items["edit"]) + 2
        newValue = textbox.edit()
        ## validate.
        ## if valid, set value to newValue
        ## massage back to proper type
        ##  if None, don't write back.
        self.mode_selected_item['interface'][edit_item[1]] = newValue.strip()
        self.key = 27
        # self.debug_message(newValue)

    # def display_confirm(self, show_selected_only=False):

    #     self.y_pos += 1
    #     msg = '%-12s' % ("Confirm")
    #     self.display_title( msg )
    #     self.y_pos += 1
    #     index_adjust = 0

    #     for index, item in enumerate(self.mode_items["addressed"]):
    #         if show_selected_only is True:
    #             # if self.config != config:
    #             if item != self.mode_selected_item['addressed']:
    #                 continue
    #             mode = curses.A_NORMAL
    #         else: 
    #             ## !!! highlight current value
    #             if self.mode_selected_item['interface']['isWan'] is False and item[1] != 'STATIC':
    #                 index_adjust -= 1
    #                 continue

    #             # self.debug_message(index)
    #             if index == self.mode_menu_pos[self.current_mode]:
    #                 mode = curses.A_REVERSE
    #             else:
    #                 mode = curses.A_NORMAL

    #         if show_selected_only is True:
    #             index = 0

    #         msg = '%-20s' % (item[0]) 
    #         self.window.addstr( self.y_pos + index + index_adjust, self.x_pos, msg, mode)

    #     self.y_pos = self.y_pos + 1
    #     if show_selected_only is False:
    #         self.y_pos = self.y_pos + len(self.mode_items["addressed"]) + index_adjust
    #         self.window.addstr( self.y_pos, self.x_pos, "Use [Up] and [Down] cursor keys to select address mode and press [Enter]")


    def navigate(self, n):
        self.mode_menu_pos[self.current_mode] += n
        if self.mode_menu_pos[self.current_mode] < 0:
            self.mode_menu_pos[self.current_mode] = 0
            return
        elif self.mode_menu_pos[self.current_mode] >= len(self.mode_items[self.current_mode]):
            self.mode_menu_pos[self.current_mode] = len(self.mode_items[self.current_mode])-1
            return

    def key_process(self):

        if self.key in [curses.KEY_ENTER, ord('\n')]:
            for index, item in enumerate(self.mode_items[self.current_mode]):
                if index == self.mode_menu_pos[self.current_mode]:
                    self.mode_selected_item[self.current_mode] = item

                    if self.current_mode == "interface":
                        self.mode_selected_item["config"] = item["configType"]
                        for index, litem in enumerate(self.config_selections):
                            if litem[1] == item["configType"]:
                                self.mode_menu_pos["config"] = index
                        if item["configType"] == "ADDRESSED":
                            self.mode_selected_item["addressed"] = item["v4ConfigType"]
                            for index, litem in enumerate(self.addressed_selections):
                                if litem[1] == item["v4ConfigType"]:
                                    self.mode_menu_pos["addressed"] = index

        if self.key == 27:
            self.mode_items[self.current_mode] = None

        if self.key in [curses.KEY_ENTER, ord('\n')]:
            if self.current_mode == "config" and self.mode_selected_item[self.current_mode][1] == "DISABLED":
                self.current_mode = "confirm"
            if self.current_mode == "edit_field":
                self.current_mode = "edit"
            else:
                if super(AssignInterfaces, self).key_process() is False:
                    return False
        else:
            if super(self.__class__, self).key_process() is False:
                return False

        if self.key == curses.KEY_UP:
            self.navigate(-1)

        elif self.key == curses.KEY_DOWN:
            self.navigate(1)

        return True


class Ping(Screen):
    title = "Ping"
    def display(self):
        super(Ping, self).display()

        curses.echo()
        mode = curses.A_NORMAL
        label = "Address to ping: "
        self.window.addstr(self.y_pos, self.x_pos, label)
        curses.curs_set(1)
        address = self.window.getstr(self.y_pos, len(label), 50)
        curses.curs_set(0)

        if len(address.strip()) == 0:
            self.process_continue = False
        else:
            self.y_pos += 2
            Uvm.exec_and_get_output("ping -c 5 " + address, self )

class Upgrade(Form):
    title = "Reboot"

    confirm_selections = [{
        "text": "Yes"
    },{
        "text": "No",
        "action": False
    }]

    def display_form(self):
        self.window.addstr(self.y_pos, self.x_pos, "Are you sure you want to upgrade the system?")

class Reboot(Form):
    title = "Reboot"

    confirm_selections = [{
        "text": "Yes"
    },{
        "text": "No",
        "action": False,
        "default": True
    }]
    confirm_message = "Use [Up] and [Down] cursor keys to confirm or cancel and press [Enter]"

    def display_form(self):
        self.message("Are you sure you want to reboot the system?")
        self.y_pos += 1
        self.message("This will interrupt network operations until the server has restarted.")
        self.message("This may take up to several minutes to complete.")

    def confirm_action(self):
        self.window.clear()
        self.window.refresh()
        Uvm.context.rebootBox()

class Shutdown(Form):
    title = "Shutdown"

    confirm_selections = [{
        "text": "Yes"
    },{
        "text": "No",
        "action": False,
        "default": True
    }]

    def display_form(self):
        self.message("Are you sure you want to shut down the system?")
        self.y_pos += 1
        self.message("This will stop all network operations.")

    def confirm_action(self):
        self.window.clear()
        self.window.refresh()
        Uvm.context.shutdownBox()

class FactoryDefaults(Form):
    title = "Shutdown"

    confirm_selections = [{
        "text": "Yes"
    },{
        "text": "No",
        "action": False
    }]

    def display_form(self):
        self.window.addstr(self.y_pos, self.x_pos, "Are you sure you want to restore the system to factory defaults?")

class UiApp(object):
    def __init__(self, stdscreen):
        self.screen = stdscreen
        curses.curs_set(0)

        menu_items = [
            ('Remap Interfaces', RemapInterfaces),
            ('Assign Interface Addresses', AssignInterfaces),
            ('Ping', Ping),
            ('Upgrade', Upgrade),
            ('Reboot', Reboot),
            ('Shutdown', Shutdown),
            ('Reset To Factory Defaults', FactoryDefaults)
        ]
            # ('Shell (secret)', curses.beep),
            # ('Exit', exit)

        menu = Menu(menu_items, stdscreen)
        menu.process()

def usage():
    """
    Show usage
    """
    print "usage"

def main(argv):
    global Debug
    global Uvm

    Uvm = UvmContext()

    try:
        opts, args = getopt.getopt(argv, "hdr:s:t:", ["help", "debug"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        if opt in ( "-d", "--debug"):
            Debug = True

    curses.wrapper(UiApp)

if __name__ == "__main__":
    main( sys.argv[1:] )
