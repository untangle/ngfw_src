Ext.define('Ung.config.network.Interface', {
    extend: 'Ext.window.Window',
    alias: 'widget.config.interface',
    width: Math.min(Renderer.calculateWith(1), 900),
    height: 600,
    onEsc: Ext.emptyFn,
    closable: false,
    layout: 'border',
    modal: true,
    withValidation: true,

    items: [{
        region: 'north',
        height: 'auto',
        // xtype: 'form',
        border: false,
        // scrollable: 'vertical',
        // bodyPadding: 10,
        // layout: {
        //     type: 'vbox',
        //     align: 'stretch'
        // },
        items: [{
            xtype: 'container',
            padding: 10,
            defaults: {
                // width: 500,
                labelWidth: 190,
                labelAlign: 'right'
            },
            items: [{
                // interface name
                xtype: 'textfield',
                fieldLabel: 'Interface Name'.t(),
                width: 400,
                name: 'interfaceName',
                allowBlank: false,
                bind: '{intf.name}'
            }, {
                xtype: 'container',
                layout: { type: 'hbox' },
                margin: '0 0 5 0',
                hidden: true,
                bind: {
                    hidden: '{!intf.isVlanInterface}'
                },
                items: [{
                    // parent VLAN
                    xtype: 'combo',
                    fieldLabel: 'Parent Interface'.t(),
                    emptyText: 'Select parent Interface ...',
                    labelAlign: 'right',
                    labelWidth: 190,
                    width: 400,
                    allowBlank: false,
                    editable: false,
                    bind: {
                        value: '{intf.vlanParent}',
                    },
                    queryMode: 'local',
                    listeners: {
                        afterrender: 'onParentInterface'
                    }
                }, {
                    // VLAN Tag
                    xtype: 'numberfield',
                    fieldLabel: '802.1q Tag'.t(),
                    labelAlign: 'right',
                    width: 180,
                    bind: {
                        value: '{intf.vlanTag}',
                    },
                    minValue: 1,
                    maxValue: 4096,
                    allowBlank: false
                }]
            }, {
                // config type
                xtype: 'radiogroup',
                itemId: 'configType',
                fieldLabel: 'Config Type'.t(),
                layout: { type: 'hbox' },
                defaults: { padding: '0 10 0 0' },
                simpleValue: true,
                bind: '{intf.configType}',
                items: [],
                listeners: {
                    afterrender: function (rg) {
                        rg.add(rg.up('window').configTypesRadios);
                    },
                    change: function (rg, newValue) {
                        var win = rg.up('window');
                        if (newValue === 'ADDRESSED') {
                            win.down('tabpanel').setActiveItem(0);
                        }
                        if (newValue === 'BRIDGED' && win.getViewModel().get('intf.isWirelessInterface')) { // in case of Wireless interface
                            win.down('tabpanel').setActiveItem(2);
                        }
                    }
                }
            }, {
                // is WAN
                xtype: 'checkbox',
                itemId: 'isWanCk',
                fieldLabel: 'Is WAN Interface'.t(),
                hidden: true,
                bind: {
                    value: '{intf.isWan}',
                    hidden: '{!isAddressed}'
                }
            }, {
                xtype: 'combo',
                allowBlank: false,
                editable: false,
                hidden: true,
                bind: {
                    value: '{intf.bridgedTo}',
                    hidden: '{!isBridged}'
                },
                fieldLabel: 'Bridged To'.t(),
                width: 400,
                valueField: 'id',
                displayField: 'name',
                queryMode: 'local',
                listeners:{
                    afterrender: 'onBridgedInteface'
                }
            }]
        },
        // {
        //     // is VLAN
        //     xtype: 'checkbox',
        //     fieldLabel: 'Is VLAN (802.1q) Interface'.t(),
        //     // readOnly: true,
        //     bind: {
        //         value: '{intf.isVlanInterface}',
        //         hidden: '{isDisabled}'
        //     }
        // }, {
        //     // is Wireless
        //     xtype: 'checkbox',
        //     fieldLabel: 'Is Wireless Interface'.t(),
        //     // readOnly: true,
        //     bind: {
        //         value: '{intf.isWirelessInterface}',
        //         hidden: '{isDisabled}'
        //     }
        // },
        ],
    }, {
        xtype: 'tabpanel',
        region: 'center',
        plain: true,
        border: false,
        bodyBorder: false,
        hidden: true,
        bind: {
            hidden: '{isDisabled || (isBridged && !intf.isWirelessInterface)}'
        },
        items: [{
            bind: {
                title: 'IPv4 Configuration'.t() // + ' ({intf.v4ConfigType})'
            },
            tabConfig: {
                hidden: true,
                bind: {
                    hidden: '{!isAddressed}'
                }
            },
            tbar: [{
                xtype: 'radiogroup',
                itemId: 'ipv4ConfigType',
                layout: { type: 'hbox' },
                defaults: { padding: '0 10' },
                simpleValue: true,
                // hidden: true,
                bind: {
                    value: '{intf.v4ConfigType}',
                    // hidden: '{!intf.isWan}'
                },
                items: [
                    { boxLabel: 'Auto (DHCP)'.t(), inputValue: 'AUTO', bind: { disabled: '{!intf.isWan}'} },
                    { boxLabel: 'Static'.t(),      inputValue: 'STATIC' },
                    { boxLabel: 'PPPoE'.t(),       inputValue: 'PPPOE', bind: { disabled: '{!intf.isWan}'} }
                ]
            }, '->', {
                xtype: 'button',
                text: 'Renew DHCP Lease'.t(),
                iconCls: 'fa fa-refresh',
                hidden: true,
                bind: { hidden: '{!isAutov4}' },
                handler: 'onRenewDhcpLease'
            }],
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                // IPV4 overrides (DHCP)
                xtype: 'container',
                flex: 1,
                padding: 10,
                hidden: true,
                bind: { hidden: '{!isAutov4}' },
                scrollable: 'y',
                items: [{
                    // ipv4 address override
                    xtype: 'fieldcontainer',
                    layout: 'column',
                    width: '100%',
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: 'Address Override'.t(),
                        labelWidth: 190,
                        width: 400,
                        labelAlign: 'right',
                        bind: {
                            value: '{intf.v4AutoAddressOverride}',
                            emptyText: '{intf.v4Address}'
                        },
                        vtype: 'ip4Address',
                    }, {
                        xtype: 'displayfield',
                        margin: '0 5',
                        fieldStyle: {
                            color: '#777',
                            fontSize: 'smaller',
                            minHeight: 'auto'
                        },
                        hidden: true,
                        bind: {
                            value: 'Current:'.t() + ' <strong>{intf.v4Address}</strong>',
                            hidden: '{!intf.v4Address}'
                        }
                    }]
                }, {
                    // ipv4 address override
                    xtype: 'fieldcontainer',
                    layout: 'column',
                    width: '100%',
                    items: [{
                        xtype: 'combo',
                        labelWidth: 190,
                        width: 400,
                        labelAlign: 'right',
                        bind: {
                            value: '{intf.v4AutoPrefixOverride}',
                            emptyText: '/{intf.v4PrefixLength} - {intf.v4Netmask}',
                        },
                        editable: false,
                        fieldLabel: 'Netmask Override'.t(),
                        store: Util.getV4NetmaskList(true),
                        queryMode: 'local'
                    }, {
                        xtype: 'displayfield',
                        margin: '0 5',
                        fieldStyle: {
                            color: '#777',
                            fontSize: 'smaller',
                            minHeight: 'auto'
                        },
                        hidden: true,
                        bind: {
                            value: 'Current:'.t() + ' <strong>/{intf.v4PrefixLength} - {intf.v4Netmask}</strong>',
                            hidden: '{!intf.v4Netmask}'
                        }
                    }]
                }, {
                    // gateway override
                    xtype: 'fieldcontainer',
                    layout: 'column',
                    width: '100%',
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: 'Gateway Override'.t(),
                        labelWidth: 190,
                        width: 400,
                        labelAlign: 'right',
                        bind: {
                            value: '{intf.v4AutoGatewayOverride}',
                            emptyText: '{intf.v4Gateway}'
                        },
                        vtype: 'ip4Address',
                    }, {
                        xtype: 'displayfield',
                        margin: '0 5',
                        fieldStyle: {
                            color: '#777',
                            fontSize: 'smaller',
                            minHeight: 'auto'
                        },
                        hidden: true,
                        bind: {
                            value: 'Current:'.t() + ' <strong>{intf.v4Gateway}</strong>',
                            hidden: '{!intf.v4Gateway}'
                        }
                    }]
                }, {
                    // primary DNS override
                    xtype: 'fieldcontainer',
                    layout: 'column',
                    width: '100%',
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: 'Primary DNS Override'.t(),
                        labelWidth: 190,
                        width: 400,
                        labelAlign: 'right',
                        bind: {
                            value: '{intf.v4AutoDns1Override}',
                            emptyText: '{intf.v4Dns1}'
                        },
                        vtype: 'ip4Address',
                    }, {
                        xtype: 'displayfield',
                        margin: '0 5',
                        fieldStyle: {
                            color: '#777',
                            fontSize: 'smaller',
                            minHeight: 'auto'
                        },
                        hidden: true,
                        bind: {
                            value: 'Current:'.t() + ' <strong>{intf.v4Dns1}</strong>',
                            hidden: '{!intf.v4Dns1}'
                        }
                    }]
                }, {
                    // secondary DNS override
                    xtype: 'fieldcontainer',
                    layout: 'column',
                    width: '100%',
                    items: [{
                        xtype: 'textfield',
                        fieldLabel: 'Secondary DNS Override'.t(),
                        labelWidth: 190,
                        width: 400,
                        labelAlign: 'right',
                        bind: {
                            value: '{intf.v4AutoDns2Override}',
                            emptyText: '{intf.v4Dns2}'
                        },
                        vtype: 'ip4Address',
                    }, {
                        xtype: 'displayfield',
                        margin: '0 5',
                        fieldStyle: {
                            color: '#777',
                            fontSize: 'smaller',
                            minHeight: 'auto'
                        },
                        hidden: true,
                        bind: {
                            value: 'Current:'.t() + ' <strong>{intf.v4Dns2}</strong>',
                            hidden: '{!intf.v4Dns2}'
                        }
                    }]
                }]
            }, {
                // IPV4 Static
                xtype: 'container',
                flex: 1,
                hidden: true,
                bind: { hidden: '{!isStaticv4}' },
                padding: 10,
                scrollable: 'y',
                layout: {
                    type: 'vbox'
                },
                defaults: {
                    labelWidth: 190,
                    width: 400,
                    labelAlign: 'right',
                    // anchor: '100%'
                },
                items: [{
                    // static address
                    xtype: 'textfield',
                    width: 400,
                    bind: {
                        value: '{intf.v4StaticAddress}',
                    },
                    fieldLabel: 'Address'.t(),
                    allowBlank: false,
                    blankText: 'Address must be specified.'.t(),
                    vtype: 'ip4Address'
                }, {
                    // static netmask
                    xtype: 'combo',
                    bind: {
                        value: '{intf.v4StaticPrefix}',
                    },
                    fieldLabel: 'Netmask'.t(),
                    allowBlank: false,
                    editable: false,
                    store: Util.getV4NetmaskList(false),
                    queryMode: 'local'
                }, {
                    // static gateway
                    xtype: 'textfield',
                    hidden: true,
                    bind: {
                        value: '{intf.v4StaticGateway}',
                        hidden: '{!intf.isWan}'
                    },
                    fieldLabel: 'Gateway'.t(),
                    allowBlank: false,
                    vtype: 'ip4Address'
                }, {
                    // static primary DNS
                    xtype: 'textfield',
                    hidden: true,
                    bind: {
                        value: '{intf.v4StaticDns1}',
                        hidden: '{!intf.isWan}'
                    },
                    fieldLabel: 'Primary DNS'.t(),
                    allowBlank: false,
                    vtype: 'ip4Address'
                }, {
                    // static secondary DNS
                    xtype: 'textfield',
                    hidden: true,
                    bind: {
                        value: '{intf.v4StaticDns2}',
                        hidden: '{!intf.isWan}'
                    },
                    fieldLabel: 'Secondary DNS'.t(),
                    vtype: 'ip4Address'
                }]
            }, {
                // PPPoE settings
                xtype: 'container',
                flex: 1,
                hidden: true,
                bind: { hidden: '{!isPPPOEv4}' },
                padding: 10,
                layout: {
                    type: 'vbox'
                },
                scrollable: 'y',
                defaults: {
                    labelWidth: 190,
                    width: 400,
                    labelAlign: 'right'
                },
                items: [{
                    // PPPoE username
                    xtype: 'textfield',
                    bind: {
                        value: '{intf.v4PPPoEUsername}',
                    },
                    fieldLabel: 'Username'.t()
                }, {
                    // PPPoE password
                    xtype: 'textfield',
                    inputType: 'password',
                    bind: {
                        value: '{intf.v4PPPoEPassword}',
                    },
                    fieldLabel: 'Password'.t()
                }, {
                    // PPPoE peer DNS
                    xtype: 'checkbox',
                    bind: {
                        value: '{intf.v4PPPoEUsePeerDns}',
                    },
                    fieldLabel: 'Use Peer DNS'.t()
                }, {
                    // PPPoE primary DNS
                    xtype: 'textfield',
                    disabled: true,
                    bind: {
                        value: '{intf.v4PPPoEDns1}',
                        disabled: '{intf.v4PPPoEUsePeerDns}'
                    },
                    fieldLabel: 'Primary DNS'.t(),
                    vtype: 'ip4Address'
                }, {
                    // PPPoE secondary DNS
                    xtype: 'textfield',
                    disabled: true,
                    bind: {
                        value: '{intf.v4PPPoEDns2}',
                        disabled: '{intf.v4PPPoEUsePeerDns}'
                    },
                    fieldLabel: 'Secondary DNS'.t(),
                    vtype: 'ip4Address'
                }]
            }, {
                xtype: 'fieldset',
                title: 'IPv4 Options'.t(),
                margin: 10,
                padding: '5 10',
                items: [{
                    xtype: 'checkbox',
                    hidden: true,
                    itemId: 'v4NatEgressTraffic',
                    bind: {
                        value: '{intf.v4NatEgressTraffic}',
                        hidden: '{!intf.isWan}'
                    },
                    fieldLabel: '',
                    boxLabel: 'NAT traffic exiting this interface (and bridged peers)'.t()
                }, {
                    xtype: 'checkbox',
                    hidden: true,
                    bind: {
                        value: '{intf.v4NatIngressTraffic}',
                        hidden: '{intf.isWan}'
                    },
                    fieldLabel: '',
                    boxLabel: 'NAT traffic coming from this interface (and bridged peers)'.t()
                }]
            }, {
                // ipv4 aliases
                xtype: 'ungrid',
                title: 'IPv4 Aliases'.t(),
                emptyText: 'No IPv4 Aliases defined'.t(),
                border: false,
                collapsible: true,
                titleCollapse: true,
                animCollapse: false,
                tbar: ['@addInline'],
                recordActions: ['delete'],
                bind: '{v4Aliases}',
                listProperty: 'v4Aliases',
                maxHeight: 140,
                emptyRow: {
                    staticAddress: '1.2.3.4',
                    staticPrefix: '24',
                    javaClass: 'com.untangle.uvm.network.InterfaceSettings$InterfaceAlias'
                },
                columns: [{
                    header: 'Address'.t(),
                    dataIndex: 'staticAddress',
                    width: Renderer.ipWidth,
                    editor : {
                        xtype: 'textfield',
                        vtype: 'ip4Address',
                        emptyText: '[enter IPv4 address]'.t(),
                        allowBlank: false
                    }
                }, {
                    header: 'Netmask / Prefix'.t(),
                    dataIndex: 'staticPrefix',
                    width: Renderer.networkWidth,
                    flex: 1,
                    editor : {
                        xtype: 'numberfield',
                        minValue: 1,
                        maxValue: 32,
                        allowDecimals: false,
                        allowBlank: false
                    }
                }],
            }]
        }, {
            bind: {
                title: 'IPv6 Configuration'.t() // + ' ({intf.v6ConfigType})',
            },
            tabConfig: {
                hidden: true,
                bind: {
                    hidden: '{!isAddressed}'
                }
            },
            tbar: [{
                xtype: 'radiogroup',
                itemId: 'ipv6ConfigType',
                layout: { type: 'hbox' },
                defaults: { padding: '0 10' },
                simpleValue: true,
                bind: {
                    value: '{intf.v6ConfigType}',
                },
                items: [
                    { boxLabel: 'Auto (SLAAC/RA)'.t(), inputValue: 'AUTO', bind: { disabled: '{!intf.isWan}'} },
                    { boxLabel: 'Static'.t(),          inputValue: 'STATIC' },
                    { boxLabel: 'Disabled'.t(),        inputValue: 'DISABLED', bind: { disabled: '{!intf.isWan}'} }
                ]
            }],
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                // IPv6 static
                xtype: 'container',
                flex: 1,
                padding: 10,
                disabled: true,
                bind: { disabled: '{!isStaticv6}' },
                defaults: {
                    labelWidth: 190,
                    width: 400,
                    labelAlign: 'right',
                    // anchor: '100%'
                },
                items: [{
                    xtype: 'textfield',
                    fieldLabel: 'Address'.t(),
                    bind: {
                        value: '{intf.v6StaticAddress}',
                    },
                    vtype: 'ip6Address'
                }, {
                    xtype: 'numberfield',
                    fieldLabel: 'Prefix Length'.t(),
                    minValue: 1,
                    maxValue: 128,
                    allowDecimals: false,
                    allowBlank: false,
                    bind: {
                        value: '{intf.v6StaticPrefixLength}',
                    },
                    width: 260
                }, {
                    xtype: 'textfield',
                    fieldLabel: 'Gateway'.t(),
                    hidden: true,
                    bind: {
                        value: '{intf.v6StaticGateway}',
                        hidden: '{!intf.isWan}'
                    },
                    vtype: 'ip6Address'
                }, {
                    xtype:'textfield',
                    fieldLabel: 'Primary DNS'.t(),
                    hidden: true,
                    bind: {
                        value: '{intf.v6StaticDns1}',
                        hidden: '{!intf.isWan}'
                    },
                    vtype: 'ip6Address'
                }, {
                    xtype:'textfield',
                    fieldLabel: 'Secondary DNS'.t(),
                    hidden: true,
                    bind: {
                        value: '{intf.v6StaticDns2}',
                        hidden: '{!intf.isWan}'
                    },
                    vtype: 'ip6Address'
                }]

            }, {
                xtype: 'fieldset',
                title: 'IPv6 Options'.t(),
                margin: 10,
                padding: '5 10 10 10',
                hidden: true,
                bind: {
                    hidden: '{isDisabledv6 || isAutov6 || intf.isWan}'
                },
                layout: {
                    type: 'hbox',
                    align: 'middle'
                },
                items: [{
                    xtype:'checkbox',
                    bind: {
                        value: '{intf.raEnabled}'
                        // hidden: '{intf.isWan}'
                    },
                    boxLabel: 'Send Router Advertisements'.t()
                }, { xtype: 'component', flex: 1 }, {
                    xtype: 'label',
                    style: {
                        color: '#777'
                    },
                    html: '<i class="fa fa-exclamation-triangle fa-orange"></i> ' + 'Warning:'.t() + ' ' +  'SLAAC only works with /64 subnets.'.t(),
                    hidden: true,
                    bind: {
                        hidden: '{!showRouterWarning}'
                    }

                }]
            }, {
                // ipv6 aliases
                title: 'IPv6 Aliases'.t(),
                emptyText: 'No IPv6 Aliases defined'.t(),
                xtype: 'ungrid',
                border: false,
                collapsible: true,
                titleCollapse: true,
                animCollapse: false,
                tbar: ['@addInline'],
                recordActions: ['delete'],
                disabled: true,
                maxHeight: 140,
                bind: {
                    store: '{v6Aliases}',
                    disabled: '{intf.v6ConfigType === "DISABLED"}'
                },
                listProperty: 'v6Aliases',
                emptyRow: {
                    staticAddress: '::1',
                    staticPrefix: '64',
                    javaClass: 'com.untangle.uvm.network.InterfaceSettings$InterfaceAlias'
                },
                columns: [{
                    header: 'Address'.t(),
                    dataIndex: 'staticAddress',
                    width: Renderer.ipWidth,
                    editor : {
                        xtype: 'textfield',
                        vtype: 'ip6Address',
                        emptyText: '[enter IPv6 address]'.t(),
                        allowBlank: false
                    }
                }, {
                    header: 'Netmask / Prefix'.t(),
                    dataIndex: 'staticPrefix',
                    width: Renderer.networkWidth,
                    flex: 1,
                    editor : {
                        xtype: 'numberfield',
                        minValue: 1,
                        maxValue: 128,
                        allowDecimals: false,
                        allowBlank: false
                    }
                }],
            }]
        }, {
            title: 'Wireless Configuration'.t(),
            tabConfig: {
                hidden: true,
                bind: {
                    hidden: '{!intf.isWirelessInterface}'
                }
            },
            bodyPadding: 10,
            scrollable: 'y',
            layout: {
                type: 'vbox'
            },
            defaults: {
                labelWidth: 190,
                width: 400,
                labelAlign: 'right'
            },
            items: [{
                // SSID
                xtype: 'textfield',
                fieldLabel: 'SSID'.t(),
                bind: '{intf.wirelessSsid}',
                allowBlank: false,
                maxLength: 30,
                maskRe: /[a-zA-Z0-9\-_=. ]/
                //maskRe: /[a-zA-Z0-9~@%_=,<>\!\-\/\?\[\]\\\^\$\+\*\.\|]/
            }, {
                // mode
                xtype: 'combo',
                fieldLabel: 'Mode'.t(),
                value: 'AP',
                bind: '{intf.wirelessMode}',
                editable: false,
                store: [
                    ['AP', 'AP'.t()],
                    ['CLIENT', 'Client'.t()],
                ],
                queryMode: 'local'
            }, {
                // visibility
                xtype: 'combo',
                fieldLabel: 'Visibility'.t(),
                value: '0',
                bind: {
                 value: '{intf.wirelessVisibility}',
                 hidden: '{intf.wirelessMode === "CLIENT"}'
                },
                editable: false,
                store: [
                    ['0', 'Advertise this SSID publicly'.t()],
                    ['1', 'Hide this SSID'.t()]
                ],
                queryMode: 'local'
            }, {
                // encryption
                xtype: 'combo',
                fieldLabel: 'Encryption'.t(),
                value: 'NONE',
                bind: '{intf.wirelessEncryption}',
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
                disabled: true,
                bind: {
                    value: '{intf.wirelessPassword}',
                    disabled: '{!(intf.wirelessEncryption !== "NONE" && intf.wirelessEncryption !== null)}'
                },
                fieldLabel: 'Password'.t(),
                allowBlank: false,
                maxLength: 63,
                minLength: 8,
                maskRe: /[a-zA-Z0-9~@#%_=,\!\-\/\?\(\)\[\]\\\^\$\+\*\.\|]/
            }, {
                // channel
                xtype: 'combo',
                bind: {
                    value: '{intf.wirelessChannel}',
                    store: '{wirelessChannelsList}',
                    hidden: '{intf.wirelessMode === "CLIENT"}'
                },
                fieldLabel: 'Channel'.t(),
                editable: false,
                queryMode: 'local'
            }]
        }, {
            title: 'DHCP Configuration'.t(),
            tabConfig: {
                hidden: true,
                bind: {
                    hidden: '{intf.isWan || !isAddressed}'
                },
            },
            tbar: [{
                // dhcp enabled
                xtype: 'checkbox',
                itemId: 'dhcpEnabled',
                margin: 2,
                bind: '{intf.dhcpEnabled}',
                boxLabel: '<strong>' + 'Enable DHCP Serving'.t() + '</strong>'
            }],
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                xtype: 'container',
                flex: 1,
                padding: 10,
                scrollable: 'y',
                layout: {
                    type: 'vbox'
                },
                disabled: true,
                bind: {
                    disabled: '{!intf.dhcpEnabled}'
                },
                defaults: {
                    labelWidth: 190,
                    width: 400,
                    labelAlign: 'right',
                    // anchor: '100%'
                },
                items: [{
                    // dhcp range start
                    xtype: 'textfield',
                    bind: {
                        value: '{intf.dhcpRangeStart}'
                    },
                    fieldLabel: 'Range Start'.t(),
                    allowBlank: false
                }, {
                    // dhcp range end
                    xtype: 'textfield',
                    bind: {
                        value: '{intf.dhcpRangeEnd}'
                    },
                    fieldLabel: 'Range End'.t(),
                    allowBlank: false
                }, {
                    // lease duration
                    xtype: 'fieldcontainer',
                    layout: 'column',
                    width: '100%',
                    items: [{
                        xtype: 'numberfield',
                        fieldLabel: 'Lease Duration'.t(),
                        labelWidth: 190,
                        width: 350,
                        labelAlign: 'right',
                        minValue: 1,
                        bind: {
                            value: '{intf.dhcpLeaseDuration}'
                        },
                        allowBlank: false
                    }, {
                        xtype: 'displayfield',
                        margin: '0 5',
                        fieldStyle: {
                            color: '#777',
                            fontSize: 'smaller',
                            minHeight: 'auto'
                        },
                        value: '(seconds)'.t()
                    }]
                }, {
                    // gateway override
                    xtype: 'textfield',
                    bind: '{intf.dhcpGatewayOverride}',
                    fieldLabel: 'Gateway Override'.t(),
                    vtype: 'ip4Address',
                }, {
                    // netmask override
                    xtype: 'combo',
                    bind: '{intf.dhcpPrefixOverride}',
                    fieldLabel: 'Netmask Override'.t(),
                    editable: false,
                    store: Util.getV4NetmaskList(false),
                    queryMode: 'local'
                }, {
                    // dns override
                    xtype: 'textfield',
                    bind: '{intf.dhcpDnsOverride}',
                    fieldLabel: 'DNS Override'.t(),
                    vtype: 'ip4AddressList',
                }]
            }, {
                // DHCP options
                xtype: 'ungrid',
                title: 'DHCP Options'.t(),
                emptyText: 'No DHCP options defined'.t(),
                border: false,
                collapsible: true,
                titleCollapse: true,
                animCollapse: false,
                disabled: true,
                bind: {
                    store: '{dhcpOptions}',
                    disabled: '{!intf.dhcpEnabled}'
                },
                tbar: ['@addInline'],
                recordActions: ['delete'],
                listProperty: 'dhcpOptions',
                emptyRow: {
                    enabled: true,
                    value: '66,1.2.3.4',
                    description: '[no description]'.t(),
                    javaClass: 'com.untangle.uvm.network.DhcpOption'
                },
                columns: [{
                    header: 'Enable'.t(),
                    xtype: 'checkcolumn',
                    dataIndex: 'enabled',
                    align: 'center',
                    width: Renderer.booleanWidth,
                    resizable: false
                }, {
                    header: 'Description'.t(),
                    dataIndex: 'description',
                    flex: 1,
                    editor : {
                        xtype: 'textfield',
                        emptyText: '[enter description]'.t(),
                        allowBlank:false
                    }
                }, {
                    header: 'Value'.t(),
                    dataIndex: 'value',
                    width: 150,
                    editor : {
                        xtype: 'textfield',
                        emptyText: '[enter value]'.t(),
                        allowBlank: false
                    }
                }],
            }]
        }, {
            title: 'Redundancy (VRRP) Configuration'.t(),
            tabConfig: {
                hidden: true,
                bind: {
                    hidden: '{!isAddressed}'
                }
            },
            tbar: [{
                // VRRP enabled
                xtype: 'checkbox',
                itemId: 'vrrpEnabled',
                margin: 2,
                bind: '{intf.vrrpEnabled}',
                boxLabel: '<strong>' + 'Enable VRRP'.t() + '</strong>'
            }, '->', {
                xtype: 'component',
                margin: 2,
                bind: {
                    html: 'Is VRRP Master'.t() + ' <i class="fa fa-circle {vrrpmaster ? "fa-green" : "fa-gray"}"></i>'
                }
            }],
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                xtype: 'container',
                flex: 1,
                disabled: true,
                bind: {
                    disabled: '{!intf.vrrpEnabled}'
                },
                padding: 10,
                items: [{
                    // VRRP ID
                    xtype: 'fieldcontainer',
                    layout: 'column',
                    width: '100%',
                    items: [{
                        xtype: 'numberfield',
                        bind: {
                            value: '{intf.vrrpId}',
                        },
                        fieldLabel: 'VRRP ID'.t(),
                        labelWidth: 190,
                        labelAlign: 'right',
                        width: 260,
                        minValue: 1,
                        maxValue: 255,
                        allowBlank: false
                    }, {
                        xtype: 'displayfield',
                        margin: '0 5',
                        fieldStyle: {
                            color: '#777',
                            fontSize: 'smaller',
                            minHeight: 'auto'
                        },
                        value: 'VRRP ID must be a valid integer between 1 and 255.'.t()
                    }]
                }, {
                    // VRRP priority
                    xtype: 'fieldcontainer',
                    layout: 'column',
                    width: '100%',
                    items: [{
                        xtype: 'numberfield',
                        bind: {
                            value: '{intf.vrrpPriority}',
                        },
                        fieldLabel: 'VRRP Priority'.t(),
                        labelWidth: 190,
                        labelAlign: 'right',
                        width: 260,
                        minValue: 1,
                        maxValue: 255,
                        allowBlank: false
                    }, {
                        xtype: 'displayfield',
                        margin: '0 5',
                        fieldStyle: {
                            color: '#777',
                            fontSize: 'smaller',
                            minHeight: 'auto'
                        },
                        value: 'VRRP Priority must be a valid integer between 1 and 255.'.t()
                    }]
                }]
            }, {
                // VRRP aliases
                xtype: 'ungrid',
                title: 'VRRP Aliases'.t(),
                emptyText: 'No VRRP Aliases defined'.t(),
                collapsible: true,
                titleCollapse: true,
                animCollapse: false,
                border: false,
                tbar: ['@addInline'],
                recordActions: ['delete'],
                maxHeight: 200,
                disabled: true,
                bind: {
                    store: '{vrrpAliases}',
                    disabled: '{!intf.vrrpEnabled}'
                },
                listProperty: 'vrrpAliases',
                emptyRow: {
                    staticAddress: '1.2.3.4',
                    staticPrefix: '24',
                    javaClass: 'com.untangle.uvm.network.InterfaceSettings$InterfaceAlias'
                },
                columns: [{
                    header: 'Address'.t(),
                    dataIndex: 'staticAddress',
                    width: Renderer.ipWidth,
                    editor : {
                        xtype: 'textfield',
                        vtype: 'ip4Address',
                        emptyText: '[enter IPv4 address]'.t(),
                        allowBlank: false
                    }
                }, {
                    header: 'Netmask / Prefix'.t(),
                    dataIndex: 'staticPrefix',
                    width: Renderer.networkWidth,
                    flex: 1,
                    editor : {
                        xtype: 'numberfield',
                        minValue: 1,
                        maxValue: 32,
                        allowDecimals: false,
                        allowBlank: false
                    }
                }],
            }]
        }]
    }],

    buttons: [{
        text: 'Cancel',
        iconCls: 'fa fa-ban fa-red',
        // handler: function (btn) {
        //     btn.up('window').close();
        // }
        handler: 'cancelEdit'
    }, {
        text: 'Done',
        iconCls: 'fa fa-check',
        handler: 'doneEdit'
    }]

});
