Ext.define('Ung.config.network.Interfaces', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.interfaces', //..

    title: 'Interfaces'.t(),
    layout: 'border',
    itemId: 'interfaces',
    tbar: [{
        xtype: 'displayfield',
        value: "Use this page to configure each interface's configuration and its mapping to a physical network card.".t()
    }],
    items: [{
        xtype: 'grid',
        itemId: 'interfacesGrid',
        reference: 'interfacesGrid',
        region: 'center',
        flex: 1,
        border: false,
        forceFit: true,
        // title: 'Interfaces'.t(),
        bind: '{interfaces}',
        fields: [{
            name: 'v4Address'
        }],
        columns: [{
            header: 'Id'.t(),
            dataIndex: 'interfaceId',
            width: 50,
            align: 'right'
        }, {
            header: 'Name'.t(),
            dataIndex: 'name',
            minWidth: 200
            // flex: 1
        }, {
            header: 'Connected'.t(),
            dataIndex: 'connected',
            width: 130
        }, {
            header: 'Device'.t(),
            dataIndex: 'physicalDev',
            width: 100
        }, {
            header: 'Speed'.t(),
            dataIndex: 'mbit',
            width: 100
        }, {
            header: 'Physical Dev'.t(),
            dataIndex: 'physicalDev',
            hidden: true,
            width: 80
        }, {
            header: 'System Dev'.t(),
            dataIndex: 'systemDev',
            hidden: true,
            width: 80
        }, {
            header: 'Symbolic Dev'.t(),
            dataIndex: 'symbolicDev',
            hidden: true,
            width: 80
        }, {
            header: 'IMQ Dev'.t(),
            dataIndex: 'imqDev',
            hidden: true,
            width: 80
        }, {
            header: 'Duplex'.t(),
            dataIndex: 'duplex',
            hidden: true,
            width: 100
            // renderer: duplexRenderer
        }, {
            header: 'Config'.t(),
            dataIndex: 'configType',
            width: 100
        }, {
            header: 'Current Address'.t(),
            dataIndex: 'v4Address',
            width: 150
        }, {
            header: 'is WAN'.t(),
            dataIndex: 'isWan'
        }],
        bbar: [{
            xtype: 'button',
            iconCls: 'fa fa-refresh',
            text: 'Refresh'.t(),
            handler: 'loadInterfaceStatusAndDevices'
        }]
    }, {
        xtype: 'tabpanel',
        region: 'east',
        split: 'true',
        collapsible: false,
        width: 450,
        // maxWidth: 450,
        hidden: true,
        bind: {
            title: '{si.name} ({si.physicalDev})',
            hidden: '{!si}',
            // activeItem: '{activePropsItem}'
        },
        items: [{
            title: 'Status'.t(),
            itemId: 'interfaceStatus',
            xtype: 'propertygrid',
            // header: false,
            hideHeaders: true,
            sortableColumns: false,
            align: 'right',
            nameColumnWidth: 150,
            // hidden: true,
            bind: {
                source: '{siStatus}',
                // hidden: '{isDisabled}'
            },
            sourceConfig: {
                device: { displayName: 'Device'.t() },
                macAddress: { displayName: 'MAC Address'.t() },
                address: { displayName: 'IPv4 Address'.t() },
                mask: { displayName: 'Mask'.t() },
                v6Addr: { displayName: 'IPv6'.t() },
                rxpkts: { displayName: 'Rx Packets'.t() },
                rxerr: { displayName: 'Rx Errors'.t() },
                rxdrop: { displayName: 'Rx Drop'.t() },
                txpkts: { displayName: 'Tx Packets'.t() },
                txerr: { displayName: 'Tx Errors'.t() },
                txdrop: { displayName: 'Tx Drop'.t() }
            },
            tbar: [{
                xtype: 'button',
                iconCls: 'fa fa-refresh',
                text: 'Refresh'
            }]
        }, {
            xtype: 'grid',
            itemId: 'interfaceArp',
            title: 'ARP Entry List'.t(),
            forceFit: true,
            bind: '{interfaceArp}',
            columns: [{
                header: 'MAC Address'.t(),
                dataIndex: 'macAddress'
            },{
                header: 'IP Address'.t(),
                dataIndex: 'address'
            },{
                header: 'Type'.t(),
                dataIndex: 'type'
            }]
        }, {
            title: 'Config'.t(),
            bodyPadding: 15,
            scrollable: 'vertical',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            defaults: {
                labelWidth: 200,
                labelAlign: 'right'
            },
            bbar: [{
                xtype: 'button',
                itemId: 'apply',
                text: 'Apply',
                iconCls: 'fa fa-floppy-o'
            }],
            items: [{
                // interface name
                xtype: 'textfield',
                fieldLabel: 'Interface Name'.t(),
                labelAlign: 'top',
                name: 'interfaceName',
                allowBlank: false,
                bind: '{si.name}'
            },
            // {
            //     // is VLAN
            //     xtype: 'checkbox',
            //     fieldLabel: 'Is VLAN (802.1q) Interface'.t(),
            //     // readOnly: true,
            //     bind: {
            //         value: '{si.isVlanInterface}',
            //         hidden: '{isDisabled}'
            //     }
            // }, {
            //     // is Wireless
            //     xtype: 'checkbox',
            //     fieldLabel: 'Is Wireless Interface'.t(),
            //     // readOnly: true,
            //     bind: {
            //         value: '{si.isWirelessInterface}',
            //         hidden: '{isDisabled}'
            //     }
            // },
            {
                // parent VLAN
                xtype: 'combo',
                allowBlank: false,
                editable: false,
                bind: {
                    value: '{si.vlanParent}',
                    hidden: '{!si.isVlanInterface || isDisabled}' // visible only if isVlanInterface
                },
                hidden: true,
                fieldLabel: 'Parent Interface'.t(),
                // store: Ung.Util.getInterfaceList(false, false),
                queryMode: 'local'
            }, {
                // VLAN Tag
                xtype: 'numberfield',
                bind: {
                    value: '{si.vlanTag}',
                    hidden: '{!si.isVlanInterface || isDisabled}' // visible only if isVlanInterface
                },
                hidden: true,
                fieldLabel: '802.1q Tag'.t(),
                minValue: 1,
                maxValue: 4096,
                allowBlank: false
            }, {
                // config type
                xtype: 'segmentedbutton',
                allowMultiple: false,
                bind: '{si.configType}',
                margin: '10 12',
                items: [{
                    text: 'Addressed'.t(),
                    value: 'ADDRESSED'
                }, {
                    text: 'Bridged'.t(),
                    value: 'BRIDGED'
                }, {
                    text: 'Disabled'.t(),
                    value: 'DISABLED'
                }]
            }, {
                // bridged to
                xtype: 'combo',
                allowBlank: false,
                editable: false,
                hidden: true,
                bind: {
                    value: '{si.bridgedTo}',
                    hidden: '{!isBridged}'
                },
                fieldLabel: 'Bridged To'.t(),
                // store: Ung.Util.getInterfaceAddressedList(),
                queryMode: 'local'
            }, {
                // is WAN
                xtype: 'checkbox',
                fieldLabel: 'Is WAN Interface'.t(),
                hidden: true,
                bind: {
                    value: '{si.isWan}',
                    hidden: '{!isAddressed}'
                }
            }, {
                // wireless conf
                xtype: 'fieldset',
                width: '100%',
                title: 'Wireless Configuration'.t(),
                collapsible: true,
                // hidden: true,
                defaults: {
                    labelWidth: 190,
                    labelAlign: 'right',
                    anchor: '100%'
                },
                hidden: true,
                bind: {
                    hidden: '{!showWireless || !isAddressed}'
                },
                items: [{
                    // SSID
                    xtype: 'textfield',
                    fieldLabel: 'SSID'.t(),
                    bind: '{si.wirelessSsid}',
                    allowBlank: false,
                    disableOnly: true,
                    maxLength: 30,
                    maskRe: /[a-zA-Z0-9\-_=]/
                }, {
                    // encryption
                    xtype: 'combo',
                    fieldLabel: 'Encryption'.t(),
                    bind: '{si.wirelessEncryption}',
                    editable: false,
                    store: [
                        ['NONE', 'None'.t()],
                        ['WPA1', 'WPA'.t()],
                        ['WPA12', 'WPA / WPA2'.t()],
                        ['WPA2', 'WPA2'.t()]
                    ],
                    queryMode: 'local'
                }, {
                    // password
                    xtype: 'textfield',
                    bind: {
                        value: '{si.wirelessPassword}',
                        hidden: '{!showWirelessPassword}'
                    },
                    fieldLabel: 'Password'.t(),
                    allowBlank: false,
                    disableOnly: true,
                    maxLength: 63,
                    minLength: 8,
                    maskRe: /[a-zA-Z0-9~@#%_=,\!\-\/\?\(\)\[\]\\\^\$\+\*\.\|]/
                }, {
                    // channel
                    xtype: 'combo',
                    bind: '{si.wirelessChannel}',
                    fieldLabel: 'Channel'.t(),
                    editable: false,
                    valueField: 'channel',
                    displayField: 'channelDescription',
                    queryMode: 'local'
                }]
            }, {
                // IPv4 conf
                xtype: 'fieldset',
                title: 'IPv4 Configuration'.t(),
                collapsible: true,
                defaults: {
                    labelWidth: 190,
                    labelAlign: 'right',
                    anchor: '100%'
                },
                hidden: true,
                bind: {
                    hidden: '{!isAddressed}'
                },
                items: [{
                    xtype: 'segmentedbutton',
                    allowMultiple: false,
                    bind: {
                        value: '{si.v4ConfigType}',
                        hidden: '{!si.isWan}'
                    },
                    margin: '10 0',
                    items: [{
                        text: 'Auto (DHCP)'.t(),
                        value: 'AUTO'
                    }, {
                        text: 'Static'.t(),
                        value: 'STATIC'
                    }, {
                        text: 'PPPoE'.t(),
                        value: 'PPPOE'
                    }]
                },
                // {
                //     // config type
                //     xtype: 'combo',
                //     bind: {
                //         value: '{si.v4ConfigType}',
                //         hidden: '{!si.isWan}'
                //     },
                //     fieldLabel: 'Config Type'.t(),
                //     allowBlank: false,
                //     editable: false,
                //     store: [
                //         ['AUTO', 'Auto (DHCP)'.t()],
                //         ['STATIC', 'Static'.t()],
                //         ['PPPOE', 'PPPoE'.t()]
                //     ],
                //     queryMode: 'local'
                // },
                {
                    // address
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4StaticAddress}',
                        hidden: '{!isStaticv4}'
                    },
                    fieldLabel: 'Address'.t(),
                    allowBlank: false
                }, {
                    // netmask
                    xtype: 'combo',
                    bind: {
                        value: '{si.v4StaticPrefix}',
                        hidden: '{!isStaticv4}'
                    },
                    fieldLabel: 'Netmask'.t(),
                    allowBlank: false,
                    editable: false,
                    store: Ung.Util.v4NetmaskList,
                    queryMode: 'local'
                }, {
                    // gateway
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4StaticGateway}',
                        hidden: '{!si.isWan || !isStaticv4}'
                    },
                    fieldLabel: 'Gateway'.t(),
                    allowBlank: false
                }, {
                    // primary DNS
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4StaticDns1}',
                        hidden: '{!si.isWan || !isStaticv4}'
                    },
                    fieldLabel: 'Primary DNS'.t(),
                    allowBlank: false
                }, {
                    // secondary DNS
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4StaticDns2}',
                        hidden: '{!si.isWan || !isStaticv4}'
                    },
                    fieldLabel: 'Secondary DNS'.t()
                }, {
                    // override address
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4AutoAddressOverride}',
                        emptyText: '{si.v4Address}',
                        hidden: '{!isAutov4}'
                    },
                    fieldLabel: 'Address Override'.t()
                }, {
                    // override netmask
                    xtype: 'combo',
                    bind: {
                        value: '{si.v4AutoPrefixOverride}',
                        hidden: '{!isAutov4}'
                    },
                    editable: false,
                    fieldLabel: 'Netmask Override'.t(),
                    store: Ung.Util.v4NetmaskList,
                    queryMode: 'local'
                }, {
                    // override gateway
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4AutoGatewayOverride}',
                        emptyText: '{si.v4Gateway}',
                        hidden: '{!isAutov4}'
                    },
                    fieldLabel: 'Gateway Override'.t()
                }, {
                    // override primary DNS
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4AutoDns1Override}',
                        emptyText: '{si.v4Dns1}',
                        hidden: '{!isAutov4}'
                    },
                    fieldLabel: 'Primary DNS Override'.t()
                }, {
                    // override secondary DNS
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4AutoDns2Override}',
                        emptyText: '{si.v4Dns2}',
                        hidden: '{!isAutov4}'
                    },
                    fieldLabel: 'Secondary DNS Override'.t()
                }, {
                    // renew DHCP lease,
                    xtype: 'button',
                    text: 'Renew DHCP Lease'.t(),
                    margin: '0 0 15 200',
                    bind: {
                        hidden: '{!isAutov4}'
                    }
                }, {
                    // PPPoE username
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4PPPoEUsername}',
                        hidden: '{!isPPPOEv4}'
                    },
                    fieldLabel: 'Username'.t()
                }, {
                    // PPPoE password
                    xtype: 'textfield',
                    inputType: 'password',
                    bind: {
                        value: '{si.v4PPPoEPassword}',
                        hidden: '{!isPPPOEv4}'
                    },
                    fieldLabel: 'Password'.t()
                }, {
                    // PPPoE peer DNS
                    xtype: 'checkbox',
                    bind: {
                        value: '{si.v4PPPoEUsePeerDns}',
                        hidden: '{!isPPPOEv4}'
                    },
                    fieldLabel: 'Use Peer DNS'.t()
                }, {
                    // PPPoE primary DNS
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4PPPoEDns1}',
                        hidden: '{!isPPPOEv4 || si.v4PPPoEUsePeerDns}'
                    },
                    fieldLabel: 'Primary DNS'.t()
                }, {
                    // PPPoE secondary DNS
                    xtype: 'textfield',
                    bind: {
                        value: '{si.v4PPPoEDns2}',
                        hidden: '{!isPPPOEv4 || si.v4PPPoEUsePeerDns}'
                    },
                    fieldLabel: 'Secondary DNS'.t()
                }, {
                    xtype: 'fieldset',
                    title: 'IPv4 Options'.t(),
                    items: [{
                        xtype:'checkbox',
                        bind: {
                            value: '{si.v4NatEgressTraffic}',
                            hidden: '{!si.isWan}'
                        },
                        boxLabel: 'NAT traffic exiting this interface (and bridged peers)'.t()
                    }, {
                        xtype:'checkbox',
                        bind: {
                            value: '{si.v4NatIngressTraffic}',
                            hidden: '{si.isWan}'
                        },
                        boxLabel: 'NAT traffic coming from this interface (and bridged peers)'.t()
                    }]
                }]
                // @todo: add aliases grid
            }, {
                // IPv6
                xtype: 'fieldset',
                title: 'IPv6 Configuration'.t(),
                collapsible: true,
                defaults: {
                    xtype: 'textfield',
                    labelWidth: 190,
                    labelAlign: 'right',
                    anchor: '100%'
                },
                hidden: true,
                bind: {
                    hidden: '{!isAddressed}',
                    collapsed: '{isDisabledv6}'
                },
                items: [{
                    // config type
                    xtype: 'segmentedbutton',
                    allowMultiple: false,
                    bind: {
                        value: '{si.v6ConfigType}',
                        hidden: '{!si.isWan}'
                    },
                    margin: '10 0',
                    items: [{
                        text: 'Disabled'.t(),
                        value: 'DISABLED'
                    }, {
                        text: 'Auto (SLAAC/RA)'.t(),
                        value: 'AUTO'
                    }, {
                        text: 'Static'.t(),
                        value: 'STATIC'
                    }]
                    // xtype: 'combo',
                    // bind: {
                    //     value: '{si.v6ConfigType}',
                    //     hidden: '{!si.isWan}'
                    // },
                    // fieldLabel: 'Config Type'.t(),
                    // editable: false,
                    // store: [
                    //     ['DISABLED', 'Disabled'.t()],
                    //     ['AUTO', 'Auto (SLAAC/RA)'.t()],
                    //     ['STATIC', 'Static'.t()]
                    // ],
                    // queryMode: 'local'
                }, {
                    // address
                    bind: {
                        value: '{si.v6StaticAddress}',
                        hidden: '{isDisabledv6 || isAutov6}'
                    },
                    fieldLabel: 'Address'.t()
                }, {
                    // prefix length
                    bind: {
                        value: '{si.v6StaticPrefixLength}',
                        hidden: '{isDisabledv6 || isAutov6}'
                    },
                    fieldLabel: 'Prefix Length'.t()
                }, {
                    // gateway
                    bind: {
                        value: '{si.v6StaticGateway}',
                        hidden: '{isDisabledv6 || isAutov6 || !si.isWan}'
                    },
                    fieldLabel: 'Gateway'.t()
                }, {
                    // primary DNS
                    bind: {
                        value: '{si.v6StaticDns1}',
                        hidden: '{isDisabledv6 || isAutov6 || !si.isWan}'
                    },
                    fieldLabel: 'Primary DNS'.t()
                }, {
                    // secondary DNS
                    bind: {
                        value: '{si.v6StaticDns2}',
                        hidden: '{isDisabledv6 || isAutov6 || !si.isWan}'
                    },
                    fieldLabel: 'Secondary DNS'.t()
                }, {
                    xtype: 'fieldset',
                    title: 'IPv6 Options'.t(),
                    bind: {
                        hidden: '{isDisabledv6 || isAutov6 || si.isWan}'
                    },
                    items: [{
                        xtype:'checkbox',
                        bind: {
                            value: '{si.raEnabled}'
                            // hidden: '{si.isWan}'
                        },
                        boxLabel: 'Send Router Advertisements'.t()
                    }, {
                        xtype: 'label',
                        style: {
                            fontSize: '10px'
                        },
                        html: '<span style="color: red">' + 'Warning:'.t() + '</span> ' + 'SLAAC only works with /64 subnets.'.t(),
                        bind: {
                            hidden: '{!showRouterWarning}'
                        }

                    }]
                }]
                // @todo: add aliases grid
            }, {
                xtype: 'fieldset',
                title: 'DHCP Configuration',
                collapsible: true,
                defaults: {
                    labelWidth: 190,
                    labelAlign: 'right',
                    anchor: '100%'
                },
                hidden: true,
                bind: {
                    hidden: '{!isAddressed || si.isWan}'
                },
                items: [{
                    // dhcp enabled
                    xtype: 'checkbox',
                    bind: '{si.dhcpEnabled}',
                    boxLabel: 'Enable DHCP Serving'.t()
                }, {
                    // dhcp range start
                    xtype: 'textfield',
                    bind: {
                        value: '{si.dhcpRangeStart}',
                        hidden: '{!si.dhcpEnabled}'
                    },
                    fieldLabel: 'Range Start'.t(),
                    allowBlank: false,
                    disableOnly: true
                }, {
                    // dhcp range end
                    xtype: 'textfield',
                    bind: {
                        value: '{si.dhcpRangeEnd}',
                        hidden: '{!si.dhcpEnabled}'
                    },
                    fieldLabel: 'Range End'.t(),
                    allowBlank: false,
                    disableOnly: true
                }, {
                    // lease duration
                    xtype: 'numberfield',
                    bind: {
                        value: '{si.dhcpLeaseDuration}',
                        hidden: '{!si.dhcpEnabled}'
                    },
                    fieldLabel: 'Lease Duration'.t() + ' ' + '(seconds)'.t(),
                    allowDecimals: false,
                    allowBlank: false,
                    disableOnly: true
                }, {
                    xtype: 'fieldset',
                    title: 'DHCP Advanced'.t(),
                    collapsible: true,
                    collapsed: true,
                    defaults: {
                        labelWidth: 180,
                        labelAlign: 'right',
                        anchor: '100%'
                    },
                    bind: {
                        hidden: '{!si.dhcpEnabled}'
                    },
                    items: [{
                        // gateway override
                        xtype: 'textfield',
                        bind: '{si.dhcpGatewayOverride}',
                        fieldLabel: 'Gateway Override'.t()
                    }, {
                        // netmask override
                        xtype: 'combo',
                        bind: '{si.dhcpPrefixOverride}',
                        fieldLabel: 'Netmask Override'.t(),
                        editable: false,
                        store: Ung.Util.v4NetmaskList,
                        queryMode: 'local'
                    }, {
                        // dns override
                        xtype: 'textfield',
                        bind: '{si.dhcpDnsOverride}',
                        fieldLabel: 'DNS Override'.t()
                    }]
                    // @todo: dhcp options editor
                }]
            }, {
                // VRRP
                xtype: 'fieldset',
                title: 'Redundancy (VRRP) Configuration'.t(),
                collapsible: true,
                defaults: {
                    labelWidth: 190,
                    labelAlign: 'right',
                    anchor: '100%'
                },
                hidden: true,
                bind: {
                    hidden: '{!isAddressed || !isStaticv4}'
                },
                items: [{
                    // VRRP enabled
                    xtype: 'checkbox',
                    bind: '{si.vrrpEnabled}',
                    boxLabel: 'Enable VRRP'.t()
                }, {
                    // VRRP ID
                    xtype: 'numberfield',
                    bind: {
                        value: '{si.vrrpId}',
                        hidden: '{!si.vrrpEnabled}'
                    },
                    fieldLabel: 'VRRP ID'.t(),
                    minValue: 1,
                    maxValue: 255,
                    allowBlank: false,
                    blankText: 'VRRP ID must be a valid integer between 1 and 255.'.t()
                }, {
                    // VRRP priority
                    xtype: 'numberfield',
                    bind: {
                        value: '{si.vrrpPriority}',
                        hidden: '{!si.vrrpEnabled}'
                    },
                    fieldLabel: 'VRRP Priority'.t(),
                    minValue: 1,
                    maxValue: 255,
                    allowBlank: false,
                    blankText: 'VRRP Priority must be a valid integer between 1 and 255.'.t()
                }]
                // @todo: vrrp aliases
            }]
        }]
    }]
});
Ext.define('Ung.config.network.Network', {
    extend: 'Ext.tab.Panel',
    xtype: 'ung.config.network',

    requires: [
        'Ung.config.network.Interfaces',

        'Ung.config.network.NetworkController',
        'Ung.config.network.NetworkModel'
    ],

    controller: 'config.network',

    viewModel: {
        type: 'config.network'
    },

    // tabPosition: 'left',
    // tabRotation: 0,
    // tabStretchMax: false,

    dockedItems: [{
        xtype: 'toolbar',
        weight: -10,
        border: false,
        items: [{
            text: 'Back',
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            href: '#config'
        }, '-', {
            xtype: 'component',
            html: 'Network'
        }]
    }],

    items: [{
        xtype: 'ung.config.network.interfaces'
    }, {
        title: 'Hostname'.t(),
        itemId: 'hostname',
        html: 'hostname'
    }, {
        title: 'Services'.t(),
        itemId: 'services',
        html: 'services'
    }, {
        title: 'Rules'.t(),
        html: 'rules'
    }, {
        title: 'Routes'.t(),
        html: 'routes'
    }, {
        title: 'DNS Server'.t(),
        html: 'dns'
    }, {
        title: 'DHCP Server'.t(),
        html: 'dhcp'
    }, {
        title: 'Advanced'.t(),
        html: 'adv'
    }, {
        title: 'Troubleshooting'.t(),
        html: 'trb'
    }, {
        title: 'Reports'.t(),
        html: 'reports'
    }]
});
Ext.define('Ung.config.network.NetworkController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.network',

    control: {
        '#': {
            beforerender: 'loadSettings'
        },
        '#interfaces': {
            beforeactivate: 'onInterfaces',

        },
        '#interfacesGrid': {
            // select: 'onInterfaceSelect'
        },
        '#interfaceStatus': {
            // activate: 'getInterfaceStatus',
            beforeedit: function () { return false; }
        },
        '#interfaceArp': {
        },
        '#apply': {
            click: 'saveSettings'
        }
    },

    saveSettings: function () {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;
        view.setLoading('Saving ...');
        rpc.networkManager.setNetworkSettings(function (result, ex) {
            console.log(ex);
            console.log(result);
            // vm.getStore('interfaces').reload();
            view.setLoading(false);
            me.loadInterfaceStatusAndDevices();
        }, vm.get('settings'));
    },

    loadSettings: function () {
        var me = this;
        rpc.networkManager.getNetworkSettings(function (result, ex) {
            me.getViewModel().set('settings', result);
            me.loadInterfaceStatusAndDevices();
            // interfaces = result.interfaces.list;
        });
    },

    loadInterfaceStatusAndDevices: function () {
        var me = this;
        var vm = this.getViewModel(),
            interfaces = vm.get('settings.interfaces.list'),
            i, j, intfStatus, devStatus;
        // load status
        // vm.set('settings.interfaces.list[0].mbit', 2000);
        // vm.notify();
        rpc.networkManager.getInterfaceStatus(function (result, ex) {
            for (i = 0; i < interfaces.length; i += 1) {
                Ext.apply(interfaces[i], {
                    'v4Address': null,
                    'v4Netmask': null,
                    'v4Gateway': null,
                    'v4Dns1': null,
                    'v4Dns2': null,
                    'v4PrefixLength': null,
                    'v6Address': null,
                    'v6Gateway': null,
                    'v6PrefixLength': null
                });

                for (j = 0; j < result.list.length; j += 1) {
                    intfStatus = result.list[j];
                    if (intfStatus.interfaceId === vm.get('settings.interfaces.list')[i].interfaceId) {
                        Ext.apply(interfaces[i], {
                            'v4Address': intfStatus.v4Address,
                            'v4Netmask': intfStatus.v4Netmask,
                            'v4Gateway': intfStatus.v4Gateway,
                            'v4Dns1': intfStatus.v4Dns1,
                            'v4Dns2': intfStatus.v4Dns2,
                            'v4PrefixLength': intfStatus.v4PrefixLength,
                            'v6Address': intfStatus.v6Address,
                            'v6Gateway': intfStatus.v6Gateway,
                            'v6PrefixLength': intfStatus.v6PrefixLength
                        });
                    }
                }
            }
            vm.getStore('interfaces').reload();
        });

        rpc.networkManager.getDeviceStatus(function (result, ex) {
            for (i = 0; i < interfaces.length; i += 1) {
                Ext.apply(interfaces[i], {
                    'deviceName': null,
                    'macAddress': null,
                    'duplex': null,
                    'vendor': null,
                    'mbit': null,
                    'connected': null
                });

                for (j = 0; j < result.list.length; j += 1) {
                    devStatus = result.list[j];
                    if (devStatus.deviceName === interfaces[i].physicalDev) {
                        Ext.apply(interfaces[i], {
                            'deviceName': devStatus.deviceName,
                            'macAddress': devStatus.macAddress,
                            'duplex': devStatus.duplex,
                            'vendor': devStatus.vendor,
                            'mbit': devStatus.mbit,
                            'connected': devStatus.connected
                        });
                    }
                }
            }
            vm.getStore('interfaces').reload();
        });

    },

    onInterfaces: function (view) {
        console.log('load interf');
        var me = this;
        var vm = this.getViewModel();

        vm.setFormulas({
            si: {
                bind: {
                    bindTo: '{interfacesGrid.selection}',
                    deep: true
                },
                get: function (intf) {
                    if (intf) {
                        me.getInterfaceStatus(intf.get('symbolicDev'));
                        me.getInterfaceArp(intf.get('symbolicDev'));
                    }
                    return intf;
                }
            }
        });

        // vm.bind({
        //     bindTo: '{interfacesGrid.selection}',
        //     deep: true
        // }, function (v) {
        //     vm.set('si', v);
        //     // return v;
        // }, this);

        // vm.bind('{si}', function (val) {
        //     if (val) {
        //         me.getInterfaceStatus(val.symbolicDev);
        //         me.getInterfaceArp(val.symbolicDev);
        //     }
        // });

        // vm.bind('{settings.interfaces}', function (v) {
        //     // console.log(v);
        // });

    },

    getInterface: function (i) {
        return i;
    },

    // onInterfaceSelect: function (grid, record) {
    //     this.getViewModel().set('si', record.getData());
    // },

    getInterfaceStatus: function (symbolicDev) {
        var vm = this.getViewModel(),
            command1 = 'ifconfig ' + symbolicDev + ' | grep "Link\\|packets" | grep -v inet6 | tr "\\n" " " | tr -s " " ',
            command2 = 'ifconfig ' + symbolicDev + ' | grep "inet addr" | tr -s " " | cut -c 7- ',
            command3 = 'ifconfig ' + symbolicDev + ' | grep inet6 | grep Global | cut -d" " -f 13',
            stat = {
                device: symbolicDev,
                macAddress: null,
                address: null,
                mask: null,
                v6Addr: null,
                rxpkts: null,
                rxerr: null,
                rxdrop: null,
                txpkts: null,
                txerr: null,
                txdrop: null
            };

        // vm.set('siStatus', stat);

        rpc.execManager.execOutput(function (result, ex) {
            if(Ext.isEmpty(result)) {
                return;
            }
            if (result.search('Device not found') >= 0) {
                return;
            }
            var lineparts = result.split(' ');
            if (result.search('Ethernet') >= 0) {
                Ext.apply(stat, {
                    macAddress: lineparts[4],
                    rxpkts: lineparts[6].split(':')[1],
                    rxerr: lineparts[7].split(':')[1],
                    rxdrop: lineparts[8].split(':')[1],
                    txpkts: lineparts[12].split(':')[1],
                    txerr: lineparts[13].split(':')[1],
                    txdrop: lineparts[14].split(':')[1]
                });
            }
            if (result.search('Point-to-Point') >= 0) {
                Ext.apply(stat, {
                    macAddress: '',
                    rxpkts: lineparts[5].split(':')[1],
                    rxerr: lineparts[6].split(':')[1],
                    rxdrop: lineparts[7].split(':')[1],
                    txpkts: lineparts[11].split(':')[1],
                    txerr: lineparts[12].split(':')[1],
                    txdrop: lineparts[13].split(':')[1]
                });
            }

            rpc.execManager.execOutput(function (result, ex) {
                if(Ext.isEmpty(result)) {
                    return;
                }
                var linep = result.split(' ');
                Ext.apply(stat, {
                    address: linep[0].split(':')[1],
                    mask: linep[2].split(':')[1]
                });

                rpc.execManager.execOutput(function (result, ex) {
                    Ext.apply(stat, {
                        v6Addr: result
                    });
                    vm.set('siStatus', stat);
                }, command3);
            }, command2);
        }, command1);
    },

    getInterfaceArp: function (symbolicDev) {
        var vm = this.getViewModel();
        var arpCommand = 'arp -n | grep ' + symbolicDev + ' | grep -v incomplete > /tmp/arp.txt ; cat /tmp/arp.txt';
        rpc.execManager.execOutput(function (result, ex) {
            var lines = Ext.isEmpty(result) ? []: result.split('\n');
            var lparts, connections = [];
            for (var i = 0 ; i < lines.length; i++ ) {
                if (!Ext.isEmpty(lines[i])) {
                    lparts = lines[i].split(/\s+/);
                    connections.push({
                        address: lparts[0],
                        type: lparts[1],
                        macAddress: lparts[2]
                    });
                }
            }
            vm.set('siArp', connections);
            // vm.getStore('interfaceArp').reload();
        }, arpCommand);
    }
});
Ext.define('Ung.config.network.NetworkModel', {
    extend: 'Ext.app.ViewModel',

    alias: 'viewmodel.config.network',

    formulas: {
        // used in view when showing/hiding interface specific configurations
        isAddressed: function (get) { return get('si.configType') === 'ADDRESSED'; },
        isDisabled: function (get) { return get('si.configType') === 'DISABLED'; },
        isBridged: function (get) { return get('si.configType') === 'BRIDGED'; },
        isStaticv4: function (get) { return get('si.v4ConfigType') === 'STATIC'; },
        isAutov4: function (get) { return get('si.v4ConfigType') === 'AUTO'; },
        isPPPOEv4: function (get) { return get('si.v4ConfigType') === 'PPPOE'; },
        isDisabledv6: function (get) { return get('si.v6ConfigType') === 'DISABLED'; },
        isStaticv6: function (get) { return get('si.v6ConfigType') === 'STATIC'; },
        isAutov6: function (get) { return get('si.v6ConfigType') === 'AUTO'; },
        showRouterWarning: function (get) { return get('si.v6StaticPrefixLength') !== 64; },
        showWireless: function (get) { return get('si.isWirelessInterface') && get('si.configType') !== 'DISABLED'; },
        showWirelessPassword: function (get) { return get('si.wirelessEncryption') !== 'NONE' && get('si.wirelessEncryption') !== null; },
        activePropsItem: function (get) { return get('si.configType') !== 'DISABLED' ? 0 : 2; }
    },
    data: {
        // si = selected interface (from grid)
        settings: null,
        // si: null,
        siStatus: null,
        siArp: null
    },
    stores: {
        // store which holds interfaces settings
        interfaces: {
            data: '{settings.interfaces.list}'
        },
        interfaceArp: {
            data: '{siArp}'
        }
    }
});