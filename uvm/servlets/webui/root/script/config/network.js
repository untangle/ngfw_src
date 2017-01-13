Ung.NetworkSettingsCmp = null;
Ext.define('Webui.config.network', {
    extend: 'Ung.ConfigWin',
    statics: {
        preload: function(config, handler) {
            Ung.Main.getNetworkManager().getNetworkSettings(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                config.settings = result;
                handler(config);
            });
        }
    },
    hasReports: true,
    reportCategory: 'Network',
    gridPortForwardRules: null,
    gridNatRules: null,
    gridBypassRules: null,
    gridStaticRoutes: null,
    panelInterfaces: null,
    panelHostName: null,
    panelServices: null,
    panelPortForwardRules: null,
    panelNatRules: null,
    panelRoutes: null,
    panelDnsServer: null,
    panelDhcpServer: null,
    panelAdvanced: null,
    panelTroubleshooting: null,

    initComponent: function() {
        Ung.NetworkSettingsCmp = this;
        this.breadcrumbs = [{
            title: i18n._("Configuration"),
            action: Ext.bind(function() {
                this.cancelAction();
            }, this)
        }, {
            title: i18n._('Network')
        }];
        rpc.networkSettings = this.settings;

        // builds the tabs
        this.buildInterfaces();
        this.buildHostName();
        this.buildServices();
        this.buildPortForwardRules();
        this.buildNatRules();
        this.buildBypassRules();
        this.buildRoutes();
        this.buildDnsServer();
        this.buildDhcpServer();
        this.buildAdvanced();
        this.buildTroubleshooting();
        // builds the tab panel with the tabs
        this.buildTabPanel([ this.panelInterfaces, this.panelHostName, this.panelServices, this.panelPortForwardRules, this.panelNatRules, this.panelBypassRules, this.panelRoutes, this.panelDnsServer, this.panelDhcpServer, this.panelAdvanced, this.panelTroubleshooting ]);

        this.initialAllowSSHEnabled = false;
        var i;
        if(this.settings.inputFilterRules && this.settings.inputFilterRules.list) {
            for( i=0; i<this.settings.inputFilterRules.list.length ; i++ ) {
                rule = this.settings.inputFilterRules.list[i];
                if ( rule.description == "Allow SSH" ) {
                    this.initialAllowSSHEnabled = rule.enabled;
                    break;
                }
            }
        }
        // Check if QoS is enabled and there are some initial WANs without downloadBandwidthKbps or uploadBandwidthKbps limits set and mark dirty if true,
        // in order to make the user save the valid settings when new WANs are added
        if(this.settings.qosSettings.qosEnabled) {
            for( i=0 ; i<this.settings.interfaces.list.length ; i++) {
                var intf =this.settings.interfaces.list[i];
                if(intf.isWan && (Ext.isEmpty(intf.downloadBandwidthKbps) || Ext.isEmpty(intf.uploadBandwidthKbps))) {
                    this.markDirty();
                    break;
                }
            }
        }

        var blockReplayPacketsCheckbox = this.panelAdvanced.down('checkbox[name=blockReplayPacketsCheckbox]');
        blockReplayPacketsCheckbox.setVisible(rpc.isExpertMode);

        this.callParent(arguments);
        Ext.defer(function() {
            this.loadDeviceAndInterfaceStatus(false);
            if(!Ext.isEmpty(this.activeTabIndex)) {
                this.tabs.setActiveTab(this.activeTabIndex);
                this.advancedTabPanel.setActiveTab(this.advancedTabIndex);
            }
        },100, this);
    },
    getPortForwardConditions: function () {
        return [
            {name:"DST_LOCAL",displayName: i18n._("Destined Local"), type: "boolean", visible: true},
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
        ];
    },
    getNatRuleConditions: function () {
        return [
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
        ];
    },
    getBypassRuleConditions: function () {
        return [
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"]], visible: true}
        ];
    },
    getQosRuleConditions: function () {
        return [
            {name:"DST_LOCAL",displayName: i18n._("Destined Local"), type: "boolean", visible: true},
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"]], visible: true},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode}
        ];
    },
    getUpnpRuleConditions: function () {
        return [
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: true}
        ];
    },
    getFilterRuleConditions: function () {
        return [
            {name:"DST_LOCAL",displayName: i18n._("Destined Local"), type: "boolean", visible: true},
            {name:"DST_ADDR",displayName: i18n._("Destination Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"DST_PORT",displayName: i18n._("Destination Port"), type: "text",vtype:"portMatcher", visible: true},
            {name:"DST_INTF",displayName: i18n._("Destination Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
            {name:"SRC_MAC" ,displayName: i18n._("Source MAC"), type: "text", visible: true},
            {name:"SRC_ADDR",displayName: i18n._("Source Address"), type: "text", visible: true, vtype:"ipMatcher"},
            {name:"SRC_PORT",displayName: i18n._("Source Port"), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
            {name:"SRC_INTF",displayName: i18n._("Source Interface"), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
            {name:"PROTOCOL",displayName: i18n._("Protocol"), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
        ];
    },
    getHostname: function() {
        var host = "";
        var domain = "";
        try {
            host = this.settings.hostName;
            domain = this.settings.domainName;
        } catch (e) {
            Ung.Util.rpcExHandler(e);
        }
        if ( domain !== null && domain !== "" )
            return host + "." + domain;
        else
            return host;
    },
    //asynchronous load of device status and interface status
    loadDeviceAndInterfaceStatus: function(refresh) {
        if(refresh) {
            Ext.MessageBox.wait(i18n._("Refreshing..."), i18n._("Please wait"));
        }
        Ung.Main.getNetworkManager().getDeviceStatus(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var deviceStatusMap=Ung.Util.createRecordsMap(( result == null ? [] : result.list ), "deviceName");
            Ung.Main.getNetworkManager().getInterfaceStatus(Ext.bind(function(result, exception) {
                var interfaceStatusMap=Ung.Util.createRecordsMap(result.list, "interfaceId");
                var i, intf, devSt, intfSt;
                for(i=0 ; i<this.settings.interfaces.list.length ; i++) {
                    intf=this.settings.interfaces.list[i];
                    devSt = deviceStatusMap[intf.physicalDev];
                    if(devSt) {
                        Ext.apply(intf, {
                            "deviceName": devSt.deviceName,
                            "macAddress": devSt.macAddress,
                            "duplex": devSt.duplex,
                            "vendor": devSt.vendor,
                            "mbit": devSt.mbit,
                            "connected": devSt.connected
                        });
                    }
                    intfSt = interfaceStatusMap[intf.interfaceId];
                    if(intfSt) {
                        Ext.apply(intf, {
                            "v4Address": intfSt.v4Address,
                            "v4Netmask": intfSt.v4Netmask,
                            "v4Gateway": intfSt.v4Gateway,
                            "v4Dns1": intfSt.v4Dns1,
                            "v4Dns2": intfSt.v4Dns2,
                            "v4PrefixLength": intfSt.v4PrefixLength,
                            "v6Address": intfSt.v6Address,
                            "v6Gateway": intfSt.v6Gateway,
                            "v6PrefixLength": intfSt.v6PrefixLength
                        });
                    }
                }
                var grid = this.gridInterfaces;
                grid.getStore().suspendEvents();
                grid.getStore().each(function( currentRow ) {
                    var isDirty = currentRow.dirty;
                    var deviceStatus = deviceStatusMap[currentRow.get("physicalDev")];
                    var interfaceStatus = interfaceStatusMap[currentRow.get("interfaceId")];
                    var isWirelessIntf = currentRow.get("isWirelessInterface");
                    var duplexStatus;
                    if(deviceStatus) {
                        if(!refresh) {
                            currentRow.set({
                                "deviceName": deviceStatus.deviceName
                            });
                        }
                        if (isWirelessIntf)
                            duplexStatus = "HALF_DUPLEX";
                        else
                            duplexStatus = deviceStatus.duplex;

                        currentRow.set({
                            "macAddress": deviceStatus.macAddress,
                            "duplex": duplexStatus,
                            "vendor": deviceStatus.vendor,
                            "mbit": deviceStatus.mbit,
                            "connected": deviceStatus.connected
                        });
                    }
                    if(interfaceStatus) {
                        currentRow.set({
                            "v4Address": interfaceStatus.v4Address,
                            "v4Netmask": interfaceStatus.v4Netmask,
                            "v4Gateway": interfaceStatus.v4Gateway,
                            "v4Dns1": interfaceStatus.v4Dns1,
                            "v4Dns2": interfaceStatus.v4Dns2,
                            "v4PrefixLength": interfaceStatus.v4PrefixLength,
                            "v6Address": interfaceStatus.v6Address,
                            "v6Gateway": interfaceStatus.v6Gateway,
                            "v6PrefixLength": interfaceStatus.v6PrefixLength
                        });
                    }
                    //To prevent coloring the row when status is changed
                    if(!isDirty && (deviceStatus || interfaceStatus)) {
                        currentRow.commit();
                    }
                });
                grid.getStore().resumeEvents();
                grid.getView().refresh();

                //--Build port forward reservations warnings--
                var portForwardWarningsHtml=[];
                for (i = 0; i < this.settings.interfaces.list.length; i++) {
                    intf = this.settings.interfaces.list[i];
                    if (intf.v4Address) {
                        portForwardWarningsHtml.push( Ext.String.format("<b>{0}:{1}</b> ",intf.v4Address, this.settings.httpsPort)+ i18n._("for HTTPS services.") + "<br/>");
                    }
                }
                for ( i = 0 ; i < this.settings.interfaces.list.length ; i++) {
                    intf = this.settings.interfaces.list[i];
                    if (intf.v4Address && !intf.isWan) {
                        portForwardWarningsHtml.push( Ext.String.format("<b>{0}:{1}</b> ",intf.v4Address, this.settings.httpPort)+i18n._("for HTTP services.")+"<br/>");
                    }
                }
                for ( i = 0 ; i < this.settings.interfaces.list.length ; i++) {
                    intf = this.settings.interfaces.list[i];
                    if (intf.v4Address && intf.isWan) {
                        for ( var j = 0 ; j < this.settings.interfaces.list.length ; j++) {
                            var sub_intf = this.settings.interfaces.list[j];
                            if (sub_intf.configType == "BRIDGED" && sub_intf.bridgedTo == intf.interfaceId) {
                                portForwardWarningsHtml.push( Ext.String.format("<b>{0}:{1}</b> ",intf.v4Address, this.settings.httpPort) +
                                                              i18n._("on") +
                                                              Ext.String.format(" {2} ",sub_intf.name) +
                                                              i18n._("for HTTP services.")+"<br/>");
                            }
                        }
                    }
                }
                this.panelPortForwardRules.down('component[name="portForwardWarnings"]').update(portForwardWarningsHtml.join(""));
                //--------
                if(refresh) {
                    Ext.MessageBox.hide();
                }
            }, this));
        }, this));
    },
    getWirelessChannelsMap: function() {
        if(!this.wirelessChannelsMap) {
            this.wirelessChannelsMap = {
                "-1": [-1, i18n._("Automatic 2.4 GHz")],
                "-2": [-2, i18n._("Automatic 5 GHz")],
                "1": [1, i18n._("1 - 2.412 GHz")],
                "2": [2, i18n._("2 - 2.417 GHz")],
                "3": [3, i18n._("3 - 2.422 GHz")],
                "4": [4, i18n._("4 - 2.427 GHz")],
                "5": [5, i18n._("5 - 2.432 GHz")],
                "6": [6, i18n._("6 - 2.437 GHz")],
                "7": [7, i18n._("7 - 2.442 GHz")],
                "8": [8, i18n._("8 - 2.447 GHz")],
                "9": [9, i18n._("9 - 2.452 GHz")],
                "10": [10, i18n._("10 - 2.457 GHz")],
                "11": [11, i18n._("11 - 2.462 GHz")],
                "12": [12, i18n._("12 - 2.467 GHz")],
                "13": [13, i18n._("13 - 2.472 GHz")],
                "14": [14, i18n._("14 - 2.484 GHz")],
                "36": [36, i18n._("36 - 5.180 GHz")],
                "40": [40, i18n._("40 - 5.200 GHz")],
                "44": [44, i18n._("44 - 5.220 GHz")],
                "48": [48, i18n._("48 - 5.240 GHz")],
                "52": [52, i18n._("52 - 5.260 GHz")],
                "56": [56, i18n._("56 - 5.280 GHz")],
                "60": [60, i18n._("60 - 5.300 GHz")],
                "64": [64, i18n._("64 - 5.320 GHz")],
                "100": [100, i18n._("100 - 5.500 GHz")],
                "104": [104, i18n._("104 - 5.520 GHz")],
                "108": [108, i18n._("108 - 5.540 GHz")],
                "112": [112, i18n._("112 - 5.560 GHz")],
                "116": [116, i18n._("116 - 5.580 GHz")],
                "120": [120, i18n._("120 - 5.600 GHz")],
                "124": [124, i18n._("124 - 5.620 GHz")],
                "128": [128, i18n._("128 - 5.640 GHz")],
                "132": [132, i18n._("132 - 5.660 GHz")],
                "136": [136, i18n._("136 - 5.680 GHz")],
                "140": [140, i18n._("140 - 5.700 GHz")],
                "144": [144, i18n._("144 - 5.720 GHz")],
                "149": [149, i18n._("149 - 5.745 GHz")],
                "153": [153, i18n._("153 - 5.765 GHz")],
                "157": [157, i18n._("157 - 5.785 GHz")],
                "161": [161, i18n._("161 - 5.805 GHz")],
                "165": [165, i18n._("165 - 5.825 GHz")]
            };
        }
        return this.wirelessChannelsMap;
    },
    buildInterfaceStatus: function() {
        if(this.winInterfaceStatus) return;
        var me = this;
        this.gridIfconfigLists = Ext.create( 'Ung.grid.Panel', {
            name: 'Interface Status',
            margin: 5,
            title: i18n._('Interface Status'),
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            hasEdit: false,
            initialLoad: function() {}, //Don't load automatically
            dataFn: function(handler) {
                var command1 = "ifconfig "+this.symbolicDev+" | grep 'Link\\|packets' | grep -v inet6 | tr '\\n' ' ' | tr -s ' ' ";
                var command2 = "ifconfig "+this.symbolicDev+" | grep 'inet addr' | tr -s ' ' | cut -c 7- ";
                var command3 = "ifconfig "+this.symbolicDev+" | grep inet6 | grep Global | cut -d' ' -f 13";
                Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var v6addr = result;
                    Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        if(Ext.isEmpty(result)) {
                            return;
                        }
                        if (result.search("Device not found") >=0) {
                            return;
                        }
                        var lineparts = result.split(" ");
                        var intf = lineparts[0];
                        var macAddress = "";
                        var rxpkts = "";
                        var rxerr = "";
                        var rxdrop = "";
                        var txpkts = "";
                        var txerr = "";
                        var txdrop = "";

                        if (result.search("Ethernet") >= 0) {
                            macAddress = lineparts[4];
                            rxpkts = lineparts[6].split(":")[1];
                            rxerr = lineparts[7].split(":")[1];
                            rxdrop = lineparts[8].split(":")[1];
                            txpkts = lineparts[12].split(":")[1];
                            txerr = lineparts[13].split(":")[1];
                            txdrop = lineparts[14].split(":")[1];
                        }
                        if (result.search("Point-to-Point") >= 0) {
                            macAddress = "";
                            rxpkts = lineparts[5].split(":")[1];
                            rxerr = lineparts[6].split(":")[1];
                            rxdrop = lineparts[7].split(":")[1];
                            txpkts = lineparts[11].split(":")[1];
                            txerr = lineparts[12].split(":")[1];
                            txdrop = lineparts[13].split(":")[1];
                        }

                        Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;

                            var address = "";
                            var mask = "";
                            if(!Ext.isEmpty(result)) {
                                var linep = result.split(" ");
                                address = linep[0].split(":")[1];
                                mask = linep[2].split(":")[1];
                            }

                            var config=[{
                                intf: intf,
                                macAddress: macAddress,
                                address: address,
                                mask: mask,
                                v6addr: v6addr,
                                rxpkts: rxpkts,
                                rxerr: rxerr,
                                rxdrop: rxdrop,
                                txpkts: txpkts,
                                txerr: txerr,
                                txdrop: txdrop
                            }];

                            handler({list: config});

                        }, this), command2);
                    }, this), command1);
                }, this), command3);
            },
            fields: [{
                name: "intf"
            },{
                name: "macAddress"
            },{
                name: "address",
                sortType: 'asIp'
            },{
                name: "mask"
            },{
                name: "v6addr"
            },{
                name: "rxpkts"
            },{
                name: "rxerr"
            },{
                name: "rxdrop"
            },{
                name: "txpkts"
            },{
                name: "txerr"
            },{
                name: "txdrop"
            }],
            columns: [{
                header: i18n._("Device"),
                dataIndex:'intf',
                width: 150
            },{
                header: i18n._("MAC Address"),
                dataIndex:'macAddress',
                width: 150
            },{
                header: i18n._("IPv4 Address"),
                dataIndex:'address',
                width: 110
            },{
                header: i18n._("Mask"),
                dataIndex:'mask',
                width: 110
            },{
                header: i18n._("IPv6"),
                dataIndex:'v6addr',
                width: 240
            },{
                header: i18n._("Rx Packets"),
                dataIndex:'rxpkts',
                width: 50,
                flex: 1
            },{
                header: i18n._("Rx Errors"),
                dataIndex:'rxerr',
                width: 30,
                flex: 1
            },{
                header: i18n._("Rx Drop"),
                dataIndex:'rxdrop',
                width: 30,
                flex: 1
            },{
                header: i18n._("Tx Packets"),
                dataIndex:'txpkts',
                width: 50,
                flex: 1
            },{
                header: i18n._("Tx Errors"),
                dataIndex:'txerr',
                width: 30,
                flex: 1
            },{
                header: i18n._("Tx Drop"),
                dataIndex:'txdrop',
                width: 30,
                flex: 1
            }]
        });

        this.gridArpLists = Ext.create( 'Ung.grid.Panel', {
            name: 'ARP Table',
            margin: 5,
            flex: 2,
            title: i18n._('ARP Table'),
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            hasEdit: false,
            initialLoad: function() {}, //Don't load automatically
            dataFn: function(handler) {
                var arpCommand = "arp -n | grep "+this.symbolicDev+" | grep -v incomplete > /tmp/arp.txt ; cat /tmp/arp.txt";
                Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var lines = Ext.isEmpty(result) ? []: result.split("\n");
                    var lparts, connections = [];
                    for (var i = 0 ; i < lines.length ; i++ ) {
                        if ( !Ext.isEmpty(lines[i]) ) {
                            lparts = lines[i].split(/\s+/);
                            connections.push({
                                address: lparts[0],
                                type: lparts[1],
                                macAddress: lparts[2]
                            });
                        }
                    }
                    handler({list: connections});
                }, this), arpCommand);
            },
            fields: [{
                name: "macAddress"
            },{
                name: "address",
                sortType: 'asIp'
            },{
                name: "type"
            }],
            columns: [{
                header: i18n._("MAC Address"),
                dataIndex:'macAddress',
                width: 150
            },{
                header: i18n._("IP Address"),
                dataIndex:'address',
                width: 200
            },{
                header: i18n._("Type"),
                dataIndex:'type',
                width: 150
            }]
        });

        this.gridWirelessLists = Ext.create( 'Ung.grid.Panel', {
            name: 'Wireless Connections',
            margin: 5,
            flex: 3,
            title: i18n._('Wireless Connections'),
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            hasEdit: false,
            initialLoad: function() {}, //Don't load automatically
            dataFn: function(handler) {
                var dnsmasqCommand = "cat /var/lib/misc/dnsmasq.leases";
                var connectionsCommand = "/sbin/iw dev "+this.systemDev+" station dump | grep 'Station\\|bytes\\|packets' |tr '\\t' ' ' ";
                var arpCommand = "cat /tmp/arp.txt";
                var addressMap = {};

                Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;

                    var lines = Ext.isEmpty(result) ? []: result.split("\n");
                    var lparts;
                    for ( i = 0 ; i < lines.length ; i++ ) {
                        if ( !Ext.isEmpty(lines[i])) {
                            lparts = lines[i].split(/\s+/);
                            addressMap[lparts[1]] = lparts[2];
                        }
                    }
                }, this), dnsmasqCommand);

                Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var lines = Ext.isEmpty(result) ? []: result.split("\n");
                    var lparts, connections = [];
                    for (var i = 0 ; i < lines.length ; i++ ) {
                        if ( !Ext.isEmpty(lines[i]) ) {
                            lparts = lines[i].split(/\s+/);
                            addressMap[lparts[2]] = lparts[0];
                        }
                    }

                    Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        var lines = Ext.isEmpty(result) ? []: result.split("\n");
                        var total = Math.floor(lines.length/5) ;
                        var ptr, macAddress, connections = [];

                        for (var i = 0 ; i < total ; i++ ) {
                            if ( Ext.isEmpty(lines[i])) continue;
                            ptr = i*5;
                            macAddress =lines[ptr].split(" ")[1];
                            connections.push( {
                                macAddress: macAddress,
                                address: addressMap[macAddress],
                                rxbytes: lines[ptr+1].split(" ")[3],
                                rxpackets: lines[ptr+2].split(" ")[3],
                                txbytes: lines[ptr+3].split(" ")[3],
                                txpackets: lines[ptr+4].split(" ")[3]
                            });
                        }
                        handler({list: connections});
                    }, this), connectionsCommand);
                }, this), arpCommand);
            },
            fields: [{
                name: "macAddress"
            },{
                name: "address",
                sortType: 'asIp'
            },{
                name: "rxbytes"
            },{
                name: "rxpackets"
            },{
                name: "txbytes"
            },{
                name: "txpackets"
            }],
            columns: [{
                header: i18n._("MAC Address"),
                dataIndex:'macAddress',
                width: 150
            },{
                header: i18n._("IP Address"),
                dataIndex:'address',
                width: 200
            },{
                header: i18n._("Rx Bytes"),
                dataIndex:'rxbytes',
                width: 150
            },{
                header: i18n._("Rx Packets"),
                dataIndex:'rxpackets',
                width: 150
            },{
                header: i18n._("Tx Bytes"),
                dataIndex:'txbytes',
                width: 150
            },{
                header: i18n._("Tx Packets"),
                dataIndex:'txpackets',
                width: 150
            }]
        });

        this.winInterfaceStatus = Ext.create('Ung.EditWindow', {
            helpSource: 'network_interface_status',
            breadcrumbs: [{
                title: i18n._("Interface"),
                action: Ext.bind(function() {
                    this.winWirelessConnections.cancelAction();
                }, this)
            }, {
                title: i18n._("Interface Status")
            }],
            bbar: ["-",{
                iconCls: 'icon-help',
                text: i18n._('Help'),
                handler: function() {
                    this.winInterfaceStatus.helpAction();
                },
                scope: this
            },'-',{
                xtype: "button",
                iconCls: 'icon-refresh',
                text: i18n._("Refresh"),
                handler: function() {
                    this.gridIfconfigLists.reload();
                    this.gridArpLists.reload();
                    this.gridWirelessLists.reload();

                    this.winInterfaceStatus.show();
                },
                scope : this
            },'->',{
                name: "Close",
                iconCls: 'cancel-icon',
                text: i18n._('Cancel'),
                handler: function() {
                    this.winInterfaceStatus.cancelAction();
                },
                scope: this
            }],
            items: [{
                xtype: 'panel',
                layout: { type: 'vbox', align: 'stretch' },
                border: false,
                items: [this.gridIfconfigLists, this.gridArpLists, this.gridWirelessLists]
            }]
        });

        this.subCmps.push(this.winInterfaceStatus);
    },
    // Interfaces Panel
    buildInterfaces: function() {
        var settingsCmp = this;
        var deleteVlanColumn = Ext.create('Ext.grid.column.Action', {
            menuDisabled:true,
            resizable: false,
            hideable: false,
            header: i18n._("Delete"),
            width: 50,
            init:function(grid) {
                this.grid=grid;
            },
            handler: function(view, rowIndex, colIndex, item, e, record) {
                if( record.get("isVlanInterface") || record.get("connected")=='MISSING' ) {
                    this.grid.deleteHandler(record);
                }
            },
            getClass: function(value, metadata, record) {
                if( record.get("isVlanInterface") || record.get("connected")=='MISSING' ) {
                    return 'icon-delete-row';
                } else {
                    return 'x-hide-display';
                }
            }
        });

        var intfStatus = Ext.create('Ext.grid.column.Action', {
            menuDisabled:true,
            resizable: false,
            hideable: false,
            header: i18n._("Status"),
            width: 50,
            init:function(grid) {
                this.grid=grid;
            },
            handler: function(view, rowIndex, colIndex, item, e, record) {
                this.grid.onIntfStatus(record.get("symbolicDev"), record.get("systemDev"), record.get("isWirelessInterface"));
            },
            getClass: function(value, metadata, record) {
                if( record.get("configType") === "DISABLED") {
                    return 'x-hide-display';
                } else if( record.get("isWirelessInterface")) {
                    return 'icon-row icon-wireless';
                } else {
                    return 'icon-detail-row';
                }
            }
        });

        var duplexRenderer = Ext.bind(function(value) {
            return (value=="FULL_DUPLEX")?i18n._("full-duplex") : (value=="HALF_DUPLEX") ? i18n._("half-duplex") : i18n._("unknown");
        }, this);

        this.gridInterfaces = Ext.create('Ung.grid.Panel',{
            flex: 1,
            name: 'Interfaces',
            settingsCmp: this,
            hasReorder: false,
            hasDelete: false,
            hasAdd: false,
            addAtTop: false,
            enableColumnHide: true,
            columnMenuDisabled: false,
            title: i18n._("Interfaces"),
            dataProperty: "interfaces",
            recordJavaClass: "com.untangle.uvm.network.InterfaceSettings",
            emptyRow: { //Used only to add VLAN Interfaces
                "interfaceId": -1,
                "isVlanInterface": true,
                "isWirelessInterface": false,
                "vlanTag": 1,
                "v4ConfigType": "STATIC",
                "v6ConfigType": "DISABLED",
                "wirelessEncryption": null
            },
            fields: [{
                name: 'interfaceId'
            }, {
                name: 'name'
            }, {
                name: 'physicalDev'
            }, {
                name: 'systemDev'
            }, {
                name: 'symbolicDev'
            }, {
                name: 'hidden'
            }, {
                name: 'imqDev'
            }, {
                name: 'isWan'
            }, {
                name: 'isVlanInterface'
            }, {
                name: 'vlanTag'
            }, {
                name: 'vlanParent'
            }, {
                name: 'configType'
            }, {
                name: 'bridgedTo'
            }, {
                name: 'v4ConfigType'
            }, {
                name: 'v4StaticAddress'
            }, {
                name: 'v4StaticPrefix'
            }, {
                name: 'v4StaticGateway'
            }, {
                name: 'v4StaticDns1'
            }, {
                name: 'v4StaticDns2'
            }, {
                name: 'v4AutoAddressOverride'
            }, {
                name: 'v4AutoPrefixOverride'
            }, {
                name: 'v4AutoGatewayOverride'
            }, {
                name: 'v4AutoDns1Override'
            }, {
                name: 'v4AutoDns2Override'
            }, {
                name: 'v4PPPoEUsername'
            }, {
                name: 'v4PPPoEPassword'
            }, {
                name: 'v4PPPoEUsePeerDns'
            }, {
                name: 'v4PPPoEDns1'
            }, {
                name: 'v4PPPoEDns2'
            }, {
                name: 'v4NatEgressTraffic'
            }, {
                name: 'v4NatIngressTraffic'
            }, {
                name: 'v4Aliases'
            }, {
                name: 'v6ConfigType'
            }, {
                name: 'v6StaticAddress'
            }, {
                name: 'v6StaticPrefixLength'
            }, {
                name: 'v6StaticGateway'
            }, {
                name: 'v6StaticDns1'
            }, {
                name: 'v6StaticDns2'
            }, {
                name: 'v6Aliases'
            }, {
                name: 'raEnabled'
            }, {
                name: 'dhcpEnabled'
            }, {
                name: 'dhcpRangeStart'
            }, {
                name: 'dhcpRangeEnd'
            }, {
                name: 'dhcpLeaseDuration'
            }, {
                name: 'dhcpGatewayOverride'
            }, {
                name: 'dhcpPrefixOverride'
            }, {
                name: 'dhcpDnsOverride'
            }, {
                name: 'dhcpOptions'
            }, {
                name: 'downloadBandwidthKbps'
            }, {
                name: 'uploadBandwidthKbps'
            }, {
                name: 'vrrpEnabled'
            }, {
                name: 'vrrpId'
            }, {
                name: 'vrrpPriority'
            }, {
                name: 'vrrpAliases'
            }, {
                name: 'isWirelessInterface'
            }, {
                name: 'wirelessSsid'
            }, {
                name: 'wirelessEncryption'
            }, {
                name: 'wirelessPassword'
            }, {
                name: 'wirelessChannel'
            }, {
                name: 'javaClass'
            },
            {
                name: "deviceName" //from deviceStatus
            }, {
                name: "macAddress" //from deviceStatus
            }, {
                name: "connected" //from deviceStatus
            }, {
                name: "duplex" //from deviceStatus
            }, {
                name: "vendor" //from deviceStatus
            }, {
                name: "mbit" //from deviceStatus
            },
            {
                name: "v4Address", //from interfaceStatus
                sortType: 'asIp'
            }, {
                name: "v4Netmask" //from interfaceStatus
            }, {
                name: "v4Gateway" //from interfaceStatus
            }, {
                name: "v4Dns1" //from interfaceStatus
            }, {
                name: "v4Dns2" //from interfaceStatus
            }, {
                name: "v4PrefixLength" //from interfaceStatus
            }],
            plugins: [deleteVlanColumn, intfStatus],
            columns: [{
                header: i18n._("Id"),
                width: 35,
                dataIndex: 'interfaceId',
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                header: i18n._("Name"),
                dataIndex: 'name',
                width:120
            }, {
                header: i18n._( "Connected" ),
                dataIndex: 'connected',
                sortable: false,
                width: 110,
                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                    if (Ext.isEmpty(value)) return "<div style='height:16px;'></div>";
                    var divClass = "ua-cell-disabled";
                    var connectedStr = i18n._("unknown");
                    if ( value == "CONNECTED" ) {
                        connectedStr = i18n._("connected");
                        divClass = "ua-cell-enabled";
                    } else if ( value == "DISCONNECTED" ) {
                        connectedStr = i18n._("disconnected");
                    } else if ( value == "MISSING" ) {
                        connectedStr = i18n._("missing");
                    }
                    var title = record.get("mbit") + " " + duplexRenderer(record.get("duplex"));
                    return "<div class='" + divClass + "' title='"+title+"'>" + connectedStr + "</div>";
                }, this)
            }, {
                header: i18n._("Device"),
                dataIndex: 'physicalDev',
                width:50,
                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                    if (record.get("isVlanInterface")) {
                        return record.get("systemDev");
                    }
                    return value;
                }, this)
            },{
                hidden: true,
                header: i18n._("Physical Dev"),
                dataIndex: 'physicalDev',
                width:80
            }, {
                hidden: true,
                header: i18n._("System Dev"),
                dataIndex: 'systemDev',
                width:80
            }, {
                hidden: true,
                header: i18n._("Symbolic Dev"),
                dataIndex: 'symbolicDev',
                width:80
            }, {
                hidden: true,
                header: i18n._("IMQ Dev"),
                dataIndex: 'imqDev',
                width:80
            }, {
                header: i18n._("Speed"),
                dataIndex: 'mbit',
                width:50
            }, {
                hidden: true,
                header: i18n._( "Duplex" ),
                dataIndex: 'duplex',
                width: 100,
                renderer: duplexRenderer
            }, {
                header: i18n._("Config"),
                dataIndex: 'configType',
                width: 90,
                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                    switch(value) {
                      case "ADDRESSED":
                        return i18n._("Addressed");
                      case "BRIDGED":
                        return i18n._("Bridged");
                      case "DISABLED":
                        return i18n._("Disabled");
                    default:
                        return i18n._(value);
                    }
                }, this)
            }, {
                header: i18n._("Current Address"),
                dataIndex: 'v4Address',
                width:130,
                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                    if (Ext.isEmpty(value)) return "";
                    return value + "/" + record.data.v4PrefixLength;
                }, this)
            }, {
                header: i18n._("is WAN"),
                dataIndex: 'isWan',
                width:50,
                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                    // only ADDRESSED interfaces can be WANs
                    return (record.data.configType == 'ADDRESSED') ? (value ? i18n._("true"):i18n._("false")): ""; // if its addressed return value
                }, this)
            }, deleteVlanColumn, intfStatus],
            bbar: ['-',{
                xtype: "button",
                name: "remap_interfaces",
                iconCls: 'icon-drag',
                text: i18n._("Remap Interfaces"),
                handler: function() {
                    this.gridInterfaces.onMapDevices();
                },
                scope : this
            },'-',{
                xtype: "button",
                iconCls: 'icon-refresh',
                text: i18n._("Refresh"),
                handler: function() {
                    this.loadDeviceAndInterfaceStatus(true);
                },
                scope : this
            },'-',{
                xtype: "button",
                hidden: !this.settings.vlansEnabled,
                text : i18n._( "Add Tagged VLAN Interface" ),
                iconCls : "icon-add-row",
                handler: function() {
                    this.gridInterfaces.addHandler();
                },
                scope : this
            }],
            onIntfStatus: Ext.bind(function(symbolicDev, systemDev, isWirelessInterface) {
                this.buildInterfaceStatus();

                this.gridIfconfigLists.symbolicDev = symbolicDev;
                this.gridArpLists.symbolicDev = symbolicDev;
                this.gridWirelessLists.systemDev = systemDev;
                this.gridWirelessLists.setVisible(isWirelessInterface);

                this.gridIfconfigLists.reload();
                this.gridArpLists.reload();
                if(isWirelessInterface) {
                    this.gridWirelessLists.reload();
                }

                this.winInterfaceStatus.show();
            }, this),
            onMapDevices: Ext.bind(function() {
                Ext.MessageBox.wait(i18n._("Loading device mapper..."), i18n._("Please wait"));
                if (!this.winMapDevices) {
                    this.mapDevicesStore = Ext.create('Ext.data.JsonStore',{
                        fields:[{name: "interfaceId"}, { name: "name" }, {name: "deviceName"}, { name: "physicalDev" }, { name: "systemDev" },{ name: "symbolicDev" }, { name: "macAddress" }, { name: "connected" }, { name: "duplex" }, { name: "vendor" }, { name: "mbit" }]
                    });
                    this.availableDevicesStore = Ext.create('Ext.data.JsonStore',{
                        fields:[{ name: "physicalDev" }]
                    });

                    this.gridMapDevices = Ext.create('Ext.grid.Panel', {
                        flex: 1,
                        margin: '5 5 5 5',
                        width: 300,
                        store: this.mapDevicesStore,
                        loadMask: true,
                        stripeRows: true,
                        settingsCmp: this,
                        enableColumnResize: true,
                        enableColumnHide: false,
                        enableColumnMove: false,
                        selModel: Ext.create('Ext.selection.RowModel', {singleSelect: true}),
                        plugins: [
                            Ext.create('Ext.grid.plugin.CellEditing', {
                                clicksToEdit: 1
                            })
                        ],
                        bbar: [{
                            xtype: "button",
                            iconCls: 'icon-refresh',
                            text: i18n._("Refresh Device Status"),
                            handler: function() {
                                this.gridMapDevices.onRefreshDeviceStatus();
                            },
                            scope : this
                        }],
                        viewConfig:{
                            forceFit: true,
                            disableSelection: false,
                            plugins:{
                                ptype: 'gridviewdragdrop',
                                dragText: i18n._('Drag and drop to reorganize')
                            },
                            listeners: {
                                "drop": {
                                    fn:  Ext.bind(function(node, data, overModel, dropPosition, eOpts) {
                                        var i = 0;
                                        this.mapDevicesStore.each( Ext.bind(function( currentRow ) {
                                            var intf=this.currentInterfaces[i];
                                            currentRow.set({
                                                "interfaceId": intf.interfaceId,
                                                "name": intf.name
                                            });
                                            i++;
                                        }, this));
                                        return true;
                                    },this )
                                }
                            }
                        },
                        columns: [{
                            header: i18n._("Name"),
                            dataIndex: 'name',
                            sortable: false,
                            width: 130,
                            renderer: function( value ) {
                                return i18n._( value );
                            }
                        }, {
                            xtype: 'templatecolumn',
                            menuDisabled: true,
                            resizable: false,
                            width: 40,
                            tpl: '<img src="'+Ext.BLANK_IMAGE_URL+'" class="icon-drag"/>'
                        }, {
                            header: i18n._( "Device" ),
                            tooltip: i18n._( "Click on a Device to open a combo and choose the desired Device from a list. When another Device is selected the 2 Devices are switched." ),
                            dataIndex: 'deviceName',
                            sortable: false,
                            width: 170,
                            tdCls: 'ua-pointer',
                            editor:{
                                xtype: 'combo',
                                store: this.availableDevicesStore,
                                valueField: 'physicalDev',
                                displayField: 'physicalDev',
                                queryMode: 'local',
                                editable: false,
                                listeners: {
                                    "change": {
                                        fn: Ext.bind(function(elem, newValue, oldValue) {
                                            var sourceRecord = null;
                                            var targetRecord = null;
                                            this.mapDevicesStore.each( function( currentRow ) {
                                                if(oldValue==currentRow.get( "physicalDev" )) {
                                                    sourceRecord=currentRow;
                                                } else if(newValue==currentRow.get( "physicalDev" )) {
                                                    targetRecord=currentRow;
                                                }
                                                if(sourceRecord !== null && targetRecord !== null) {
                                                    return false;
                                                }
                                                return true;
                                            });
                                            if(sourceRecord === null || targetRecord === null || sourceRecord === targetRecord) {
                                                console.log(oldValue, newValue, sourceRecord, targetRecord);
                                                return false;
                                            }
                                            var soruceData = Ext.decode(Ext.encode(sourceRecord.data));
                                            var targetData = Ext.decode(Ext.encode(targetRecord.data));
                                            soruceData.deviceName=oldValue;
                                            targetData.deviceName=newValue;
                                            sourceRecord.set({
                                                "physicalDev": targetData.physicalDev,
                                                "systemDev": targetData.systemDev,
                                                "symbolicDev": targetData.symbolicDev,
                                                "macAddress": targetData.macAddress,
                                                "duplex": targetData.duplex,
                                                "vendor": targetData.vendor,
                                                "mbit": targetData.mbit,
                                                "connected": targetData.connected
                                            });
                                            targetRecord.set({
                                                "deviceName": soruceData.deviceName,
                                                "physicalDev": soruceData.physicalDev,
                                                "systemDev": soruceData.systemDev,
                                                "symbolicDev": soruceData.symbolicDev,
                                                "macAddress": soruceData.macAddress,
                                                "duplex": soruceData.duplex,
                                                "vendor": soruceData.vendor,
                                                "mbit": soruceData.mbit,
                                                "connected": soruceData.connected
                                            });
                                            return true;
                                        }, this)
                                    }
                                }
                            }
                        }, {
                            header: i18n._( "Connected" ),
                            dataIndex: 'connected',
                            sortable: false,
                            width: 120,
                            tdCls: 'ua-draggable',
                            renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                                var divClass = "ua-cell-disabled";
                                var connectedStr = i18n._("unknown");
                                if ( value == "CONNECTED" ) {
                                    connectedStr = i18n._("connected");
                                    divClass = "ua-cell-enabled";
                                } else if ( value == "DISCONNECTED" ) {
                                    connectedStr = i18n._("disconnected");
                                } else if ( value == "MISSING" ) {
                                    connectedStr = i18n._("missing");
                                }
                                return "<div class='" + divClass + "'>" + connectedStr + "</div>";
                            }, this)
                        }, {
                            header: i18n._( "Speed" ),
                            dataIndex: 'mbit',
                            sortable: false,
                            tdCls: 'ua-draggable',
                            width: 100
                        }, {
                            header: i18n._( "Duplex" ),
                            dataIndex: 'duplex',
                            sortable: false,
                            tdCls: 'ua-draggable',
                            width: 100,
                            renderer: duplexRenderer
                        }, {
                            header: i18n._( "Vendor" ),
                            dataIndex: 'vendor',
                            sortable: false,
                            tdCls: 'ua-draggable',
                            flex: 1
                        }, {
                            header: i18n._( "MAC Address" ),
                            dataIndex: 'macAddress',
                            sortable: false,
                            width: 150,
                            renderer: function(value, metadata, record, rowIndex, colIndex, store, view) {
                                var text = "";
                                if ( value && value.length > 0 ) {
                                    // Build the link for the mac address
                                    text = '<a target="_blank" href="http://standards.ieee.org/cgi-bin/ouisearch?' +
                                    value.substring( 0, 8 ).replace( /:/g, "" ) + '">' + value + '</a>';
                                }
                                return text;
                            }
                        }],
                        onRefreshDeviceStatus: function() {
                            var grid = this;
                            Ext.MessageBox.wait(i18n._("Refreshing Device Status..."), i18n._("Please wait"));
                            Ung.Main.getNetworkManager().getDeviceStatus(Ext.bind(function(result, exception) {
                                if(Ung.Util.handleException(exception)) return;
                                var deviceStatusMap=Ung.Util.createRecordsMap(( result == null ? [] : result.list ), "deviceName");
                                grid.getStore().suspendEvents();
                                grid.getStore().each(function( currentRow ) {
                                    var deviceStatus = deviceStatusMap[currentRow.get("deviceName")];
                                    if(deviceStatus) {
                                        var isDirty = currentRow.dirty;
                                        currentRow.set({
                                            "macAddress": deviceStatus.macAddress,
                                            "duplex": deviceStatus.duplex,
                                            "vendor": deviceStatus.vendor,
                                            "mbit": deviceStatus.mbit,
                                            "connected": deviceStatus.connected
                                        });
                                        //To prevent coloring the row when device status is changed
                                        if(!isDirty) {
                                            currentRow.commit();
                                        }
                                    }
                                });
                                grid.getStore().resumeEvents();
                                grid.getView().refresh();
                                Ext.MessageBox.hide();
                            }, this.settingsCmp));
                        }
                    });

                    this.winMapDevices = Ext.create('Ung.EditWindow', {
                        breadcrumbs: [{
                            title: i18n._("Interfaces"),
                            action: Ext.bind(function() {
                                this.winMapDevices.cancelAction();
                            }, this)
                        }, {
                            title: i18n._("Remap Interfaces")
                        }],
                        items: [{
                            xtype: 'panel',
                            layout: { type: 'vbox', align: 'stretch' },
                            items: [{
                                xtype: 'fieldset',
                                flex: 0,
                                margin: '5 0 0 0',
                                title: i18n._("How to map Devices with Interfaces"),
                                html: i18n._("<b>Method 1:</b> <b>Drag and Drop</b> the Device to the desired Interface<br/><b>Method 2:</b> <b>Click on a Device</b> to open a combo and choose the desired Device from a list. When another Device is selected the 2 Devices are switched.")
                            }, this.gridMapDevices]
                        }],
                        updateAction: Ext.bind(function() {
                            var interfaceDataMap = {};
                            this.mapDevicesStore.each( function( currentRow ) {
                                interfaceDataMap[currentRow.get( "interfaceId" )] = currentRow.getData();
                            });
                            this.gridInterfaces.getStore().each(function( currentRow ) {
                                var interfaceData = interfaceDataMap[currentRow.get("interfaceId")];
                                if(interfaceData) {
                                    currentRow.set({
                                        "deviceName": interfaceData.deviceName,
                                        "physicalDev": interfaceData.physicalDev,
                                        "systemDev": interfaceData.systemDev,
                                        "symbolicDev": interfaceData.symbolicDev,
                                        "macAddress": interfaceData.macAddress,
                                        "connected": interfaceData.connected,
                                        "duplex": interfaceData.duplex,
                                        "vendor": interfaceData.vendor,
                                        "mbit": interfaceData.mbit
                                    });
                                }
                            });
                            this.winMapDevices.cancelAction();
                        }, this)
                    });
                }
                Ext.MessageBox.hide();
                var allInterfaces=this.gridInterfaces.getList();
                this.currentInterfaces = [];
                for(var i=0; i<allInterfaces.length; i++) {
                    if(!allInterfaces[i].isVlanInterface) {
                        this.currentInterfaces.push(allInterfaces[i]);
                    }
                }
                this.mapDevicesStore.loadData( Ext.decode(Ext.encode(this.currentInterfaces)) );
                this.availableDevicesStore.loadData( Ext.decode(Ext.encode(this.currentInterfaces)));
                this.winMapDevices.show();
            }, this)
        });

        this.gridInterfaces.getStore().filterBy(function (record) {
            return !record.get('hidden');
        });

        this.panelInterfaces = Ext.create('Ext.panel.Panel',{
            name: 'Interfaces',
            helpSource: 'network_interfaces',
            title: i18n._('Interfaces'),
            layout: { type: 'vbox', pack: 'start', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: i18n._("Interface configuration"),
                html: i18n._("Use this page to configure each interface's configuration and its mapping to a physical network card.")
            }, this.gridInterfaces]
        });
        this.gridInterfacesV4AliasesEditor = Ext.create('Ung.grid.Panel',{
            name: 'IPv4 Aliases',
            height: 180,
            width: 450,
            settingsCmp: this,
            hasEdit: false,
            dataIndex: 'v4Aliases',
            columnsDefaultSortable: false,
            recordJavaClass: "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
            emptyRow: {
                "staticAddress": "1.2.3.4",
                "staticPrefix": "24"
            },
            fields: [{
                name: 'staticAddress'
            }, {
                name: 'staticPrefix'
            }],
            columns: [{
                header: i18n._("Address"),
                dataIndex: 'staticAddress',
                width: 200,
                editor : {
                    xtype: 'textfield',
                    vtype: 'ip4Address',
                    emptyText: i18n._("[enter IPv4 address]"),
                    allowBlank: false
                }
            }, {
                header: i18n._("Netmask / Prefix"),
                dataIndex: 'staticPrefix',
                flex: 1,
                editor : {
                    xtype: 'numberfield',
                    minValue: 1,
                    maxValue: 32,
                    allowDecimals: false,
                    allowBlank: false
                }
            }],
            setValue: function (value) {
                var data = [];
                if(value && value.list) {
                    data=value.list;
                }
                this.reload({data:data});
            },
            getValue: function () {
                return {
                    javaClass: "java.util.LinkedList",
                    list: this.getList()
                };
            }
        });
        this.gridInterfacesV6AliasesEditor = Ext.create('Ung.grid.Panel',{
            name: 'IPv6 Aliases',
            height: 180,
            width: 450,
            settingsCmp: this,
            hasEdit: false,
            dataIndex: 'v6Aliases',
            columnsDefaultSortable: false,
            recordJavaClass: "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
            emptyRow: {
                "staticAddress": "::1",
                "staticPrefix": "64"
            },
            fields: [{
                name: 'staticAddress'
            }, {
                name: 'staticPrefix'
            }],
            columns: [{
                header: i18n._("Address"),
                dataIndex: 'staticAddress',
                width:200,
                editor : {
                    xtype: 'textfield',
                    vtype: 'ip6Address',
                    emptyText: i18n._("[enter IPv6 address]"),
                    allowBlank: false
                }
            }, {
                header: i18n._("Netmask / Prefix"),
                dataIndex: 'staticPrefix',
                flex: 1,
                editor : {
                    xtype: 'numberfield',
                    minValue: 1,
                    maxValue: 128,
                    allowDecimals: false,
                    allowBlank: false
                }
            }],
            setValue: function (value) {
                var data = [];
                if(value && value.list) {
                    data=value.list;
                }
                this.reload({data:data});
            },
            getValue: function () {
                return {
                    javaClass: "java.util.LinkedList",
                    list: this.getList()
                };
            }
        });
        this.gridInterfacesVrrpAliasesEditor = Ext.create('Ung.grid.Panel',{
            name: 'VRRP Aliases',
            height: 180,
            width: 450,
            settingsCmp: this,
            hasEdit: false,
            dataIndex: 'vrrpAliases',
            columnsDefaultSortable: false,
            recordJavaClass: "com.untangle.uvm.network.InterfaceSettings$InterfaceAlias",
            emptyRow: {
                "staticAddress": "1.2.3.4",
                "staticPrefix": "24"
            },
            fields: [{
                name: 'staticAddress'
            }, {
                name: 'staticPrefix'
            }],
            columns: [{
                header: i18n._("Address"),
                dataIndex: 'staticAddress',
                width:200,
                editor : {
                    xtype: 'textfield',
                    emptyText: i18n._("[enter IPv4 address]"),
                    vtype: 'ip4Address',
                    allowBlank: false
                }
            }, {
                header: i18n._("Netmask / Prefix"),
                dataIndex: 'staticPrefix',
                flex: 1,
                editor : {
                    xtype: 'numberfield',
                    minValue: 1,
                    maxValue: 32,
                    allowDecimals: false,
                    allowBlank: false
                }
            }],
            setValue: function (value) {
                var data = [];
                if(value && value.list) {
                    data=value.list;
                }
                this.reload({data:data});
            },
            getValue: function () {
                return {
                    javaClass: "java.util.LinkedList",
                    list: this.getList()
                };
            }
        });
        this.gridInterfacesDhcpOptionsEditor = Ext.create('Ung.grid.Panel',{
            name: 'DHCP Options',
            height: 180,
            width: 450,
            settingsCmp: this,
            hasEdit: false,
            dataIndex: 'dhcpOptions',
            disableOnly: true,
            columnsDefaultSortable: false,
            recordJavaClass: "com.untangle.uvm.network.DhcpOption",
            emptyRow: {
                "enabled": true,
                "value": "66,1.2.3.4",
                "description": i18n._("[no description]")
            },
            fields: [{
                name: 'enabled'
            }, {
                name: 'value'
            }, {
                name: 'description'
            }],
            columns: [{
                xtype:'checkcolumn',
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter description]"),
                    allowBlank:false
                }
            }, {
                header: i18n._("Value"),
                dataIndex: 'value',
                width:200,
                editor : {
                    xtype: 'textfield',
                    emptyText: i18n._("[enter value]"),
                    allowBlank: false
                }
            }],
            setValue: function (value) {
                var data = [];
                if( value && value.list ) {
                    data=value.list;
                }
                this.reload({data:data});
            },
            getValue: function () {
                return {
                    javaClass: "java.util.LinkedList",
                    list: this.getList()
                };
            }
        });
        this.gridInterfaces.setRowEditor( Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            helpSource: 'network_interfaces',
            title: i18n._('Edit Interface'),
            inputLines: [{
                xtype:'textfield',
                dataIndex: "name",
                fieldLabel: i18n._("Interface Name"),
                allowBlank: false,
                width: 300
            }, {
                xtype:'checkbox',
                dataIndex: "isVlanInterface",
                fieldLabel: i18n._("Is VLAN (802.1q) Interface"),
                readOnly: true,
                width: 300
            }, {
                xtype:'checkbox',
                dataIndex: "isWirelessInterface",
                fieldLabel: i18n._("Is Wireless Interface"),
                readOnly: true,
                width: 300
            }, {
                xtype: "combo",
                allowBlank: false,
                dataIndex: "vlanParent",
                fieldLabel: i18n._("Parent Interface"),
                store: Ung.Util.getInterfaceList(false, false),
                width: 300,
                queryMode: 'local',
                editable: false
            }, {
                xtype: "numberfield",
                dataIndex: "vlanTag",
                fieldLabel: i18n._("802.1q Tag"),
                minValue: 1,
                maxValue: 4096,
                allowBlank: false,
                blankText: i18n._("802.1q Tag must be a valid integer."),
                width: 300
            }, {
                xtype: "combo",
                allowBlank: false,
                dataIndex: "configType",
                fieldLabel: i18n._("Config Type"),
                editable: false,
                store: [["ADDRESSED", i18n._('Addressed')], ["BRIDGED", i18n._('Bridged')], ["DISABLED", i18n._('Disabled')]],
                width: 300,
                queryMode: 'local',
                listeners: {
                    "select": {
                        fn: Ext.bind(function(combo, records, eOpts) {
                            this.gridInterfaces.rowEditor.syncComponents();
                        }, this)
                    }
                }
            }, {
                xtype: "combo",
                allowBlank: false,
                dataIndex: "bridgedTo",
                fieldLabel: i18n._("Bridged To"),
                store: Ung.Util.getInterfaceAddressedList(),
                width: 300,
                queryMode: 'local',
                editable: false
            }, {
                xtype:'checkbox',
                dataIndex: "isWan",
                fieldLabel: i18n._("Is WAN Interface"),
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.gridInterfaces.rowEditor.syncComponents();
                            if ( newValue ) {
                                var v4NatEgressTraffic = this.gridInterfaces.rowEditor.down('checkbox[dataIndex="v4NatEgressTraffic"]');
                                // auto-enable egress NAT when checking isWan
                                v4NatEgressTraffic.setValue( true );
                            }
                        }, this)
                    }
                }
            }, {
                xtype: 'fieldset',
                name: 'wireless',
                border: true,
                title: i18n._("Wireless Configuration"),
                collapsible: false,
                collapsed: false,
                defaults: {
                    labelWidth: 150
                },
                items: [{
                    xtype:'textfield',
                    dataIndex: "wirelessSsid",
                    fieldLabel: i18n._("SSID"),
                    allowBlank: false,
                    disableOnly: true,
                    maxLength: 30,
                    maskRe: /[a-zA-Z0-9\-_=]/,
                    //maskRe: /[a-zA-Z0-9~@%_=,<>\!\-\/\?\[\]\\\^\$\+\*\.\|]/,
                    width: 350
                }, {
                    xtype: "combo",
                    allowBlank: false,
                    dataIndex: "wirelessEncryption",
                    fieldLabel: i18n._("Encryption"),
                    editable: false,
                    store: [["NONE", i18n._('None')], ["WPA1", i18n._('WPA')], ["WPA12", i18n._('WPA / WPA2')], ["WPA2", i18n._('WPA2')]],
                    width: 300,
                    queryMode: 'local',
                    listeners: {
                        "select": {
                            fn: Ext.bind(function(combo, records, eOpts) {
                                this.gridInterfaces.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                }, {
                    xtype:'textfield',
                    dataIndex: "wirelessPassword",
                    fieldLabel: i18n._("Password"),
                    allowBlank: false,
                    disableOnly: true,
                    maxLength: 63,
                    minLength: 8,
                    maskRe: /[a-zA-Z0-9~@#%_=,\!\-\/\?\(\)\[\]\\\^\$\+\*\.\|]/,
                    width: 350
                }, {
                    xtype: "combo",
                    allowBlank: false,
                    dataIndex: "wirelessChannel",
                    fieldLabel: i18n._("Channel"),
                    editable: false,
                    valueField: "channel",
                    displayField: "channelDescription",
                    store: Ext.create('Ext.data.ArrayStore', {
                        fields:[{ name: "channel" }, { name: "channelDescription" }],
                        data: []
                    }),
                    width: 300,
                    queryMode: 'local'
                }]
            }, {
                xtype:'fieldset',
                name: 'v4Config',
                border: true,
                title: i18n._("IPv4 Configuration"),
                collapsible: true,
                collapsed: false,
                items: [{
                    xtype: 'fieldset',
                    defaults: {
                        labelWidth: 150,
                        width: 350
                    },
                    items:[{
                        xtype: "combo",
                        dataIndex: "v4ConfigType",
                        fieldLabel: i18n._("Config Type"),
                        allowBlank: false,
                        editable: false,
                        store: [ ["AUTO", i18n._('Auto (DHCP)')], ["STATIC", i18n._('Static')],  ["PPPOE", i18n._('PPPoE')]],
                        queryMode: 'local',
                        listeners: {
                            "select": {
                                fn: Ext.bind(function(combo, records, eOpts) {
                                    this.gridInterfaces.rowEditor.syncComponents();
                                }, this)
                            }
                        }
                    }, {
                        xtype:'textfield',
                        dataIndex: "v4StaticAddress",
                        fieldLabel: i18n._("Address"),
                        allowBlank: false,
                        vtype: "ip4Address"
                    }, {
                        xtype: "combo",
                        dataIndex: "v4StaticPrefix",
                        fieldLabel: i18n._( "Netmask" ),
                        store: Ung.Util.getV4NetmaskList( false ),
                        queryMode: 'local',
                        allowBlank: false,
                        editable: false
                    }, {
                        xtype:'textfield',
                        dataIndex: "v4StaticGateway",
                        fieldLabel: i18n._("Gateway"),
                        allowBlank: false,
                        vtype: "ip4Address"
                    }, {
                        xtype:'textfield',
                        dataIndex: "v4StaticDns1",
                        fieldLabel: i18n._("Primary DNS"),
                        allowBlank: false,
                        vtype: "ip4Address"
                    }, {
                        xtype:'textfield',
                        dataIndex: "v4StaticDns2",
                        fieldLabel: i18n._("Secondary DNS"),
                        vtype: "ip4Address"
                    }, {
                        xtype: 'container',
                        layout: 'column',
                        name: "v4AutoAddressOverrideContainer",
                        margin: '0 0 5 0',
                        width: null,
                        items: [{
                            xtype:'textfield',
                            dataIndex: "v4AutoAddressOverride",
                            fieldLabel: i18n._("Address Override"),
                            vtype: "ip4Address",
                            labelWidth: 150,
                            width: 350
                        }, {
                            xtype: 'label',
                            dataIndex: "v4Address",
                            setValue: function(value) {
                                this.dataValue=value;
                                this.setText(Ext.isEmpty(value)?"":i18n._("Current:") + " " + Ext.String.format("{0}", value));
                            },
                            getValue: function() {
                                return this.dataValue;
                            },
                            html: "",
                            cls: 'boxlabel'
                        }]
                    },{
                        xtype: 'container',
                        layout: 'column',
                        name: "v4AutoPrefixOverrideContainer",
                        margin: '0 0 5 0',
                        width: null,
                        items: [{
                            xtype: "combo",
                            dataIndex: "v4AutoPrefixOverride",
                            fieldLabel: i18n._("Netmask Override"),
                            store: Ung.Util.getV4NetmaskList( true ),
                            queryMode: 'local',
                            editable: false,
                            labelWidth: 150,
                            width: 350

                        }, {
                            xtype: 'label',
                            dataIndex: "v4PrefixLength",
                            setValue: function(value, record) {
                                this.dataValue=value;
                                this.setText(Ext.isEmpty(value)?"":i18n._("Current:") + " " + Ext.String.format("/{0} - {1}", value, record.get("v4Netmask")));
                            },
                            getValue: function() {
                                return this.dataValue;
                            },
                            html: "",
                            cls: 'boxlabel'
                        }]
                    }, {
                        xtype: 'container',
                        layout: 'column',
                        name: "v4AutoGatewayOverrideContainer",
                        margin: '0 0 5 0',
                        width: null,
                        items: [{
                            xtype:'textfield',
                            dataIndex: "v4AutoGatewayOverride",
                            fieldLabel: i18n._("Gateway Override"),
                            vtype: "ip4Address",
                            labelWidth: 150,
                            width: 350
                        }, {
                            xtype: 'label',
                            dataIndex: "v4Gateway",
                            setValue: function(value) {
                                this.dataValue=value;
                                this.setText(Ext.isEmpty(value)?"":i18n._("Current:") + " " + Ext.String.format("{0}", value));
                            },
                            getValue: function() {
                                return this.dataValue;
                            },
                            html: "",
                            cls: 'boxlabel'
                        }]
                    }, {
                        xtype: 'container',
                        layout: 'column',
                        name: "v4AutoDns1OverrideContainer",
                        margin: '0 0 5 0',
                        width: null,
                        items: [{
                            xtype:'textfield',
                            dataIndex: "v4AutoDns1Override",
                            fieldLabel: i18n._("Primary DNS Override"),
                            vtype: "ip4Address",
                            labelWidth: 150,
                            width: 350
                        }, {
                            xtype: 'label',
                            dataIndex: "v4Dns1",
                            setValue: function(value) {
                                this.dataValue=value;
                                this.setText(Ext.isEmpty(value)?"":i18n._("Current:") + " " + Ext.String.format("{0}", value));
                            },
                            getValue: function() {
                                return this.dataValue;
                            },
                            html: "",
                            cls: 'boxlabel'
                        }]
                    }, {
                        xtype: 'container',
                        layout: 'column',
                        name: "v4AutoDns2OverrideContainer",
                        margin: '0 0 5 0',
                        width: null,
                        items: [{
                            xtype:'textfield',
                            dataIndex: "v4AutoDns2Override",
                            fieldLabel: i18n._("Secondary DNS Override"),
                            vtype: "ip4Address",
                            labelWidth: 150,
                            width: 350
                        }, {
                            xtype: 'label',
                            dataIndex: "v4Dns2",
                            setValue: function(value) {
                                this.dataValue=value;
                                this.setText(Ext.isEmpty(value)?"":i18n._("Current:") + " " + Ext.String.format("{0}", value));
                            },
                            getValue: function() {
                                return this.dataValue;
                            },
                            html: "",
                            cls: 'boxlabel'
                        }]
                    }, {
                        xtype: 'button',
                        name: "v4AutoRenewDhcpLease",
                        text: i18n._( "Renew DHCP Lease" ),
                        width: 195,
                        margin: "5 0 5 155",
                        handler: Ext.bind(function() {
                            this.gridInterfaces.rowEditor.onRenewDhcpLease();
                        }, this)
                    }, {
                        xtype:'textfield',
                        dataIndex: "v4PPPoEUsername",
                        fieldLabel: i18n._("Username"),
                        width: 350
                    }, {
                        xtype:'textfield',
                        inputType:'password',
                        dataIndex: "v4PPPoEPassword",
                        fieldLabel: i18n._("Password"),
                        width: 350
                    }, {
                        xtype:'checkbox',
                        dataIndex: "v4PPPoEUsePeerDns",
                        fieldLabel: i18n._("Use Peer DNS"),
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.gridInterfaces.rowEditor.syncComponents();
                                }, this)
                            }
                        }
                    }, {
                        xtype:'textfield',
                        dataIndex: "v4PPPoEDns1",
                        fieldLabel: i18n._("Primary DNS"),
                        vtype: "ip4Address",
                        width: 350
                    }, {
                        xtype:'textfield',
                        dataIndex: "v4PPPoEDns2",
                        fieldLabel: i18n._("Secondary DNS"),
                        vtype: "ip4Address",
                        width: 350
                    }]
                }, {
                    xtype: 'fieldset',
                    title: i18n._("IPv4 Aliases"),
                    name: "v4AliasesContainer",
                    items: [this.gridInterfacesV4AliasesEditor]
                }, {
                    xtype: 'fieldset',
                    title: i18n._("IPv4 Options"),
                    name: "v4OptionsContainer",
                    items: [{
                        xtype:'checkbox',
                        dataIndex: "v4NatEgressTraffic",
                        boxLabel: i18n._("NAT traffic exiting this interface (and bridged peers)")
                    }, {
                        xtype:'checkbox',
                        dataIndex: "v4NatIngressTraffic",
                        boxLabel: i18n._("NAT traffic coming from this interface (and bridged peers)")
                    }]
                }]
            }, {
                xtype: 'fieldset',
                name: 'v6Config',
                border: true,
                title: i18n._("IPv6 Configuration"),
                collapsible: true,
                collapsed: false,
                defaults: {
                    labelWidth: 150
                },
                items: [{
                    xtype: "combo",
                    dataIndex: "v6ConfigType",
                    allowBlank: false,
                    fieldLabel: i18n._("Config Type"),
                    editable: false,
                    store: [ ["DISABLED", i18n._('Disabled')], ["AUTO", i18n._('Auto (SLAAC/RA)')], ["STATIC", i18n._('Static')] ],
                    queryMode: 'local',
                    width: 350,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.gridInterfaces.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                }, {
                    xtype:'textfield',
                    dataIndex: "v6StaticAddress",
                    fieldLabel: i18n._("Address"),
                    vtype: "ip6Address",
                    width: 450
                }, {
                    xtype:'textfield',
                    dataIndex: "v6StaticPrefixLength",
                    fieldLabel: i18n._("Prefix Length"),
                    width: 200
                }, {
                    xtype:'textfield',
                    dataIndex: "v6StaticGateway",
                    fieldLabel: i18n._("Gateway"),
                    vtype: "ip6Address",
                    width: 350
                }, {
                    xtype:'textfield',
                    dataIndex: "v6StaticDns1",
                    fieldLabel: i18n._("Primary DNS"),
                    vtype: "ip6Address",
                    width: 350
                }, {
                    xtype:'textfield',
                    dataIndex: "v6StaticDns2",
                    fieldLabel: i18n._("Secondary DNS"),
                    vtype: "ip6Address",
                    width: 350
                }, {
                    xtype: 'fieldset',
                    title: i18n._("IPv6 Aliases"),
                    name: "v6AliasesContainer",
                    items: [this.gridInterfacesV6AliasesEditor]
                }, {
                    xtype: 'fieldset',
                    title: i18n._("IPv6 Options"),
                    name: "v6OptionsContainer",
                    items: [{
                        xtype:'checkbox',
                        dataIndex: "raEnabled",
                        boxLabel: i18n._("Send Router Advertisements"),
                        labelWidth: 150,
                        width: 350
                    },{
                        xtype: 'label',
                        name: "v6RouterAdvertisementWarning",
                        html: "<font color=\"red\">" + i18n._("Warning:") + " </font>" + i18n._("SLAAC only works with /64 subnets."),
                        cls: 'boxlabel'
                    }]
                }]
            }, {
                xtype: 'fieldset',
                name: 'dhcp',
                border: true,
                title: i18n._("DHCP Configuration"),
                collapsible: true,
                collapsed: false,
                defaults: {
                    labelWidth: 150
                },
                items: [{
                    xtype:'checkbox',
                    dataIndex: "dhcpEnabled",
                    boxLabel: i18n._("Enable DHCP Serving"),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.gridInterfaces.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                }, {
                    xtype: 'textfield',
                    dataIndex: "dhcpRangeStart",
                    fieldLabel: i18n._("Range Start"),
                    vtype: "ip4Address",
                    allowBlank: false,
                    disableOnly: true,
                    width: 350
                }, {
                    xtype:'textfield',
                    dataIndex: "dhcpRangeEnd",
                    fieldLabel: i18n._("Range End"),
                    vtype: "ip4Address",
                    allowBlank: false,
                    disableOnly: true,
                    width: 350
                }, {
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype: 'numberfield',
                        dataIndex: "dhcpLeaseDuration",
                        fieldLabel: i18n._("Lease Duration"),
                        allowDecimals: false,
                        //minValue: 0,
                        allowBlank: false,
                        disableOnly: true,
                        labelWidth: 150,
                        width: 350
                    }, {
                        xtype: 'label',
                        html: i18n._("(seconds)"),
                        cls: 'boxlabel'
                    }]
                }, {
                    xtype:'fieldset',
                    name: "dhcpAdvanced",
                    border: true,
                    title: i18n._("DHCP Advanced"),
                    collapsible: true,
                    collapsed: true,
                    defaults: {
                        labelWidth: 150
                    },
                    items: [{
                        xtype:'textfield',
                        dataIndex: "dhcpGatewayOverride",
                        fieldLabel: i18n._("Gateway Override"),
                        vtype: "ip4Address",
                        disableOnly: true,
                        width: 350
                    }, {
                        xtype: "combo",
                        dataIndex: "dhcpPrefixOverride",
                        fieldLabel: i18n._("Netmask Override"),
                        store: Ung.Util.getV4NetmaskList( true ),
                        queryMode: 'local',
                        editable: false,
                        disableOnly: true,
                        width: 350
                    }, {
                        xtype:'textfield',
                        dataIndex: "dhcpDnsOverride",
                        fieldLabel: i18n._("DNS Override"),
                        vtype: "ip4AddressList",
                        disableOnly: true,
                        width: 350
                    }, {
                        xtype: 'fieldset',
                        title: i18n._("DHCP Options"),
                        items: [this.gridInterfacesDhcpOptionsEditor]
                    }]
                }]
            }, {
                xtype: 'fieldset',
                name: 'vrrp',
                border: true,
                title: i18n._("Redundancy (VRRP) Configuration"),
                collapsible: true,
                collapsed: true,
                defaults: {
                    labelWidth: 150
                },
                items: [{
                    xtype:'checkbox',
                    dataIndex: "vrrpEnabled",
                    boxLabel: i18n._("Enable VRRP"),
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.gridInterfaces.rowEditor.syncComponents();
                            }, this)
                        }
                    }
                }, {
                    xtype: 'displayfield',
                    fieldLabel: i18n._("Is VRRP Master"),
                    name: 'isVrrpMaster',
                    value: "<div class='ua-cell-disabled' style='width:16px;'></div>"
                }, {
                    xtype: "numberfield",
                    dataIndex: "vrrpId",
                    fieldLabel: i18n._("VRRP ID"),
                    minValue: 1,
                    maxValue: 255,
                    allowBlank: false,
                    blankText: i18n._("VRRP ID must be a valid integer between 1 and 255."),
                    disableOnly: true,
                    width: 250
                }, {
                    xtype: "numberfield",
                    dataIndex: "vrrpPriority",
                    fieldLabel: i18n._("VRRP Priority"),
                    minValue: 1,
                    maxValue: 255,
                    allowBlank: false,
                    blankText: i18n._("VRRP Priority must be a valid integer between 1 and 255."),
                    disableOnly: true,
                    width: 250
                }, {
                    xtype: 'fieldset',
                    title: i18n._("VRRP Aliases"),
                    name: "vrrpAliasesContainer",
                    items: [this.gridInterfacesVrrpAliasesEditor]
                }]
            }],
            syncComponents: function() {
                var property;
                var cmp;
                if(!this.cmps) {
                    this.cmps = {
                        isVlanInterface: this.down('checkbox[dataIndex="isVlanInterface"]'),
                        vlanParent: this.down('combo[dataIndex="vlanParent"]'),
                        vlanTag: this.down('numberfield[dataIndex="vlanTag"]'),
                        isWan: this.down('checkbox[dataIndex="isWan"]'),

                        v4Config: this.down('fieldset[name="v4Config"]'),
                        v4ConfigType: this.down('combo[dataIndex="v4ConfigType"]'),
                        v4StaticAddress: this.down('textfield[dataIndex="v4StaticAddress"]'),
                        v4StaticPrefix: this.down('combo[dataIndex="v4StaticPrefix"]'),
                        v4StaticGateway: this.down('textfield[dataIndex="v4StaticGateway"]'),
                        v4StaticDns1: this.down('textfield[dataIndex="v4StaticDns1"]'),
                        v4StaticDns2: this.down('textfield[dataIndex="v4StaticDns2"]'),

                        v4AutoAddressOverrideContainer: this.down('container[name="v4AutoAddressOverrideContainer"]'),
                        v4AutoPrefixOverrideContainer: this.down('container[name="v4AutoPrefixOverrideContainer"]'),
                        v4AutoGatewayOverrideContainer: this.down('container[name="v4AutoGatewayOverrideContainer"]'),
                        v4AutoDns1OverrideContainer: this.down('container[name="v4AutoDns1OverrideContainer"]'),
                        v4AutoDns2OverrideContainer: this.down('container[name="v4AutoDns2OverrideContainer"]'),
                        v4AutoRenewDhcpLeaseButton: this.down('button[name="v4AutoRenewDhcpLease"]'),

                        v4PPPoEUsername: this.down('textfield[dataIndex="v4PPPoEUsername"]'),
                        v4PPPoEPassword: this.down('textfield[dataIndex="v4PPPoEPassword"]'),
                        v4PPPoEUsePeerDns: this.down('checkbox[dataIndex="v4PPPoEUsePeerDns"]'),
                        v4PPPoEDns1: this.down('textfield[dataIndex="v4PPPoEDns1"]'),
                        v4PPPoEDns2: this.down('textfield[dataIndex="v4PPPoEDns2"]'),

                        v4NatEgressTraffic: this.down('checkbox[dataIndex="v4NatEgressTraffic"]'),
                        v4NatIngressTraffic: this.down('checkbox[dataIndex="v4NatIngressTraffic"]'),

                        v6Config: this.down('fieldset[name="v6Config"]'),
                        v6ConfigType: this.down('combo[dataIndex="v6ConfigType"]'),
                        v6StaticAddress: this.down('textfield[dataIndex="v6StaticAddress"]'),
                        v6StaticPrefixLength: this.down('textfield[dataIndex="v6StaticPrefixLength"]'),
                        v6StaticGateway: this.down('textfield[dataIndex="v6StaticGateway"]'),
                        v6StaticDns1: this.down('textfield[dataIndex="v6StaticDns1"]'),
                        v6StaticDns2: this.down('textfield[dataIndex="v6StaticDns2"]'),
                        v6AliasesContainer: this.down('container[name="v6AliasesContainer"]'),
                        v6OptionsContainer: this.down('container[name="v6OptionsContainer"]'),
                        v6SendRouterAdvertisements: this.down('checkbox[dataIndex="raEnabled"]'),
                        v6SendRouterAdvertisementsWarning: this.down('label[name="v6RouterAdvertisementWarning"]'),

                        dhcp: this.down('fieldset[name="dhcp"]'),
                        dhcpEnabled: this.down('checkbox[dataIndex="dhcpEnabled"]'),
                        dhcpRangeStart: this.down('textfield[dataIndex="dhcpRangeStart"]'),
                        dhcpRangeEnd: this.down('textfield[dataIndex="dhcpRangeEnd"]'),
                        dhcpLeaseDuration: this.down('numberfield[dataIndex="dhcpLeaseDuration"]'),
                        dhcpPrefixOverride: this.down('combo[dataIndex="dhcpPrefixOverride"]'),
                        dhcpGatewayOverride: this.down('textfield[dataIndex="dhcpGatewayOverride"]'),
                        dhcpDnsOverride: this.down('textfield[dataIndex="dhcpDnsOverride"]'),
                        dhcpOptions: this.down('[dataIndex="dhcpOptions"]'),

                        vrrp: this.down('fieldset[name="vrrp"]'),
                        vrrpEnabled: this.down('checkbox[dataIndex="vrrpEnabled"]'),
                        vrrpId: this.down('numberfield[dataIndex="vrrpId"]'),
                        vrrpPriority: this.down('numberfield[dataIndex="vrrpPriority"]'),
                        vrrpAliasesContainer: this.down('container[name="vrrpAliasesContainer"]'),

                        wireless: this.down('fieldset[name="wireless"]'),
                        isWirelessInterface: this.down('checkbox[dataIndex="isWirelessInterface"]'),
                        wirelessSsid: this.down('textfield[dataIndex="wirelessSsid"]'),
                        wirelessEncryption: this.down('combo[dataIndex="wirelessEncryption"]'),
                        wirelessPassword: this.down('textfield[dataIndex="wirelessPassword"]'),
                        wirelessChannel: this.down('combo[dataIndex="wirelessChannel"]'),

                        bridgedTo: this.down('combo[dataIndex="bridgedTo"]')
                    };
                    this.configType=this.down('combo[dataIndex="configType"]');
                    for( property in this.cmps ) {
                        cmp = this.cmps[property];
                        if(!cmp.disableOnly) {
                            cmp.setVisible(false);
                        }
                    }
                }
                for( property in this.cmps ) {
                    this.cmps[property].status=false;
                }
                var configTypeValue = this.configType.getValue();
                var isVlanInterfaceValue = this.cmps.isVlanInterface.getValue();
                var isWanValue = this.cmps.isWan.getValue();
                var isWirelessInterfaceValue = this.cmps.isWirelessInterface.getValue();

                if ( isVlanInterfaceValue ) {
                    this.cmps.isVlanInterface.status = true;
                    this.cmps.vlanParent.status = true;
                    this.cmps.vlanTag.status = true;
                }

                // show wireless settings in all cases unless wireless is disabled
                if ( isWirelessInterfaceValue && configTypeValue != "DISABLED" ) {
                    this.cmps.isWirelessInterface.status = true;
                    this.cmps.wireless.status = true;
                    this.cmps.wirelessSsid.status = true;
                    this.cmps.wirelessEncryption.status = true;
                    this.cmps.wirelessChannel.status = true;
                    if ( this.cmps.wirelessEncryption.getValue() != "NONE" ) {
                        this.cmps.wirelessPassword.status = true;
                    } else {
                        this.cmps.wirelessPassword.status = false;
                    }
                }

                if ( configTypeValue == "DISABLED") {
                    // if config disabled show nothing
                } else if ( configTypeValue == "BRIDGED") {
                    // if config bridged just show the one field
                    this.cmps.bridgedTo.status = true;
                } else if ( configTypeValue == "ADDRESSED") {
                    // if config addressed show necessary options
                    this.cmps.isWan.status = true;
                    this.cmps.v4Config.status = true;
                    this.cmps.v6Config.status = true;
                    // if not a WAN, must configure statically
                    // if a WAN, can use auto or static
                    if ( isWanValue ) {
                        this.cmps.v4ConfigType.status = true; //show full config options for WANs
                        this.cmps.v6ConfigType.status = true; //show full config options for WANs
                        this.cmps.v4NatEgressTraffic.status = true; // show NAT egress option on WANs
                    } else {
                        this.cmps.v4ConfigType.setValue("STATIC"); //don't allow auto/pppoe for non-WAN
                        this.cmps.v6ConfigType.setValue("STATIC"); //don't allow auto for non-WAN
                        this.cmps.v6ConfigType.status = false; //don't allow auto/pppoe for non-WAN

                        this.cmps.v4StaticGateway.status = false; // no gateways for non-WAN
                        this.cmps.v6StaticGateway.status = false; // no gateways for non-WAN

                        this.cmps.v4NatIngressTraffic.status = true; // show NAT ingress options on non-WANs

                        this.cmps.dhcp.status = true; // show DHCP options on non-WANs
                        this.cmps.dhcpEnabled.status = true;
                        if ( this.cmps.dhcpEnabled.getValue() ) {
                            this.cmps.dhcpRangeStart.status = true;
                            this.cmps.dhcpRangeEnd.status = true;
                            this.cmps.dhcpLeaseDuration.status = true;
                            this.cmps.dhcpGatewayOverride.status = true;
                            this.cmps.dhcpPrefixOverride.status = true;
                            this.cmps.dhcpDnsOverride.status = true;
                            this.cmps.dhcpOptions.status = true;
                        }

                    }
                    // if static show static fields
                    // if auto show override fields (auto is only allowed on WANs)
                    // if pppoe show pppoe fields (pppoe is only allowed on WANs)
                    if ( this.cmps.v4ConfigType.getValue() == "STATIC" ) {
                        this.cmps.v4StaticAddress.status = true;
                        this.cmps.v4StaticPrefix.status = true;
                        if (isWanValue) {
                            this.cmps.v4StaticGateway.status = true;
                            this.cmps.v4StaticDns1.status = true;
                            this.cmps.v4StaticDns2.status = true;
                        }
                        // if its a STATIC interface (whether WAN or non-WAN, show vrrp)
                        this.cmps.vrrp.status = true;
                        this.cmps.vrrpEnabled.status = true;
                        if ( this.cmps.vrrpEnabled.getValue() ) {
                            this.cmps.vrrpId.status = true;
                            this.cmps.vrrpPriority.status = true;
                            this.cmps.vrrpAliasesContainer.status = true;
                        }
                    } else if ( this.cmps.v4ConfigType.getValue() == "AUTO" ) {
                        this.cmps.v4AutoAddressOverrideContainer.status = true;
                        this.cmps.v4AutoPrefixOverrideContainer.status = true;
                        this.cmps.v4AutoGatewayOverrideContainer.status = true;
                        this.cmps.v4AutoDns1OverrideContainer.status = true;
                        this.cmps.v4AutoDns2OverrideContainer.status = true;
                        this.cmps.v4AutoRenewDhcpLeaseButton.status = true;
                    } else if ( this.cmps.v4ConfigType.getValue() == "PPPOE" ) {
                        this.cmps.v4PPPoEUsername.status = true;
                        this.cmps.v4PPPoEPassword.status = true;
                        this.cmps.v4PPPoEUsePeerDns.status = true;
                        if ( !this.cmps.v4PPPoEUsePeerDns.getValue()) {
                            this.cmps.v4PPPoEDns1.status = true;
                            this.cmps.v4PPPoEDns2.status = true;
                        }
                    }
                    // if static show static fields
                    // if auto show override fields
                    if ( this.cmps.v6ConfigType.getValue() == "STATIC" ) {
                        this.cmps.v6AliasesContainer.status = true;
                        this.cmps.v6StaticAddress.status = true;
                        this.cmps.v6StaticPrefixLength.status = true;
                        if (isWanValue) {
                            this.cmps.v6StaticGateway.status = true;
                            this.cmps.v6StaticDns1.status = true;
                            this.cmps.v6StaticDns2.status = true;
                        } else {
                            this.cmps.v6OptionsContainer.status = true;
                            this.cmps.v6SendRouterAdvertisements.status = true;
                            if (this.cmps.v6StaticPrefixLength.getValue() != 64){
                                this.cmps.v6SendRouterAdvertisementsWarning.status = true;
                            }
                        }
                    } else if ( this.cmps.v6ConfigType.getValue() == "AUTO" ) {
                        this.cmps.v6AliasesContainer.status = true;
                    } else { //v6ConfigType == DISABLED
                        this.cmps.v6Config.collapse();
                    }
                }
                for( property in this.cmps ) {
                    cmp = this.cmps[property];
                    if(!cmp.disableOnly && cmp.isHidden() === cmp.status) {
                        cmp.setVisible(cmp.status);
                    }
                    if(cmp.disableOnly && !cmp.status && !cmp.isDisabled()) {
                        cmp.disable();
                    }
                    if(cmp.status && cmp.isDisabled()) {
                        cmp.enable();
                    }
                }
            },
            populate: function(record, addMode) {
                var interfaceId=record.get("interfaceId");
                var allInterfaces=this.grid.getList();
                var bridgedToInterfaces = [];
                var vlanParentInterfaces = [];
                for(var i=0; i<allInterfaces.length; i++) {
                    var intf=allInterfaces[i];
                    if(intf.configType == 'ADDRESSED' && intf.interfaceId!=interfaceId) {
                        bridgedToInterfaces.push([ intf.interfaceId, intf.name]);
                    }
                    if(!intf.isVlanInterface) {
                        vlanParentInterfaces.push([intf.interfaceId, intf.name]);
                    }
                }
                // set ConfigType store
                if (record.get('supportedConfigTypes') != null) {
                    var configTypes = [], configType;
                    for (var j = 0; j < record.get('supportedConfigTypes').length; j += 1) {
                        configType = record.get('supportedConfigTypes')[j];
                        configTypes.push([configType, i18n._(configType.charAt(0) + configType.slice(1).toLowerCase())]);
                    }
                    this.down('combo[dataIndex="configType"]').setStore(configTypes);
                }
                // refresh interface selector stores
                var bridgedTo = this.down('combo[dataIndex="bridgedTo"]');
                bridgedTo.getStore().loadData( bridgedToInterfaces );
                var vlanParent = this.down('combo[dataIndex="vlanParent"]');
                vlanParent.getStore().loadData( vlanParentInterfaces );
                //Populate wirelessChannel store with device supported channnels
                if(record.get("isWirelessInterface")) {
                    var wirelessChannel = this.down('combo[dataIndex="wirelessChannel"]');
                    Ung.Main.getNetworkManager().getWirelessChannels(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        var availableChannels=[];
                        if(result && result.list) {
                            var allChannelsMap = this.getWirelessChannelsMap();
                            for(var j=0; j<result.list.length; j++) {
                                var item=result.list[j];
                                if (allChannelsMap[item]) {
                                    availableChannels.push(allChannelsMap[item]);
                                }
                            }
                        }
                        wirelessChannel.getStore().loadData(availableChannels);
                        wirelessChannel.suspendEvents();
                        wirelessChannel.setValue(record.get("wirelessChannel"));
                        wirelessChannel.resumeEvents();
                    }, this.grid.settingsCmp), record.get("systemDev"));
                }

                Ung.RowEditorWindow.prototype.populate.apply(this, arguments);

                if(this.down('checkbox[dataIndex="dhcpEnabled"]').getValue()) {
                    this.down('fieldset[name="dhcp"]').expand();
                }

                var vrrp= this.down('fieldset[name="vrrp"]');
                var vrrpEnabled = this.down('checkbox[dataIndex="vrrpEnabled"]');
                var isVrrpMaster = this.down('displayfield[name="isVrrpMaster"]');
                isVrrpMaster.setValue("<div class='ua-cell-disabled' style='width:16px;'></div>");
                if(vrrpEnabled.getValue()) {
                    vrrp.expand();
                    if(interfaceId>=0) {
                        Ung.Main.getNetworkManager().isVrrpMaster(Ext.bind(function(result, exception) {
                            if(Ung.Util.handleException(exception)) return;
                            if(result) {
                                isVrrpMaster.setValue("<div class='ua-cell-enabled' style='width:16px;'></div>");
                            }
                        }, true), interfaceId);
                    }
                } else {
                    vrrp.collapse();
                }
            },
            updateAction: function() {
                //many fields must be set empty when not displayed
                for(var property in this.cmps) {
                    var cmp=this.cmps[property];
                    if(!cmp.status) {
                        if((cmp.allowBlank === false || cmp.vtype ) && !cmp.isDisabled()) {
                            cmp.disable();
                        }
                    }
                }
                if(Ung.RowEditorWindow.prototype.updateAction.apply(this, arguments)) {
                    var interfaces = this.grid.getList();
                    var interfacesMap=Ung.Util.createRecordsMap(interfaces, "interfaceId");
                    var qosBandwidthStore=this.grid.settingsCmp.gridQosWanBandwidth.getStore();
                    qosBandwidthStore.suspendEvents();
                    qosBandwidthStore.clearFilter();
                    //reload grid data, in case of new/removed vlans
                    qosBandwidthStore.getProxy().setData(interfaces);
                    qosBandwidthStore.load();
                    qosBandwidthStore.resumeEvents();
                    qosBandwidthStore.filter([{property: "configType", value: "ADDRESSED"}, {property:"isWan", value: true}]);
                    this.grid.settingsCmp.gridQosWanBandwidth.updateTotalBandwidth();
                }
            },
            onRenewDhcpLease: function() {
                var settingsCmp = this.grid.settingsCmp;
                Ext.MessageBox.wait( i18n._( "Renewing DHCP Lease..." ), i18n._( "Please wait" ));
                var inerfaceId = this.record.get("interfaceId");
                Ung.Main.getNetworkManager().renewDhcpLease(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    //refresh DHCP status
                    Ung.Main.getNetworkManager().getInterfaceStatus(Ext.bind(function(result, exception) {
                        if(Ung.Util.handleException(exception)) return;
                        var interfaceStatus = result;
                        for( i=0 ; i<settingsCmp.settings.interfaces.list.length; i++) {
                            var intf=settingsCmp.settings.interfaces.list[i];
                            if( interfaceStatus.interfaceId == intf.interfaceId ) {
                                delete interfaceStatus.javaClass;
                                delete interfaceStatus.interfaceId;
                                this.grid.getStore().suspendEvents();
                                this.record.set(interfaceStatus); // apply to the current record
                                this.grid.getStore().resumeEvents();
                                Ext.apply(intf, interfaceStatus); // apply to settings
                                //apply in the current rowEditor
                                this.down('label[dataIndex="v4Address"]').setValue(this.record.get("v4Address"),this.record);
                                this.down('label[dataIndex="v4Gateway"]').setValue(this.record.get("v4Gateway"),this.record);
                                this.down('label[dataIndex="v4Dns1"]').setValue(this.record.get("v4Dns1"),this.record);
                                this.down('label[dataIndex="v4Dns2"]').setValue(this.record.get("v4Dns2"),this.record);
                                this.down('label[dataIndex="v4PrefixLength"]').setValue(this.record.get("v4PrefixLength"),this.record);
                                break;
                            }
                        }
                        Ext.MessageBox.hide();
                    }, this), inerfaceId);
                }, this), inerfaceId);
            }
        }) );
    },
    // HostName Panel
    buildHostName: function() {
        this.panelHostName = Ext.create('Ext.panel.Panel',{
            name: 'Hostname',
            helpSource: 'network_hostname',
            title: i18n._('Hostname'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                xtype: 'fieldset',
                title: i18n._('Hostname'),
                items: [{
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype: "textfield",
                        fieldLabel: i18n._("Hostname"),
                        emptyText: "untangle",
                        name: 'HostName',
                        value: this.settings.hostName,
                        maskRe: /[a-zA-Z0-9\-]/,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.hostName = newValue;
                                }, this)
                            }
                        }
                    },{
                        xtype: 'label',
                        html: i18n._("(eg: gateway)"),
                        cls: 'boxlabel'
                    }]
                },{
                    xtype: 'container',
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype: "textfield",
                        fieldLabel: i18n._("Domain Name"),
                        emptyText: "example.com",
                        allowBlank: false,
                        name: 'DomainName',
                        value: this.settings.domainName,
                        listeners: {
                            "change": {
                                fn: Ext.bind(function(elem, newValue) {
                                    this.settings.domainName = newValue;
                                }, this)
                            }
                        }
                    },{
                        xtype: 'label',
                        html: i18n._("(eg: example.com)"),
                        cls: 'boxlabel'
                    }]
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Dynamic DNS Service Configuration'),
                items: [{
                    xtype: "checkbox",
                    fieldLabel: i18n._("Enabled"),
                    checked: this.settings.dynamicDnsServiceEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.dynamicDnsServiceEnabled = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "combo",
                    fieldLabel: i18n._("Service"),
                    value: this.settings.dynamicDnsServiceName,
                    store: [['easydns','EasyDNS'],
                            ['zoneedit','ZoneEdit'],
                            ['dyndns','DynDNS'],
                            ['namecheap','Namecheap'],
                            ['dslreports','DSL-Reports'],
                            ['dnspark','DNSPark'],
                            ['no-ip','No-IP'],
                            ['dnsomatic','DNS-O-Matic'],
                            ['cloudflare','Cloudflare']],
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.dynamicDnsServiceName = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "textfield",
                    fieldLabel: i18n._("Username"),
                    value: this.settings.dynamicDnsServiceUsername,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.dynamicDnsServiceUsername = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "textfield",
                    fieldLabel: i18n._("Password"),
                    value: this.settings.dynamicDnsServicePassword,
                    inputType: 'password',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.dynamicDnsServicePassword = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "textfield",
                    fieldLabel: i18n._("Hostname(s)"),
                    value: this.settings.dynamicDnsServiceHostnames,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.dynamicDnsServiceHostnames = newValue;
                            }, this)
                        }
                    }
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Public Address Configuration'),
                items: [{
                    xtype: 'component',
                    margin: '0 0 10 0',
                    html: Ext.String.format(i18n._('The Public Address is the address/URL that provides a public location for the {0} Server. This address will be used in emails sent by the {0} Server to link back to services hosted on the {0} Server such as Quarantine Digests and OpenVPN Client emails.'), rpc.companyName)
                },{
                    xtype: 'radio',
                    boxLabel: i18n._('Use IP address from External interface (default)'),
                    hideLabel: true,
                    name: 'publicUrl',
                    checked: this.settings.publicUrlMethod == "external",
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                if (checked) {
                                    this.settings.publicUrlMethod = "external";
                                    this.panelHostName.down('textfield[name="publicUrlAddress"]').disable();
                                    this.panelHostName.down('numberfield[name="publicUrlPort"]').disable();
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'component',
                    margin: '0 0 10 25',
                    html: Ext.String.format(i18n._('This works if your {0} Server has a routable public static IP address.'), rpc.companyName)
                },{
                    xtype: 'radio',
                    boxLabel: i18n._('Use Hostname'),
                    hideLabel: true,
                    name: 'publicUrl',
                    checked: this.settings.publicUrlMethod == "hostname",
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                if (checked) {
                                    this.settings.publicUrlMethod = "hostname";
                                    this.panelHostName.down('textfield[name="publicUrlAddress"]').disable();
                                    this.panelHostName.down('numberfield[name="publicUrlPort"]').disable();
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'component',
                    margin: '0 0 5 25',
                    html: Ext.String.format(i18n._('This is recommended if the {0} Server\'s fully qualified domain name looks up to its IP address both internally and externally.'), rpc.companyName)
                }, {
                    xtype: 'component',
                    margin: '0 0 10 25',
                    html: i18n._( 'Current Hostname' ) + ':<i>' + this.getHostname() + '</i>'
                }, {
                    xtype: 'radio',
                    boxLabel: i18n._('Use Manually Specified Address'),
                    hideLabel: true,
                    name: 'publicUrl',
                    checked: this.settings.publicUrlMethod == "address_and_port",
                    listeners: {
                        "afterrender": {
                            fn: Ext.bind(function(elem) {
                                if(elem.getValue()) {
                                    this.panelHostName.down('textfield[name="publicUrlAddress"]').enable();
                                    this.panelHostName.down('numberfield[name="publicUrlPort"]').enable();
                                } else {
                                    this.panelHostName.down('textfield[name="publicUrlAddress"]').disable();
                                    this.panelHostName.down('numberfield[name="publicUrlPort"]').disable();
                                }
                            }, this)
                        },
                        "change": {
                            fn: Ext.bind(function(elem, checked) {
                                if (checked) {
                                    this.settings.publicUrlMethod = "address_and_port";
                                    this.panelHostName.down('textfield[name="publicUrlAddress"]').enable();
                                    this.panelHostName.down('numberfield[name="publicUrlPort"]').enable();
                                }
                            }, this)
                        }
                    }
                },{
                    xtype: 'component',
                    margin: '0 0 10 25',
                    html: Ext.String.format(i18n._('This is recommended if the {0} Server is installed behind another firewall with a port forward from the specified hostname/IP that redirects traffic to the {0} Server.'), rpc.companyName)
                },{
                    xtype: 'textfield',
                    margin: '0 0 5 25',
                    fieldLabel: i18n._('IP/Hostname'),
                    name: 'publicUrlAddress',
                    value: this.settings.publicUrlAddress,
                    allowBlank: false,
                    width: 400,
                    blankText: i18n._("You must provide a valid IP Address or hostname."),
                    disabled: this.settings.publicUrlMethod != "address_and_port",
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.publicUrlAddress = newValue;
                            }, this)
                        }
                    }
                },{
                    xtype: 'numberfield',
                    margin: '0 0 5 25',
                    fieldLabel: i18n._('Port'),
                    name: 'publicUrlPort',
                    value: this.settings.publicUrlPort,
                    allowDecimals: false,
                    minValue: 0,
                    allowBlank: false,
                    width: 210,
                    blankText: i18n._("You must provide a valid port."),
                    vtype: 'port',
                    disabled: this.settings.publicUrlMethod != "address_and_port",
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.publicUrlPort = newValue;
                            }, this)
                        }
                    }
                }]
            }]
        });
    },
    // Services Panel
    buildServices: function() {
        this.panelServices = Ext.create('Ext.panel.Panel',{
            name: 'Services',
            helpSource: 'network_services',
            title: i18n._('Services'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                xtype: 'fieldset',
                title: i18n._('Local Services'),
                items: [{
                    xtype: 'component',
                    html: "<br/>" + i18n._('The specified HTTPS port will be forwarded from all interfaces to the local HTTPS server to provide administration and other services.') + "<br/>",
                    margin: '0 0 10 0'
                }, {
                    xtype: 'numberfield',
                    fieldLabel: i18n._('HTTPS port'),
                    name: 'httpsPort',
                    value: this.settings.httpsPort,
                    allowDecimals: false,
                    minValue: 0,
                    allowBlank: false,
                    blankText: i18n._("You must provide a valid port."),
                    vtype: 'port',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.httpsPort = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: 'component',
                    html: "<br/>" + i18n._('The specified HTTP port will be forwarded on non-WAN interfaces to the local HTTP server to provide administration, blockpages, and other services.') + "<br/>",
                    margin: '0 0 10 0'
                }, {
                    xtype: 'numberfield',
                    fieldLabel: i18n._('HTTP port'),
                    name: 'httpPort',
                    value: this.settings.httpPort,
                    allowDecimals: false,
                    minValue: 0,
                    allowBlank: false,
                    blankText: i18n._("You must provide a valid port."),
                    vtype: 'port',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.httpPort = newValue;
                            }, this)
                        }
                    }
                }]
            }]
        });
    },
    // PortForwardRules Panel
    buildPortForwardRules: function() {
        var troubleshootColumn = Ext.create('Ext.grid.column.Action',{
            header: i18n._("Troubleshooting"),
            width: 100,
            iconCls: 'icon-detail-row',
            init: function(grid) {
                this.grid = grid;
            },
            handler: function(view, rowIndex, colIndex, item, e, record) {
                // select current row
                this.grid.getSelectionModel().select(record);
                // show details
                this.grid.onTroubleshoot(record);
            }
        });

        this.gridPortForwardRules = Ext.create( 'Ung.grid.Panel', {
            flex: 3,
            name: 'Port Forward Rules',
            settingsCmp: this,
            hasReorder: true,
            hasAdd: false,
            hasImportExport: true,
            addAtTop: false,
            dataProperty:'portForwardRules',
            addSimpleRuleHandler:function() {
                var record = Ext.create(Ext.getClassName(this.getStore().getProxy().getModel()), Ext.apply(Ext.decode(Ext.encode(this.emptyRow)), {
                    "simple":true,
                    "conditions": {
                        javaClass: "java.util.LinkedList",
                        list:[{
                            conditionType:'DST_LOCAL',
                            invert: false,
                            value: "true",
                            javaClass: "com.untangle.uvm.network.PortForwardRuleCondition"
                        }, {
                            conditionType: 'PROTOCOL',
                            invert: false,
                            value: "TCP",
                            javaClass: "com.untangle.uvm.network.PortForwardRuleCondition"
                        }, {
                            conditionType:'DST_PORT',
                            invert: false,
                            value: "80",
                            javaClass: "com.untangle.uvm.network.PortForwardRuleCondition"
                        }]
                    },
                    "newPort": 80
                }));
                record.set("internalId", this.genAddedId());
                this.stopEditing();
                this.rowEditor.populate(record, true);
                this.rowEditor.show();
            },
            initComponent : function() {
                this.tbar= [{
                    text: i18n._('Add'),
                    iconCls: 'icon-add-row',
                    handler: this.addSimpleRuleHandler,
                    scope: this
                }];
                Ung.grid.Panel.prototype.initComponent.apply(this, arguments);
            },
            title: i18n._("Port Forward Rules"),
            recordJavaClass: "com.untangle.uvm.network.PortForwardRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "description": "",
                "simple": false
            },
            plugins:[troubleshootColumn],
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'newDestination',
                sortType: 'asIp'
            }, {
                name: 'newPort',
                sortType: 'asInt'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            },{
                name: 'simple'
            },{
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule Id"),
                width: 50,
                dataIndex: 'ruleId',
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width: 55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }, {
                header: i18n._("New Destination"),
                dataIndex: 'newDestination',
                width: 150
            }, {
                header: i18n._("New Port"),
                dataIndex: 'newPort',
                width: 65
            },troubleshootColumn],
            onTroubleshoot: Ext.bind(function(record) {
                if(!this.portForwardTroubleshootWin) {
                    this.portForwardTroubleshootWin = Ext.create('Ung.Window', {
                        title: i18n._('Port Forward Troubleshooter'),
                        helpSource: 'port_forward_troubleshooting_guide',
                        settingsCmp: this,
                        helpAction: function() {
                            Ung.Main.openHelp(this.helpSource);
                        },
                        bbar: [{
                            iconCls: 'icon-help',
                            text: i18n._('Help'),
                            handler: function() {
                                this.portForwardTroubleshootWin.helpAction();
                            },
                            scope: this
                        },'->',{
                            name: "Close",
                            iconCls: 'cancel-icon',
                            text: i18n._('Cancel'),
                            handler: function() {
                                this.portForwardTroubleshootWin.cancelAction();
                            },
                            scope: this
                        }],
                        items: [{
                            xtype: "panel",
                            autoScroll: true,
                            items: [{
                                xtype: 'fieldset',
                                layout: "vbox",
                                margin: '5 0 0 0',
                                title: i18n._('Troubleshooting Port Forwards'),
                                items: [{
                                    xtype: "label",
                                    html: i18n._( 'Test 1: Verify pinging the <b>new destination</b>' )
                                },{
                                    xtype: "button",
                                    margin: '5 0 0 0',
                                    text: i18n._( "Ping Test" ),
                                    handler: function () {
                                        var destination = this.portForwardTroubleshootWin.recordData.newDestination;
                                        this.openPingTest(destination);
                                    },
                                    scope: this
                                },{
                                    xtype: "component",
                                    margin: '10 0 0 0',
                                    html: i18n._( "Test 2: Verify connecting to the new destination<br/><i>This test applies only to TCP port forwards.</i>" )
                                },{
                                    xtype: "button",
                                    margin: '5 0 0 0',
                                    name: "connect_test_button",
                                    text: i18n._( "Connect Test" ),
                                    handler: function () {
                                        var recordData = this.portForwardTroubleshootWin.recordData;
                                        this.openTcpTest(recordData.newDestination, recordData.newPort);
                                    },
                                    scope: this
                                },{
                                    xtype: "component",
                                    margin: '10 0 0 0',
                                    html: i18n._( "Test 3: Watch traffic using the Packet Test" )
                                },{
                                    xtype: "button",
                                    margin: '5 0 0 0',
                                    text: i18n._( "Packet Test" ),
                                    handler: this.openPacketTest,
                                    scope: this
                                },{
                                    xtype: "component",
                                    margin: '10 0 0 0',
                                    html: Ext.String.format( i18n._( "For more help troubleshooting port forwards view the<br/>{0}Port Forward Troubleshooting Guide{1}" ), "<a href='http://wiki.untangle.com/index.php/Port_Forward_Troubleshooting_Guide'target='_blank'>", "</a>")
                                }]
                            }]
                        }],
                        showForRule: function(record) {
                            this.recordData = record.getData();
                            //Show connect_test_button buton only if it has TCP protocol
                            var hasTcpProtocol=false;
                            var conditionsList=this.recordData.conditions.list;
                            for(var i=0;i<conditionsList.length;i++) {
                                if(conditionsList[i].conditionType == "PROTOCOL") {
                                    hasTcpProtocol = (conditionsList[i].value.indexOf("TCP") != -1);
                                    break;
                                }
                            }
                            var connectTestButton = this.down('button[name="connect_test_button"]');
                            if(hasTcpProtocol) {
                                connectTestButton.enable();
                            } else {
                                connectTestButton.disable();
                            }
                            this.show();
                        }
                    });
                    this.subCmps.push(this.portForwardTroubleshootWin);
                }
                this.portForwardTroubleshootWin.showForRule(record);
            }, this)
        });

        var protocolStore =[[ "TCP,UDP",  "TCP & UDP"],[ "TCP",  "TCP"],[ "UDP", "UDP"]];
        var portStore =[[ "21", "FTP (21)" ],[ "25", "SMTP (25)" ],[ "53", "DNS (53)" ],[ "80", "HTTP (80)" ],[ "110", "POP3 (110)" ],[ "143", "IMAP (143)" ],[ "443", "HTTPS (443)" ],[ "1723", "PPTP (1723)" ],[ "-1", i18n._("Other") ]];
        this.panelPortForwardRules = Ext.create('Ext.panel.Panel',{
            name: 'PortForwardRules',
            helpSource: 'network_port_forward_rules',
            title: i18n._('Port Forward Rules'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('Port Forward Rules'),
                html: i18n._("Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.")
            }, this.gridPortForwardRules, {
                xtype: 'fieldset',
                flex: 2,
                style: "margin-top: 10px;",
                border: true,
                collapsible: true,
                collapsed: false,
                autoScroll: true,
                title: i18n._('The following ports are currently reserved and can not be forwarded:'),
                items: [{
                    xtype: 'component',
                    name: 'portForwardWarnings',
                    html: ' '
                }]

            }]
        });
        var settingsCmp = this;
        this.gridPortForwardRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            helpSource: 'network_port_forward_rules',
            rowEditorLabelWidth: 160,
            populate: function(record, addMode) {
                //reinitialize dataIndex on both editors
                this.down('fieldset[name="simple_portforward_editor"]').dataIndex="conditions";
                this.down('rulebuilder').dataIndex="conditions";
                Ung.RowEditorWindow.prototype.populate.apply(this, arguments);
            },
            syncComponents: function () {
                var isSimple = this.down('checkbox[dataIndex="simple"]').getValue();
                var simpleEditor=this.down('fieldset[name="simple_portforward_editor"]');
                var advancedEditor=this.down('[name="advanced_portforward_editor"]');
                var rulebuilder=advancedEditor.down('rulebuilder');
                simpleEditor.setVisible(isSimple);
                advancedEditor.setVisible(!isSimple);
                if(isSimple) {
                    simpleEditor.dataIndex="conditions";
                    rulebuilder.dataIndex="";
                } else {
                    simpleEditor.dataIndex="";
                    rulebuilder.dataIndex="conditions";
                }
                this.down('[name="switch_advanced_btn"]').setVisible(isSimple);
                this.down('[name="new_port_container"]').setVisible(!isSimple);

                Ext.defer( function(){
                    this.down('fieldset[name="fwd_description"]').setTitle( isSimple ?
                        i18n._('Traffic matching the above description destined to any Untangle IP will be forwarded to the new location:') :
                        i18n._('Forward to the following location:'));
                },1, this);
            },
            inputLines: [{
                xtype:'checkbox',
                hidden:true,
                dataIndex: "simple"
            }, {
                xtype:'checkbox',
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable Port Forward Rule")
            }, {
                xtype:'textfield',
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype : 'fieldset',
                name: 'simple_portforward_editor',
                autoHeight : true,
                dataIndex: "conditions",
                setValue: function(value, record) {
                    if(record.get("simple")) {
                        var conditionsMap=Ung.Util.createRecordsMap(record.get("conditions").list, "conditionType");
                        var protocol = conditionsMap["PROTOCOL"]?conditionsMap["PROTOCOL"].value:"TCP";
                        this.down('combo[name="simple_protocol"]').setValue(protocol);

                        var dstPort = conditionsMap["DST_PORT"]?conditionsMap["DST_PORT"].value:"";
                        var dstPortOther=this.down('numberfield[name="simple_destination_port"]');
                        dstPortOther.setValue(dstPort);
                        var dstPortCombo=this.down('combo[name="simple_basic_port"]');
                        var isOtherPort=true;
                        for(var i=0;i<portStore.length;i++) {
                            if(dstPort==portStore[i][0]) {
                                isOtherPort=false;
                                dstPortCombo.setValue(dstPort);
                                break;
                            }
                        }
                        if(isOtherPort) {
                            dstPortCombo.setValue("-1");
                        }
                        dstPortOther.setVisible(isOtherPort);
                    }
                },
                getValue: function() {
                    var isSimple = settingsCmp.gridPortForwardRules.rowEditor.down('checkbox[dataIndex="simple"]').getValue();
                    if(isSimple) {
                        var protocol = this.down('combo[name="simple_protocol"]').getValue();
                        var port = ""+this.down('[name="simple_destination_port"]').getValue();
                        return {
                            javaClass: "java.util.LinkedList",
                            list:[{
                                conditionType:'DST_LOCAL',
                                invert: false,
                                value: "true",
                                javaClass: "com.untangle.uvm.network.PortForwardRuleCondition"
                            }, {
                                conditionType: 'PROTOCOL',
                                invert: false,
                                value: protocol,
                                javaClass: "com.untangle.uvm.network.PortForwardRuleCondition"
                            }, {
                                conditionType:'DST_PORT',
                                invert: false,
                                value: port,
                                javaClass: "com.untangle.uvm.network.PortForwardRuleCondition"
                            }]
                        };
                    } else {
                        return undefined;
                    }
                },
                title : i18n._("Forward the following traffic:"),
                items : [{
                    xtype : "combo",
                    fieldLabel : i18n._("Protocol"),
                    width : 300,
                    name : "simple_protocol",
                    store : protocolStore,
                    editable : false,
                    queryMode: 'local'
                }, {
                    xtype : "combo",
                    fieldLabel : i18n._("Port"),
                    width : 300,
                    name : "simple_basic_port",
                    store : portStore,
                    editable : false,
                    queryMode: 'local',
                    listeners: {
                        "select": {
                            fn: Ext.bind(function(combo, records, eOpts) {
                                var value = combo.getValue();
                                var isVisible = (value == "-1");
                                var port = this.gridPortForwardRules.rowEditor.down('[name="simple_destination_port"]');
                                port.setVisible( isVisible );
                                if ( !isVisible ) {
                                    port.setValue( value );
                                }
                            }, this)
                        }
                    }
                }, {
                    xtype : "numberfield",
                    fieldLabel : i18n._("Port Number"),
                    hidden: true,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(field, value) {
                                this.gridPortForwardRules.rowEditor.down('numberfield[dataIndex="newPort"]').setValue(value);
                            }, this)
                        }
                    },
                    name : "simple_destination_port",
                    minValue : 1,
                    maxValue : 0xFFFF,
                    width : 200
                }]
            }, {
                xtype:'fieldset',
                name:"advanced_portforward_editor",
                hidden:true,
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype: 'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.uvm.network.PortForwardRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getPortForwardConditions()
                }]
            }, {
                xtype: 'fieldset',
                name: 'fwd_description',
                title: "",
                items: [{
                    xtype:'textfield',
                    allowBlank: false,
                    dataIndex: "newDestination",
                    fieldLabel: i18n._("New Destination"),
                    vtype: 'ipAddress'
                }, {
                    xtype: 'container',
                    hidden:true,
                    name: "new_port_container",
                    layout: 'column',
                    margin: '0 0 5 0',
                    items: [{
                        xtype:'numberfield',
                        allowBlank: true,
                        width: 200,
                        dataIndex: "newPort",
                        fieldLabel: i18n._("New Port"),
                        minValue : 1,
                        maxValue : 0xFFFF,
                        vtype: 'port'
                    }, {
                        xtype: 'label',
                        html: i18n._("(optional)"),
                        cls: 'boxlabel'
                    }]
                }]
            }, {
                xtype: "button",
                name: "switch_advanced_btn",
                text: i18n._( "Switch to Advanced" ),
                handler: function() {
                    var rowEditor = this.gridPortForwardRules.rowEditor;
                    var conditions = rowEditor.down('fieldset[name="simple_portforward_editor"]').getValue();
                    rowEditor.down('checkbox[dataIndex="simple"]').setValue(false);
                    rowEditor.down('rulebuilder').setValue(conditions);
                    rowEditor.syncComponents();
                },
                scope: this
            }]
        }));
    },
    // NatRules Panel
    buildNatRules: function() {
        this.gridNatRules = Ext.create( 'Ung.grid.Panel', {
            flex: 1,
            name: 'NAT Rules',
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            title: i18n._("NAT Rules"),
            dataProperty:'natRules',
            recordJavaClass: "com.untangle.uvm.network.NatRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "auto": true,
                "description": ""
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'auto'
            }, {
                name: 'newSource'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule Id"),
                width: 50,
                dataIndex: 'ruleId',
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }]
        });

        this.panelNatRules = Ext.create('Ext.panel.Panel',{
            name: 'NatRules',
            helpSource: 'network_nat_rules',
            title: i18n._('NAT Rules'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('NAT Rules'),
                flex: 0,
                html: i18n._("NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.")
            },  this.gridNatRules]
        });
        this.gridNatRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            helpSource: 'network_nat_rules',
            inputLines: [{
                xtype:'checkbox',
                name: "Enable NAT Rule",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable NAT Rule")
            }, {
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.uvm.network.NatRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getNatRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: "combo",
                    allowBlank: false,
                    dataIndex: "auto",
                    fieldLabel: i18n._("NAT Type"),
                    editable: false,
                    store: [[true, i18n._('Auto')], [false, i18n._('Custom')]],
                    queryMode: 'local',
                    listeners: {
                        select: Ext.bind(function(combo, newVal, oldVal) {
                            this.gridNatRules.rowEditor.syncComponents();
                        }, this )
                    }
                }, {
                    xtype:'textfield',
                    name: "newSource",
                    allowBlank: true,
                    dataIndex: "newSource",
                    fieldLabel: i18n._("New Source"),
                    hidden: true,
                    vtype: 'ipAddress'
                }]
            }],
            syncComponents: function () {
                var natType  = this.down('combo[dataIndex="auto"]');
                var newSource = this.down('textfield[dataIndex="newSource"]');
                if (natType.value) { //Auto
                    newSource.disable();
                    newSource.hide();
                } else {
                    newSource.enable();
                    newSource.show();
                }
            }
        }));
    },
    // BypassRules Panel
    buildBypassRules: function() {
        this.gridBypassRules = Ext.create( 'Ung.grid.Panel', {
            flex: 1,
            name: 'Bypass Rules',
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            title: i18n._("Bypass Rules"),
            dataProperty:'bypassRules',
            recordJavaClass: "com.untangle.uvm.network.BypassRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "bypass": true,
                "description": ""
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'bypass'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule Id"),
                width: 50,
                dataIndex: 'ruleId',
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Bypass"),
                dataIndex: 'bypass',
                resizable: false,
                width:55
            }]
        });

        this.panelBypassRules = Ext.create('Ext.panel.Panel',{
            name: 'BypassRules',
            helpSource: 'network_bypass_rules',
            title: i18n._('Bypass Rules'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: i18n._('Bypass Rules'),
                html: i18n._("Bypass Rules control what traffic is scanned by the applications. Bypassed traffic skips application processing. The rules are evaluated in order. Sessions that meet no rule are not bypassed.")
            }, this.gridBypassRules]
        });
        this.gridBypassRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            helpSource: 'network_bypass_rules',
            inputLines:[{
                xtype:'checkbox',
                name: "Enable Bypass Rule",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable Bypass Rule")
            }, {
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.uvm.network.BypassRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getBypassRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: "combo",
                    name: "bypass",
                    allowBlank: false,
                    dataIndex: "bypass",
                    fieldLabel: i18n._("Action"),
                    editable: false,
                    store: [[true, i18n._('Bypass')], [false, i18n._('Process')]],
                    queryMode: 'local'
                }]
            }]
        }));
    },
    // Routes Panel
    buildRoutes: function() {
        var devList = [];
        var devMap = {};
        for ( var c = 0 ; c < this.settings.interfaces.list.length ; c++ ) {
            var intf = this.settings.interfaces.list[c];
            var name = Ext.String.format(i18n._("Local on {0} ({1})"),intf.name, intf.systemDev);
            var key = ""+intf.interfaceId;
            devList.push( [ key, name ] );
            devMap[key]=name;
        }
        this.gridStaticRoutes = Ext.create('Ung.grid.Panel', {
            height: 300,
            name: 'Static Routes',
            settingsCmp: this,
            title: i18n._("Static Routes"),
            dataProperty: 'staticRoutes',
            recordJavaClass: "com.untangle.uvm.network.StaticRoute",
            emptyRow: {
                "ruleId": -1,
                "network": "",
                "prefix": 24,
                "nextHop": "4.3.2.1",
                "description": ""
            },
            sortField: 'network',
            fields: [{
                name: 'ruleId'
            }, {
                name: 'network',
                sortType: 'asIp'
            }, {
                name: 'prefix',
                sortType: 'asInt'
            }, {
                name: 'nextHop'
            },{
                name: 'description'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Network"),
                width: 170,
                dataIndex: 'network'
            }, {
                header: i18n._("Netmask/Prefix"),
                width: 170,
                dataIndex: 'prefix'
            }, {
                header: i18n._("Next Hop"),
                width: 300,
                dataIndex: 'nextHop',
                renderer: Ext.bind(function(value, metadata, record, rowIndex, colIndex, store, view) {
                    var intRegex = /^\d+$/;
                    if ( intRegex.test( value ) ) {
                        return devMap[value]?devMap[value]:i18n._("Local interface");
                    } else {
                        return value;
                    }
                }, this)
            }, {
                header: i18n._("Description"),
                width: 300,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter description]"),
                    allowBlank: false
                }
            }],
            rowEditorHelpSource: 'network_routes',
            rowEditorInputLines: [{
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: "[enter description]",
                allowBlank: false,
                width: 400
            }, {
                xtype:'textfield',
                name: "Network",
                dataIndex: "network",
                fieldLabel: i18n._("Network"),
                emptyText: i18n._("[1.2.3.0]"),
                allowBlank: false,
                vtype: "ipAddress",
                width: 300
            }, {
                xtype: "combo",
                dataIndex: "prefix",
                fieldLabel: i18n._( "Netmask/Prefix" ),
                store: Ung.Util.getV4NetmaskList( false ),
                width: 300,
                listWidth: 70,
                queryMode: 'local',
                editable: false
            }, {
                xtype: "combo",
                dataIndex: "nextHop",
                fieldLabel: i18n._("Next Hop"),
                store: devList,
                width: 300,
                queryMode: 'local',
                editable : true,
                allowBlank: false
            }, {
                xtype: 'component',
                margin: '10 0 0 20',
                html: i18n._("If <b>Next Hop</b> is an IP address that network will be routed via the specified IP address.") + "<br/>" +
                    i18n._("If <b>Next Hop</b> is an interface that network will be routed <b>locally</b> on that interface.")
            }]
        });

        this.routeArea = Ext.create('Ext.form.TextArea',{
            style: "font-family: monospace",
            name: "state",
            hideLabel: true,
            labelSeperator: "",
            readOnly: true,
            autoCreate: { tag: 'textarea', autocomplete: 'off', spellcheck: 'false' },
            height: 200,
            width: "100%",
            isDirty: function() { return false; }
        });

        this.routeButton = Ext.create('Ext.button.Button',{
            text: " " + i18n._("Refresh Routes") + " ",
            handler: function(b,e) {
                Ung.Main.getExecManager().exec(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    this.routeArea.setValue( result.output );
                }, this), "/usr/share/untangle/bin/ut-routedump.sh");
            },
            scope: this
        });

        this.panelRoutes = Ext.create('Ext.panel.Panel',{
            name: 'Routes',
            helpSource: 'network_routes',
            title: i18n._('Routes'),
            autoScroll: true,
            reserveScrollbar: true,
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('Static Routes'),
                html: i18n._("Static Routes are global routes that control how traffic is routed by destination address. The most specific Static Route is taken for a particular packet, order is not important.")
            }, this.gridStaticRoutes, {
                xtype: 'fieldset',
                style: {marginTop: '10px'},
                title: i18n._('Current Routes'),
                html: i18n._("Current Routes shows the current routing system's configuration and how all traffic will be routed.")
            }, this.routeArea, this.routeButton]
        });
    },
    // DnsServer Panel
    buildDnsServer: function() {
        this.gridDnsStaticEntries = Ext.create( 'Ung.grid.Panel', {
            flex: 1,
            name: 'Static DNS Entries',
            settingsCmp: this,
            hasEdit: false,
            title: i18n._("Static DNS Entries"),
            dataExpression:'settings.dnsSettings.staticEntries.list',
            recordJavaClass: "com.untangle.uvm.network.DnsStaticEntry",
            emptyRow: {
                "name": i18n._("[no name]"),
                "address": "1.2.3.4"
            },
            fields: [{
                name: 'name'
            }, {
                name: 'address',
                sortType: 'asIp'
            }],
            columns: [{
                header: i18n._("Name"),
                flex: 1,
                dataIndex: 'name',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter name]"),
                    allowBlank:false
                }
            },{
                header: i18n._("Address"),
                dataIndex: 'address',
                width: 200,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter address]"),
                    allowBlank: false,
                    vtype:"ipAddress"
                }
            }]
        });
        this.gridDnsLocalServers = Ext.create( 'Ung.grid.Panel', {
            flex: 1,
            margin: '5 0 0 0',
            name: 'Domain DNS Servers',
            settingsCmp: this,
            hasEdit: false,
            title: i18n._("Domain DNS Servers"),
            dataExpression:'settings.dnsSettings.localServers.list',
            recordJavaClass: "com.untangle.uvm.network.DnsLocalServer",
            emptyRow: {
                "domain": i18n._("[no domain]"),
                "localServer": "1.2.3.4"
            },
            fields: [{
                name: 'domain'
            }, {
                name: 'localServer',
                sortType: 'asIp'
            }],
            columns: [{
                header: i18n._("Domain"),
                flex: 1,
                dataIndex: 'domain',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter domain]"),
                    allowBlank:false
                }
            },{
                header: i18n._("Server"),
                dataIndex: 'localServer',
                width: 200,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter DNS server]"),
                    allowBlank: false,
                    vtype:"ipAddress"
                }
            }]
        });
        this.panelDnsServer = Ext.create('Ext.panel.Panel',{
            name: 'DnsServer',
            helpSource: 'network_dns_server',
            title: i18n._('DNS Server'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [ this.gridDnsStaticEntries, this.gridDnsLocalServers ]
        });
    },
    // DhcpServer Panel
    buildDhcpServer: function() {
        this.gridDhcpStaticEntries = Ext.create( 'Ung.grid.Panel', {
            flex: 1,
            name: 'Static DHCP Entries',
            settingsCmp: this,
            hasEdit: false,
            title: i18n._("Static DHCP Entries"),
            dataExpression:'settings.staticDhcpEntries.list',
            recordJavaClass: "com.untangle.uvm.network.DhcpStaticEntry",
            emptyRow: {
                "macAddress": "11:22:33:44:55:66",
                "address": "1.2.3.4",
                "description": i18n._("[no description]")
            },
            fields: [{
                name: 'macAddress'
            }, {
                name: 'address',
                sortType: 'asIp'
            }, {
                name: 'description'
            }],
            columns: [{
                header: i18n._("MAC Address"),
                width: 200,
                dataIndex: 'macAddress',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter MAC address]"),
                    allowBlank:false,
                    vtype:"macAddress",
                    maskRe: /[a-fA-F0-9:]/
                }
            },{
                header: i18n._("Address"),
                dataIndex: 'address',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter address]"),
                    allowBlank: false,
                    vtype:"ipAddress"
                }
            },{
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[enter description]"),
                    allowBlank:false
                }
            }],
            onAddStatic: function(rec) {
                var record = Ext.create(Ext.getClassName(this.getStore().getProxy().getModel()), {
                    "macAddress": rec.get("macAddress"),
                    "address": rec.get("address"),
                    "description": rec.get("hostname"),
                    "javaClass": "com.untangle.uvm.network.DhcpStaticEntry"
                });
                record.set("internalId", this.genAddedId());
                this.stopEditing();
                this.getStore().insert(0, [record]);
                this.updateChangedData(record, "added");
            }
        });

        var addStaticColumn = Ext.create('Ext.grid.column.Action',{
            header: i18n._("Add Static"),
            width: 100,
            iconCls: 'icon-row icon-add-inline-row',
            init: function(grid) {
                this.grid = grid;
            },
            handler: function(view, rowIndex, colIndex, item, e, record) {
                // add static
                this.grid.settingsCmp.gridDhcpStaticEntries.onAddStatic(record);
            }
        });

        this.gridCurrentDhcpLeases = Ext.create('Ung.grid.Panel',{
            name: "gridCurrentDhcpLeases",
            flex: 1,
            settingsCmp: this,
            hasAdd: false,
            hasEdit: false,
            hasDelete: false,
            hasRefresh: true,
            title: i18n._("Current DHCP Leases"),
            dataFn: function(handler) {
                var me = this;
                Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var lines = result.split("\n");
                    var leases = [];
                    for ( var i = 0 ; i < lines.length ; i++ ) {
                        if ( lines[i] === null || lines[i] === "" ) continue;

                        var lineparts = lines[i].split(/\s+/);
                        leases.push( {
                            date: lineparts[0],
                            macAddress: lineparts[1],
                            address: lineparts[2],
                            hostname: lineparts[3],
                            clientId: lineparts[4]
                        } );
                    }
                    handler({list: leases});
                    me.setTitle( i18n._("Current DHCP Leases") + " " + Ext.String.format("[{0}]", leases.length));
                }, this),"cat /var/lib/misc/dnsmasq.leases");

            },
            fields: [{
                name: "date"
            },{
                name: "macAddress"
            },{
                name: "address",
                sortType: 'asIp'
            },{
                name: "hostname"
            }],
            plugins: [addStaticColumn],
            columns: [{
                header: i18n._("MAC Address"),
                dataIndex:'macAddress',
                width: 150
            },{
                header: i18n._("Address"),
                dataIndex:'address',
                width: 200
            },{
                header: i18n._("Hostname"),
                dataIndex:'hostname',
                width: 200
            },{
                header: i18n._("Expiration Time"),
                dataIndex:'date',
                width: 180,
                renderer: function(value) { return i18n.timestampFormat(value*1000); }
            }, addStaticColumn]
        });

        this.panelDhcpServer = Ext.create('Ext.panel.Panel',{
            name: 'DhcpServer',
            helpSource: 'network_dhcp_server',
            title: i18n._('DHCP Server'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [ this.gridDhcpStaticEntries, this.gridCurrentDhcpLeases]
        });
    },
    // Advanced Panel
    buildAdvanced: function() {
        this.buildOptions();
        this.buildQoS();
        this.buildFilter();
        this.buildUPnP();
        this.buildDnsDhcp();
        this.buildNetworkCards();

        this.advancedTabPanel = Ext.create('Ext.tab.Panel',{
            activeTab: 0,
            deferredRender: false,
            autoHeight: true,
            flex: 1,
            items: [ this.panelOptions, this.panelQoS, this.panelFilter, this.panelUPnP, this.panelDnsDhcp, this.gridNetworkCards ]
        });

        this.panelAdvanced = Ext.create('Ext.panel.Panel',{
            name: 'Advanced',
            getHelpSource: Ext.bind(function() {
                var helpSource="network_advanced";
                var activeTab=this.advancedTabPanel.getActiveTab();
                if(activeTab!=null && activeTab.helpSource != null) {
                    helpSource = this.advancedTabPanel.getActiveTab().helpSource;
                }
                return helpSource;
            }, this),
            title: i18n._('Advanced'),
            cls: 'ung-panel',
            layout: { type: 'vbox', pack: 'start', align: 'stretch' },
            items: [{
                xtype: 'fieldset',
                flex: 0,
                title: i18n._("Advanced"),
                html: i18n._("Advanced settings require careful configuration. Misconfiguration can compromise the proper operation and security of your server.")
            }, this.advancedTabPanel]
        });
    },
    // Options Panel
    buildOptions: function() {
        this.panelOptions = Ext.create('Ext.panel.Panel',{
            name: 'Options',
            helpSource: 'network_options',
            title: i18n._('Options'),
            cls: 'ung-panel',
            items: [{
                xtype: "checkbox",
                fieldLabel: i18n._("Enable SIP NAT Helper"),
                labelWidth: 190,
                checked: this.settings.enableSipNatHelper,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.enableSipNatHelper = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                fieldLabel: i18n._("Send ICMP Redirects"),
                labelWidth: 190,
                checked: this.settings.sendIcmpRedirects,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.sendIcmpRedirects = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                fieldLabel: i18n._("Enable STP (Spanning Tree) on Bridges"),
                labelWidth: 190,
                checked: this.settings.stpEnabled,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.stpEnabled = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                hidden: Ung.Util.hideDangerous,
                fieldLabel: i18n._("Enable Strict ARP mode"),
                labelWidth: 190,
                checked: this.settings.strictArpMode,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.strictArpMode = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                fieldLabel: i18n._("DHCP Authoritative"),
                labelWidth: 190,
                checked: this.settings.dhcpAuthoritative,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.dhcpAuthoritative = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                fieldLabel: i18n._("Block new sessions during network configuration"),
                labelWidth: 190,
                checked: this.settings.blockDuringRestarts,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.blockDuringRestarts = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                name: "blockReplayPacketsCheckbox",
                fieldLabel: i18n._("Block replay packets"),
                labelWidth: 190,
                checked: this.settings.blockReplayPackets,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.blockReplayPackets = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                name: "logBypassedSessions",
                fieldLabel: i18n._("Log bypassed sessions"),
                labelWidth: 190,
                checked: this.settings.logBypassedSessions,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.logBypassedSessions = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                name: "logLocalOutboundSessions",
                fieldLabel: i18n._("Log local outbound sessions"),
                labelWidth: 190,
                checked: this.settings.logLocalOutboundSessions,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.logLocalOutboundSessions = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                name: "logLocalInboundSessions",
                fieldLabel: i18n._("Log local inbound sessions"),
                labelWidth: 190,
                checked: this.settings.logLocalInboundSessions,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.logLocalInboundSessions = newValue;
                        }, this)
                    }
                }
            },{
                xtype: "checkbox",
                name: "logBlockedSessions",
                fieldLabel: i18n._("Log blocked sessions"),
                labelWidth: 190,
                checked: this.settings.logBlockedSessions,
                listeners: {
                    "change": {
                        fn: Ext.bind(function(elem, newValue) {
                            this.settings.logBlockedSessions = newValue;
                        }, this)
                    }
                }
            }]
        });
    },
    // QoS Panel
    buildQoS: function() {
        this.qosPriorityStore = [
            [0, i18n._( "Default" )],
            [1, i18n._( "Very High" )],
            [2, i18n._( "High" )],
            [3, i18n._( "Medium" )],
            [4, i18n._( "Low" )],
            [5, i18n._( "Limited" )],
            [6, i18n._( "Limited More" )],
            [7, i18n._( "Limited Severely" )]
        ];
        this.qosPriorityMap = Ung.Util.createStoreMap(this.qosPriorityStore);

        this.qosPriorityNoDefaultStore = [
            [1, i18n._( "Very High" )],
            [2, i18n._( "High" )],
            [3, i18n._( "Medium" )],
            [4, i18n._( "Low" )],
            [5, i18n._( "Limited" )],
            [6, i18n._( "Limited More" )],
            [7, i18n._( "Limited Severely" )]
        ];
        this.qosPriorityNoDefaultMap = Ung.Util.createStoreMap(this.qosPriorityNoDefaultStore);

        this.gridQosWanBandwidth = Ext.create('Ung.grid.Panel', {
            name: 'QoS Priorities',
            margin: '5 0 0 0',
            height: 160,
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            hasEdit: false,
            columnsDefaultSortable: false,
            dataProperty: "interfaces",
            recordJavaClass: "com.untangle.uvm.network.InterfaceSettings",
            fields: [{
                name: 'interfaceId'
            }, {
                name: 'name'
            }, {
                name: 'configType'
            }, {
                name: 'downloadBandwidthKbps'
            }, {
                name: 'uploadBandwidthKbps'
            },{
                name: 'isWan'
            }],
            columns: [{
                header: i18n._("Id"),
                width: 30,
                dataIndex: 'interfaceId',
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                header: i18n._("WAN"),
                width: 150,
                dataIndex: 'name'
            }, {
                header: i18n._("Config Type"),
                dataIndex: 'configType',
                width: 150
            }, {
                header: i18n._("Download Bandwidth"),
                dataIndex: 'downloadBandwidthKbps',
                width: 180,
                editor: {
                    xtype: 'numberfield',
                    allowBlank : false,
                    allowDecimals: false,
                    minValue: 0
                },
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (Ext.isEmpty(value)) {
                        return i18n._("Not set");
                    } else {
                        var mbit_value = value/1000;
                        return value + " kbps" + " (" + mbit_value + " Mbit" + ")";
                    }
                }, this )
            }, {
                header: i18n._("Upload Bandwidth"),
                dataIndex: 'uploadBandwidthKbps',
                width: 180,
                editor : {
                    xtype: 'numberfield',
                    allowBlank : false,
                    allowDecimals: false,
                    minValue: 0
                },
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (Ext.isEmpty(value)) {
                        return i18n._("Not set");
                    } else {
                        var mbit_value = value/1000;
                        return value + " kbps" + " (" + mbit_value + " Mbit" + ")";
                    }
                }, this )
            }],
            updateTotalBandwidth: Ext.bind(function() {
                var interfaceList=this.gridQosWanBandwidth.getList();
                var u = 0;
                var d = 0;

                for ( var i = 0 ; i < interfaceList.length ; i++ ) {
                    if(interfaceList[i].isWan) {
                        if(!Ext.isEmpty(interfaceList[i].uploadBandwidthKbps)) {
                            u += interfaceList[i].uploadBandwidthKbps;
                        }
                        if(!Ext.isEmpty(interfaceList[i].downloadBandwidthKbps)) {
                            d += interfaceList[i].downloadBandwidthKbps;
                        }
                    }
                }

                var d_Mbit = d/1000;
                var u_Mbit = u/1000;

                var message = Ext.String.format( i18n._( "Total: {0} kbps ({1} Mbit) download, {2} kbps ({3} Mbit) upload" ), d, d_Mbit, u, u_Mbit );
                var bandwidthLabel = this.panelQoS.down('component[name="bandwidthLabel"]');
                bandwidthLabel.update(Ext.String.format(i18n._("{0}Note{1}: When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces."),'<font color="red">','</font>')+"</br><i>"+message+'</i>');
            }, this)
        });
        this.gridQosWanBandwidth.getStore().on("update", Ext.bind(function() {
            this.gridQosWanBandwidth.updateTotalBandwidth();
        }, this));
        this.gridQosWanBandwidth.getStore().filter([{property: "configType", value: "ADDRESSED"}, {property:"isWan", value: true}]);

        this.gridQosRules = Ext.create('Ung.grid.Panel', {
            name: 'QoS Custom Rules',
            margin: '5 0 0 0',
            height: 200,
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            dataExpression:'settings.qosSettings.qosRules.list',
            recordJavaClass: "com.untangle.uvm.network.QosRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "description": "",
                "priority": 1
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'priority'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule Id"),
                width: 50,
                dataIndex: 'ruleId',
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._( "Priority" ),
                width: 100,
                dataIndex: "priority",
                renderer: Ext.bind(function( value, metadata, record ) {
                    return this.qosPriorityNoDefaultMap[value];
                }, this ),
                editor: {
                    xtype: 'combo',
                    store: this.qosPriorityNoDefaultStore,
                    queryMode: 'local',
                    editable: false
                }
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }]
        });

        this.gridQosPriorities = Ext.create('Ung.grid.Panel', {
            name: 'QoS Priorities',
            margin: '5 0 0 0',
            height: 190,
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            hasEdit: false,
            columnsDefaultSortable: false,
            dataExpression:'settings.qosSettings.qosPriorities.list',
            recordJavaClass: "com.untangle.uvm.network.QosPriority",
            fields: [{
                name: 'priorityId'
            }, {
                name: 'priorityName'
            }, {
                name: 'uploadReservation'
            }, {
                name: 'uploadLimit'
            },{
                name: 'downloadReservation'
            },{
                name: 'downloadLimit'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Priority"),
                width: 150,
                dataIndex: 'priorityName',
                renderer: Ext.bind(function( value, metadata, record ) {
                    return i18n._(value);
                }, this )
            }, {
                header: i18n._("Upload Reservation"),
                dataIndex: 'uploadReservation',
                width: 150,
                editor : {
                    xtype: 'numberfield',
                    allowBlank : false,
                    minValue : 0.1,
                    maxValue : 100
                },
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (value === 0) {
                        return i18n._("No reservation");
                    } else {
                        return value + "%";
                    }
                }, this )
            }, {
                header: i18n._("Upload Limit"),
                dataIndex: 'uploadLimit',
                width: 150,
                editor : {
                    xtype: 'numberfield',
                    allowBlank : false,
                    minValue : 0.1,
                    maxValue : 100
                },
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (value === 0) {
                        return i18n._("No limit");
                    } else {
                        return value + "%";
                    }
                }, this )
            }, {
                header: i18n._("Download Reservation"),
                dataIndex: 'downloadReservation',
                width: 150,
                editor : {
                    xtype: 'numberfield',
                    allowBlank : false,
                    minValue : 0.1,
                    maxValue : 100
                },
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (value === 0) {
                        return i18n._("No reservation");
                    } else {
                        return value + "%";
                    }
                }, this )
            }, {
                header: i18n._("Download Limit"),
                dataIndex: 'downloadLimit',
                width: 150,
                editor : {
                    xtype: 'numberfield',
                    allowBlank : false,
                    minValue : 0.1,
                    maxValue : 100
                },
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (value === 0) {
                        return i18n._("No limit");
                    } else {
                        return value + "%";
                    }
                }, this )
            }]
        });

        this.interfaceList = Ung.Util.getInterfaceList(true, true);

        this.gridQosStatistics = Ext.create('Ung.grid.Panel', {
            name: 'QoS Statistics',
            margin: '5 0 0 0',
            height: 190,
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            hasEdit: false,
            hasRefresh: true,
            dataFn: function(handler) {
                Ung.Main.getExecManager().execOutput(Ext.bind(function(result, exception) {
                    if(Ung.Util.handleException(exception)) return;
                    var list = [];
                    try {
                        list = eval(result);
                    } catch (e) {
                        console.error("Could not execute /usr/share/untangle-netd/bin/qos-service.py output: ", result, e);
                    }
                    handler ({list: list}, exception);
                }, this), "/usr/share/untangle-netd/bin/qos-service.py status");
            },
            initialLoad: function() {}, //Don't load automatically
            groupField:'interface_name',
            fields: [{
                name: 'interface_name'
            },{
                name: 'priority'
            },{
                name: 'sent'
            }],
            columns: [{
                header: i18n._("Interface"),
                width: 150,
                dataIndex: 'interface_name',
                renderer: Ext.bind(function( value, metadata, record ) {
                    return i18n._(value);
                }, this )
            }, {
                header: i18n._("Priority"),
                dataIndex: 'priority',
                width: 150
            }, {
                header: i18n._("Data"),
                dataIndex: 'sent',
                width: 150,
                flex: 1
            }]
        });

        this.panelQoS = Ext.create('Ext.panel.Panel',{
            name: 'QoS',
            helpSource: 'network_qos',
            title: i18n._('QoS'),
            autoScroll: true,
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('QoS'),
                items: [{
                    xtype: "checkbox",
                    fieldLabel: i18n._("Enabled"),
                    checked: this.settings.qosSettings.qosEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.qosSettings.qosEnabled = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "combo",
                    fieldLabel: i18n._("Default Priority"),
                    value: this.settings.qosSettings.defaultPriority,
                    store : this.qosPriorityNoDefaultStore,
                    editable: false,
                    queryMode: 'local',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.qosSettings.defaultPriority = newValue;
                            }, this)
                        }
                    }
                }]
            }, {
                xtype: 'fieldset',
                name: 'bandwidth_fieldset',
                title: i18n._('WAN Bandwidth'),
                items: [{
                    xtype: 'component',
                    name: 'bandwidthLabel',
                    html: ' '
                }, this.gridQosWanBandwidth]
            }, {
                xtype: 'fieldset',
                title: i18n._('QoS Rules'),
                items: [{
                    xtype: "combo",
                    fieldLabel: i18n._("Ping Priority"),
                    value: this.settings.qosSettings.pingPriority,
                    store : this.qosPriorityStore,
                    editable: false,
                    queryMode: 'local',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.qosSettings.pingPriority = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "combo",
                    fieldLabel: i18n._("DNS Priority"),
                    value: this.settings.qosSettings.dnsPriority,
                    store : this.qosPriorityStore,
                    editable: false,
                    queryMode: 'local',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.qosSettings.dnsPriority = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "combo",
                    fieldLabel: i18n._("SSH Priority"),
                    value: this.settings.qosSettings.sshPriority,
                    store : this.qosPriorityStore,
                    editable: false,
                    queryMode: 'local',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.qosSettings.sshPriority = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: "combo",
                    fieldLabel: i18n._("OpenVPN Priority"),
                    value: this.settings.qosSettings.openvpnPriority,
                    store : this.qosPriorityStore,
                    editable: false,
                    queryMode: 'local',
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.qosSettings.openvpnPriority = newValue;
                            }, this)
                        }
                    }
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('QoS Custom Rules'),
                items: [{
                    xtype: 'label',
                    html: Ext.String.format(i18n._("{0}Note{1}: Custom Rules only match <b>Bypassed</b> traffic."),'<font color="red">','</font>')
                }, this.gridQosRules]
            }, {
                xtype: 'fieldset',
                title: i18n._('QoS Priorities'),
                items: [this.gridQosPriorities]
            }, {
                xtype: 'fieldset',
                title: i18n._('QoS Statistics'),
                items: [this.gridQosStatistics]
            }]
        });
        this.gridQosRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            inputLines:[{
                xtype:'checkbox',
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable")
            }, {
                xtype:'textfield',
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.uvm.network.QosRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getQosRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: "combo",
                    allowBlank: false,
                    dataIndex: "priority",
                    fieldLabel: i18n._("Priority"),
                    editable: false,
                    store: this.qosPriorityNoDefaultStore,
                    queryMode: 'local'
                }]
            }]
        }));
        this.gridQosWanBandwidth.updateTotalBandwidth();
    },
    // DNS & DHCP
    buildDnsDhcp: function (){
        this.panelDnsDhcp = Ext.create('Ext.panel.Panel',{
            name: 'DNS & DHCP',
            helpSource: 'network_dns_and_dhcp',
            title: i18n._('DNS & DHCP'),
            autoScroll: true,
            layout: { type: 'vbox', pack: 'start', align: 'stretch' },
            cls: 'ung-panel',
            items: [{
                 xtype: 'component',
                 html: "<br/><b>" + i18n._('Custom dnsmasq options.') + "</b><br/>" +
                     "<font color=\"red\">" + i18n._("Warning: Invalid syntax will halt all DHCP & DNS services.") + "</font>" + "<br/>",
                 margin: '0 0 10 0'
             }, {
                 xtype: "textarea",
                 width : 397,
                 flex: 1,
                 value: this.settings.dnsmasqOptions,
                 listeners: {
                     "change": {
                         fn: Ext.bind(function(elem, newValue) {
                             this.settings.dnsmasqOptions = newValue;
                         }, this)
                     }
                 }
             }]
        });
    },
    // Filter Panel
    buildFilter: function() {
        this.gridForwardFilterRules = Ext.create('Ung.grid.Panel', {
            flex: 2,
            name: 'Forward Filter Rules',
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            title: i18n._("Forward Filter Rules"),
            dataProperty:'forwardFilterRules',
            recordJavaClass: "com.untangle.uvm.network.FilterRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "ipv6Enabled": false,
                "blocked": false,
                "description": ""
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'ipv6Enabled'
            }, {
                name: 'blocked'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule Id"),
                width: 50,
                dataIndex: 'ruleId',
                renderer: Ext.bind(function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }, this)
            }, {
                xtype:'checkcolumn',
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                xtype:'checkcolumn',
                header: i18n._("IPv6"),
                dataIndex: 'ipv6Enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Block"),
                dataIndex: 'blocked',
                resizable: false,
                width:55
            }]
        });
        this.gridInputFilterRules = Ext.create('Ung.grid.Panel', {
            flex: 3,
            name: 'Input Filter Rules',
            settingsCmp: this,
            hasReorder: true,
            hasReadOnly: true,
            changableFields: ['enabled', 'ipv6Enabled'],
            addAtTop: false,
            title: i18n._("Input Filter Rules"),
            dataProperty:'inputFilterRules',
            recordJavaClass: "com.untangle.uvm.network.FilterRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "ipv6Enabled": false,
                "blocked": false,
                "readOnly": null,
                "description": ""
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            }, {
                name: 'ipv6Enabled'
            }, {
                name: 'blocked'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            }, {
                name: 'readOnly'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule Id"),
                width: 50,
                dataIndex: 'ruleId',
                renderer: Ext.bind(function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }, this)
            }, {
                xtype:'checkcolumn',
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                xtype:'checkcolumn',
                header: i18n._("IPv6"),
                dataIndex: 'ipv6Enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Block"),
                dataIndex: 'blocked',
                resizable: false,
                width:55
            }]
        });
        this.panelFilter = Ext.create('Ext.panel.Panel',{
            name: 'FilterRules',
            helpSource: 'network_filter_rules',
            title: i18n._('Filter Rules'),
            layout: { type: 'vbox', align: 'stretch' },
            cls: 'ung-panel',
            items: [this.gridForwardFilterRules, this.gridInputFilterRules]
        });
        this.gridForwardFilterRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            helpSource: 'network_filter_rules',
            rowEditorLabelWidth: 160,
            inputLines:[{
                xtype:'checkbox',
                name: "Enable Forward Filter Rule",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable Forward Filter Rule")
            }, {
                xtype:'checkbox',
                name: "Enable IPv6 Support",
                dataIndex: "ipv6Enabled",
                fieldLabel: i18n._("Enable IPv6 Support")
            }, {
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.uvm.network.FilterRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getFilterRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: "combo",
                    name: "blocked",
                    allowBlank: false,
                    dataIndex: "blocked",
                    fieldLabel: i18n._("Action"),
                    editable: false,
                    store: [[true,i18n._('Block')], [false,i18n._('Pass')]],
                    queryMode: 'local'
                }]
            }]
        }));
        this.gridInputFilterRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            helpSource: 'network_filter_rules',
            inputLines:[{
                xtype:'checkbox',
                name: "Enable Forward Filter Rule",
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable Input Filter Rule")
            }, {
                xtype:'checkbox',
                name: "Enable IPv6 Support",
                dataIndex: "ipv6Enabled",
                fieldLabel: i18n._("Enable IPv6 Support")
            }, {
                xtype:'textfield',
                name: "Description",
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.uvm.network.FilterRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getFilterRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: "combo",
                    name: "blocked",
                    allowBlank: false,
                    dataIndex: "blocked",
                    fieldLabel: i18n._("Action"),
                    editable: false,
                    store: [[true,i18n._('Block')], [false,i18n._('Pass')]],
                    queryMode: 'local'
                }]
            }]
        }));
    },
    // UPnP Panel
    buildUPnP: function() {
        this.upnpActionStore = [
            [false, i18n._("Deny")],
            [true, i18n._("Allow")]
        ];
        this.upnpActionMap = Ung.Util.createStoreMap(this.upnpActionStore);

        this.gridUpnpRules = Ext.create('Ung.grid.Panel', {
            name: 'UPnP Rules',
            margin: '5 0 0 0',
            height: 200,
            settingsCmp: this,
            hasReorder: true,
            addAtTop: false,
            dataExpression:'settings.upnpSettings.upnpRules.list',
            recordJavaClass: "com.untangle.uvm.network.UpnpRule",
            emptyRow: {
                "ruleId": -1,
                "enabled": true,
                "description": "",
                "priority": 1
            },
            fields: [{
                name: 'ruleId'
            }, {
                name: 'enabled'
            },{
                name: 'allow'
            }, {
                name: 'conditions'
            },{
                name: 'description'
            }, {
                name: 'javaClass'
            }],
            columns: [{
                header: i18n._("Rule Id"),
                width: 50,
                dataIndex: 'ruleId',
                renderer: function(value) {
                    if (value < 0) {
                        return i18n._("new");
                    } else {
                        return value;
                    }
                }
            }, {
                xtype:'checkcolumn',
                header: i18n._("Enable"),
                dataIndex: 'enabled',
                resizable: false,
                width:55
            }, {
                header: i18n._( "Action" ),
                width: 100,
                dataIndex: "allow",
                renderer: Ext.bind(function( value, metadata, record ) {
                    return this.upnpActionMap[value];
                }, this ),
                editor: {
                    xtype: 'combo',
                    store: this.upnpActionStore,
                    queryMode: 'local',
                    editable: false
                }
            }, {
                header: i18n._("Description"),
                width: 200,
                dataIndex: 'description',
                flex: 1,
                editor: {
                    xtype:'textfield',
                    emptyText: i18n._("[no description]")
                }
            }]
        });

        this.gridUPnpStatus = Ext.create('Ung.grid.Panel', {
            name: 'UPnP Status',
            margin: '5 0 0 0',
            height: 160,
            settingsCmp: this,
            hasAdd: false,
            hasDelete: true,
            hasEdit: false,
            hasReadOnly: true,
            columnsDefaultSortable: false,
            fields:[
                    'upnp_protocol',
                    'upnp_client_ip_address',
                    'upnp_client_port',
                    'upnp_destination_port',
                    'bytes'
            ],
            forceFit: true,
            autoExpandColumn: 'bytes',
            columns: [{
                header: i18n._("Protocol"),
                width: 80,
                dataIndex: 'upnp_protocol'
            }, {
                header: i18n._("Client IP Address"),
                width: 150,
                dataIndex: 'upnp_client_ip_address'
            }, {
                header: i18n._("Client Port"),
                width: 150,
                dataIndex: 'upnp_client_port'
            }, {
                header: i18n._("Destination Port"),
                width: 150,
                dataIndex: 'upnp_destination_port'
            }, {
                header: i18n._("Bytes"),
                dataIndex: 'bytes',
                width: 180,
                renderer: Ext.bind(function( value, metadata, record ) {
                    if (Ext.isEmpty(value)) {
                        return i18n._("Not set");
                    } else {
                        var v = value;
                        switch(value[value.length-1]){
                            case 'K':
                                v = parseInt(value.substr(0,value.length - 1), 10);
                                value = v * 1024;
                                break;
                            case 'M':
                                v = parseInt(value.substr(0,value.length - 1), 10);
                                value = v * 1024 * 1024;
                                break;
                        }
                        value = value / 1000;
                        var mbit_value = value/1000;
                        return Number(value).toFixed(2) + " kbps" + " (" + Number(mbit_value).toFixed(2) + " Mbit" + ")";
                    }
                }, this )
            }],
            bbar: [{
                xtype: 'button',
                text: '<i class="material-icons" style="font-size: 20px;">refresh</i> <span style="vertical-align: middle;">' + i18n._("Refresh") + '</span>',
                handler: function(button) {
                    button.up('panel').updateStatus();
                }
            }],
            updateStatus: function(){
                Ung.Main.getNetworkManager().getUpnpManager(Ext.bind(function(status, exception){
                    if(Ung.Util.handleException(exception)) return;
                    this.getStore().loadData(Ext.decode(status)["active"]);
                }, this), "--status", "");
            },
            deleteHandler: function(record){
                Ung.Main.getNetworkManager().getUpnpManager(Ext.bind(function(result, exception){
                    if(Ung.Util.handleException(exception)) return;
                    this.updateStatus();
                }, this), "--delete", "'" + Ext.encode(record.data) + "'");
            }
        });

        this.panelUPnP = Ext.create('Ext.panel.Panel',{
            name: 'UPnP',
            helpSource: 'network_upnp',
            title: i18n._('UPnP'),
            autoScroll: true,
            cls: 'ung-panel',
            items: [{
                xtype: 'fieldset',
                title: i18n._('UPnP'),
                defaults: {
                    labelWidth: 250
                },
                items: [{
                    xtype: "checkbox",
                    fieldLabel: i18n._("Enabled"),
                    checked: this.settings.upnpSettings.upnpEnabled,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.upnpSettings.upnpEnabled = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: 'fieldset',
                    name: 'upnp_status_fieldset',
                    title: i18n._('Status'),
                    items: [{
                        xtype: 'component',
                        name: 'upnpStatusLabel',
                        html: ' '
                    }, this.gridUPnpStatus]
                },{
                    xtype: "checkbox",
                    fieldLabel: i18n._("Secure Mode"),
                    checked: this.settings.upnpSettings.secureMode,
                    listeners: {
                        "change": {
                            fn: Ext.bind(function(elem, newValue) {
                                this.settings.upnpSettings.secureMode = newValue;
                            }, this)
                        }
                    }
                }, {
                    xtype: 'fieldset',
                    title: i18n._('Access Control Rules'),
                    items: [
                    this.gridUpnpRules
                    ]
                }]
            }]
        });
        this.gridUpnpRules.setRowEditor(Ext.create('Ung.RowEditorWindow',{
            sizeToParent: true,
            inputLines:[{
                xtype:'checkbox',
                dataIndex: "enabled",
                fieldLabel: i18n._("Enable")
            }, {
                xtype:'textfield',
                dataIndex: "description",
                fieldLabel: i18n._("Description"),
                emptyText: i18n._("[no description]"),
                width: 500
            }, {
                xtype:'fieldset',
                title: i18n._("If all of the following conditions are met:"),
                items:[{
                    xtype:'rulebuilder',
                    settingsCmp: this,
                    javaClass: "com.untangle.uvm.network.UpnpRuleCondition",
                    dataIndex: "conditions",
                    conditions: this.getUpnpRuleConditions()
                }]
            }, {
                xtype: 'fieldset',
                title: i18n._('Perform the following action(s):'),
                items: [{
                    xtype: "combo",
                    allowBlank: false,
                    dataIndex: "allow",
                    fieldLabel: i18n._("Action"),
                    editable: false,
                    store: this.upnpActionStore,
                    queryMode: 'local'
                }]
            }]
        }));
        // !!! only do when tab is selected
        this.gridUPnpStatus.updateStatus();
    },
    // NetworkCards Panel
    buildNetworkCards: function() {
        this.duplexStore = [
            ["AUTO", i18n._( "Auto" )],
            ["M10000_FULL_DUPLEX", i18n._( "10000 Mbps, Full Duplex" )],
            ["M10000_HALF_DUPLEX", i18n._( "10000 Mbps, Half Duplex" )],
            ["M1000_FULL_DUPLEX", i18n._( "1000 Mbps, Full Duplex" )],
            ["M1000_HALF_DUPLEX", i18n._( "1000 Mbps, Half Duplex" )],
            ["M100_FULL_DUPLEX", i18n._( "100 Mbps, Full Duplex" )],
            ["M100_HALF_DUPLEX", i18n._( "100 Mbps, Half Duplex" )],
            ["M10_FULL_DUPLEX", i18n._( "10 Mbps, Full Duplex" )],
            ["M10_HALF_DUPLEX", i18n._( "10 Mbps, Half Duplex" )]
        ];
        this.duplexMap = Ung.Util.createStoreMap(this.duplexStore);

        this.gridNetworkCards = Ext.create('Ung.grid.Panel', {
            name: 'Network Cards',
            helpSource: 'network_network_cards',
            title: i18n._('Network Cards'),
            settingsCmp: this,
            hasAdd: false,
            hasDelete: false,
            hasEdit: false,
            columnsDefaultSortable: false,
            dataProperty: 'devices',
            recordJavaClass: "com.untangle.uvm.network.DeviceSettings",
            fields: [{
                name: 'deviceName'
            }, {
                name: 'duplex'
            }, {
                name: 'mtu'
            }],
            columns: [{
                header: i18n._("Device Name"),
                width: 250,
                dataIndex: 'deviceName'
            }, {
                header: i18n._("MTU"),
                dataIndex: 'mtu',
                width: 100,
                renderer: function(value) {
                    if ( value === "" || value === null ) {
                        return "Auto";
                    } else {
                        return value;
                    }
                },
                editor: {
                    xtype:'numberfield'
                }
            }, {
                header: i18n._("Ethernet Media"),
                dataIndex: 'duplex',
                width: 250,
                renderer: Ext.bind(function( value, metadata, record ) {
                    return this.duplexMap[value];
                }, this ),
                editor: {
                    xtype: 'combo',
                    store: this.duplexStore,
                    queryMode: 'local',
                    editable: false
                }
            }]
        });
    },
    // Troubleshooting Panel
    buildTroubleshooting: function() {
        this.panelTroubleshooting = Ext.create('Ext.panel.Panel',{
            name: 'Troubleshooting',
            helpSource: 'network_troubleshooting',
            title: i18n._('Troubleshooting'),
            cls: 'ung-panel',
            autoScroll: true,
            items: [{
                xtype: 'fieldset',
                title: i18n._('Network Tests'),
                layout: 'vbox',
                defaults: {
                    xtype: 'button',
                    width: 200,
                    margin: '10 0 0 0',
                    textAlign: 'left',
                    scope: this
                },
                items: [{
                    text: i18n._( "Connectivity Test" ),
                    iconCls: 'icon-test-connectivity',
                    handler: function() {
                        this.openConnectivityTest();
                    }
                }, {
                    text: i18n._( "Ping Test" ),
                    iconCls: "icon-test-ping",
                    handler: function() {
                        this.openPingTest();
                    }
                },{
                    text: i18n._( "DNS Test" ),
                    iconCls: "icon-test-dns",
                    handler: function() {
                        this.openDnsTest();
                    }
                },{
                    text: i18n._( "Connection Test" ),
                    iconCls: "icon-test-tcp",
                    handler: function() {
                        this.openTcpTest();
                    }
                },{
                    text: i18n._( "Traceroute Test" ),
                    iconCls: "icon-test-traceroute",
                    handler: function() {
                        this.openTracerouteTest();
                    }
                },{
                    text: i18n._( "Download Test" ),
                    iconCls: "icon-test-download",
                    handler: function() {
                        this.openDownloadTest();
                    }
                },{
                    text: i18n._( "Packet Test" ),
                    iconCls: "icon-test-packet",
                    handler: function() {
                        this.openPacketTest();
                    }
                }]
            }],
            isDirty: function() {
                return false;
            }
        });
    },
    openConnectivityTest: function() {
        if(!this.connectivityTest) {
            this.connectivityTest = Ext.create('Webui.config.network.NetworkTest', {
                helpSource: 'network_troubleshooting',
                settingsCmp: this,
                title: i18n._('Connectivity Test'),
                testDescription: i18n._("The <b>Connectivity Test</b> verifies a working connection to the Internet."),
                testErrorMessage : i18n._( "Unable to complete the Connectivity Test." ),
                testEmptyText: i18n._("Connectivity Test Output"),
                getCommand: function() {
                    var script= [
                        'echo -n "Testing DNS ... " ; success="Successful";',
                        'dig updates.untangle.com > /dev/null 2>&1; if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                        'echo -n "Testing TCP Connectivity ... ";',
                        'echo "GET /" | netcat -q 0 -w 15 updates.untangle.com 80 > /dev/null 2>&1;',
                        'if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                        'echo "Test ${success}!"'
                    ];
                    return ["/bin/bash","-c", script.join("")];
                }
            });
            this.subCmps.push(this.connectivityTest);
        }
        this.connectivityTest.show();
    },
    testConnectivity: function () {
        Ext.MessageBox.wait( i18n._( "Testing Internet Connectivity" ), i18n._( "Please wait" ));
        var script = [
            'dig updates.untangle.com > /dev/null 2>&1;',
            'if [ "$?" != "0" ]; then echo "'+i18n._('Failed to connect to the Internet, DNS failed.')+'"; exit 1; fi;',
            'echo "GET /" | netcat -q 0 -w 15 updates.untangle.com 80 > /dev/null 2>&1;',
            'if [ "$?" != "0" ]; then echo "'+i18n._('Failed to connect to the Internet, TCP failed.')+'"; exit 1; fi;',
            'echo "'+i18n._('Successfully connected to the Internet.')+'";'
        ];
        var command =  "/bin/bash -c " + script.join("");
        var execResultReader = null;
        Ung.Main.getExecManager().exec(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            Ext.MessageBox.alert(i18n._("Test Connectivity Result"), result.output);
        }, this), command);
    },
    openPingTest: function(destination) {
        if(!this.pingTest) {
            this.pingTest = Ext.create('Webui.config.network.NetworkTest',{
                helpSource: 'network_troubleshooting',
                settingsCmp: this,
                title: i18n._('Ping Test'),
                testDescription: i18n._("The <b>Ping Test</b> can be used to test that a particular host or client can be pinged"),
                testErrorMessage : i18n._( "Unable to complete the Ping Test." ),
                testEmptyText: i18n._("Ping Test Output"),
                initComponent : function() {
                    var a = this;
                    this.testTopToolbar = [this.destination = new Ext.form.TextField({
                        xtype : "textfield",
                        width:150,
                        emptyText : i18n._( "IP Address or Hostname" ),
                        listeners: {
                            specialkey: function(field, e){
                                if (e.getKey() == e.ENTER) {
                                    a.onRunTest();
                                }
                            }
                        }
                    })];
                    Webui.config.network.NetworkTest.prototype.initComponent.apply(this, arguments);
                },
                getCommand: function() {
                    var destination = this.destination.getValue();
                    return "ping -c 5 " + destination + "";
                },
                enableParameters : function( isEnabled ){
                    if ( isEnabled ) {
                        this.destination.enable();
                    } else {
                        this.destination.disable();
                    }
                },
                isValid : function() {
                    var destination = this.destination.getValue();
                    if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                         ( !Ext.form.VTypes.ipAddress( destination, this.destination ) &&
                           !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                        Ext.MessageBox.show({
                            title : i18n._( "Warning" ),
                            msg : i18n._( "Please enter a valid IP Address or Hostname" ),
                            icon : Ext.MessageBox.WARNING,
                            buttons : Ext.MessageBox.OK
                        });
                        return false;
                    }
                    return true;
                }
            });
            this.subCmps.push(this.pingTest);
        }
        if(destination !== undefined) {
            this.pingTest.destination.setValue(destination);
        }
        this.pingTest.show();
    },
    openDnsTest: function() {
        if(!this.dnsTest) {
            this.dnsTest = Ext.create('Webui.config.network.NetworkTest',{
                helpSource: 'network_troubleshooting',
                settingsCmp: this,
                title: i18n._('DNS Test'),
                testDescription: i18n._("The <b>DNS Test</b> can be used to test DNS lookups"),
                testErrorMessage : i18n._( "Unable to complete DNS test." ),
                testEmptyText: i18n._("DNS Test Output"),
                initComponent : function() {
                    var a = this;
                    this.testTopToolbar = [this.destination = new Ext.form.TextField({
                        xtype : "textfield",
                        width:150,
                        emptyText : i18n._( "Hostname" ),
                        listeners: {
                            specialkey: function(field, e){
                                if (e.getKey() == e.ENTER) {
                                    a.onRunTest();
                                }
                            }
                        }
                    })];
                    Webui.config.network.NetworkTest.prototype.initComponent.apply(this, arguments);
                },
                getCommand: function() {
                    var destination = this.destination.getValue().replace('\'','');
                    var script=['host \''+ destination +'\';',
                        'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'];
                    return ["/bin/bash","-c", script.join("")];
                },
                enableParameters : function( isEnabled ){
                    if ( isEnabled ) {
                        this.destination.enable();
                    } else {
                        this.destination.disable();
                    }
                },
                isValid : function() {
                    var destination = this.destination.getValue();
                    if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                         ( !Ext.form.VTypes.ipAddress( destination, this.destination ) &&
                           !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                        Ext.MessageBox.show({
                            title : i18n._( "Warning" ),
                            msg : i18n._( "Please enter a valid Hostname" ),
                            icon : Ext.MessageBox.WARNING,
                            buttons : Ext.MessageBox.OK
                        });
                        return false;
                    }
                    return true;
                }
            });
            this.subCmps.push(this.dnsTest);
        }
        this.dnsTest.show();
    },
    openTcpTest: function(destination, port) {
        if(!this.tcpTest) {
            this.tcpTest = Ext.create('Webui.config.network.NetworkTest',{
                helpSource: 'network_troubleshooting',
                settingsCmp: this,
                title: i18n._('Connection Test'),
                testDescription: i18n._("The <b>Connection Test</b> verifies that Untangle can open a TCP connection to a port on the given host or client."),
                testErrorMessage : i18n._( "Unable to complete Connection test." ),
                testEmptyText: i18n._("Connection Test Output"),
                initComponent : function() {
                    var a = this;
                    this.testTopToolbar = [this.destination = new Ext.form.TextField({
                        xtype : "textfield",
                        width:150,
                        emptyText : i18n._( "IP Address or Hostname" ),
                        listeners: {
                            specialkey: function(field, e){
                                if (e.getKey() == e.ENTER) {
                                    a.onRunTest();
                                }
                            }
                        }
                    }), this.port = new Ext.form.field.Number({
                        xtype : "numberfield",
                        minValue : 1,
                        maxValue : 65536,
                        width: 60,
                        emptyText : i18n._( "Port" ),
                        listeners: {
                            specialkey: function(field, e){
                                if (e.getKey() == e.ENTER) {
                                    a.onRunTest();
                                }
                            }
                        }
                    })];
                    Webui.config.network.NetworkTest.prototype.initComponent.apply(this, arguments);
                },
                getCommand: function() {
                    var destination = this.destination.getValue().replace('\'','');
                    var port = this.port.getValue();
                    var script=['echo 1 | netcat -q 0 -v -w 15 \'' + destination + '\' \'' + port +'\';',
                        'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'];
                    return ["/bin/bash","-c", script.join("")];
                },
                enableParameters : function( isEnabled ){
                    if ( isEnabled ) {
                        this.destination.enable();
                        this.port.enable();
                    } else {
                        this.destination.disable();
                        this.port.disable();
                    }
                },
                isValid : function() {
                    var destination = this.destination.getValue();
                    var port = this.port.getValue();
                    if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                         ( !Ext.form.VTypes.ipAddress( destination, this.destination ) &&
                           !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                        Ext.MessageBox.show({
                            title : i18n._( "Warning" ),
                            msg : i18n._( "Please enter a valid IP Address or Hostname" ),
                            icon : Ext.MessageBox.WARNING,
                            buttons : Ext.MessageBox.OK
                        });
                        return false;
                    }
                    return true;
                }
            });
            this.subCmps.push(this.tcpTest);
        }
        if(destination !== undefined) {
            this.tcpTest.destination.setValue(destination);
        }
        if(port !== undefined) {
            this.tcpTest.port.setValue(port);
        }
        this.tcpTest.show();
    },
    openTracerouteTest: function() {
        if(!this.tracerouteTest) {
            this.tracerouteTest = Ext.create('Webui.config.network.NetworkTest',{
                helpSource: 'network_troubleshooting',
                settingsCmp: this,
                title: i18n._('Traceroute Test'),
                testDescription: i18n._("The <b>Traceroute Test</b> traces the route to a given host or client."),
                testErrorMessage : i18n._( "Unable to complete the Traceroute Test." ),
                testEmptyText: i18n._("Traceroute Test Output"),
                initComponent : function() {
                    var a = this;
                    this.testTopToolbar = [this.destination = new Ext.form.TextField({
                        xtype : "textfield",
                        width:150,
                        emptyText : i18n._( "IP Address or Hostname" ),
                        listeners: {
                            specialkey: function(field, e){
                                if (e.getKey() == e.ENTER) {
                                    a.onRunTest();
                                }
                            }
                        }
                    }), this.protocol = new Ext.form.field.ComboBox({
                        xtype: "combo",
                        editable: false,
                        style: "margin-left: 10px",
                        width: 100,
                        value: "U",
                        store: [['U','UDP'], ['T','TCP'], ['I','ICMP']]
                    })];
                    Webui.config.network.NetworkTest.prototype.initComponent.apply(this, arguments);
                },
                getCommand: function() {
                    var destination = this.destination.getValue().replace('\'','');
                    var protocol = "-" + this.protocol.getValue().replace('\'','');
                    var script = ['traceroute' + ' \'' + protocol + '\' \'' + destination + '\' ;',
                      'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'];
                    return ["/bin/bash","-c", script.join("")];
                },
                enableParameters : function( isEnabled ){
                    if ( isEnabled ) {
                        this.destination.enable();
                        this.protocol.enable();
                    } else {
                        this.destination.disable();
                        this.protocol.disable();
                    }
                },
                isValid : function() {
                    var destination = this.destination.getValue();
                    if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                         ( !Ext.form.VTypes.ipAddress( destination, this.destination ) &&
                           !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                        Ext.MessageBox.show({
                            title : i18n._( "Warning" ),
                            msg : i18n._( "Please enter a valid IP Address or Hostname" ),
                            icon : Ext.MessageBox.WARNING,
                            buttons : Ext.MessageBox.OK
                        });
                        return false;
                    }
                    return true;
                }
            });
            this.subCmps.push(this.tracerouteTest);
        }
        this.tracerouteTest.show();
    },
    openDownloadTest: function() {
        if(!this.downloadTest) {
            this.downloadTest = Ext.create('Webui.config.network.NetworkTest',{
                helpSource: 'network_troubleshooting',
                settingsCmp: this,
                title: i18n._('Download Test'),
                testDescription: i18n._("The <b>Download Test</b> downloads a file."),
                testErrorMessage : i18n._( "Unable to complete the Download Test." ),
                testEmptyText: i18n._("Download Test Output"),
                initComponent : function() {
                    var a = this;
                    this.testTopToolbar = [this.url = new Ext.form.field.ComboBox({
                        xtype : "combo",
                        editable : true,
                        style : "margin-left: 10px",
                        width : 500,
                        value : "http://cachefly.cachefly.net/5mb.test",
                        store : [
                            ['http://cachefly.cachefly.net/50mb.test','http://cachefly.cachefly.net/50mb.test'],
                            ['http://cachefly.cachefly.net/5mb.test','http://cachefly.cachefly.net/5mb.test'],
                            ['http://download.thinkbroadband.com/50MB.zip','http://download.thinkbroadband.com/50MB.zip'],
                            ['http://download.thinkbroadband.com/5MB.zip','http://download.thinkbroadband.com/5MB.zip'],
                            ['http://download.untangle.com/data.php','http://download.untangle.com/data.php']
                        ],
                        listeners: {
                            specialkey: function(field, e){
                                if (e.getKey() == e.ENTER) {
                                    a.onRunTest();
                                }
                            }
                        }
                    })];
                    Webui.config.network.NetworkTest.prototype.initComponent.apply(this, arguments);
                },
                getCommand: function() {
                    var url = this.url.getValue().replace('\'','');
                    var script = ['wget --output-document=/dev/null ' + ' \'' + url + '\' ;'];
                    return ["/bin/bash","-c", script.join("")];
                },
                enableParameters : function( isEnabled ){
                    if ( isEnabled ) {
                        this.url.enable();
                    } else {
                        this.url.disable();
                    }
                },
                isValid : function() {
                    var url = this.url.getValue();
                    if ( Ext.isEmpty( url ) ) {
                        Ext.MessageBox.show({
                            title : i18n._( "Warning" ),
                            msg : i18n._( "Please enter a valid Url" ),
                            icon : Ext.MessageBox.WARNING,
                            buttons : Ext.MessageBox.OK
                        });
                        return false;
                    }
                    return true;
                }
            });
            this.subCmps.push(this.downloadTest);
        }
        this.downloadTest.show();
    },
    openPacketTest: function() {
        if(!this.packetTest) {
            this.packetTest = Ext.create('Webui.config.network.NetworkTest',{
                helpSource: 'network_troubleshooting',
                settingsCmp: this,
                title: i18n._('Packet Test'),
                testDescription: i18n._("The <b>Packet Test</b> can be used to view packets on the network wire for troubleshooting."),
                testErrorMessage : i18n._( "Unable to complete the Packet Test." ),
                testEmptyText: i18n._("Packet Test Output"),
                testAdvancedText: i18n._("tcpdump arguments and expression"),
                outputFilename: null,
                initComponent : function() {
                    var a = this;
                    var timeouts = [[ 5, i18n._( "5 seconds" )],
                                    [ 30, i18n._( "30 seconds" )],
                                    [ 120, i18n._( "120 seconds" )]];
                    var interfaceStore = Ung.Util.getInterfaceListSystemDev(false, false, true);
                    this.testTopToolbar = [this.destination = new Ext.form.TextField({
                        xtype : "textfield",
                        value : "any",
                        width:150,
                        emptyText : i18n._( "IP Address or Hostname" ),
                        listeners: {
                            specialkey: function(field, e){
                                if (e.getKey() == e.ENTER) {
                                    a.onRunTest();
                                }
                            }
                        }
                    }), this.port = new Ext.form.field.Number({
                        xtype : "numberfield",
                        minValue : 1,
                        maxValue : 65536,
                        width: 60,
                        emptyText : i18n._( "Port" ),
                        listeners: {
                            specialkey: function(field, e){
                                if (e.getKey() == e.ENTER) {
                                    a.onRunTest();
                                }
                            }
                        }
                    }), this.intf = new Ext.form.field.ComboBox({
                        xtype : "combo",
                        editable : false,
                        style : "margin-left: 10px",
                        width : 100,
                        value : interfaceStore[0][0],
                        store : interfaceStore
                    }),this.advancedToggleButton = Ext.create("Ext.button.Button",{
                        text : i18n._("Advanced"),
                        toggleHandler : this.onAdvanced,
                        enableToggle: true,
                        scope : this,
                        width: 65
                    }),
                    {
                        xtype : "label",
                        html : i18n._("Timeout:"),
                        style : "margin-left: 18px"
                    }, this.timeout = new Ext.form.ComboBox({
                        xtype : "combo",
                        style : "margin-left: 2px",
                        value : timeouts[0][0],
                        editable : false,
                        width : 100,
                        store : timeouts
                    })];

                    Webui.config.network.NetworkTest.prototype.initComponent.apply(this, arguments);

                    var toolbar = this.down('panel>panel[name="testpanel"]').getDockedItems()[0];
                    this.exportButton = Ext.create("Ext.Button", {
                        text: i18n._('Export'),
                        tooltip: i18n._('Export To File'),
                        iconCls: 'icon-export',
                        name: 'export',
                        disabled: true,
                        handler: this.onExport,
                        scope: this
                    });
                    toolbar.add( this.exportButton );
                },
                buildTraceCommand: function(){
                    var traceFixedOptionsTemplate = [
                        "-U",
                        "-l",
                        "-v"
                    ];
                    var traceOverrideOptionsTemplate = [
                        "-n",
                        "-s 65535",
                        "-i " + this.intf.getValue()
                    ];
                    var traceOptions = traceFixedOptionsTemplate.concat( traceOverrideOptionsTemplate );
                    var traceExpression = [];
                    if( this.advancedToggleButton.pressed ){
                        traceExpression = [this.advancedInput.getValue()];
                    } else {
                        var destination = this.destination.getValue();
                        var port = this.port.getValue();
                        if( destination !== null & destination.toLowerCase() !== "any") {
                            traceExpression.push( "host " + destination );
                        }
                        if( port !== null) {
                            traceExpression.push( "port " + port );
                        }
                    }
                    var traceArguments = traceOptions.join(" ") + " " + traceExpression.join( " and ");
                    return traceArguments;
                },
                getCommand: function() {
                    var timeout = this.timeout.getValue();
                    var filename = this.generateExportFilename().replace('\'','');
                    var traceCommand = this.buildTraceCommand().replace('\'','');
                    var script = [
                        '/usr/share/untangle/bin/ut-network-tests-packet.py'+
                            ' --timeout ' + timeout  +
                            ' --filename ' + filename +
                            ' --arguments \'' + traceCommand + '\''
                    ];
                    return ["/bin/bash","-c", script.join("")];
                },
                enableParameters : function( isEnabled ){
                    if ( isEnabled ) {
                        this.destination.enable();
                        this.port.enable();
                        this.intf.enable();
                        this.timeout.enable();
                        this.advancedToggleButton.enable();
                        this.exportButton.enable();
                    } else {
                        this.destination.disable();
                        this.port.disable();
                        this.intf.disable();
                        this.timeout.disable();
                        this.advancedToggleButton.disable();
                        this.exportButton.disable();
                    }
                },
                isValid : function() {
                    var destination = this.destination.getValue();
                    if( this.advancedToggleButton.pressed == false ){
                        if ( Ext.isEmpty(destination) /*TODO: verify host or ip
                             ( !Ext.form.VTypes.ipAddress( destination, this.destination ) &&
                            !Ext.form.VTypes.hostname( destination, this.destination ))*/) {
                            Ext.MessageBox.show({
                                title: i18n._( "Warning" ),
                                msg: i18n._( "Please enter a valid IP Address or Hostname" ),
                                icon: Ext.MessageBox.WARNING,
                                buttons: Ext.MessageBox.OK
                            });
                            return false;
                        }
                    }
                    return true;
                },
                onAdvanced: function( button, state ){
                    button.setText( state ? i18n._("Basic") : i18n._("Advanced") );
                    if( !this.advancedInput ){
                        var defaultValue = "";
                        var destination = this.destination.getValue();
                        var port = this.port.getValue();
                        var intf = this.intf.getValue();
                        var timeout = this.timeout.getValue();
                        if(destination !== null && destination.toLowerCase() != "any") {
                            defaultValue += "host " + destination;
                        }
                        if(port !== null) {
                            defaultValue += ( defaultValue.length > 0 ? " and ": "") + "port " + port;
                        }

                        this.advancedInput =Ext.create("Ext.form.field.TextArea", {
                            name : "advancedInput",
                            emptyText: this.testAdvancedText,
                            hideLabel : true,
                            fieldCls : "ua-test-output",
                            value: defaultValue
                        });
                        var testpanel = this.down('panel>panel[name="testpanel"]');
                        testpanel.insert(0, this.advancedInput );
                    }
                    this.advancedInput.setVisible( state );
                    if( state ){
                        this.destination.disable();
                        this.port.disable();
                    }else{
                        this.destination.enable();
                        this.port.enable();
                    }
                },
                onExport: function() {
                    Ext.MessageBox.wait(i18n._("Exporting Packet Dump..."), i18n._("Please wait"));
                    var downloadForm = document.getElementById('downloadForm');
                    downloadForm["type"].value="NetworkTestExport";
                    downloadForm["arg1"].value=this.outputFilename;
                    downloadForm.submit();
                    Ext.MessageBox.hide();
                },
                generateExportFilename: function(){
                    this.outputFilename =
                        "/tmp/network-tests/" +
                        "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function(c) {
                            var r = Math.random()*16|0, v = c == 'x' ? r : (r&0x3|0x8);
                            return v.toString(16);
                        }) + ".pcap";
                    return this.outputFilename;
                }
            });
            this.subCmps.push(this.packetTest);
        }
        this.packetTest.show();
    },
    validate: function(isApply) {
        var i;
        var domainNameCmp =this.panelHostName.down('textfield[name="DomainName"]');
        if (!domainNameCmp.isValid()) {
            this.tabs.setActiveTab(this.panelHostName);
            domainNameCmp.focus(true);
            Ext.MessageBox.alert(i18n._('Warning'), i18n._('A Domain Name must be specified.'));
            return false;
        }
        var httpsPortCmp =this.panelServices.down('numberfield[name="httpsPort"]');
        if (!httpsPortCmp.isValid()) {
            this.tabs.setActiveTab(this.panelServices);
            httpsPortCmp.focus(true);
            Ext.MessageBox.alert(i18n._('Warning'), i18n._('A HTTPS port must be specified.'));
            return false;
        }

        var httpPortCmp =this.panelServices.down('numberfield[name="httpPort"]');
        if (!httpPortCmp.isValid()) {
            this.tabs.setActiveTab(this.panelServices);
            httpPortCmp.focus(true);
            Ext.MessageBox.alert(i18n._('Warning'), i18n._('A HTTP port must be specified.'));
            return false;
        }
        if(this.settings.qosSettings.qosEnabled) {
            var qosBandwidthList = this.gridQosWanBandwidth.getList();
            for( i=0; i<qosBandwidthList.length; i++) {
                var qosBandwidth = qosBandwidthList[i];
                if(qosBandwidth.configType == "ADDRESSED" &&
                   qosBandwidth.isWan &&
                   ( Ext.isEmpty(qosBandwidth.downloadBandwidthKbps) || Ext.isEmpty(qosBandwidth.uploadBandwidthKbps) )) {
                    this.tabs.setActiveTab(this.panelAdvanced);
                    this.advancedTabPanel.setActiveTab(this.panelQoS);
                    this.gridQosWanBandwidth.focus();
                    Ext.MessageBox.alert(i18n._("Failed"), i18n._("QoS is Enabled. Please set valid Download Bandwidth and Upload Bandwidth limits in WAN Bandwidth for all WAN interfaces."));
                    return false;
                }
            }
        }
        var rule, found = false;
        var rules = this.gridInputFilterRules.getList();
        if( rules ) {
            for( i=0; i<rules.length ; i++ ) {
                rule = rules[i];
                if ( rule.description == "Block All" ) {
                    found = true;
                    if ( rule.enabled == false ) {
                        this.tabs.setActiveTab(this.panelAdvanced);
                        this.advancedTabPanel.setActiveTab(this.panelFilter);
                        this.gridInputFilterRules.focus();
                        Ext.MessageBox.alert(i18n._("Failed"), i18n._("The Block All rule in Input Filter Rules is disabled. This is dangerous and not allowed! Refer to the documentation."));
                        return false;
                    }
                    break;
                }
            }
        }
        if (!found) {
            this.tabs.setActiveTab(this.panelAdvanced);
            this.advancedTabPanel.setActiveTab(this.panelFilter);
            this.gridInputFilterRules.focus();
            Ext.MessageBox.alert(i18n._("Failed"), i18n._("The Block All rule in Input Filter Rules is missing. This is dangerous and not allowed! Refer to the documentation."));
            return false;
        }
        if(!this.initialAllowSSHEnabled && rules && !this.confirmedAllowSSHEnabled) {
            for( i=0; i<rules.length ; i++ ) {
                rule = rules[i];
                if ( rule.description == "Allow SSH" ) {
                    if ( rule.enabled == true) {
                        this.tabs.setActiveTab(this.panelAdvanced);
                        this.advancedTabPanel.setActiveTab(this.panelFilter);
                        this.gridInputFilterRules.focus();
                        Ext.MessageBox.confirm(i18n._("Warning"),
                                               i18n._("The Allow SSH rule in Input Filter Rules has been enabled.") + "<br/><br/>" +
                                               i18n._("If the admin/root password is poorly chosen, enabling SSH can be very dangerous and will compromise the security of the server.") + "<br/><br/>" +
                                               i18n._("Do you want to continue anyway?"),
                                               Ext.bind(function(btn, text) {
                                                   if (btn == 'yes') {
                                                       this.confirmedAllowSSHEnabled=true;
                                                       this.saveAction(isApply);
                                                   }
                                               }, this));
                        return false;
                    }
                    break;
                }
            }
        }
        return true;
    },
    needRackReload: false,
    save: function (isApply) {
        Ung.MetricManager.stop(); //stop all RPC calls
        Ung.Main.getNetworkManager().setNetworkSettings(Ext.bind(function(result, exception) {
            Ung.MetricManager.start(false); //resume all RPC calls

            if(Ung.Util.handleException(exception)) return;
            delete rpc.networkSettings; // clear cached settings object
            if(isApply) {
                //On apply we have to reload all and keep selected tabs
                var configNetwork = Ext.clone(Ung.Main.configMap["network"]);
                Ext.apply(configNetwork, {
                    needRackReload: true,
                    activeTabIndex: this.tabs.items.findIndex('id', this.tabs.getActiveTab().id),
                    advancedTabIndex: this.advancedTabPanel.items.findIndex('id', this.advancedTabPanel.getActiveTab().id)
                });
                Webui.config.network.superclass.closeWindow.call(this);
                Ung.Main.openConfig(configNetwork);
            } else {
                Ext.MessageBox.hide();
                this.closeWindow();
            }
        }, this), this.settings);
    },
    beforeSave: function(isApply, handler) {
        Ext.MessageBox.wait(i18n._("Applying Network Settings..."), i18n._("Please wait"));
        this.needRackReload = true;
        this.gridInterfaces.getStore().clearFilter(true);
        this.settings.interfaces.list = this.gridInterfaces.getList();
        var i,
            qosBandwidthList = this.gridQosWanBandwidth.getList(),
            qosBandwidthMap = {};
        for(i=0; i<qosBandwidthList.length; i++) {
            qosBandwidthMap[qosBandwidthList[i].interfaceId] = qosBandwidthList[i];
        }
        for(i=0; i<this.settings.interfaces.list.length; i++) {
            var intf=this.settings.interfaces.list[i];
            var intfBandwidth = qosBandwidthMap[intf.interfaceId];
            if(intfBandwidth) {
                intf.downloadBandwidthKbps = intfBandwidth.downloadBandwidthKbps;
                intf.uploadBandwidthKbps = intfBandwidth.uploadBandwidthKbps;
            }
        }

        this.settings.portForwardRules.list = this.gridPortForwardRules.getList();
        this.settings.natRules.list = this.gridNatRules.getList();
        this.settings.bypassRules.list = this.gridBypassRules.getList();
        this.settings.staticRoutes.list = this.gridStaticRoutes.getList();
        this.settings.qosSettings.qosRules.list = this.gridQosRules.getList();
        this.settings.qosSettings.qosPriorities.list = this.gridQosPriorities.getList();
        this.settings.upnpSettings.upnpRules.list = this.gridUpnpRules.getList();
        this.settings.forwardFilterRules.list = this.gridForwardFilterRules.getList();
        this.settings.inputFilterRules.list = this.gridInputFilterRules.getList();
        this.settings.dnsSettings.staticEntries.list =  this.gridDnsStaticEntries.getList();
        this.settings.dnsSettings.localServers.list = this.gridDnsLocalServers.getList();
        this.settings.staticDhcpEntries.list = this.gridDhcpStaticEntries.getList();
        this.settings.devices.list = this.gridNetworkCards.getList();
        handler.call(this, isApply);
    },
    closeWindow: function() {
        this.callParent(arguments);
        if (this.needRackReload) {
            Ung.Util.goToStartPage();
        }
    }
});

Ext.define("Webui.config.network.NetworkTest", {
    extend: "Ung.Window",
    settingsCmp: null,
    execResultReader: null,
    initComponent : function( ){
        Ext.applyIf( this, {
            testErrorMessage: i18n._( "Unable to run this Network Utility." ),
            testTopToolbar: []
        });
        this.bbar = [{
            iconCls: 'icon-help',
            text: i18n._('Help'),
            handler: function() {
                this.helpAction();
            },
            scope: this
        },'->',{
            name: "Close",
            iconCls: 'cancel-icon',
            text: i18n._('Cancel'),
            handler: function() {
                this.cancelAction();
            },
            scope: this
        }];

        this.items = [{
            xtype: 'panel',
            layout: { type: 'vbox', align: 'stretch' },
            items : [{
                xtype: "component",
                flex: 0,
                html: this.testDescription,
                margin: '10 10 10 10'
            },{
                name: 'testpanel',
                xtype: "panel",
                margin: '10 10 10 10',
                flex: 1,
                layout: "fit",
                tbar: this.testTopToolbar.concat([this.runTest = Ext.create("Ext.button.Button",{
                    text: i18n._("Run Test"),
                    iconCls: "icon-test-run",
                    handler: this.onRunTest,
                    scope: this
                }),"->",this.clearOutput = Ext.create("Ext.button.Button",{
                    text: i18n._("Clear Output"),
                    iconCls: "icon-clear-output",
                    handler: this.onClearOutput,
                    scope: this
                })]),
                items : [this.output=Ext.create("Ext.form.field.TextArea", {
                    name : "output",
                    emptyText : this.testEmptyText,
                    hideLabel : true,
                    readOnly : true,
                    fieldCls : "ua-test-output"
                })]
            }]
        }];
        this.callParent(arguments);
    },
    helpAction: function() {
        Ung.Main.openHelp(this.helpSource);
    },
    getCommand: function () {
        return this.command;
    },
    onRunTest : function() {
        if ( Ext.isFunction(this.isValid)) {
            if ( !this.isValid()) {
                return;
            }
        }
        // Disable the run test button
        this.runTest.setIconCls( "icon-test-running" );
        this.output.focus();
        this.runTest.disable();
        this.enableParameters( false );
        var text = [];
        text.push( this.output.getValue());
        text.push( "" + i18n.timestampFormat((new Date()).getTime()) + " - " + i18n._("Test Started")+"\n");
        this.output.setValue( text.join( "" ));
        Ung.Main.getExecManager().execEvil(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            this.execResultReader = result;
            this.continueNetworkUtility();
        }, this), this.getCommand());
    },
    finishNetworkUtility: function() {
        this.runTest.setIconCls( "icon-test-run" );
        this.enableParameters( true );
        this.runTest.enable();
        this.execResultReader = null;
        this.scrollStart=0;
    },
    continueNetworkUtility: function(){
        if( this.execResultReader === null ) {
            return;
        }
        if ( this.hidden) {
            this.execResultReader.getResult(Ext.bind(function(result, exception) {
                if(Ung.Util.handleException(exception)) return;
                if(result == -1) {
                    this.execResultReader.destroy(Ext.bind(function(result, exception) {
                        this.finishNetworkUtility();
                    }, this));
                }
            }, this));
            return;
        }

        this.execResultReader.readFromOutput(Ext.bind(function(result, exception) {
            if(Ung.Util.handleException(exception)) return;
            var element=this.output.getEl();
            var text = [];
            text.push( this.output.getValue());
            if(result !== null) { //Test is running
                text.push(result);
                window.setTimeout( Ext.bind(this.continueNetworkUtility, this), 1000 );
            } else { //Test is finished
                text.push( "" + i18n.timestampFormat((new Date()).getTime()) + " - " + i18n._("Test Completed") +"\n\n");
                this.finishNetworkUtility();
            }
            this.output.setValue( text.join(""));
            //scroll to bottom
            var t = this.down('textarea[name="output"]');
            var t1 = t.getEl().down('textarea');
            t1.dom.scrollTop = 99999;
        }, this));
    },
    onClearOutput: function() {
        this.output.setValue( "" );
    },
    enableParameters: function( isEnabled ){
    }
});
//# sourceURL=network.js
