Ext.define('Ung.config.network.Interface', {    extend: 'Ext.window.Window',    alias: 'widget.config.interface',    width: 420,    height: 550,    constrainTo: 'body',    layout: 'fit',    modal: true,//    viewModel: true,    title: 'Config'.t(),    items: [{        xtype: 'form',        border: false,        scrollable: 'vertical',        bodyPadding: 10,        layout: {            type: 'vbox',            align: 'stretch'        },        defaults: {            labelWidth: 200,            labelAlign: 'right'        },        items: [{            // interface name            xtype: 'textfield',            fieldLabel: 'Interface Name'.t(),            labelAlign: 'top',            name: 'interfaceName',            allowBlank: false,            bind: '{si2.name}'        },        // {        //     // is VLAN        //     xtype: 'checkbox',        //     fieldLabel: 'Is VLAN (802.1q) Interface'.t(),        //     // readOnly: true,        //     bind: {        //         value: '{si.isVlanInterface}',        //         hidden: '{isDisabled}'        //     }        // }, {        //     // is Wireless        //     xtype: 'checkbox',        //     fieldLabel: 'Is Wireless Interface'.t(),        //     // readOnly: true,        //     bind: {        //         value: '{si.isWirelessInterface}',        //         hidden: '{isDisabled}'        //     }        // },        {            // parent VLAN            xtype: 'combo',            allowBlank: false,            editable: false,            bind: {                value: '{si.vlanParent}',                hidden: '{!si.isVlanInterface || isDisabled}' // visible only if isVlanInterface            },            hidden: true,            fieldLabel: 'Parent Interface'.t(),            // store: Ung.Util.getInterfaceList(false, false),            queryMode: 'local'        }, {            // VLAN Tag            xtype: 'numberfield',            bind: {                value: '{si.vlanTag}',                hidden: '{!si.isVlanInterface || isDisabled}' // visible only if isVlanInterface            },            hidden: true,            fieldLabel: '802.1q Tag'.t(),            minValue: 1,            maxValue: 4096,            allowBlank: false        }, {            // config type            xtype: 'segmentedbutton',            allowMultiple: false,            bind: '{si.configType}',            margin: '10 12',            items: [{                text: 'Addressed'.t(),                value: 'ADDRESSED'            }, {                text: 'Bridged'.t(),                value: 'BRIDGED'            }, {                text: 'Disabled'.t(),                value: 'DISABLED'            }]        }, {            // bridged to            xtype: 'combo',            allowBlank: false,            editable: false,            hidden: true,            bind: {                value: '{si.bridgedTo}',                hidden: '{!isBridged}'            },            fieldLabel: 'Bridged To'.t(),            // store: Ung.Util.getInterfaceAddressedList(),            queryMode: 'local'        }, {            // is WAN            xtype: 'checkbox',            fieldLabel: 'Is WAN Interface'.t(),            hidden: true,            bind: {                value: '{si.isWan}',                hidden: '{!isAddressed}'            }        }, {            // wireless conf            xtype: 'fieldset',            width: '100%',            title: 'Wireless Configuration'.t(),            collapsible: true,            // hidden: true,            defaults: {                labelWidth: 190,                labelAlign: 'right',                anchor: '100%'            },            hidden: true,            bind: {                hidden: '{!showWireless || !isAddressed}'            },            items: [{                // SSID                xtype: 'textfield',                fieldLabel: 'SSID'.t(),                bind: '{si.wirelessSsid}',                allowBlank: false,                disableOnly: true,                maxLength: 30,                maskRe: /[a-zA-Z0-9\-_=]/            }, {                // encryption                xtype: 'combo',                fieldLabel: 'Encryption'.t(),                bind: '{si.wirelessEncryption}',                editable: false,                store: [                    ['NONE', 'None'.t()],                    ['WPA1', 'WPA'.t()],                    ['WPA12', 'WPA / WPA2'.t()],                    ['WPA2', 'WPA2'.t()]                ],                queryMode: 'local'            }, {                // password                xtype: 'textfield',                bind: {                    value: '{si.wirelessPassword}',                    hidden: '{!showWirelessPassword}'                },                fieldLabel: 'Password'.t(),                allowBlank: false,                disableOnly: true,                maxLength: 63,                minLength: 8,                maskRe: /[a-zA-Z0-9~@#%_=,\!\-\/\?\(\)\[\]\\\^\$\+\*\.\|]/            }, {                // channel                xtype: 'combo',                bind: '{si.wirelessChannel}',                fieldLabel: 'Channel'.t(),                editable: false,                valueField: 'channel',                displayField: 'channelDescription',                queryMode: 'local'            }]        }, {            // IPv4 conf            xtype: 'fieldset',            title: 'IPv4 Configuration'.t(),            collapsible: true,            defaults: {                labelWidth: 190,                labelAlign: 'right',                anchor: '100%'            },            hidden: true,            bind: {                hidden: '{!isAddressed}'            },            items: [{                xtype: 'segmentedbutton',                allowMultiple: false,                bind: {                    value: '{si.v4ConfigType}',                    hidden: '{!si.isWan}'                },                margin: '10 0',                items: [{                    text: 'Auto (DHCP)'.t(),                    value: 'AUTO'                }, {                    text: 'Static'.t(),                    value: 'STATIC'                }, {                    text: 'PPPoE'.t(),                    value: 'PPPOE'                }]            },            // {            //     // config type            //     xtype: 'combo',            //     bind: {            //         value: '{si.v4ConfigType}',            //         hidden: '{!si.isWan}'            //     },            //     fieldLabel: 'Config Type'.t(),            //     allowBlank: false,            //     editable: false,            //     store: [            //         ['AUTO', 'Auto (DHCP)'.t()],            //         ['STATIC', 'Static'.t()],            //         ['PPPOE', 'PPPoE'.t()]            //     ],            //     queryMode: 'local'            // },            {                // address                xtype: 'textfield',                bind: {                    value: '{si.v4StaticAddress}',                    hidden: '{!isStaticv4}'                },                fieldLabel: 'Address'.t(),                allowBlank: false            }, {                // netmask                xtype: 'combo',                bind: {                    value: '{si.v4StaticPrefix}',                    hidden: '{!isStaticv4}'                },                fieldLabel: 'Netmask'.t(),                allowBlank: false,                editable: false,                store: Ung.Util.v4NetmaskList,                queryMode: 'local'            }, {                // gateway                xtype: 'textfield',                bind: {                    value: '{si.v4StaticGateway}',                    hidden: '{!si.isWan || !isStaticv4}'                },                fieldLabel: 'Gateway'.t(),                allowBlank: false            }, {                // primary DNS                xtype: 'textfield',                bind: {                    value: '{si.v4StaticDns1}',                    hidden: '{!si.isWan || !isStaticv4}'                },                fieldLabel: 'Primary DNS'.t(),                allowBlank: false            }, {                // secondary DNS                xtype: 'textfield',                bind: {                    value: '{si.v4StaticDns2}',                    hidden: '{!si.isWan || !isStaticv4}'                },                fieldLabel: 'Secondary DNS'.t()            }, {                // override address                xtype: 'textfield',                bind: {                    value: '{si.v4AutoAddressOverride}',                    emptyText: '{si.v4Address}',                    hidden: '{!isAutov4}'                },                fieldLabel: 'Address Override'.t()            }, {                // override netmask                xtype: 'combo',                bind: {                    value: '{si.v4AutoPrefixOverride}',                    hidden: '{!isAutov4}'                },                editable: false,                fieldLabel: 'Netmask Override'.t(),                store: Ung.Util.v4NetmaskList,                queryMode: 'local'            }, {                // override gateway                xtype: 'textfield',                bind: {                    value: '{si.v4AutoGatewayOverride}',                    emptyText: '{si.v4Gateway}',                    hidden: '{!isAutov4}'                },                fieldLabel: 'Gateway Override'.t()            }, {                // override primary DNS                xtype: 'textfield',                bind: {                    value: '{si.v4AutoDns1Override}',                    emptyText: '{si.v4Dns1}',                    hidden: '{!isAutov4}'                },                fieldLabel: 'Primary DNS Override'.t()            }, {                // override secondary DNS                xtype: 'textfield',                bind: {                    value: '{si.v4AutoDns2Override}',                    emptyText: '{si.v4Dns2}',                    hidden: '{!isAutov4}'                },                fieldLabel: 'Secondary DNS Override'.t()            }, {                // renew DHCP lease,                xtype: 'button',                text: 'Renew DHCP Lease'.t(),                margin: '0 0 15 200',                bind: {                    hidden: '{!isAutov4}'                }            }, {                // PPPoE username                xtype: 'textfield',                bind: {                    value: '{si.v4PPPoEUsername}',                    hidden: '{!isPPPOEv4}'                },                fieldLabel: 'Username'.t()            }, {                // PPPoE password                xtype: 'textfield',                inputType: 'password',                bind: {                    value: '{si.v4PPPoEPassword}',                    hidden: '{!isPPPOEv4}'                },                fieldLabel: 'Password'.t()            }, {                // PPPoE peer DNS                xtype: 'checkbox',                bind: {                    value: '{si.v4PPPoEUsePeerDns}',                    hidden: '{!isPPPOEv4}'                },                fieldLabel: 'Use Peer DNS'.t()            }, {                // PPPoE primary DNS                xtype: 'textfield',                bind: {                    value: '{si.v4PPPoEDns1}',                    hidden: '{!isPPPOEv4 || si.v4PPPoEUsePeerDns}'                },                fieldLabel: 'Primary DNS'.t()            }, {                // PPPoE secondary DNS                xtype: 'textfield',                bind: {                    value: '{si.v4PPPoEDns2}',                    hidden: '{!isPPPOEv4 || si.v4PPPoEUsePeerDns}'                },                fieldLabel: 'Secondary DNS'.t()            }, {                xtype: 'fieldset',                title: 'IPv4 Options'.t(),                items: [{                    xtype:'checkbox',                    bind: {                        value: '{si.v4NatEgressTraffic}',                        hidden: '{!si.isWan}'                    },                    boxLabel: 'NAT traffic exiting this interface (and bridged peers)'.t()                }, {                    xtype:'checkbox',                    bind: {                        value: '{si.v4NatIngressTraffic}',                        hidden: '{si.isWan}'                    },                    boxLabel: 'NAT traffic coming from this interface (and bridged peers)'.t()                }]            }]            // @todo: add aliases grid        }, {            // IPv6            xtype: 'fieldset',            title: 'IPv6 Configuration'.t(),            collapsible: true,            defaults: {                xtype: 'textfield',                labelWidth: 190,                labelAlign: 'right',                anchor: '100%'            },            hidden: true,            bind: {                hidden: '{!isAddressed}',                collapsed: '{isDisabledv6}'            },            items: [{                // config type                xtype: 'segmentedbutton',                allowMultiple: false,                bind: {                    value: '{si.v6ConfigType}',                    hidden: '{!si.isWan}'                },                margin: '10 0',                items: [{                    text: 'Disabled'.t(),                    value: 'DISABLED'                }, {                    text: 'Auto (SLAAC/RA)'.t(),                    value: 'AUTO'                }, {                    text: 'Static'.t(),                    value: 'STATIC'                }]                // xtype: 'combo',                // bind: {                //     value: '{si.v6ConfigType}',                //     hidden: '{!si.isWan}'                // },                // fieldLabel: 'Config Type'.t(),                // editable: false,                // store: [                //     ['DISABLED', 'Disabled'.t()],                //     ['AUTO', 'Auto (SLAAC/RA)'.t()],                //     ['STATIC', 'Static'.t()]                // ],                // queryMode: 'local'            }, {                // address                bind: {                    value: '{si.v6StaticAddress}',                    hidden: '{isDisabledv6 || isAutov6}'                },                fieldLabel: 'Address'.t()            }, {                // prefix length                bind: {                    value: '{si.v6StaticPrefixLength}',                    hidden: '{isDisabledv6 || isAutov6}'                },                fieldLabel: 'Prefix Length'.t()            }, {                // gateway                bind: {                    value: '{si.v6StaticGateway}',                    hidden: '{isDisabledv6 || isAutov6 || !si.isWan}'                },                fieldLabel: 'Gateway'.t()            }, {                // primary DNS                bind: {                    value: '{si.v6StaticDns1}',                    hidden: '{isDisabledv6 || isAutov6 || !si.isWan}'                },                fieldLabel: 'Primary DNS'.t()            }, {                // secondary DNS                bind: {                    value: '{si.v6StaticDns2}',                    hidden: '{isDisabledv6 || isAutov6 || !si.isWan}'                },                fieldLabel: 'Secondary DNS'.t()            }, {                xtype: 'fieldset',                title: 'IPv6 Options'.t(),                bind: {                    hidden: '{isDisabledv6 || isAutov6 || si.isWan}'                },                items: [{                    xtype:'checkbox',                    bind: {                        value: '{si.raEnabled}'                        // hidden: '{si.isWan}'                    },                    boxLabel: 'Send Router Advertisements'.t()                }, {                    xtype: 'label',                    style: {                        fontSize: '10px'                    },                    html: '<span style="color: red">' + 'Warning:'.t() + '</span> ' + 'SLAAC only works with /64 subnets.'.t(),                    bind: {                        hidden: '{!showRouterWarning}'                    }                }]            }]            // @todo: add aliases grid        }, {            xtype: 'fieldset',            title: 'DHCP Configuration',            collapsible: true,            defaults: {                labelWidth: 190,                labelAlign: 'right',                anchor: '100%'            },            hidden: true,            bind: {                hidden: '{!isAddressed || si.isWan}'            },            items: [{                // dhcp enabled                xtype: 'checkbox',                bind: '{si.dhcpEnabled}',                boxLabel: 'Enable DHCP Serving'.t()            }, {                // dhcp range start                xtype: 'textfield',                bind: {                    value: '{si.dhcpRangeStart}',                    hidden: '{!si.dhcpEnabled}'                },                fieldLabel: 'Range Start'.t(),                allowBlank: false,                disableOnly: true            }, {                // dhcp range end                xtype: 'textfield',                bind: {                    value: '{si.dhcpRangeEnd}',                    hidden: '{!si.dhcpEnabled}'                },                fieldLabel: 'Range End'.t(),                allowBlank: false,                disableOnly: true            }, {                // lease duration                xtype: 'numberfield',                bind: {                    value: '{si.dhcpLeaseDuration}',                    hidden: '{!si.dhcpEnabled}'                },                fieldLabel: 'Lease Duration'.t() + ' ' + '(seconds)'.t(),                allowDecimals: false,                allowBlank: false,                disableOnly: true            }, {                xtype: 'fieldset',                title: 'DHCP Advanced'.t(),                collapsible: true,                collapsed: true,                defaults: {                    labelWidth: 180,                    labelAlign: 'right',                    anchor: '100%'                },                bind: {                    hidden: '{!si.dhcpEnabled}'                },                items: [{                    // gateway override                    xtype: 'textfield',                    bind: '{si.dhcpGatewayOverride}',                    fieldLabel: 'Gateway Override'.t()                }, {                    // netmask override                    xtype: 'combo',                    bind: '{si.dhcpPrefixOverride}',                    fieldLabel: 'Netmask Override'.t(),                    editable: false,                    store: Ung.Util.v4NetmaskList,                    queryMode: 'local'                }, {                    // dns override                    xtype: 'textfield',                    bind: '{si.dhcpDnsOverride}',                    fieldLabel: 'DNS Override'.t()                }]                // @todo: dhcp options editor            }]        }, {            // VRRP            xtype: 'fieldset',            title: 'Redundancy (VRRP) Configuration'.t(),            collapsible: true,            defaults: {                labelWidth: 190,                labelAlign: 'right',                anchor: '100%'            },            hidden: true,            bind: {                hidden: '{!isAddressed || !isStaticv4}'            },            items: [{                // VRRP enabled                xtype: 'checkbox',                bind: '{si.vrrpEnabled}',                boxLabel: 'Enable VRRP'.t()            }, {                // VRRP ID                xtype: 'numberfield',                bind: {                    value: '{si.vrrpId}',                    hidden: '{!si.vrrpEnabled}'                },                fieldLabel: 'VRRP ID'.t(),                minValue: 1,                maxValue: 255,                allowBlank: false,                blankText: 'VRRP ID must be a valid integer between 1 and 255.'.t()            }, {                // VRRP priority                xtype: 'numberfield',                bind: {                    value: '{si.vrrpPriority}',                    hidden: '{!si.vrrpEnabled}'                },                fieldLabel: 'VRRP Priority'.t(),                minValue: 1,                maxValue: 255,                allowBlank: false,                blankText: 'VRRP Priority must be a valid integer between 1 and 255.'.t()            }]            // @todo: vrrp aliases        }],        buttons: [{            text: 'Cancel',            iconCls: 'fa fa-ban fa-red'        }, {            text: 'Done',            iconCls: 'fa fa-check'        }]    }]});Ext.define('Ung.config.network.Network', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.config.network',
    requires: [
        'Ung.config.network.NetworkController',
        'Ung.config.network.NetworkModel',
        'Ung.config.network.Interface',
        // 'Ung.view.grid.Grid',
        // 'Ung.store.RuleConditions',
        'Ung.store.Rule',
        'Ung.model.Rule',
        'Ung.cmp.Grid'
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
            xtype: 'tbtext',
            html: '<strong>' + 'Network'.t() + '</strong>'
        }],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        border: false,
        items: ['->', {
            text: 'Apply Changes'.t(),
            scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],
    items: [{
        xtype: 'config.network.interfaces'
    }, {
        xtype: 'config.network.hostname'
    }, {
        xtype: 'config.network.services'
    }, {
        xtype: 'config.network.portforwardrules'
    }, {
        xtype: 'config.network.natrules'
    }, {
        xtype: 'config.network.bypassrules'
    }, {
        xtype: 'config.network.routes'
    }, {
        xtype: 'config.network.dnsserver'
    }, {
        xtype: 'config.network.dhcpserver'
    }, {
        xtype: 'config.network.advanced'
    }, {
        xtype: 'config.network.troubleshooting'
    }]
});
Ext.define('Ung.config.network.NetworkController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.network',
    control: {
        '#': { afterrender: 'loadSettings' },
        '#interfaces': { beforeactivate: 'onInterfaces' },
        '#interfaceStatus': { beforeedit: function () { return false; } },
        'networktest': { afterrender: 'networkTestRender' }
    },
    getNetworkSettings: function () {
        var deferred = new Ext.Deferred();
        rpc.networkManager.getNetworkSettings(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        });
        return deferred.promise;
    },
    getInterfaceStatus: function () {
        var deferred = new Ext.Deferred();
        rpc.networkManager.getInterfaceStatus(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        });
        return deferred.promise;
    },
    getDeviceStatus: function () {
        var deferred = new Ext.Deferred();
        rpc.networkManager.getDeviceStatus(function (result, ex) {
            if (ex) { deferred.reject(ex); }
            deferred.resolve(result);
        });
        return deferred.promise;
    },
    loadSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        Ext.Deferred.sequence([
            this.getNetworkSettings,
            this.getInterfaceStatus,
            this.getDeviceStatus
        ], this).then(function (result) {
            v.setLoading(false);
            var intf, intfStatus, devStatus;
            result[0].interfaces.list.forEach(function (intf) {
                intfStatus = Ext.Array.findBy(result[1].list, function (intfSt) {
                    return intfSt.interfaceId === intf.interfaceId;
                });
                delete intfStatus.javaClass;
                Ext.apply(intf, intfStatus);
                devStatus = Ext.Array.findBy(result[2].list, function (devSt) {
                    return devSt.deviceName === intf.physicalDev;
                });
                delete devStatus.javaClass;
                Ext.apply(intf, devStatus);
            });
            vm.set('settings', result[0]);
        }, function (ex) {
            v.setLoading(false);
            console.error(ex);
            Ung.Util.exceptionToast(ex);
        });
    },
    saveSettings: function () {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;
        if (!Ung.Util.validateForms(view)) {
            return;
        }
        view.setLoading('Saving ...');
        // used to update all tabs data
        view.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            /**
             * Important!
             * update custom grids only if are modified records or it was reordered via drag/drop
             */
            if (store.getModifiedRecords().length > 0 || store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
                // store.commitChanges();
            }
        });
        rpc.networkManager.setNetworkSettings(function (result, ex) {
            view.setLoading (false);
            me.loadSettings();
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            Ung.Util.successToast('Network'.t() + ' settings saved!');
            // me.loadInterfaceStatusAndDevices();
        }, vm.get('settings'));
    },
    // /**
    //  * Loads netowrk settings
    //  */
    // loadSettings: function () {
    //     var me = this;
    //     rpc.networkManager.getNetworkSettings(function (result, ex) {
    //         if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
    //         me.getViewModel().set('settings', result);
    //         me.loadInterfaceStatusAndDevices();
    //         console.log(result);
    //     });
    // },
    onInterfaces: function () {
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
                        me.getSelectedInterfaceStatus(intf.get('symbolicDev'));
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
    setPortForwardWarnings: function () {
        var vm = this.getViewModel(),
            interfaces = vm.get('settings.interfaces.list'), intf, i,
            portForwardWarningsHtml = [];
        for (i = 0; i < interfaces.length; i += 1) {
            intf = interfaces[i];
            if (intf.v4Address) {
                portForwardWarningsHtml.push(Ext.String.format('<b>{0}:{1}</b> ', intf.v4Address, vm.get('settings.httpsPort')) + 'for HTTPS services.'.t() + '<br/>');
            }
        }
        for (i = 0; i < interfaces.length ; i += 1) {
            intf = interfaces[i];
            if (intf.v4Address && !intf.isWan) {
                portForwardWarningsHtml.push(Ext.String.format('<b>{0}:{1}</b> ', intf.v4Address, vm.get('settings.httpPort')) + 'for HTTP services.'.t() + '<br/>');
            }
        }
        for (i = 0; i < interfaces.length ; i += 1) {
            intf = interfaces[i];
            if (intf.v4Address && intf.isWan) {
                for (var j = 0; j < interfaces.length; j++) {
                    var sub_intf = interfaces[j];
                    if (sub_intf.configType === 'BRIDGED' && sub_intf.bridgedTo === intf.interfaceId) {
                        portForwardWarningsHtml.push(Ext.String.format('<b>{0}:{1}</b> ', intf.v4Address, vm.get('settings.httpPort')) +
                                                        'on'.t() +
                                                        Ext.String.format(' {2} ', sub_intf.name) +
                                                        'for HTTP services.'.t() + '<br/>');
                    }
                }
            }
        }
        vm.set('portForwardWarnings', portForwardWarningsHtml.join(''));
    },
    getInterface: function (i) {
        return i;
    },
    // onInterfaceSelect: function (grid, record) {
    //     this.getViewModel().set('si', record.getData());
    // },
    getSelectedInterfaceStatus: function (symbolicDev) {
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
        rpc.execManager.execOutput(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            if (Ext.isEmpty(result)) { return; }
            if (result.search('Device not found') >= 0) { return; }
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
                if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
                if (Ext.isEmpty(result)) { return; }
                var linep = result.split(' ');
                Ext.apply(stat, {
                    address: linep[0].split(':')[1],
                    mask: linep[2].split(':')[1]
                });
                rpc.execManager.execOutput(function (result, ex) {
                    if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
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
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
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
    refreshRoutes: function () {
        var v = this.getView();
        rpc.execManager.exec(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            v.down('#currentRoutes').setValue(result.output);
        }, '/usr/share/untangle/bin/ut-routedump.sh');
    },
    refreshQosStatistics: function () {
        rpc.execManager.execOutput(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
        }, '/usr/share/untangle-netd/bin/qos-service.py status');
    },
    // Network Tests
    networkTestRender: function (view) {
        view.down('form').insert(0, view.commandFields);
    },
    runTest: function (btn) {
        console.log(btn);
        var v = btn.up('networktest'),
            output = v.down('textarea'),
            text = [],
            me = this;
            btn.setDisabled(true);
            text.push(output.getValue());
            text.push('' + (new Date()) + ' - ' + 'Test Started'.t() + '\n');
            rpc.execManager.execEvil(function (result, ex) {
                if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
                me.readOutput(result, text, output, btn);
            }, v.getViewModel().get('command'));
    },
    readOutput: function (resultReader, text, output, btn) {
        var me = this;
        if (!resultReader) {
            return;
        }
        resultReader.readFromOutput(function (res, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            // console.log(res);
            if (res !== null) {
                text.push(res);
                Ext.Function.defer(me.readOutput, 1000, me, [resultReader, text, output, btn]);
            } else {
                btn.setDisabled(false);
                text.push('' + (new Date()) + ' - ' + 'Test Completed'.t());
                text.push('\n\n--------------------------------------------------------\n\n');
            }
            output.setValue(text.join(''));
            output.getEl().down('textarea').dom.scrollTop = 99999;
        });
    },
    clearOutput: function (btn) {
        var v = btn.up('networktest');
        v.down('textarea').setValue('');
    },
    editInterface: function () {
        var v = this.getView(),
            vm = this.getViewModel();
        // var win = Ext.create('config.interface');
        this.dialog = v.add({
            xtype: 'config.interface',
            viewModel: {
                data: {
                    si2: vm.get('si').copy(null)
                }
            }
        });
        this.dialog.show();
    }
});
Ext.define('Ung.config.network.NetworkModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.config.network',
    formulas: {
        // used in Interfaces view when showing/hiding interface specific configurations
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
        qosPriorityNoDefaultStore: function (get) {
            return get('qosPriorityStore').slice(1);
        },
    },
    data: {
        // si = selected interface (from grid)
        settings: null,
        // si: null,
        siStatus: null,
        siArp: null,
        qosPriorityStore: [
            [0, 'Default'.t()],
            [1, 'Very High'.t()],
            [2, 'High'.t()],
            [3, 'Medium'.t()],
            [4, 'Low'.t()],
            [5, 'Limited'.t()],
            [6, 'Limited More'.t()],
            [7, 'Limited Severely'.t()]
        ],
    },
    stores: {
        interfaces:         { data: '{settings.interfaces.list}' },
        interfaceArp:       { data: '{siArp}' },
        // Port Forward
        portForwardRules:   { data: '{settings.portForwardRules.list}' },
        // NAT
        natRules:           { data: '{settings.natRules.list}' },
        // Bypass
        bypassRules:        { data: '{settings.bypassRules.list}' },
        // Routes
        staticRoutes:       { data: '{settings.staticRoutes.list}' },
        // DNS
        staticDnsEntries:   { data: '{settings.dnsSettings.staticEntries.list}' },
        localServers:       { data: '{settings.dnsSettings.localServers.list}' },
        // DHCP
        staticDhcpEntries:  { data: '{settings.staticDhcpEntries.list}' },
        // Advanced
        devices:            { data: '{settings.devices.list}' },
        qosPriorities:      { data: '{settings.qosSettings.qosPriorities.list}' },
        qosRules:           { data: '{settings.qosSettings.qosRules.list}' },
        forwardFilterRules: { data: '{settings.forwardFilterRules.list}' },
        inputFilterRules:   { data: '{settings.inputFilterRules.list}' },
        upnpRules:          { data: '{settings.upnpSettings.upnpRules.list}' },
        wanInterfaces: {
            source: '{interfaces}',
            filters: [{ property: 'configType', value: 'ADDRESSED' }, { property: 'isWan', value: true }]
        },
    }
});
Ext.define('Ung.config.network.NetworkTest', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.config.networktest',
    alias: 'widget.networktest',
    layout: 'fit',
    dockedItems: [{
        xtype: 'toolbar',
        layout: 'fit',
        items: [{
            xtype: 'container',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                xtype: 'component',
                padding: '10 10 0 10',
                bind: {
                    html: '{description}'
                }
            }, {
                xtype: 'form',
                border: false,
                layout: {
                    type: 'hbox'
                },
                padding: 10,
                bodyStyle: {
                    background: 'transparent'
                },
                items: [{
                    xtype: 'button',
                    text: 'Run Test'.t(),
                    iconCls: 'fa fa-play',
                    margin: '0 0 0 10',
                    handler: 'runTest',
                    formBind: true
                }, {
                    xtype: 'component',
                    flex: 1
                }, {
                    xtype: 'button',
                    text: 'Clear Output'.t(),
                    iconCls: 'fa fa-eraser',
                    margin: '0 0 0 10',
                    handler: 'clearOutput'
                }]
            }]
        }]
    }],
    items: [{
        xtype: 'textarea',
        border: false,
        bind: {
            emptyText: '{emptyText}'
        },
        fieldStyle: {
            fontFamily: 'Courier, monospace',
            fontSize: '14px',
            background: '#1b1e26',
            color: 'lime'
        },
        // margin: 10,
        readOnly: true
    }]
});
Ext.define('Ung.config.network.view.Advanced', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.advanced',
    viewModel: true,
    title: 'Advanced'.t(),
    layout: 'fit',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<i class="fa fa-exclamation-triangle" style="color: red;"></i> '  + 'Advanced settings require careful configuration. Misconfiguration can compromise the proper operation and security of your server.'.t()
    }],
    items: [{
        xtype: 'tabpanel',
        // tabPosition: 'left',
        // tabRotation: 0,
        // tabStretchMax: true,
        items: [{
            title: 'Options'.t(),
            padding: 10,
            defaults: {
                xtype: 'checkbox',
                // labelWidth: 250,
                // labelAlign: 'right'
            },
            items: [{
                boxLabel: 'Enable SIP NAT Helper'.t(),
                bind: '{settings.enableSipNatHelper}'
            }, {
                boxLabel: 'Send ICMP Redirects'.t(),
                bind: '{settings.sendIcmpRedirects}'
            }, {
                boxLabel: 'Enable STP (Spanning Tree) on Bridges'.t(),
                bind: '{settings.stpEnabled}'
            }, {
                boxLabel: 'Enable Strict ARP mode'.t(),
                bind: '{settings.strictArpMode}'
            }, {
                boxLabel: 'DHCP Authoritative'.t(),
                bind: '{settings.dhcpAuthoritative}'
            }, {
                boxLabel: 'Block new sessions during network configuration'.t(),
                bind: '{settings.blockDuringRestarts}'
            }, {
                boxLabel: 'Block replay packets'.t(),
                bind: '{settings.blockReplayPackets}'
            }, {
                boxLabel: 'Log bypassed sessions'.t(),
                bind: '{settings.logBypassedSessions}'
            }, {
                boxLabel: 'Log local outbound sessions'.t(),
                bind: '{settings.logLocalOutboundSessions}'
            }, {
                boxLabel: 'Log local inbound sessions'.t(),
                bind: '{settings.logLocalInboundSessions}'
            }, {
                boxLabel: 'Log blocked sessions'.t(),
                bind: '{settings.logBlockedSessions}'
            }, {
                boxLabel: 'Log intermediate session updates'.t(),
                bind: '{settings.logSessionUpdates}'
            }]
        }, {
            title: 'QoS'.t(),
            layout: 'fit',
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'top',
                padding: '8 5',
                style: { fontSize: '12px' },
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: 'Enabled'.t(),
                    labelAlign: 'right',
                    bind: '{settings.qosSettings.qosEnabled}'
                }, {
                    xtype: 'combo',
                    fieldLabel: 'Default Priority'.t(),
                    labelAlign: 'right',
                    bind: {
                        store: '{qosPriorityNoDefaultStore}',
                        value: '{settings.qosSettings.defaultPriority}',
                        disabled: '{!settings.qosSettings.qosEnabled}'
                    },
                    queryMode: 'local',
                    editable: false
                }]
            }],
            items: [{
                xtype: 'tabpanel',
                // tabPosition: 'left',
                // tabRotation: 0,
                // tabStretchMax: false,
                layout: 'fit',
                disabled: true,
                bind: {
                    disabled: '{!settings.qosSettings.qosEnabled}'
                },
                items: [{
                    xtype: 'grid', // normal grid because uses a chained store
                    title: 'WAN Bandwidth'.t(),
                    // bodyPadding: 10,
                    tbar: [{
                        xtype: 'tbtext',
                        padding: '8 5',
                        style: { fontSize: '12px' },
                        html: Ext.String.format('{0}Note{1}: When enabling QoS valid Download Bandwidth and Upload Bandwidth limits must be set for all WAN interfaces.'.t(), '<font color="red">','</font>') + '<br/>'
                            // Ext.String.format('Total: {0} kbps ({1} Mbit) download, {2} kbps ({3} Mbit) upload'.t(), d, d_Mbit, u, u_Mbit )
                    }],
                    listProperty: 'settings.interfaces.list',
                    bind: '{wanInterfaces}',
                    selModel: {
                        type: 'cellmodel'
                    },
                    plugins: {
                        ptype: 'cellediting',
                        clicksToEdit: 1
                    },
                    header: false,
                    sortableColumns: false,
                    enableColumnHide: false,
                    columns: [{
                        header: 'Id'.t(),
                        width: 70,
                        align: 'right',
                        dataIndex: 'interfaceId'
                    }, {
                        header: 'WAN'.t(),
                        flex: 1,
                        dataIndex: 'name'
                    }, {
                        header: 'Config Type'.t(),
                        dataIndex: 'configType',
                        width: 150
                    }, {
                        header: 'Download Bandwidth'.t(),
                        dataIndex: 'downloadBandwidthKbps',
                        width: 250,
                        editor: {
                            xtype: 'numberfield',
                            allowBlank : true,
                            allowDecimals: false,
                            minValue: 0
                        },
                        renderer: function (value) {
                            return Ext.isEmpty(value) ? 'Not set'.t() : value + ' kbps' + ' (' + value/1000 + ' Mbit' + ')';
                        }
                    }, {
                        header: 'Upload Bandwidth'.t(),
                        dataIndex: 'uploadBandwidthKbps',
                        width: 250,
                        editor: {
                            xtype: 'numberfield',
                            allowBlank : true,
                            allowDecimals: false,
                            minValue: 0
                        },
                        renderer: function (value) {
                            return Ext.isEmpty(value) ? 'Not set'.t() : value + ' kbps' + ' (' + value/1000 + ' Mbit' + ')';
                        }
                    }]
                }, {
                    xtype: 'panel',
                    title: 'QoS Rules'.t(),
                    // bodyPadding: 10,
                    layout: {
                        type: 'vbox',
                        align: 'stretch'
                    },
                    items: [{
                        border: false,
                        defaults: {
                            xtype: 'combo',
                            labelAlign: 'top',
                            labelWidth: 120,
                            editable: false,
                            queryMode: 'local',
                            padding: 10,
                        },
                        layout: {
                            type: 'hbox',
                            align: 'stretch'
                        },
                        items: [{
                            fieldLabel: 'Ping Priority'.t(),
                            bind: {
                                store: '{qosPriorityStore}',
                                value: '{settings.qosSettings.pingPriority}'
                            }
                        }, {
                            fieldLabel: 'DNS Priority'.t(),
                            bind: {
                                store: '{qosPriorityStore}',
                                value: '{settings.qosSettings.dnsPriority}'
                            }
                        }, {
                            fieldLabel: 'SSH Priority'.t(),
                            bind: {
                                store: '{qosPriorityStore}',
                                value: '{settings.qosSettings.sshPriority}'
                            }
                        }, {
                            fieldLabel: 'OpenVPN Priority'.t(),
                            bind: {
                                store: '{qosPriorityStore}',
                                value: '{settings.qosSettings.openvpnPriority}'
                            }
                        }]
                    }, {
                        xtype: 'ungrid',
                        title: 'QoS Custom Rules'.t(),
                        border: false,
                        tbar: ['@add', '->', {
                            xtype: 'tbtext',
                            padding: '8 5',
                            style: { fontSize: '12px' },
                            html: Ext.String.format('{0}Note{1}: Custom Rules only match <b>Bypassed</b> traffic.'.t(), '<font color="red">','</font>')
                        }],
                        recordActions: ['@edit', '@delete', '@reorder'],
                        listProperty: 'settings.qosSettings.qosRules.list',
                        ruleJavaClass: 'com.untangle.uvm.network.QosRuleCondition',
                        emptyRow: {
                            ruleId: -1,
                            enabled: true,
                            description: '',
                            priority: 1,
                            javaClass: 'com.untangle.uvm.network.QosRule',
                            conditions: {
                                javaClass: 'java.util.LinkedList',
                                list: []
                            }
                        },
                        conditions: [
                            { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean' },
                            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype:'ipMatcher' },
                            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'portMatcher' },
                            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'], ['UDP','UDP']] },
                            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
                            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype:'ipMatcher' },
                            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'portMatcher' }
                        ],
                        label: 'Perform the following action(s):'.t(),
                        bind: '{qosRules}',
                        columns: [{
                            header: 'Rule Id'.t(),
                            width: 70,
                            align: 'right',
                            resizable: false,
                            dataIndex: 'ruleId',
                            renderer: function(value) {
                                return value < 0 ? 'new'.t() : value;
                            }
                        }, {
                            xtype: 'checkcolumn',
                            header: 'Enable'.t(),
                            dataIndex: 'enabled',
                            resizable: false,
                            width: 70
                        }, {
                            header: 'Description',
                            width: 200,
                            dataIndex: 'description',
                            renderer: function (value) {
                                return value || '<em>no description<em>';
                            }
                        }, {
                            header: 'Conditions'.t(),
                            flex: 1,
                            dataIndex: 'conditions',
                            renderer: 'conditionsRenderer'
                        }, {
                            header: 'Priority'.t(),
                            width: 100,
                            dataIndex: 'priority',
                            renderer: function (value) {
                                switch (value) {
                                    case 1: return 'Very High'.t();
                                    case 2: return 'High'.t();
                                    case 3: return 'Medium'.t();
                                    case 4: return 'Low'.t();
                                    case 5: return 'Limited'.t();
                                    case 6: return 'Limited More'.t();
                                    case 7: return 'Limited Severely'.t();
                                }
                            }
                        }],
                        editorFields: [
                            Fields.enableRule(),
                            Fields.description,
                            Fields.conditions,
                            Fields.priority
                        ]
                    }]
                }, {
                    xtype: 'grid',
                    title: 'QoS Priorities'.t(),
                    bind: '{qosPriorities}',
                    selModel: {
                        type: 'cellmodel'
                    },
                    plugins: {
                        ptype: 'cellediting',
                        clicksToEdit: 1
                    },
                    columnLines: true,
                    sortableColumns: false,
                    enableColumnHide: false,
                    columns: [{
                        header: 'Priority'.t(),
                        width: 150,
                        align: 'right',
                        dataIndex: 'priorityName',
                        renderer: function (value) {
                            return value.t();
                        }
                    }, {
                        header: 'Upload Reservation'.t(),
                        dataIndex: 'uploadReservation',
                        width: 150,
                        editor : {
                            xtype: 'numberfield',
                            allowBlank : false,
                            minValue : 0.1,
                            maxValue : 100
                        },
                        renderer: function (value, metadata, record) {
                            return value === 0 ? 'No reservation' : value + '%';
                        }
                    }, {
                        header: 'Upload Limit'.t(),
                        dataIndex: 'uploadLimit',
                        width: 150,
                        editor : {
                            xtype: 'numberfield',
                            allowBlank : false,
                            minValue : 0.1,
                            maxValue : 100
                        },
                        renderer: function (value, metadata, record) {
                            return value === 0 ? 'No reservation' : value + '%';
                        }
                    }, {
                        header: 'Download Reservation'.t(),
                        dataIndex: 'downloadReservation',
                        width: 150,
                        editor : {
                            xtype: 'numberfield',
                            allowBlank : false,
                            minValue : 0.1,
                            maxValue : 100
                        },
                        renderer: function (value, metadata, record) {
                            return value === 0 ? 'No reservation' : value + '%';
                        }
                    }, {
                        header: 'Download Limit'.t(),
                        dataIndex: 'downloadLimit',
                        width: 150,
                        editor : {
                            xtype: 'numberfield',
                            allowBlank : false,
                            minValue : 0.1,
                            maxValue : 100
                        },
                        renderer: function (value, metadata, record) {
                            return value === 0 ? 'No limit' : value + '%';
                        }
                    }, {
                        flex: 1
                    }]
                }, {
                    xtype: 'grid',
                    title: 'QoS Statistics'.t(),
                    groupField:'interface_name',
                    columnLines: true,
                    enableColumnHide: false,
                    tbar: [{
                        text: 'Refresh'.t(),
                        iconCls: 'fa fa-refresh',
                        handler: 'refreshQosStatistics'
                    }],
                    columns: [{
                        header: 'Interface'.t(),
                        width: 150,
                        dataIndex: 'interface_name',
                        renderer: function (value) {
                            return value.t();
                        }
                    }, {
                        header: 'Priority'.t(),
                        dataIndex: 'priority',
                        width: 150
                    }, {
                        header: 'Data'.t(),
                        dataIndex: 'sent',
                        width: 150,
                    }, {
                        flex: 1
                    }]
                }]
            }],
        }, {
            title: 'Filter Rules'.t(),
            layout: 'border',
            items: [{
                xtype: 'ungrid',
                region: 'center',
                title: 'Forward Filter Rules'.t(),
                tbar: ['@add'],
                recordActions: ['@edit', '@delete', '@reorder'],
                listProperty: 'settings.forwardFilterRules.list',
                ruleJavaClass: 'com.untangle.uvm.network.FilterRuleCondition',
                conditions: [
                    { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean' },
                    { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype:'ipMatcher' },
                    { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'portMatcher' },
                    { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
                    { name: 'SRC_MAC' , displayName: 'Source MAC'.t(), type: 'textfield' },
                    { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype: 'ipMatcher'},
                    { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield', vtype: 'portMatcher' },
                    { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
                    { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']] }
                ],
                emptyRow: {
                    ruleId: -1,
                    enabled: true,
                    ipvsEnabled: false,
                    description: '',
                    javaClass: 'com.untangle.uvm.network.FilterRule',
                    conditions: {
                        javaClass: 'java.util.LinkedList',
                        list: []
                    },
                    blocked: false
                },
                bind: '{forwardFilterRules}',
                columns: [{
                    header: 'Rule Id'.t(),
                    width: 70,
                    align: 'right',
                    resizable: false,
                    dataIndex: 'ruleId',
                    renderer: function (value) {
                        return value < 0 ? 'new'.t() : value;
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Enable'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: 70
                }, {
                    xtype: 'checkcolumn',
                    header: 'IPv6'.t(),
                    dataIndex: 'ipv6Enabled',
                    resizable: false,
                    width: 70
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        return value || '<em>no description<em>';
                    }
                }, {
                    header: 'Conditions'.t(),
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    xtype: 'checkcolumn',
                    header: 'Block'.t(),
                    dataIndex: 'blocked',
                    resizable: false,
                    width: 70
                }],
                editorFields: [
                    Fields.enableRule('Enable Forward Filter Rule'.t()),
                    Fields.enableIpv6,
                    Fields.description,
                    Fields.conditions,
                    Fields.blocked
                ]
            }, {
                xtype: 'ungrid',
                region: 'south',
                height: '70%',
                split: true,
                title: 'Input Filter Rules'.t(),
                tbar: ['@add'],
                recordActions: ['@edit', '@delete', '@reorder'],
                listProperty: 'settings.inputFilterRules.list',
                ruleJavaClass: 'com.untangle.uvm.network.FilterRuleCondition',
                conditions: [
                    { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean' },
                    { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype:'ipMatcher' },
                    { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'portMatcher' },
                    { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
                    { name: 'SRC_MAC' , displayName: 'Source MAC'.t(), type: 'textfield' },
                    { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype: 'ipMatcher'},
                    { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield', vtype: 'portMatcher' },
                    { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
                    { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']] }
                ],
                emptyRow: {
                    ruleId: -1,
                    enabled: true,
                    ipvsEnabled: false,
                    description: '',
                    javaClass: 'com.untangle.uvm.network.FilterRule',
                    conditions: {
                        javaClass: 'java.util.LinkedList',
                        list: []
                    },
                    blocked: false,
                    readOnly: false
                },
                bind: '{inputFilterRules}',
                columns: [{
                    header: 'Rule Id'.t(),
                    width: 70,
                    align: 'right',
                    resizable: false,
                    dataIndex: 'ruleId',
                    renderer: function (value) {
                        return value < 0 ? 'new'.t() : value;
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Enable'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: 70
                }, {
                    xtype: 'checkcolumn',
                    header: 'IPv6'.t(),
                    dataIndex: 'ipv6Enabled',
                    resizable: false,
                    width: 70
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        return value || '<em>no description<em>';
                    }
                }, {
                    header: 'Conditions'.t(),
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    xtype: 'checkcolumn',
                    header: 'Block'.t(),
                    dataIndex: 'blocked',
                    resizable: false,
                    width: 70,
                    listeners: {
                        beforecheckchange: function (col, rowIndex, checked, record) {
                            if (record.get('readOnly')) {
                                Ext.MessageBox.alert('Info', '<strong>' + record.get('description') + '</strong> connot be edited!');
                                return false;
                            }
                        }
                    }
                }],
                editorFields: [
                    Fields.enableRule('Enable Input Filter Rule'.t()),
                    Fields.enableIpv6,
                    Fields.description,
                    Fields.conditions,
                    Fields.blocked
                ]
            }]
        }, {
            title: 'UPnP'.t(),
            layout: 'border',
            dockedItems: [{
                xtype: 'toolbar',
                dock: 'top',
                items: [{
                    xtype: 'checkbox',
                    fieldLabel: 'Enabled'.t(),
                    labelAlign: 'right',
                    bind: '{settings.upnpSettings.upnpEnabled}'
                }, {
                    xtype: 'checkbox',
                    fieldLabel: 'Secure Mode'.t(),
                    labelAlign: 'right',
                    bind: {
                        value: '{settings.upnpSettings.secureMode}',
                        disabled: '{!settings.upnpSettings.upnpEnabled}'
                    }
                }]
            }],
            items: [{
                xtype: 'grid',
                region: 'center',
                title: 'Status'.t(),
                enableColumnHide: false,
                enableSorting: false,
                tbar: [{
                    text: 'Refresh'.t(),
                    iconCls: 'fa fa-refresh',
                    // handler: 'refreshUpnpStatus'
                }],
                disabled: true,
                bind: {
                    disabled: '{!settings.upnpSettings.upnpEnabled}'
                },
                columns: [{
                    header: 'Protocol'.t(),
                    width: 100,
                    dataIndex: 'upnp_protocol'
                }, {
                    header: 'Client IP Address'.t(),
                    width: 150,
                    dataIndex: 'upnp_client_ip_address'
                }, {
                    header: 'Client Port'.t(),
                    width: 150,
                    dataIndex: 'upnp_client_port'
                }, {
                    header: 'Destination Port'.t(),
                    width: 150,
                    dataIndex: 'upnp_destination_port'
                }, {
                    header: 'Bytes'.t(),
                    width: 150,
                    dataIndex: 'bytes'
                    // renderer ????
                }]
            }, {
                xtype: 'ungrid',
                region: 'south',
                height: '50%',
                split: true,
                title: 'Access Control Rules'.t(),
                tbar: ['@add'],
                recordActions: ['@edit', '@delete', '@reorder'],
                listProperty: 'settings.upnpSettings.upnpRules.list',
                ruleJavaClass: 'com.untangle.uvm.network.UpnpRuleCondition',
                conditions: [
                    { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: "textfield", vtype: 'portMatcher' },
                    { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: "textfield", vtype: 'ipMatcher' },
                    { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: "textfield", vtype: 'portMatcher' }
                ],
                label: 'Perform the following action(s):'.t(),
                emptyRow: {
                    ruleId: -1,
                    enabled: true,
                    description: '',
                    javaClass: 'com.untangle.uvm.network.UpnpRule',
                    conditions: {
                        javaClass: 'java.util.LinkedList',
                        list: []
                    },
                    priority: 1,
                    allow: true
                },
                disabled: true,
                bind: {
                    store: '{upnpRules}',
                    disabled: '{!settings.upnpSettings.upnpEnabled}'
                },
                columns: [{
                    header: 'Rule Id'.t(),
                    width: 70,
                    align: 'right',
                    resizable: false,
                    dataIndex: 'ruleId',
                    renderer: function(value) {
                        return value < 0 ? 'new'.t() : value;
                    }
                }, {
                    xtype: 'checkcolumn',
                    header: 'Enable'.t(),
                    dataIndex: 'enabled',
                    resizable: false,
                    width: 70
                }, {
                    header: 'Description',
                    width: 200,
                    dataIndex: 'description',
                    renderer: function (value) {
                        return value || '<em>no description<em>';
                    }
                }, {
                    header: 'Conditions'.t(),
                    flex: 1,
                    dataIndex: 'conditions',
                    renderer: 'conditionsRenderer'
                }, {
                    header: 'Action'.t(),
                    width: 100,
                    dataIndex: 'allow',
                    renderer: function (value) {
                        return value ? 'Allow'.t() : 'Deny'.t();
                    }
                }],
                editorFields: [
                    Fields.enableRule(),
                    Fields.description,
                    Fields.conditions,
                    Fields.allow
                ]
            }]
        }, {
            title: 'DNS & DHCP'.t(),
            xtype: 'panel',
            tbar: [{
                xtype: 'tbtext',
                padding: '8 5',
                style: { fontSize: '12px' },
                html: '<strong>' + 'Custom dnsmasq options.'.t() + '</strong> <br/>' +
                      '<i class="fa fa-exclamation-triangle" style="color: red;"></i> ' + 'Warning: Invalid syntax will halt all DHCP & DNS services.'.t()
            }],
            layout: 'fit',
            items: [{
                xtype: 'textarea',
                margin: 10,
                fieldStyle: {
                    fontFamily: 'monospace'
                },
                bind: '{settings.dnsmasqOptions}'
            }]
        }, {
            xtype: 'grid',
            title: 'Network Cards'.t(),
            bind: '{devices}',
            selModel: {
                type: 'cellmodel'
            },
            plugins: {
                ptype: 'cellediting',
                clicksToEdit: 1
            },
            columns: [{
                header: 'Device Name'.t(),
                width: 250,
                dataIndex: 'deviceName'
            }, {
                header: 'MTU'.t(),
                width: 100,
                dataIndex: 'mtu',
                renderer: function (value) {
                    return value || 'Auto'.t();
                },
                editor: {
                    xtype: 'numberfield'
                }
            }, {
                header: 'Ethernet Media'.t(),
                dataIndex: 'duplex',
                width: 250,
                renderer: function (value) {
                    switch (value) {
                        case 'AUTO': return 'Auto'.t();
                        case 'M10000_FULL_DUPLEX': return '10000 Mbps, Full Duplex'.t();
                        case 'M10000_HALF_DUPLEX': return '10000 Mbps, Half Duplex'.t();
                        case 'M1000_FULL_DUPLEX': return '1000 Mbps, Full Duplex'.t();
                        case 'M1000_HALF_DUPLEX': return '1000 Mbps, Half Duplex'.t();
                        case 'M100_FULL_DUPLEX': return '100 Mbps, Full Duplex'.t();
                        case 'M100_HALF_DUPLEX': return '100 Mbps, Half Duplex'.t();
                        case 'M10_FULL_DUPLEX': return '10 Mbps, Full Duplex'.t();
                        case 'M10_HALF_DUPLEX': return '10 Mbps, Half Duplex'.t();
                    }
                },
                editor: {
                    xtype: 'combo',
                    store: [
                        ['AUTO', 'Auto'.t()],
                        ['M10000_FULL_DUPLEX', '10000 Mbps, Full Duplex'.t()],
                        ['M10000_HALF_DUPLEX', '10000 Mbps, Half Duplex'.t()],
                        ['M1000_FULL_DUPLEX', '1000 Mbps, Full Duplex'.t()],
                        ['M1000_HALF_DUPLEX', '1000 Mbps, Half Duplex'.t()],
                        ['M100_FULL_DUPLEX', '100 Mbps, Full Duplex'.t()],
                        ['M100_HALF_DUPLEX', '100 Mbps, Half Duplex'.t()],
                        ['M10_FULL_DUPLEX', '10 Mbps, Full Duplex'.t()],
                        ['M10_HALF_DUPLEX', '10 Mbps, Half Duplex'.t()]
                    ],
                    queryMode: 'local',
                    editable: false
                }
            }, {
                flex: 1
            }]
        }]
    }]
});
Ext.define('Ung.config.network.view.BypassRules', {
    extend: 'Ext.panel.Panel',
    // xtype: 'ung.config.network.bypassrules',
    alias: 'widget.config.network.bypassrules',
    viewModel: true,
    requires: [
        // 'Ung.config.network.ConditionWidget',
        // 'Ung.config.network.CondWidget'
    ],
    title: 'Bypass Rules'.t(),
    layout: 'fit',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'Bypass Rules control what traffic is scanned by the applications. Bypassed traffic skips application processing. The rules are evaluated in order. Sessions that meet no rule are not bypassed.'.t()
    }],
    items: [{
        xtype: 'ungrid',
        flex: 3,
        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],
        listProperty: 'settings.bypassRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.BypassRuleCondition',
        conditions: [
            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype:'ipMatcher' },
            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'portMatcher' },
            { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype:'ipMatcher' },
            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'portMatcher' },
            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP']] }
        ],
        label: 'Perform the following action(s):'.t(),
        description: "NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.".t(),
        emptyRow: {
            ruleId: -1,
            enabled: true,
            bypass: true,
            javaClass: 'com.untangle.uvm.network.BypassRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            description: ''
        },
        bind: '{bypassRules}',
        columns: [{
            header: 'Rule Id'.t(),
            width: 70,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function(value) {
                return value < 0 ? 'new'.t() : value;
            }
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: 70
        }, {
            header: 'Description',
            width: 200,
            dataIndex: 'description',
            renderer: function (value) {
                return value || '<em>no description<em>';
            }
        }, {
            header: 'Conditions'.t(),
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        }, {
            header: 'Bypass'.t(),
            xtype: 'checkcolumn',
            dataIndex: 'bypass',
            width: 100
        }],
        editorFields: [
            Fields.enableRule(),
            Fields.description,
            Fields.conditions,
            Fields.bypass
        ]
    }]
});
Ext.define('Ung.config.network.view.DhcpServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.dhcpserver',
    viewModel: true,
    title: 'DHCP Server'.t(),
    layout: 'border',
    items: [{
        xtype: 'ungrid',
        region: 'center',
        title: 'Static DHCP Entries'.t(),
        tbar: ['@add'],
        recordActions: ['@delete'],
        listProperty: 'settings.staticDhcpEntries.list',
        emptyRow: {
            macAddress: '11:22:33:44:55:66',
            address: '1.2.3.4',
            javaClass: 'com.untangle.uvm.network.DhcpStaticEntry',
            description: '[no description]'.t()
        },
        bind: '{staticDhcpEntries}',
        columns: [{
            header: 'MAC Address'.t(),
            dataIndex: 'macAddress',
            width: 200
        }, {
            header: 'Address'.t(),
            width: 200,
            dataIndex: 'address'
        }, {
            header: 'Description'.t(),
            flex: 1,
            dataIndex: 'description'
        }],
        editorFields: [
            Fields.macAddress,
            Fields.ipAddress,
            Fields.description
        ]
    }, {
        xtype: 'grid',
        title: 'Current DHCP Leases'.t(),
        region: 'south',
        height: '50%',
        split: true,
        tbar: [{
            text: 'Refresh'.t(),
            iconCls: 'fa fa-refresh',
            // handler: 'refreshDhcpLeases'
        }],
        columns: [{
            header: 'MAC Address'.t(),
            dataIndex:'macAddress',
            width: 150
        },{
            header: 'Address'.t(),
            dataIndex:'address',
            width: 200
        },{
            header: 'Hostname'.t(),
            dataIndex:'hostname',
            width: 200
        },{
            header: 'Expiration Time'.t(),
            dataIndex:'date',
            width: 180,
            // renderer: function(value) { return i18n.timestampFormat(value*1000); }
        }, {
            xtype: 'actioncolumn',
            header: 'Add Static'.t(),
            iconCls: 'fa fa-plus',
            handler: function () {
                alert('to add');
            }
        }]
    }]
});
Ext.define('Ung.config.network.view.DnsServer', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.dnsserver',
    viewModel: true,
    title: 'DNS Server'.t(),
    layout: 'border',
    items: [{
        xtype: 'ungrid',
        region: 'center',
        title: 'Static DNS Entries'.t(),
        tbar: ['@add'],
        recordActions: ['@delete'],
        listProperty: 'settings.dnsSettings.staticEntries.list',
        emptyRow: {
            name: '[no name]'.t(),
            address: '1.2.3.4',
            javaClass: 'com.untangle.uvm.network.DnsStaticEntry'
        },
        bind: '{staticDnsEntries}',
        columns: [{
            header: 'Name'.t(),
            dataIndex: 'name',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                bind: '{record.name}',
                emptyText: '[enter name]'.t()
            }
        }, {
            header: 'Address'.t(),
            width: 200,
            dataIndex: 'address',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter address]'.t(),
                bind: '{record.address}',
                allowBlank: false,
                vtype: 'ipAddress',
            }
        }]
    }, {
        xtype: 'ungrid',
        region: 'south',
        height: '50%',
        split: true,
        title: 'Domain DNS Servers'.t(),
        tbar: ['@add'],
        recordActions: ['@delete'],
        listProperty: 'settings.dnsSettings.localServers.list',
        emptyRow: {
            domain: '[no domain]'.t(),
            localServer: '1.2.3.4',
            javaClass: 'com.untangle.uvm.network.DnsLocalServer'
        },
        bind: '{localServers}',
        columns: [{
            header: 'Domain'.t(),
            dataIndex: 'domain',
            flex: 1,
            editor: {
                xtype: 'textfield',
                allowBlank: false,
                emptyText: '[enter domain]'.t(),
                bind: '{record.domain}',
            }
        }, {
            header: 'Server'.t(),
            width: 200,
            dataIndex: 'localServer',
            editor: {
                xtype: 'textfield',
                emptyText: '[enter DNS server]'.t(),
                allowBlank: false,
                bind: '{record.localServer}',
                vtype: 'ipAddress',
            }
        }],
    }]
});
Ext.define('Ung.config.network.view.Hostname', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config.network.hostname',
    withValidation: true, // requires validation on save
    viewModel: true,
    title: 'Hostname'.t(),
    bodyPadding: 10,
    scrollable: true,
    items: [{
        xtype: 'fieldset',
        title: 'Hostname'.t(),
        padding: 10,
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
                xtype: 'displayfield',
                value: '(eg: gateway)'.t(),
                margin: '0 0 0 5'
                // cls: 'boxlabel'
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
                xtype: 'displayfield',
                value: '(eg: example.com)'.t(),
                margin: '0 0 0 5'
                // cls: 'boxlabel'
            }]
        }]
    }, {
        xtype: 'fieldset',
        title: 'Dynamic DNS Service Configuration'.t(),
        checkboxToggle: true,
        collapsible: true,
        collapsed: true,
        padding: 10,
        checkbox: {
            bind: {
                value: '{settings.dynamicDnsServiceEnabled}'
            }
        },
        defaults: {
            labelAlign: 'right'
        },
        items: [{
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
            boxLabel: '<strong>' + 'Use IP address from External interface (default)'.t() + '</strong>',
            name: 'publicUrl',
            inputValue: 'external'
        }, {
            xtype: 'component',
            margin: '0 0 10 25',
            html: Ext.String.format('This works if your {0} Server has a routable public static IP address.'.t(), rpc.companyName)
        }, {
            xtype: 'radio',
            boxLabel: '<strong>' + 'Use Hostname'.t() + '</strong>',
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
            boxLabel: '<strong>' + 'Use Manually Specified Address'.t() + '</strong>',
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
Ext.define('Ung.config.network.view.Interfaces', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.interfaces', //..
    title: 'Interfaces'.t(),
    layout: 'border',
    itemId: 'interfaces',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Interface configuration'.t() + '</strong> <br/>' +  "Use this page to configure each interface's configuration and its mapping to a physical network card.".t()
    }],
    actions: {
        refresh: {
            xtype: 'button',
            iconCls: 'fa fa-refresh',
            text: 'Refresh'.t(),
            handler: 'loadSettings'
        }
    },
    items: [{
        xtype: 'grid',
        itemId: 'interfacesGrid',
        reference: 'interfacesGrid',
        region: 'center',
        border: false,
        tbar: ['@refresh'],
        layout: 'fit',
        // forceFit: true,
        // viewConfig: {
        //     plugins: {
        //         ptype: 'gridviewdragdrop',
        //         dragText: 'Drag and drop to reorganize'.t(),
        //         // allow drag only from drag column icons
        //         dragZone: {
        //             onBeforeDrag: function (data, e) {
        //                 return Ext.get(e.target).hasCls('fa-arrows');
        //             }
        //         }
        //     }
        // },
        // title: 'Interfaces'.t(),
        bind: '{interfaces}',
        // fields: [{
        //     name: 'v4Address'
        // }],
        columns: [
        // {
        //     xtype: 'gridcolumn',
        //     header: '<i class="fa fa-sort"></i>',
        //     align: 'center',
        //     width: 30,
        //     tdCls: 'action-cell',
        //     // iconCls: 'fa fa-arrows'
        //     renderer: function() {
        //         return '<i class="fa fa-arrows" style="cursor: move;"></i>';
        //     },
        // },
        {
            header: 'Id'.t(),
            dataIndex: 'interfaceId',
            width: 50,
            align: 'right'
        }, {
            header: 'Name'.t(),
            dataIndex: 'name',
            minWidth: 200
        }, {
            header: 'Connected'.t(),
            dataIndex: 'connected',
            width: 130,
            renderer: function (value) {
                switch (value) {
                    case 'CONNECTED': return 'connected'.t();
                    case 'DISCONNECTED': return 'disconnected'.t();
                    case 'MISSING': return 'missing'.t();
                    default: 'unknown'.t();
                }
            }
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
            width: 100,
            renderer: function (value) {
                switch (value) {
                    case 'FULL_DUPLEX': return 'full-duplex'.t();
                    case 'HALF_DUPLEX': return 'half-duplex'.t();
                    default: return 'unknown'.t();
                }
            }
        }, {
            header: 'Config'.t(),
            dataIndex: 'configType',
            width: 100,
            renderer: function (value) {
                switch (value) {
                    case 'ADDRESSED': return 'Addressed'.t();
                    case 'BRIDGED': return 'Bridged'.t();
                    case 'DISABLED': return 'Disabled'.t();
                    default: value.t();
                }
            }
        }, {
            header: 'Current Address'.t(),
            dataIndex: 'v4Address',
            width: 150,
            renderer: function (value, metaData, record) {
                return Ext.isEmpty(value) ? '' : value + ' / ' + record.get('v4PrefixLength');
            }
        }, {
            header: 'is WAN'.t(),
            dataIndex: 'isWan',
            align: 'center',
            renderer: function (value, metaData, record) {
                return (record.get('configType') === 'ADDRESSED') ? (value ? '<i class="fa fa-check fa-lg"></i>' : '<i class="fa fa-minus fa-lg"></i>') : '<i class="fa fa-minus fa-lg"></i>'; // if its addressed return value
            }
        }, {
            header: 'is Vlan'.t(),
            dataIndex: 'isVlanInterface',
            align: 'center',
            renderer: function (value) {
                return value ? '<i class="fa fa-check fa-lg"></i>' : '<i class="fa fa-minus fa-lg"></i>';
            }
        }, {
            header: 'MAC Address'.t(),
            width: 160,
            dataIndex: 'macAddress'
        }, {
            header: 'Vendor'.t(),
            width: 160,
            dataIndex: 'vendor'
        }]
    }, {
        xtype: 'panel',
        region: 'east',
        split: 'true',
        collapsible: false,
        width: 350,
        maxWidth: 450,
        hidden: true,
        layout: 'border',
        bind: {
            title: '{si.name} ({si.physicalDev})',
            hidden: '{!si}',
            // activeItem: '{activePropsItem}'
        },
        tbar: [{
            bind: {
                text: '<strong>' + 'Edit'.t() + ' {si.name} ({si.physicalDev})' + '</strong>',
                iconCls: 'fa fa-pencil',
                scale: 'large',
                width: '100%',
                handler: 'editInterface'
            }
        }],
        items: [{
            title: 'Status'.t(),
            region: 'center',
            border: false,
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
            region: 'south',
            height: '30%',
            split: true,
            itemId: 'interfaceArp',
            title: 'ARP Entry List'.t(),
            forceFit: true,
            bind: '{interfaceArp}',
            columns: [{
                header: 'MAC Address'.t(),
                dataIndex: 'macAddress'
            }, {
                header: 'IP Address'.t(),
                dataIndex: 'address'
            }, {
                header: 'Type'.t(),
                dataIndex: 'type'
            }]
        }]
    }]
});
Ext.define('Ung.config.network.view.NatRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.natrules',
    viewModel: true,
    requires: [
        // 'Ung.config.network.ConditionWidget',
        // 'Ung.config.network.CondWidget'
    ],
    title: 'NAT Rules'.t(),
    layout: 'fit',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.'.t()
    }],
    items: [{
        xtype: 'ungrid',
        flex: 3,
        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],
        listProperty: 'settings.natRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.NatRuleCondition',
        conditions: [
            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype: 'ipMatcher' },
            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype: 'portMatcher' },
            { name: 'DST_INTF', displayName: 'Destination Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype:'ipMatcher'},
            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'textfield', vtype:'portMatcher' },
            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']]}
        ],
        description: "NAT Rules control the rewriting of the IP source address of traffic (Network Address Translation). The rules are evaluated in order.".t(),
        emptyRow: {
            ruleId: -1,
            enabled: true,
            auto: true,
            javaClass: 'com.untangle.uvm.network.NatRule',
            conditions: {
                javaClass: 'java.util.LinkedList',
                list: []
            },
            description: ''
        },
        bind: '{natRules}',
        columns: [{
            header: 'Rule Id'.t(),
            width: 70,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function(value) {
                return value < 0 ? 'new'.t() : value;
            }
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: 70
        }, {
            header: 'Description',
            width: 200,
            dataIndex: 'description',
            renderer: function (value) {
                return value || '<em>no description<em>';
            }
        }, {
            header: 'Conditions'.t(),
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        }, {
            header: 'NAT Type'.t(),
            dataIndex: 'auto',
            width: 100,
            renderer: function (val) {
                return val ? 'Auto'.t() : 'Custom'.t();
            }
        }, {
            header: 'New Source'.t(),
            dataIndex: 'newSource',
            width: 120,
            renderer: function (value, metaData, record) {
                return record.get('auto') ? '' : value;
            }
        }],
        editorFields: [
            Fields.enableRule('Enable NAT Rule'.t()),
            Fields.description,
            Fields.conditions,
            Fields.natType,
            Fields.natSource
        ]
    }]
});
Ext.define('Ung.config.network.view.PortForwardRules', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.portforwardrules',
    viewModel: true,
    requires: [
        // 'Ung.config.network.ConditionWidget',
        // 'Ung.config.network.CondWidget'
    ],
    title: 'Port Forward Rules'.t(),
    layout: { type: 'vbox', align: 'stretch' },
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: "Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.".t()
    }],
    items: [{
        xtype: 'ungrid',
        flex: 3,
        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],
        listProperty: 'settings.portForwardRules.list',
        ruleJavaClass: 'com.untangle.uvm.network.PortForwardRuleCondition',
        conditions: [
            { name: 'DST_LOCAL', displayName: 'Destined Local'.t(), type: 'boolean' },
            { name: 'DST_ADDR', displayName: 'Destination Address'.t(), type: 'textfield', vtype:'ipMatcher' },
            { name: 'DST_PORT', displayName: 'Destination Port'.t(), type: 'textfield', vtype:'portMatcher' },
            { name: 'SRC_ADDR', displayName: 'Source Address'.t(), type: 'textfield', vtype:'ipMatcher' },
            { name: 'SRC_PORT', displayName: 'Source Port'.t(), type: 'numberfield', vtype:'portMatcher' },
            { name: 'SRC_INTF', displayName: 'Source Interface'.t(), type: 'checkboxgroup', values: Ung.Util.getInterfaceList(true, true) },
            { name: 'PROTOCOL', displayName: 'Protocol'.t(), type: 'checkboxgroup', values: [['TCP','TCP'],['UDP','UDP'],['ICMP','ICMP'],['GRE','GRE'],['ESP','ESP'],['AH','AH'],['SCTP','SCTP']] }
        ],
        actionDescription: 'Forward to the following location:'.t(),
        description: "Port Forward rules forward sessions matching the configured criteria from a public IP to an IP on an internal (NAT'd) network. The rules are evaluated in order.".t(),
        emptyRow: {
            ruleId: -1,
            simple: true,
            enabled: true,
            // description: '',
            javaClass: 'com.untangle.uvm.network.PortForwardRule',
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
        bind: '{portForwardRules}',
        columns: [{
            header: 'Rule Id'.t(),
            width: 70,
            align: 'right',
            resizable: false,
            dataIndex: 'ruleId',
            renderer: function(value) {
                return value < 0 ? 'new'.t() : value;
            }
        }, {
            xtype: 'checkcolumn',
            header: 'Enable'.t(),
            dataIndex: 'enabled',
            resizable: false,
            width: 70
        }, {
            header: 'Description',
            width: 200,
            dataIndex: 'description',
            renderer: function (value) {
                return value || '<em>no description<em>';
            }
        }, {
            header: 'Conditions'.t(),
            flex: 1,
            dataIndex: 'conditions',
            renderer: 'conditionsRenderer'
        }, {
            header: 'New Destination'.t(),
            dataIndex: 'newDestination',
            width: 150
        }, {
            header: 'New Port'.t(),
            dataIndex: 'newPort',
            width: 80
        }],
        editorFields: [
            Fields.enableRule('Enable Port Forward Rule'.t()),
            Fields.description,
            Fields.conditions,
            Fields.newDestination,
            Fields.newPort
        ]
    }, {
        xtype: 'fieldset',
        flex: 2,
        margin: 10,
        padding: 10,
        // border: true,
        collapsible: false,
        collapsed: false,
        autoScroll: true,
        style: {
            lineHeight: 1.4
        },
        title: 'The following ports are currently reserved and can not be forwarded:'.t(),
        items: [{
            xtype: 'component',
            // name: 'portForwardWarnings',
            bind: {
                html: '{portForwardWarnings}'
            }
        }]
    }]
});
Ext.define('Ung.config.network.view.Routes', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.routes',
    viewModel: true,
    title: 'Routes'.t(),
    layout: 'border',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: 'Static Routes are global routes that control how traffic is routed by destination address. The most specific Static Route is taken for a particular packet, order is not important.'.t()
    }],
    items: [{
        xtype: 'ungrid',
        region: 'center',
        title: 'Static Routes'.t(),
        tbar: ['@add'],
        recordActions: ['@edit', '@delete', '@reorder'],
        listProperty: 'settings.staticRoutes.list',
        emptyRow: {
            ruleId: -1,
            network: '',
            prefix: 24,
            nextHop: '4.3.2.1',
            javaClass: 'com.untangle.uvm.network.StaticRoute',
            description: ''
        },
        bind: '{staticRoutes}',
        columns: [{
            header: 'Description'.t(),
            dataIndex: 'description',
            flex: 1
        }, {
            header: 'Network'.t(),
            width: 170,
            dataIndex: 'network'
        }, {
            header: 'Netmask/Prefix'.t(),
            width: 170,
            dataIndex: 'prefix'
        }, {
            header: 'Next Hop'.t(),
            width: 300,
            dataIndex: 'nextHop',
            renderer: function (value) {
                return value || '<em>no description<em>';
            }
        }],
        editorFields: [
            Fields.description,
            Fields.network,
            Fields.netMask, {
                xtype: 'combo',
                fieldLabel: 'Next Hop'.t(),
                bind: '{record.nextHop}',
                // store: Ung.Util.getV4NetmaskList(false),
                queryMode: 'local',
                allowBlank: false,
                editable: true
            }, {
                xtype: 'component',
                margin: '10 0 0 20',
                html: 'If <b>Next Hop</b> is an IP address that network will be routed via the specified IP address.'.t() + '<br/>' +
                    'If <b>Next Hop</b> is an interface that network will be routed <b>locally</b> on that interface.'.t()
            }
        ]
    }, {
        xtype: 'panel',
        title: 'Current Routes'.t(),
        region: 'south',
        height: '50%',
        split: true,
        border: false,
        tbar: [{
            xtype: 'tbtext',
            padding: '8 5',
            style: { fontSize: '12px' },
            value: "Current Routes shows the current routing system's configuration and how all traffic will be routed.".t()
        }, '->', {
            text: 'Refresh Routes'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'refreshRoutes'
        }],
        layout: 'fit',
        items: [{
            xtype: 'textarea',
            itemId: 'currentRoutes',
            border: false,
            fieldStyle: {
                fontFamily: 'monospace'
            },
            // name: "state",
            hideLabel: true,
            labelSeperator: '',
            readOnly: true,
            // autoCreate: { tag: 'textarea', autocomplete: 'off', spellcheck: 'false' },
            // height: 200,
            // width: "100%",
            // isDirty: function() { return false; }
        }]
    }]
});
Ext.define('Ung.config.network.view.Services', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config.network.services',
    withValidation: true, // requires validation on save
    viewModel: true,
    title: 'Services'.t(),
    bodyPadding: 10,
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Local Services'.t() + '</strong>'
    }],
    defaults: {
        allowDecimals: false,
        minValue: 0,
        allowBlank: false,
        vtype: 'port',
        labelAlign: 'right'
    },
    items: [{
        xtype: 'component',
        html: '<br/>' + 'The specified HTTPS port will be forwarded from all interfaces to the local HTTPS server to provide administration and other services.'.t() + '<br/>',
        margin: '0 0 10 0'
    }, {
        xtype: 'numberfield',
        fieldLabel: 'HTTPS port'.t(),
        width: 200,
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
        width: 200,
        name: 'httpPort',
        bind: '{settings.httpPort}',
        blankText: 'You must provide a valid port.'.t(),
    }]
});
Ext.define('Ung.config.network.view.Troubleshooting', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.troubleshooting',
    title: 'Troubleshooting'.t(),
    layout: 'fit',
    tbar: [{
        xtype: 'tbtext',
        padding: '8 5',
        style: { fontSize: '12px' },
        html: '<strong>' + 'Network Tests'.t() + '</strong>'
    }],
    items: [{
        xtype: 'tabpanel',
        items: [{
            xtype: 'networktest',
            title: 'Connectivity Test'.t(),
            viewModel: {
                data: {
                    description: 'The <b>Connectivity Test</b> verifies a working connection to the Internet.'.t(),
                    emptyText: 'Connectivity Test Output'.t(),
                    command: [
                        '/bin/bash',
                        '-c',
                        ['echo -n "Testing DNS ... " ; success="Successful";',
                        'dig updates.untangle.com > /dev/null 2>&1; if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                        'echo -n "Testing TCP Connectivity ... ";',
                        'echo "GET /" | netcat -q 0 -w 15 updates.untangle.com 80 > /dev/null 2>&1;',
                        'if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                        'echo "Test ${success}!"'].join('')
                    ]
                }
            }
        }, {
            xtype: 'networktest',
            title: 'Ping Test'.t(),
            commandFields: [{
                xtype: 'textfield',
                width: 200,
                emptyText : 'IP Address or Hostname'.t(),
                allowBlank: false,
                bind: '{destination}'
            }],
            viewModel: {
                data: {
                    description: 'The <b>Ping Test</b> can be used to test that a particular host or client can be pinged'.t(),
                    emptyText: 'Ping Test Output'.t(),
                    destination: null
                },
                formulas: {
                    command: function (get) {
                        return 'ping -c 5 ' + get('destination');
                    }
                }
            }
        }, {
            xtype: 'networktest',
            title: 'DNS Test'.t(),
            commandFields: [{
                xtype: 'textfield',
                width: 200,
                emptyText : 'Hostname'.t(),
                allowBlank: false,
                bind: '{destination}'
            }],
            viewModel: {
                data: {
                    description: 'The <b>DNS Test</b> can be used to test DNS lookups'.t(),
                    emptyText: 'DNS Test Output'.t(),
                    destination: null
                },
                formulas: {
                    command: function (get) {
                        return [
                            '/bin/bash',
                            '-c',
                            ['host \'' + get('destination') +'\';',
                            'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'].join('')
                        ];
                    }
                }
            }
        }, {
            xtype: 'networktest',
            title: 'Connection Test'.t(),
            commandFields: [{
                xtype: 'textfield',
                width: 200,
                emptyText : 'IP Address or Hostname'.t(),
                allowBlank: false,
                bind: '{destination}'
            }, {
                xtype: 'numberfield',
                minValue : 1,
                maxValue : 65536,
                width: 80,
                emptyText: 'Port'.t(),
                allowBlank: false,
                bind: '{port}'
            }],
            viewModel: {
                data: {
                    description: 'The <b>Connection Test</b> verifies that Untangle can open a TCP connection to a port on the given host or client.'.t(),
                    emptyText: 'Connection Test Output'.t(),
                    destination: null,
                    port: null
                },
                formulas: {
                    command: function (get) {
                        return [
                            '/bin/bash',
                            '-c',
                            ['echo 1 | netcat -q 0 -v -w 15 \'' + get('destination') + '\' \'' + get('port') +'\';',
                            'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'].join('')
                        ];
                    }
                }
            }
        }, {
            xtype: 'networktest',
            title: 'Traceroute Test'.t(),
            commandFields: [{
                xtype: 'textfield',
                width: 200,
                emptyText : 'IP Address or Hostname'.t(),
                allowBlank: false,
                bind: '{destination}'
            }, {
                xtype: 'combo',
                editable: false,
                width: 100,
                store: [['U','UDP'], ['T','TCP'], ['I','ICMP']],
                bind: '{protocol}'
            }],
            viewModel: {
                data: {
                    description: 'The <b>Traceroute Test</b> traces the route to a given host or client.'.t(),
                    emptyText: 'Traceroute Test Output'.t(),
                    destination: '',
                    protocol: 'U'
                },
                formulas: {
                    command: function (get) {
                        return [
                            '/bin/bash',
                            '-c',
                            ['traceroute' + ' -' + get('protocol') + ' ' + get('destination') + ';',
                            'if [ "$?" = "0" ]; then echo "Test Successful"; else echo "Test Failure"; fi;'].join('')
                        ];
                    }
                }
            }
        }, {
            xtype: 'networktest',
            title: 'Download Test'.t(),
            commandFields: [{
                xtype: 'combo',
                width: 500,
                store : [
                    ['http://cachefly.cachefly.net/50mb.test','http://cachefly.cachefly.net/50mb.test'],
                    ['http://cachefly.cachefly.net/5mb.test','http://cachefly.cachefly.net/5mb.test'],
                    ['http://download.thinkbroadband.com/50MB.zip','http://download.thinkbroadband.com/50MB.zip'],
                    ['http://download.thinkbroadband.com/5MB.zip','http://download.thinkbroadband.com/5MB.zip'],
                    ['http://download.untangle.com/data.php','http://download.untangle.com/data.php']
                ],
                bind: '{url}'
            }],
            viewModel: {
                data: {
                    description: 'The <b>Download Test</b> downloads a file.'.t(),
                    emptyText: 'Download Test Output'.t(),
                    url: 'http://cachefly.cachefly.net/5mb.test'
                },
                formulas: {
                    command: function (get) {
                        return [
                            '/bin/bash',
                            '-c',
                            ['wget --output-document=/dev/null ' + ' \'' + get('url') + '\' ;'].join('')
                        ];
                    }
                }
            }
        }, {
            xtype: 'networktest',
            title: 'Packet Test'.t(),
            commandFields: [{
                xtype: 'checkbox',
                boxLabel: 'Advanced'.t(),
                margin: '0 10 0 0',
                bind: '{advanced}'
            }, {
                xtype: 'textfield',
                width: 200,
                emptyText : 'IP Address or Hostname'.t(),
                disabled: true,
                bind: {
                    value: '{destination}',
                    disabled: '{!advanced}'
                }
            }, {
                xtype: 'numberfield',
                minValue : 1,
                maxValue : 65536,
                width: 80,
                emptyText: 'Port'.t(),
                disabled: true,
                bind: {
                    value: '{port}',
                    disabled: '{!advanced}'
                }
            }, {
                xtype: 'combo',
                fieldLabel: 'Interface'.t(),
                labelAlign: 'right',
                editable: false,
                // width: 100,
                forceSelection: true,
                bind: {
                    store: '{interfacesListSystemDev}',
                    value: '{interface}'
                }
            }, {
                xtype: 'combo',
                fieldLabel: 'Timeout'.t(),
                labelAlign: 'right',
                editable: false,
                store: [[ 5, '5 seconds'.t()],
                        [ 30, '30 seconds'.t()],
                        [ 120, '120 seconds'.t()]],
                bind: '{timeout}'
            }],
            viewModel: {
                data: {
                    description: 'The <b>Packet Test</b> can be used to view packets on the network wire for troubleshooting.'.t(),
                    emptyText: 'Packet Test Output'.t(),
                    advanced: true,
                    destination: 'any',
                    port: '',
                    timeout: 5
                },
                formulas: {
                    command: function (get) {
                        var traceFixedOptionsTemplate = ["-U", "-l", "-v"];
                        var traceOverrideOptionsTemplate = ["-n", "-s 65535", "-i " + get('interface')];
                        var traceOptions = traceFixedOptionsTemplate.concat(traceOverrideOptionsTemplate);
                        var traceExpression = [];
                        if (get('advanced')) {
                            // traceExpression = [this.advancedInput.getValue()];
                        } else {
                            if (get('destination') !== null & get('destination').toLowerCase() !== "any") {
                                traceExpression.push('host ' + get('destination'));
                            }
                            if (get('port') !== null) {
                                traceExpression.push('port ' + get('port'));
                            }
                        }
                        var traceArguments = traceOptions.join(' ') + ' ' + traceExpression.join( ' and ');
                        return traceArguments;
                    },
                    interfacesListSystemDev: function (get) {
                        var data = [], i,
                            interfaces = get('settings.interfaces.list');
                        for (i = 0 ; i < interfaces.length; i += 1) {
                            data.push([interfaces[i].systemDev, interfaces[i].name]);
                        }
                        data.push(['tun0', 'OpenVPN']);
                        return data;
                    },
                    interface: function (get) {
                        return get('interfacesListSystemDev')[0][0];
                    }
                }
            }
        }]
    }],
});