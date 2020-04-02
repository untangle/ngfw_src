#!/usr/bin/python
"""
Text-based user inteface for configuring a subset of critcal items neccessary
to connect with browser (for complete configuration)
"""
import base64
import getopt
import json
import md5
import os
import re
import signal
import sys
import subprocess
import traceback

if "@PREFIX@" != '':
    sys.path.insert(0, '@PREFIX@/usr/lib/python2.7/dist-packages')

import curses
import curses.textpad
from curses import panel

from time import sleep

from jsonrpc import ServiceProxy
from jsonrpc import JSONRPCException
from jsonrpc import JSONDecodeException

import uvm

Debug = False
AppTitle = "Untangle Next Generation Firewall Configuration Console"
Require_Auth = True

class UvmContext:
    """
    UVM managemnt
    """
    context = None
    def __init__(self, tries=30, wait=10):
        """
        Connect to uvm as localhost with optional configurable tries and waits
        """
        while tries > 0:
            try:
                self.context = uvm.Uvm().getUvmContext( "localhost", None, None, 60 )
                return
            except (JSONRPCException, JSONDecodeException) as e:
                self.context = None
                pass
            tries -= 1
            if tries == 0:
                break
            else:
                print("Connecting to Untangle...")
            sleep(wait)
        raise

    def execute(self, command):
        """
        Execute a command and send output to the output screen
        """
        self.context.execManager().execResult(command)

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

class Validate:
    """
    General class validator
    """

    def check(self, value, validators):
        messages = []
        for validator in validators:
            if hasattr(self, validator):
                try:
                    message = getattr(self, validator)(value)
                except:
                    validator_invalid_message = validator + "_invalid_message"
                    if hasattr(self, validator_invalid_message):
                        message = getattr(self, validator_invalid_message)
                    else:
                        message = "Invalid value"

                if message != True:
                    messages.append(message)
                    if validator == "empty":
                        return messages

        if len(messages) > 0:
            return messages
        else:
            return None

    def empty(self, value):
        if len(value.strip()) == 0:
            return "Value must be specified."
        return True

    ip_invalid_message = "Invalid IP address"
    def ip(self, value):
        ip_regex = re.compile(r'^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$')
        if re.match(ip_regex, value.strip()) is None:
            return self.ip_invalid_message
        return True

    prefix_invalid_message = "Invalid prefix"
    def prefix(self, value):
        ivalue = int(value)
        if ivalue < 0 or ivalue > 32:
            return self.prefix_invalid_message
        return True

    def username(self, value):
        return True

    def password(self, value):
        return True

Validator = Validate()

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

        self.screen_height, self.screen_width = self.stdscreen.getmaxyx()

    def debug_message(self, msg, clear=False):
        """
        print(a message on the bottom of the screen)
        """
        if clear is True:
            self.debug_x_pos = 0

        str_msg = str(msg)
        if len(str_msg) + self.debug_x_pos > ( self.screen_width - 1):
            self.debug_x_pos = 0

        if len(str_msg) > self.screen_width:
            str_msg = str_msg[:self.screen_width - 1]

        self.window.addstr( self.screen_height - 1, self.debug_x_pos, str_msg )

        self.debug_x_pos += len(str_msg) + 1

        if self.debug_x_pos >= self.screen_width:
            self.debug_x_pos = 0

    def message(self, msg, cont=False, mode=None):
        """
        Line based message
        """
        str_msg = str(msg)
        # Clear entire line

        if self.y_pos > self.screen_height - 1:
            self.y_pos = self.screen_height - 1
        self.stdscreen.hline(self.y_pos, 0, " " , 79)

        if mode is None:
            self.window.addstr( self.y_pos, self.x_pos, msg)
        else:
            self.window.addstr( self.y_pos, self.x_pos, msg, mode )

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
        self.message(title, mode=curses.A_BOLD)

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
        self.message("Starting operation...");
        self.y_pos += 1

    def external_call_footer(self):
        """
        For external calls, display footer
        """
        self.y_pos += 1
        self.message("Press any key to perform operation again");
        self.message("Press [Esc] to return to menu")
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
        """
        Create/display/return a textbox with the current value.
        """
        win = curses.newwin( 1, 40, y, x)
        textbox = curses.textpad.Textbox( win )
        self.stdscreen.hline(y, x, "_" , 40)
        self.stdscreen.addstr(y, x, value)

        win.addstr(0,0, value)
        win.refresh()
        self.window.move(y, x + len(value) )

        try:
            curses.curs_set(1)
        except:
            pass

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

        """
        Get list of visible items (with "text" attribute).
        Some items (like shell) are not displayed but accessible via key.
        """
        self.visible_items = []
        for item in items:
            if "text" in item:
                self.visible_items.append(item)

        """
        Get IP address of first interface that is not WAN to display recommended URL
        """
        uvm = UvmContext()
        self.networkSettings = uvm.context.networkManager().getNetworkSettings()
        self.interfaceStatus = uvm.context.networkManager().getInterfaceStatus()
        self.deviceStatus = uvm.context.networkManager().getDeviceStatus()
        self.interfaces = filter( lambda i: i['isVlanInterface'] is False, self.networkSettings["interfaces"]["list"] )
        uvm = None

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

        for index, item in enumerate(self.visible_items):
            if index == self.menu_pos:
                mode = curses.A_REVERSE
            else:
                mode = curses.A_NORMAL

            msg = '%s' % (item["text"])
            self.window.addstr( self.y_pos + index, self.x_pos, msg, mode)

        self.y_pos = self.y_pos + len(self.visible_items) + 1
        self.message( "Use [Up] and [Down] keys to select menu and press [Enter]")

        # Footer
        height, width = self.stdscreen.getmaxyx()
        self.y_pos = height - 3
        self.message( "Console is only for initial network configuration")
        self.message( "For full configuration browse to https://%s" % (self.internal_ip_address) )

    def navigate(self, n):
        """
        Record current index position
        """
        self.menu_pos += n
        if self.menu_pos < 0:
            self.menu_pos = 0
        elif self.menu_pos >= len(self.visible_items):
            self.menu_pos = len(self.visible_items)-1

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
            self.call_class(self.visible_items[self.menu_pos])

        elif self.key == curses.KEY_UP:
            self.navigate(-1)

        elif self.key == curses.KEY_DOWN:
            self.navigate(1)

        elif self.key in range( ord("1"), ord("1") + len(self.visible_items) ):
            self.menu_pos = self.key - ord("1")

        else:
            for item in self.items:
                if "key" in item and self.key == ord(item["key"]):
                    self.call_class(item)

        return True

    def call_class(self, item):
        """
        Instantiate a class or call the already instantiated object.
        """
        if type(item["class"]) is type:
            obj = item["class"](self.stdscreen)
            obj.process()
        else:
            item["class"]()

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
    confirm_message = "Use [Up] and [Down] keys to save or cancel changes and press [Enter]"
    multi_mode_confirm_message = "Press [Esc] to continue editing"

    def invalid_messages(self, messages):
        message = ".".join(messages)
        self.stdscreen.hline(self.y_pos - 2, 0, " " , 79)
        self.stdscreen.hline(self.y_pos - 1, 0, " " , 79)

        self.y_pos -= 2
        self.message( message )
        self.message( "Press any key to continue" )
        key = self.window.getch()

    def display(self):
        """ 
        Display
        """
        super(Form, self).display()

        if hasattr(self, self.current_mode + '_selections'):
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

        if hasattr(self,'display_' + self.current_mode):
            getattr(self,'display_' + self.current_mode)()

    def display_confirm(self):
        """
        Display confirm form
        """
        last_mode = None
        if len(self.modes) > 1:
            for mode in self.modes:
                if mode == self.current_mode:
                    break
                last_mode = mode

        if last_mode is not None and hasattr(self,'display_' + last_mode):
            getattr(self, 'display_' + last_mode)(show_selected_only=True)

        self.y_pos += 2
        self.display_title( "Confirm" )

        for index, item in enumerate(self.confirm_selections):
            if index == self.mode_menu_pos[self.current_mode]:
                mode = curses.A_REVERSE
            else:
                mode = curses.A_NORMAL

            msg = '%-12s' % (item["text"]) 
            self.window.addstr( self.y_pos + index, self.x_pos, item["text"], mode)

        self.y_pos += len(self.confirm_selections) + 1
        self.message(self.confirm_message)
        if len(self.modes) > 1:
            self.message(self.multi_mode_confirm_message)

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
                    if hasattr( self, 'action_' + self.current_mode ):
                        if getattr(self, 'action_' + self.current_mode)() is False:
                            return False

        if self.key == 27:
            self.mode_items[self.current_mode] = None

        if super(Form, self).key_process() is False:
            return False

        if self.key == curses.KEY_UP:
            self.navigate(-1)

        elif self.key == curses.KEY_DOWN:
            self.navigate(1)

        return True

class RemapInterfaces(Form):
    """
    Change physical device to interface assignment.
    """
    title = "Remap Interfaces"
    modes = ["interface", "confirm"]

    switch_values = [
        "physicalDev",
        "systemDev",
        "symbolicDev",
        "deviceName",
        "mbit",
        "duplex",
        "vendor",
        "macAddress",
    ]

    def __init__(self, stdscreen):
        super(RemapInterfaces, self).__init__(stdscreen)
        self.selected_device = None

        """
        Build interfaces to use
        """
        uvm = UvmContext()
        self.networkSettings = uvm.context.networkManager().getNetworkSettings()
        self.interfaceStatus = uvm.context.networkManager().getInterfaceStatus()
        self.deviceStatus = uvm.context.networkManager().getDeviceStatus()
        self.interface_selections = filter( lambda i: i['isVlanInterface'] is False, self.networkSettings["interfaces"]["list"] )
        uvm = None

        for interface in self.interface_selections:
            for device in self.deviceStatus["list"]:
                if interface["physicalDev"] == device["deviceName"]:
                    for k,v in device.iteritems():
                        interface[k] = v

    def display_interface(self, show_selected_only=False):
        """
        Display interface list
        """
        msg = '%-12s %-17s %-5s %-5s  %-6s %-10s %-10s' % ("Status", "Name", "Dev", "Speed", "Duplex", "Vendor", "MAC Address" )
        self.display_title( msg )

        for index, interface in enumerate(self.interface_selections):
            if show_selected_only:
                select_mode = curses.A_NORMAL
            else:
                if self.selected_device is not None:
                    if ( interface["deviceName"] == self.selected_device ):
                        select_mode = curses.A_REVERSE
                    else:    
                        select_mode = curses.A_NORMAL
                else:
                    if index == self.mode_menu_pos[self.current_mode]:
                        select_mode = curses.A_REVERSE
                    else:
                        select_mode = curses.A_NORMAL

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

            msg = '%-12s %-17s %-5s %-5s  %-6s %-10s %-10s' % (str(interface["connected"]), interface["name"], interface["deviceName"], str(interface["mbit"]), duplex, vendor, interface["macAddress"] )
            self.window.addstr( self.y_pos + index, self.x_pos, msg)
            self.window.addstr( self.y_pos + index, self.x_pos + 31, msg[31:], select_mode)

        self.y_pos = self.y_pos + len(self.interface_selections) + 1
        if self.selected_device is not None:
            self.message( "Use [Up] and [Down] keys to move device to new location and press [Enter]")
        else:
            self.message( "Use [Up] and [Down] keys to select device to move and press [Right]")
            self.message( "Press [Esc] to return without making changes")

    def navigate(self, n):
        """
        If in interface mode, switch the current interfaces physical values.
        """
        super(RemapInterfaces, self).navigate(n)

        if self.current_mode == "interface" and self.selected_device is not None:
            other_index = None
            selected_index = None
            for index, interface in enumerate(self.interface_selections):
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
                        if selected_index == len(self.interface_selections):
                            other_index = None
                            break

            if ( other_index is not None ) and ( selected_index is not None ):
                for key in self.switch_values:
                    old_value = self.interface_selections[other_index][key]
                    self.interface_selections[other_index][key] = self.interface_selections[selected_index][key]
                    self.interface_selections[selected_index][key] = old_value

    def key_process(self):
        """
        Select interface with right-key.
        """
        if self.current_mode == "interface" and self.key == curses.KEY_RIGHT:
            if self.selected_device is None:
                for index, interface in enumerate(self.interface_selections):
                    if index == self.mode_menu_pos[self.current_mode]:
                        self.selected_device = interface['deviceName']
            else:
                self.selected_device = None
        else:
            if super(self.__class__, self).key_process() is False:
                return False

    def action_confirm(self):
        """
        Load settings and modify interface device values with changes.
        """
        self.window.clear()
        if self.mode_selected_item[self.current_mode] != self.confirm_selections[0]:
            return

        uvm = UvmContext()
        networkSettings = uvm.context.networkManager().getNetworkSettings()
        for interface in networkSettings["interfaces"]["list"]:
            for i in self.interface_selections:
                if interface["interfaceId"] == i["interfaceId"]:
                    interface["physicalDev"] = i["physicalDev"]
                    interface["systemDev"] = i["systemDev"]
                    interface["symbolicDev"] = i["symbolicDev"]

        self.message("Saving network settings...")
        self.window.refresh()
        self.current_mode = None
        uvm.context.networkManager().setNetworkSettings(networkSettings)
        uvm = None
        return False

class AssignInterfaces(Form):
    """
    Configure interfaces for addressing, bridging, disabled.
    """
    title = "Configure Interfaces"

    modes_addressed = ['interface', 'config', 'addressed', 'edit', 'confirm']
    modes_bridged = ['interface', 'config', 'bridged', 'confirm']
    modes_disabled = ['interface', 'config', 'confirm']

    config_selections = [{
        "text": "Addressed", 
        "value": "ADDRESSED",
    },{
        "text": "Bridged", 
        "value": "BRIDGED",
    },{
        "text": "Disabled", 
        "value": "DISABLED",
    }]

    bridged_selections = []

    addressed_wan_selections = [{
        "text": "DHCP", 
        "value": "DHCP"
    },{
        "text": "Static",
        "value": "STATIC"
    },{
        "text": "PPPoE",
        "value": "PPPOE"
    }]

    addressed_nonwan_selections = [{
        "text": "Static",
        "value": "STATIC"
    }]

    # primary/secondary only for WAN
    # need to get/show current
    dhcp_field_selections = [{
        "text": "Address Override",
        "key": "v4AutoAddressOverride",
        "allow_empty": True,
        "validators": ["ip"]
    },{
        "text": "Netmask Override",
        "key": "v4AutoPrefixOverride",
        "allow_empty": True,
        "validators": ["prefix"]
    },{
        "text": "Gateway Override",
        "key": "v4AutoGatewayOverride",
        "allow_empty": True,
        "validators": ["ip"]
    },{
        "text": "Primary DNS Override",
        "key": "v4AutoDns1Override",
        "allow_empty": True,
        "validators": ["ip"]
    },{
        "text": "Secondary DNS Override",
        "key": "v4AutoDns2Override",
        "allow_empty": True,
        "validators": ["ip"]
    }]

    static_wan_field_selections = [{
        "text": "Address",
        "key": "v4StaticAddress",
        "validators": ["empty", "ip"]
    },{
        "text": "Netmask",
        "key": "v4StaticPrefix",
        "validators": ["empty", "prefix"]
    },{
        "text": "Gateway",
        "key": "v4StaticGateway",
        "validators": ["empty", "ip"]
    },{
        "text": "Primary DNS",
        "key": "v4StaticDns1",
        "validators": ["empty", "ip"]
    },{
        "text": "Secondary DNS",
        "key": "v4StaticDns2",
        "allow_empty": True,
        "validators": ["ip"]
    }]

    static_nonwan_field_selections = [{
        "text": "Address",
        "key": "v4StaticAddress",
        "validators": ["empty", "ip"]
    },{
        "text": "Netmask",
        "key": "v4StaticPrefix",
        "validators": ["empty", "prefix"]
    }]

    # primary/secondary only if use peer dns =false
    pppoe_field_selections = [{
        "text": "Username",
        "key": "v4PPPoEUsername",
        "validators": ["empty"]
    },{
        "text": "Password",
        "key": "v4PPPoEPassword",
        "validators": ["empty"]
    },{
        "text": "Use Peer DNS",
        "key": "v4PPPoEUsePeerDns",
        "validators": ["empty"]
    },{
        "text": "Primary DNS",
        "key": "v4PPPoEDns1",
        "allow_empty": True,
        "validators": ["ip"]
    },{
        "text": "Secondary DNS",
        "key": "v4PPPoEDns1",
        "allow_empty": True,
        "validators": ["ip"]
    }]


    def __init__(self, stdscreen):
        """
        Get interfaces and integrate device and status.
        """
        self.modes = self.modes_addressed

        super(AssignInterfaces, self).__init__(stdscreen)

        uvm  = UvmContext()
        self.networkSettings = uvm.context.networkManager().getNetworkSettings()
        self.interfaceStatus = uvm.context.networkManager().getInterfaceStatus()
        self.deviceStatus = uvm.context.networkManager().getDeviceStatus()
        self.interface_selections = filter( lambda i: i['isVlanInterface'] is False, self.networkSettings["interfaces"]["list"] )
        uvm = None
        
        for interface in self.interface_selections:
            for device in self.deviceStatus["list"]:
                if interface["physicalDev"] == device["deviceName"]:
                    for k,v in device.iteritems():
                        interface[k] = v
            for interfaceStatus in self.interfaceStatus["list"]:
                if interface["interfaceId"] == interfaceStatus["interfaceId"]:
                    for k,v in interfaceStatus.iteritems():
                        interface[k] = v

        self.modes = self.modes_addressed

    def display(self):
        """
        Handle specific modes for field editing and address type.
        """
        if self.current_mode == "edit_field":
            self.mode_items[self.current_mode] = self.mode_items[self.modes[self.modes.index(self.current_mode) -1]]
        elif self.current_mode == "addressed":
            if self.mode_selected_item['interface']["isWan"] is True:
                self.mode_items[self.current_mode] = self.addressed_wan_selections
            else:
                self.mode_items[self.current_mode] = self.addressed_nonwan_selections
                self.mode_menu_pos[self.current_mode] = 0
        elif self.current_mode == "edit":
            if self.mode_selected_item["addressed"]["value"] == "DHCP":
                self.mode_items[self.current_mode] = self.dhcp_field_selections
            elif self.mode_selected_item["addressed"]["value"] == "STATIC":
                if self.mode_selected_item['interface']["isWan"] is True:
                    self.mode_items[self.current_mode] = self.static_wan_field_selections
                else:
                    self.mode_items[self.current_mode] = self.static_nonwan_field_selections
            elif self.mode_selected_item["addressed"]["value"] == "PPPOE":
                self.mode_items[self.current_mode] = self.pppoe_field_selections
        else:
            self.mode_items[self.current_mode] = getattr(self, self.current_mode + '_selections')

        super(AssignInterfaces, self).display()

    def display_interface(self, show_selected_only = False):
        """
        Display interfaces
        """
        msg = '%-12s %-17s %-5s %-10s %-10s %-18s' % ("Status", "Name", "is Wan", "Config", "Addressed", "Address/Bridged To" )
        self.display_title( msg )

        for index, interface in enumerate(self.mode_items["interface"]):
            if show_selected_only:
                select_mode = curses.A_NORMAL
                if interface != self.mode_selected_item['interface']:
                    continue
            else:
                if index == self.mode_menu_pos[self.current_mode]:
                    select_mode = curses.A_REVERSE
                else:
                    select_mode = curses.A_NORMAL

            config = interface["configType"]
            for c in self.config_selections:
                if c["value"] == config:
                    config = c["text"] 

            addressed = ""
            address = ''
            if interface["configType"] == "ADDRESSED":
                addressed = interface["v4ConfigType"]
                for a in self.addressed_wan_selections:
                    if a["value"] == addressed:
                        addressed = a["text"]

                if 'v4Address' in interface and interface['v4Address'] is not None:
                    address = interface['v4Address']
                    if  'v4PrefixLength' in interface and interface['v4PrefixLength'] is not None:
                        address = address + '/' + str(interface['v4PrefixLength'])
            elif interface["configType"] == "BRIDGED":
                for i in self.interface_selections:
                    if i["interfaceId"] == interface["bridgedTo"]:
                        address = i["name"]

            msg = '%-12s %-17s %-5s  %-10s %-10s %-18s' % (str(interface["connected"]), interface["name"], interface["isWan"], config, addressed, address,  )
            if show_selected_only is True:
                index = 0            
            self.window.addstr( self.y_pos + index, self.x_pos, msg, select_mode)

        self.y_pos = self.y_pos + 1
        if show_selected_only is False:
            self.y_pos = self.y_pos + len(self.mode_items["interface"])
            self.message( "Use [Up] and [Down] keys to select interface to edit and press [Enter]")
            self.message( "Press [Esc] to return without making changes")

    def display_config(self, show_selected_only=False):
        """
        Display interface configuration type
        """
        self.display_interface(show_selected_only=True)

        self.y_pos += 1
        msg = '%-12s' % ("Config")
        self.display_title( msg )

        for index, item in enumerate(self.mode_items["config"]):
            if show_selected_only is True:
                if item != self.mode_selected_item['config']:
                    continue
                mode = curses.A_NORMAL
            else: 
                if index == self.mode_menu_pos[self.current_mode]:
                    mode = curses.A_REVERSE
                else:
                    mode = curses.A_NORMAL

            msg = '%-12s' % (item["text"]) 
            if show_selected_only is True:
                index = 0            
            self.window.addstr( self.y_pos + index, self.x_pos, msg, mode)

        self.y_pos += 1
        if show_selected_only is False:
            self.y_pos = self.y_pos + len(self.mode_items["config"])
            self.message( "Use [Up] and [Down] keys to select config mode and press [Enter]")
            self.message( "Press [Esc] to select interface")

    def display_addressed(self, show_selected_only=False):
        """
        In address configuration mode, display address typs.
        """
        self.display_config(show_selected_only=True)

        if "addressed" not in self.mode_items:
            return

        self.y_pos += 1
        msg = '%-12s' % ("Addressed")
        self.display_title( msg )

        for index, item in enumerate(self.mode_items["addressed"]):
            if show_selected_only is True:
                if item != self.mode_selected_item['addressed']:
                    continue
                mode = curses.A_NORMAL
            else: 
                if index == self.mode_menu_pos[self.current_mode]:
                    mode = curses.A_REVERSE
                else:
                    mode = curses.A_NORMAL

            if show_selected_only is True:
                index = 0

            msg = '%-20s' % (item["text"]) 
            if show_selected_only is True:
                index = 0            
            self.window.addstr( self.y_pos + index, self.x_pos, msg, mode)

        self.y_pos += 1
        if show_selected_only is False:
            self.y_pos = self.y_pos + len(self.mode_items["addressed"])
            self.message( "Use [Up] and [Down] keys to select addressed mode and press [Enter]")
            self.message( "Press [Esc] to select config mode")

    def display_bridged(self, show_selected_only=False):
        """
        In bridged mode, display available bridged interfaces
        """
        self.display_config(show_selected_only=True)

        if len(self.bridged_selections) == 0:
            for interface in self.interface_selections:
                if interface["interfaceId"] == self.mode_selected_item["interface"]["interfaceId"]:
                    continue
                if interface["configType"] != "ADDRESSED":
                    continue
                self.bridged_selections.append({"text": interface["name"], "value": interface["interfaceId"]})

        self.y_pos += 1
        msg = '%-12s' % ("Bridged")
        self.display_title( msg )

        for index, item in enumerate(self.mode_items["bridged"]):
            if show_selected_only is True:
                if item != self.mode_selected_item['bridged']:
                    continue
                mode = curses.A_NORMAL
            else:
                if index == self.mode_menu_pos[self.current_mode]:
                    mode = curses.A_REVERSE
                else:
                    mode = curses.A_NORMAL

            if show_selected_only is True:
                index = 0

            msg = '%-20s' % (item["text"])
            if show_selected_only is True:
                index = 0
            self.window.addstr( self.y_pos + index, self.x_pos, msg, mode)

        self.y_pos += 1
        if show_selected_only is False:
            self.y_pos = self.y_pos + len(self.mode_items["bridged"])
            self.message( "Use [Up] and [Down] keys to select interface to bridge and press [Enter]")
            self.message( "Press [Esc] to select config mode")

    def display_edit(self,show_selected_only = False):
        """
        Display address field edit 
        """
        self.display_addressed(show_selected_only=True)
    
        if "edit" not in self.mode_items:
            return

        self.y_pos += 1
        msg = '%-12s' % ("Edit")
        self.display_title( msg )
        # self.y_pos += 1

        for index, item in enumerate(self.mode_items["edit"]):
            if show_selected_only is True:
                mode = curses.A_NORMAL
            else:
                if index == self.mode_menu_pos[self.current_mode]:
                    mode = curses.A_REVERSE
                    self.edit_index = self.y_pos + index
                    self.edit_item = item
                else:
                    mode = curses.A_NORMAL

            value = self.mode_selected_item['interface'][item["key"]]
            if value is None:
                value = ""

            msg = '%-30s %-40s' % (item["text"], value) 
            self.window.addstr( self.y_pos + index, self.x_pos, msg, mode)

        self.y_pos = self.y_pos + len(self.mode_items["edit"])
        if show_selected_only is False:
            self.y_pos += 1
            self.message( "Use [Up] and [Down] keys to select field to edit and press [Right]")
            self.message( "Press [Enter] to confirm")
            self.message( "Press [Esc] to change addressed mode")

    def edit_field(self, edit_index, edit_item):
        """
        Edit the specified field with a textbox.
        """
        value = self.mode_selected_item['interface'][edit_item["key"]]
        if value is None:
            value = ""
        textbox = self.textbox( edit_index, 31, str(value) )
        newValue = textbox.edit()

        invalid_messages = None
        if "validators" in edit_item:
            invalid_messages = Validator.check(newValue, edit_item["validators"] )

        if  ( "allow_empty" in edit_item ) and ( edit_item["allow_empty"] is True ) and ( len(newValue.strip()) == 0 ):
            invalid_messages = None

        if invalid_messages is not None:
            self.invalid_messages(invalid_messages)
        else:
            if newValue != value:
                self.mode_selected_item['interface'][edit_item["key"]] = newValue.strip()
            self.key = 27

    def action_interface(self):
        """
        When interface is selected, populate current values for other modes
        """
        for index, item in enumerate(self.interface_selections):
            if index == self.mode_menu_pos[self.current_mode]:
                self.mode_selected_item["config"] = item["configType"]
                for index, litem in enumerate(self.config_selections):
                    if litem["value"] == item["configType"]:
                        self.mode_menu_pos["config"] = index
                if item["configType"] == "ADDRESSED":
                    self.mode_selected_item["addressed"] = item["v4ConfigType"]
                    for index, litem in enumerate(self.addressed_wan_selections):
                        if litem["value"] == item["v4ConfigType"]:
                            self.mode_menu_pos["addressed"] = index


    def key_process(self):
        """
        Special processing extension
        """

        """
        Special edit field processing.
        """
        if self.current_mode == "edit" and self.key == curses.KEY_RIGHT:
            self.edit_field(self.edit_index, self.edit_item)
        else:
            if super(self.__class__, self).key_process() is False:
                return False

        """
        If we just came out of configuraiton mode, change the underlying
        mode list for the configuration types.
        """
        if self.current_mode == "addressed":
            if self.mode_selected_item["config"]["value"] == "BRIDGED":
                self.current_mode = 'bridged'
                self.modes = self.modes_bridged
            elif self.mode_selected_item["config"]["value"] == "ADDRESSED":
                self.current_mode = 'addressed'
                self.modes = self.modes_addressed
            elif self.mode_selected_item["config"]["value"] == "DISABLED":
                self.current_mode = 'confirm'
                self.modes = self.modes_disabled

    def action_confirm(self):
        """
        Perform confirm actions
        """
        self.window.clear()
        if self.mode_selected_item[self.current_mode] != self.confirm_selections[0]:
            return

        """
        Modify live values with modified values and save
        """
        uvm = UvmContext()
        networkSettings = uvm.context.networkManager().getNetworkSettings()
        for interface in networkSettings["interfaces"]["list"]:
            if interface["name"] == self.mode_selected_item["interface"]["name"]:
                if self.mode_selected_item["config"] is not None:
                    interface["configType"] = self.mode_selected_item["config"]["value"];
                    if interface["configType"] == "ADDRESSED":
                        if self.mode_selected_item["addressed"] is not None:
                            interface["v4ConfigType"] = self.mode_selected_item["addressed"]["value"];
                            for field in self.mode_items["edit"]:
                                if self.mode_selected_item['interface'][field["key"]] is not None:
                                    interface[field["key"]] = self.mode_selected_item['interface'][field["key"]]
                    elif interface["configType"] == "BRIDGED":
                        interface["bridgedTo"] = self.mode_selected_item['bridged']["value"]


        self.message("Saving network settings...")
        self.window.refresh()
        self.current_mode = None
        try:
            uvm.context.networkManager().setNetworkSettings(networkSettings)
        except:
            pass
        uvm = None
        return False

class Ping(Screen):
    """
    Perform ping commands to test network connectivity.
    """
    title = "Ping"
    def display(self):
        super(Ping, self).display()

        curses.echo()
        mode = curses.A_NORMAL
        self.message("Address to ping")

        try:
            curses.curs_set(1)
        except:
            pass

        address = self.window.getstr(self.y_pos -1, 31, 50)

        try:
            curses.curs_set(0)
        except:
            pass

        if len(address.strip()) == 0:
            self.process_continue = False
        else:
            self.y_pos += 2
            uvm = UvmContext()
            uvm.exec_and_get_output("ping -c 5 " + address, self )
            uvm = None

class RemoteSupport(Form):
    """
    Toggle remote support
    """
    modes = ["configure", "confirm"]
    title = "Remote Support"

    configure_selections = [{
        "text": "Enable",
        "value": True
    },{
        "text": "Disable",
        "value": False
    }]

    def __init__(self, stdscreen):
        """
        Get Remote support setting
        """
        super(RemoteSupport, self).__init__(stdscreen)

        uvm = UvmContext()
        system_settings = uvm.context.systemManager().getSettings()
        self.remote_support = system_settings["supportEnabled"]
        uvm = None

    def display_form(self):
        """
        Display current setting
        """
        self.message("Allow secure remote access to support team")
        if self.remote_support is True:
            self.message("Currently enabled")
        else:
            self.message("Currently disabled")
        self.window.refresh()

    def display_configure(self, show_selected_only=False):
        """
        Select Enable or Disable
        """
        self.y_pos += 1
        self.display_title( "Change" )

        for index, configure in enumerate(self.configure_selections):
            if show_selected_only:
                select_mode = curses.A_NORMAL
                if configure != self.mode_selected_item['configure']:
                    continue
                self.window.addstr( self.y_pos, self.x_pos, configure["text"], select_mode)
            else:
                if index == self.mode_menu_pos[self.current_mode]:
                    select_mode = curses.A_REVERSE
                else:
                    select_mode = curses.A_NORMAL
                self.window.addstr( self.y_pos + index, self.x_pos, configure["text"], select_mode)

    def action_confirm(self):
        self.window.clear()
        self.message("Changing remote support...")
        self.window.refresh()

        uvm = UvmContext()
        system_settings = uvm.context.systemManager().getSettings()
        system_settings["supportEnabled"] = self.mode_selected_item["configure"]["value"]
        system_settings = uvm.context.systemManager().setSettings(system_settings)
        uvm = None
        return False

class Upgrade(Form):
    """
    Upgrade system software
    """
    title = "Upgrade"

    confirm_selections = [{
        "text": "Yes"
    },{
        "text": "No",
        "action": False
    }]

    confirm_message = "Use [Up] and [Down] keys to confirm or cancel operation"

    upgrades_available = False
    def display_form(self):
        self.message("Checking for upgrades...")
        self.window.refresh()
        uvm = UvmContext()
        available = uvm.context.systemManager().upgradesAvailable()
        uvm = None
        if available:
            self.message("Upgrades are available")
            self.y_pos += 1
            self.message("Are you sure you want to upgrade the system?")
        else:
            self.message("No upgrades are available")
            self.y_pos += 1
            self.message("Press any key to continue")
            key = self.window.getch()
            if key != ord('F'):
                self.process_continue = False

    def action_confirm(self):
        self.window.clear()
        self.message("Upgrading...")
        self.window.refresh()

        uvm = UvmContext()
        uvm.context.systemManager().upgrade()
        uvm = None
        return False


class Reboot(Form):
    """
    Reboot system
    """
    title = "Reboot"

    confirm_selections = [{
        "text": "Yes"
    },{
        "text": "No",
        "action": False,
        "default": True
    }]

    confirm_message = "Use [Up] and [Down] keys to confirm or cancel operation"

    def display_form(self):
        self.message("Are you sure you want to reboot the system?")
        self.y_pos += 1
        self.message("This will interrupt network operations until the server has restarted.")
        self.message("This may take up to several minutes to complete.")

    def action_confirm(self):
        self.window.clear()
        self.window.refresh()

        uvm = UvmContext()
        uvm.context.rebootBox()

class Shutdown(Form):
    """
    Shutdown system
    """
    title = "Shutdown"

    confirm_selections = [{
        "text": "Yes"
    },{
        "text": "No",
        "action": False,
        "default": True
    }]

    confirm_message = "Use [Up] and [Down] keys to confirm or cancel operation"

    def display_form(self):
        self.message("Are you sure you want to shut down the system?")
        self.y_pos += 1
        self.message("This will stop all network operations.")

    def action_confirm(self):
        self.window.clear()
        self.window.refresh()

        uvm = UvmContext()
        uvm.context.shutdownBox()

class FactoryDefaults(Form):
    """
    Restore settings to factory defaults
    """
    title = "Factory Defaults"

    confirm_selections = [{
        "text": "Yes"
    },{
        "text": "No",
        "action": False,
        "default": True
    }]

    confirm_message = "Use [Up] and [Down] keys to confirm or cancel operation"

    def display_form(self):
        self.message("Are you sure you want to RESET ALL SETTINGS to factory defaults?")
        self.y_pos += 1
        self.message("ALL current settings WILL BE LOST")

    def action_confirm(self):
        self.window.clear()
        self.window.refresh()

        uvm = UvmContext()
        try:
            uvm.execute("nohup /usr/share/untangle/bin/ut-factory-defaults")
        except:
            pass
        uvm = None
        print("Connecting to Untangle...")
        sleep(30)

class suspend_curses():
    """
    Special class to handle suspension of curses for shell
    """
    def __enter__(self):
        curses.endwin()

    def __exit__(self, exc_type, exc_val, tb):
        newscr = curses.initscr()
        newscr.refresh()
        curses.doupdate()

class Shell(Screen):
    """
    Open shell
    """
    def process(self):
        self.window.clear()
        self.window.refresh()
        with suspend_curses():
            sys.exit(0)
        return

class Login(Screen):
    """
    Perform password entry for admin user
    """
    title = "Login"
    authorized = False

    def display(self):
        super(Login, self).display()

        curses.echo()
        mode = curses.A_NORMAL
        label = "Administrator password: "
        self.message(label)

        try:
            curses.curs_set(1)
        except:
            pass

        curses.noecho()
        password = self.window.getstr(self.y_pos -1, len(label), 50)

        try:
            curses.curs_set(0)
        except:
            pass

        self.y_pos += 2

        uvm = UvmContext()
        adminSettings = uvm.context.adminManager().getSettings()
        for user in adminSettings["users"]["list"]:
            if user["username"] == "admin":
                pw_hash_base64 = user['passwordHashBase64']
                pw_hash = base64.b64decode(pw_hash_base64)
                raw_pw = pw_hash[0:len(pw_hash) - 8]
                salt = pw_hash[len(pw_hash) - 8:]
                if raw_pw == md5.new(password.strip() + salt).digest():
                    self.authorized = True
                    self.process_continue = False
                else:
                    self.authorized = False
        uvm = None

        if self.authorized == False:
            self.message("Invalid password.  Press any key to try again")


class UiApp(object):
    """
    Application entry point
    """
    def __init__(self, stdscreen):
        self.screen = stdscreen
        try:
            curses.curs_set(0)
        except:
            pass

        authorized = False
        if Require_Auth:
            uvm = UvmContext()
            if uvm.context.getWizardSettings()["wizardComplete"] is True:
                login = Login(stdscreen)
                login.process()
                authorized = login.authorized
            else:
                authorized = True
            uvm = None

        if Require_Auth is False or authorized is True:
            menu_items = [{
                "text": "Remap Interfaces",
                "class": RemapInterfaces
            },{
                "text": "Configure Interface",
                "class": AssignInterfaces
            },{
                "text": "Ping",
                "class": Ping
            },{
                "text": "Remote Support",
                "class": RemoteSupport
            },{
                "text": "Upgrade",
                "class": Upgrade,
            },{
                "text": "Reboot",
                "class": Reboot
            },{
                "text": "Shutdown",
                "class": Shutdown
            },{
                "text": "Reset To Factory Defaults",
                "class": FactoryDefaults
            },{
                "text": "Exit to Shell",
                "class": Shell,
                "key": '#'
            }]

            menu = Menu(menu_items, stdscreen)
            menu.process()

def usage():
    """
    Show usage
    """
    print("usage")

def signal_handler( signla, frame):
    return

def handle_exceptions(tb):
    os.system('clear')
    traceback_list = traceback.extract_tb(tb)
    traceback_length = len(traceback_list)
    print("Configuration Console experienced the following problem:")

    max_count = 2
    count = 1
    while count <= max_count:
        traceback_args = traceback_list[traceback_length-count]
        print("\n   Line: {0}\n Method: {1}\nCommand: {2}".format(traceback_args[1], traceback_args[2], traceback_args[3]))
        count += 1
    print("\n")
    raw_input("Press [Enter] key to continue...")

def main(argv):
    global Debug
    global Require_Auth

    Require_Auth = True

    try:
        opts, args = getopt.getopt(argv, "hdr:s:t:n", ["help", "debug", "noauth"] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        if opt in ( "-d", "--debug"):
            Debug = True
        if opt in ( "--noauth"):
            Require_Auth = False

    if Debug is False:
        signal.signal(signal.SIGINT, signal_handler)

    try:
        uvm = UvmContext(tries=30)
        uvm = None
    except:
        print("Untangle is unavailable at this time")
        sys.exit(1)

    curses.wrapper(UiApp)

if __name__ == "__main__":
    while True:
        try:
            main( sys.argv[1:] )
        except SystemExit:
            break
        except:
            handle_exceptions(sys.exc_info()[2])
            pass
