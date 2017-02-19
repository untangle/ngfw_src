Ext.define('Ung.config.network.Interface', {
    extend: 'Ext.window.Window',
    alias: 'widget.config.interface',
    width: 420,
    height: 550,
    constrainTo: 'body',
    layout: 'fit',

    modal: true,
//    viewModel: true,

    title: 'Config'.t(),

    items: [{
        xtype: 'form',
        border: false,
        scrollable: 'vertical',
        bodyPadding: 10,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        defaults: {
            labelWidth: 200,
            labelAlign: 'right'
        },

        items: [{
            // interface name
            xtype: 'textfield',
            fieldLabel: 'Interface Name'.t(),
            labelAlign: 'top',
            name: 'interfaceName',
            allowBlank: false,
            bind: '{si2.name}'
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
        }],
        buttons: [{
            text: 'Cancel',
            iconCls: 'fa fa-ban fa-red'
        }, {
            text: 'Done',
            iconCls: 'fa fa-check'
        }]

    }]

});

