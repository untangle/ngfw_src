Ext.define('Ung.config.network.Hostname', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.hostname',

    viewModel: true,

    title: 'Hostname'.t(),
    padding: 10,
    // itemId: 'interfaces',

    items: [{
        xtype: 'fieldset',
        title: 'Hostname'.t(),
        items: [{
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Hostname'.t(),
                labelAlign: 'right',
                emptyText: 'untangle',
                name: 'HostName',
                bind: '{settings.hostName}',
                maskRe: /[a-zA-Z0-9\-]/
            }, {
                xtype: 'label',
                html: '(eg: gateway)'.t(),
                cls: 'boxlabel'
            }]
        },{
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Domain Name'.t(),
                labelAlign: 'right',
                emptyText: 'example.com',
                allowBlank: false,
                name: 'DomainName',
                bind: '{settings.domainName}'
            }, {
                xtype: 'label',
                html: '(eg: example.com)',
                cls: 'boxlabel'
            }]
        }]
    }, {
        xtype: 'fieldset',
        title: 'Dynamic DNS Service Configuration'.t(),
        defaults: {
            labelAlign: 'right'
        },
        items: [{
            xtype: 'checkbox',
            fieldLabel: 'Enabled',
            bind: '{settings.dynamicDnsServiceEnabled}',
        }, {
            xtype: 'combo',
            fieldLabel: 'Service'.t(),
            bind: '{settings.dynamicDnsServiceName}',
            store: [['easydns','EasyDNS'],
                    ['zoneedit','ZoneEdit'],
                    ['dyndns','DynDNS'],
                    ['namecheap','Namecheap'],
                    ['dslreports','DSL-Reports'],
                    ['dnspark','DNSPark'],
                    ['no-ip','No-IP'],
                    ['dnsomatic','DNS-O-Matic'],
                    ['cloudflare','Cloudflare']]
        }, {
            xtype: 'textfield',
            fieldLabel: 'Username'.t(),
            bind: '{settings.dynamicDnsServiceUsername}'
        }, {
            xtype: 'textfield',
            fieldLabel: 'Password'.t(),
            bind: '{settings.dynamicDnsServicePassword}',
            inputType: 'password'
        }, {
            xtype: 'textfield',
            fieldLabel: 'Hostname(s)'.t(),
            bind: '{settings.dynamicDnsServiceHostnames}',
        }]
    }, {
        xtype: 'radiogroup',
        title: 'Public Address Configuration'.t(),
        columns: 1,
        simpleValue: true,
        bind: '{settings.publicUrlMethod}',
        items: [{
            xtype: 'component',
            margin: '0 0 10 0',
            html: Ext.String.format('The Public Address is the address/URL that provides a public location for the {0} Server. This address will be used in emails sent by the {0} Server to link back to services hosted on the {0} Server such as Quarantine Digests and OpenVPN Client emails.'.t(), rpc.companyName)
        }, {
            xtype: 'radio',
            boxLabel: 'Use IP address from External interface (default)'.t(),
            name: 'publicUrl',
            inputValue: 'external'
        }, {
            xtype: 'component',
            margin: '0 0 10 25',
            html: Ext.String.format('This works if your {0} Server has a routable public static IP address.'.t(), rpc.companyName)
        }, {
            xtype: 'radio',
            boxLabel: 'Use Hostname'.t(),
            name: 'publicUrl',
            inputValue: 'hostname'
        }, {
            xtype: 'component',
            margin: '0 0 5 25',
            html: Ext.String.format('This is recommended if the {0} Server\'s fully qualified domain name looks up to its IP address both internally and externally.'.t(), rpc.companyName)
        }, {
            xtype: 'component',
            margin: '0 0 10 25',
            bind: {
                html: 'Current Hostname'.t() + ':<i> {fullHostName} </i>'
            }
        }, {
            xtype: 'radio',
            boxLabel: 'Use Manually Specified Address'.t(),
            name: 'publicUrl',
            inputValue: 'address_and_port'
        }, {
            xtype: 'component',
            margin: '0 0 10 25',
            html: Ext.String.format('This is recommended if the {0} Server is installed behind another firewall with a port forward from the specified hostname/IP that redirects traffic to the {0} Server.'.t(), rpc.companyName)
        }, {
            xtype: 'textfield',
            margin: '0 0 5 25',
            fieldLabel: 'IP/Hostname'.t(),
            name: 'publicUrlAddress',
            allowBlank: false,
            width: 400,
            blankText: 'You must provide a valid IP Address or hostname.'.t(),
            disabled: true,
            bind: {
                value: '{settings.publicUrlAddress}',
                disabled: '{settings.publicUrlMethod != "address_and_port"}',
            }
        }, {
            xtype: 'numberfield',
            margin: '0 0 5 25',
            fieldLabel: 'Port'.t(),
            name: 'publicUrlPort',
            allowDecimals: false,
            minValue: 0,
            allowBlank: false,
            width: 210,
            blankText: 'You must provide a valid port.'.t(),
            vtype: 'port',
            disabled: true,
            bind: {
                value: '{settings.publicUrlPort}',
                disabled: '{settings.publicUrlMethod != "address_and_port"}',
            }
        }]
    }]
});
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
        tbar: [{
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
        'Ung.config.network.NetworkController',
        'Ung.config.network.NetworkModel',

        'Ung.view.grid.Grid',
        'Ung.store.RuleConditions',
        'Ung.store.Rule',
        'Ung.cmp.Rules'
    ],

    controller: 'config.network',

    viewModel: {
        type: 'config.network'
    },

    portForwardConditions: [
        {name:"DST_LOCAL",displayName: 'Destined Local'.t(), type: "boolean", visible: true},
        {name:"DST_ADDR",displayName: 'Destination Address'.t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"DST_PORT",displayName: 'Destination Port'.t(), type: "textfield",vtype:"portMatcher", visible: true},
        {name:"SRC_ADDR",displayName: 'Source Address'.t(), type: "textfield", visible: true, vtype:"ipMatcher"},
        {name:"SRC_PORT",displayName: 'Source Port'.t(), type: "textfield",vtype:"portMatcher", visible: rpc.isExpertMode},
        {name:"SRC_INTF",displayName: 'Source Interface'.t(), type: "checkboxgroup", values: [['a', 'a'], ['b', 'b']], visible: true},
        {name:"PROTOCOL",displayName: 'Protocol'.t(), type: "checkboxgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
    ],

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
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    items: [{
        xtype: 'ung.config.network.interfaces'
    }, {
        xtype: 'ung.config.network.hostname'
    }, {
        xtype: 'ung.config.network.services'
    }, {
        xtype: 'ung.config.network.portforwardrules'
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
        // '#apply': {
        //     click: 'saveSettings'
        // }
    },

    saveSettings: function () {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;
        // view.setLoading('Saving ...');
        console.log(vm.get('settings'));
        // rpc.networkManager.setNetworkSettings(function (result, ex) {
        //     console.log(ex);
        //     console.log(result);
        //     // vm.getStore('interfaces').reload();
        //     view.setLoading(false);
        //     me.loadInterfaceStatusAndDevices();
        // }, vm.get('settings'));
    },

    loadSettings: function () {
        var me = this;
        rpc.networkManager.getNetworkSettings(function (result, ex) {
            me.getViewModel().set('settings', result);
            me.loadInterfaceStatusAndDevices();

            console.log(result);
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
    },


    editWin: function () {
        // console.log(this.getViewModel());
        // var pf = this.getViewModel().get('portforwardrules');
        // // console.log(pf);
        // pf.getAt(0).data.conditions.list[0].value = 'false';
        // console.log(pf.getAt(0));

        // pf.getAt(0).set('description', 'aaa');
        // pf.getAt(0).set('conditions', {
        //     list: [{
        //         conditionType: 'DST_LOCAL'
        //     }]
        // });

        // Ext.widget('ung.config.network.editorwin', {
        //     // config: {
        //         conditions: [
        //             {name:"DST_LOCAL",displayName: 'Destined Local'.t(), type: "boolean", visible: true},
        //             {name:"DST_ADDR",displayName: 'Destination Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
        //             {name:"DST_PORT",displayName: 'Destination Port'.t(), type: "text",vtype:"portMatcher", visible: true},
        //             {name:"SRC_ADDR",displayName: 'Source Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
        //             {name:"SRC_PORT",displayName: 'Source Port'.t(), type: "text",vtype:"portMatcher", visible: rpc.isExpertMode},
        //             {name:"SRC_INTF",displayName: 'Source Interface'.t(), type: "checkgroup", values: Ung.Util.getInterfaceList(true, true), visible: true},
        //             {name:"PROTOCOL",displayName: 'Protocol'.t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
        //         ],
        //         rule: record,
        //         viewModel: {
        //             data: {
        //                 rule:
        //             }
        //         }
            // }
            // conditions: {
            //     DST_LOCAL: {displayName: 'Destined Local'.t(), type: "boolean", visible: true},
            //     DST_ADDR: {displayName: 'Destination Address'.t(), type: "text", visible: true, vtype:"ipMatcher"},
            //     DST_PORT: {displayName: 'Destination Port'.t(), type: "text", vtype:"portMatcher", visible: true},
            //     PROTOCOL: {displayName: 'Protocol'.t(), type: "checkgroup", values: [["TCP","TCP"],["UDP","UDP"],["ICMP","ICMP"],["GRE","GRE"],["ESP","ESP"],["AH","AH"],["SCTP","SCTP"]], visible: true}
            // },
            // viewModel: {
            //     data: {
            //         rule: record
            //     }
            // },
        // });


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
        activePropsItem: function (get) { return get('si.configType') !== 'DISABLED' ? 0 : 2; },

        fullHostName: function (get) {
            var domain = get('settings.domainName'),
                host = get('settings.hostName');
            if (domain !== null && domain !== '') {
                return host + "." + domain;
            }
            return host;
        },

                                conditionsData: {
                                    bind: '{rule.conditions.list}',
                                    get: function (coll) {
                                        return coll || [];
                                    }
                                },

        portForwardRulesData: {
            bind: '{settings.portForwardRules.list}',
            get: function (rules) {
                return rules || null;
            }
        }
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
        },

        portforwardrules: {
            type: 'rule',
            data: '{settings.portForwardRules.list}'
        }
    }
});
Ext.define('Ung.config.network.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.portforwardrules',

    viewModel: true,

    requires: [
        // 'Ung.config.network.ConditionWidget',
        // 'Ung.config.network.CondWidget'
    ],

    title: 'Port Forward Rules'.t(),

    layout: { type: 'vbox', align: 'stretch' },

    tbar: [{
        xtype: 'displayfield',
        value: "Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.".t()
    }],

    items: [{
        xtype: 'ung.cmp.rules',
        flex: 3,
        columnFeatures: ['reorder', 'delete', 'edit'], // which columns to add
        recordActions: ['@edit', '@delete'],

        conditions: [
            { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean', visible: true},
            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'port', visible: true},
            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', visible: true, vtype:'ipall'},
            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield', vtype:'port', visible: rpc.isExpertMode},
            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: [['a', 'a'], ['b', 'b']], visible: true},
            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']], visible: true}
        ],

        emptyRow: {
            ruleId: -1,
            simple: true,
            enabled: true,
            // description: '',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: [{
                    conditionType: 'DST_LOCAL',
                    invert: false,
                    value: 'true',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }, {
                    conditionType: 'PROTOCOL',
                    invert: false,
                    value: 'TCP',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }, {
                    conditionType:'DST_PORT',
                    invert: false,
                    value: '80',
                    javaClass: 'com.untangle.uvm.network.PortForwardRuleCondition'
                }]
            },
            newPort: 80
        },

        bind: {
            store: '{portforwardrules}'
        },

        modelValidation: true,

        sortableColumns: false,
        enableColumnHide: false,

        columns: [{
            header: 'Rule Id'.t(),
            width: 70,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function(value) {
                if (value < 0) {
                    return 'new'.t();
                } else {
                    return value;
                }
            }
        }, {
            xtype:'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: 70,
            // renderer: function (val) {
            //     return '<i class="fa + ' + (val ? 'fa-check' : 'fa-check-o') + '"></i>';
            // }
        }, {
            header: 'Description',
            width: 200,
            dataIndex: 'description',
            editor: {
                xtype: 'textfield',
                emptyText: '[no description]'.t(),
                allowBlank: false
            }
        }, {
            header: 'Conditions'.t(),
            itemId: 'conditions',
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        },
        // {
        //     xtype: 'actioncolumn', //
        //     iconCls: 'fa fa-edit',
        //     handler: 'editRuleWin'
        // },
        {
            header: 'New Destination'.t(),
            dataIndex: 'newDestination',
            width: 150,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                vtype: 'ipall'
            }
        }, {
            header: 'New Port'.t(),
            dataIndex: 'newPort',
            // align: 'right',
            width: 80,
            editor: {
                xtype: 'numberfield',
                allowBlank: true,
                minValue : 1,
                maxValue : 0xFFFF,
                vtype: 'port'
            }
        }],

        editFields: [{

        }]
    }, {
        xtype: 'fieldset',
        flex: 2,
        margin: 10,
        // border: true,
        collapsible: true,
        collapsed: false,
        autoScroll: true,
        title: 'The following ports are currently reserved and can not be forwarded:'.t(),
        items: [{
            xtype: 'component',
            name: 'portForwardWarnings',
            html: ' '
        }]
    }]
});
Ext.define('Ung.config.network.Services', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.network.services',

    viewModel: true,

    title: 'Services'.t(),
    padding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'Local Services'.t(),
        defaults: {
            allowDecimals: false,
            minValue: 0,
            allowBlank: false,
            vtype: 'port'
        },
        items: [{
            xtype: 'component',
            html: '<br/>' + 'The specified HTTPS port will be forwarded from all interfaces to the local HTTPS server to provide administration and other services.'.t() + '<br/>',
            margin: '0 0 10 0'
        }, {
            xtype: 'numberfield',
            fieldLabel: 'HTTPS port'.t(),
            name: 'httpsPort',
            bind: '{settings.httpsPort}',
            blankText: 'You must provide a valid port.'.t()
        }, {
            xtype: 'component',
            html: '<br/>' + 'The specified HTTP port will be forwarded on non-WAN interfaces to the local HTTP server to provide administration, blockpages, and other services.'.t() + '<br/>',
            margin: '0 0 10 0'
        }, {
            xtype: 'numberfield',
            fieldLabel: 'HTTP port'.t(),
            name: 'httpPort',
            bind: '{settings.httpPort}',
            blankText: 'You must provide a valid port.'.t(),
        }]
    }]

});