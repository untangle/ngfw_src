Ext.define('Ung.Setup.Internet', {
    extend: 'Ext.form.Panel',
    alias: 'widget.Internet',

    title: 'Internet Connection'.t(),
    description: 'Configure the Internet Connection'.t(),

    layout: {
        type: 'hbox',
        align: 'begin',
        pack: 'center'
    },

    items: [{
        xtype: 'container',
        margin: '50 20 0 0',
        width: 300,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        hidden: true,
        bind: {
            hidden: '{!wan}'
        },
        items: [{
            xtype: 'component',
            cls: 'sectionheader',
            margin: '0 0 10 0',
            html: 'Configuration Type'.t()
        }, {
            xtype: 'radiogroup',
            // fieldLabel: 'Configuration Type'.t(),
            // labelWidth: 160,
            labelAlign: 'right',
            simpleValue: true,
            layout: { type: 'hbox' },
            defaults: { padding: '1 15 1 0' },
            items: [
                { boxLabel: '<strong>' + 'Auto (DHCP)'.t() + '</strong>', inputValue: 'AUTO' },
                { boxLabel: '<strong>' + 'Static'.t() + '</strong>', inputValue: 'STATIC' },
                { boxLabel: '<strong>' + 'PPPoE'.t() + '</strong>', inputValue: 'PPPOE' }
            ],
            bind: {
                value: '{wan.v4ConfigType}'
            }
        }, {
            xtype: 'button',
            margin: 10,
            text: 'Renew DHCP'.t(),
            iconCls: 'fa fa-refresh',
            handler: 'renewDhcp', // renew DHCP and refresh status
            bind: {
                hidden: '{wan.v4ConfigType !== "AUTO"}'
            }
        }, {
            xtype: 'container',
            width: 200,
            margin: '0 10',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            hidden: true,
            bind: {
                hidden: '{wan.v4ConfigType !== "STATIC"}'
            },
            defaults: {
                xtype: 'textfield',
                labelAlign: 'top',
                msgTarget: 'side',
                validationEvent: 'blur',
                maskRe: /(\d+|\.)/,
                vtype: 'ipAddress',
                disabled: true,
                bind: {
                    disabled: '{wan.v4ConfigType !== "STATIC"}'
                }
            },
            items: [{
                fieldLabel: 'IP Address'.t(),
                allowBlank: false,
                bind: { value: '{wan.v4StaticAddress}' }
            }, {
                fieldLabel: 'Netmask'.t(),
                xtype: 'combo',
                store: Util.v4NetmaskList,
                queryMode: 'local',
                triggerAction: 'all',
                value: 24,
                bind: { value: '{wan.v4StaticPrefix}' },
                editable: false,
                allowBlank: false
            }, {
                fieldLabel: 'Gateway'.t(),
                allowBlank: false,
                bind: { value: '{wan.v4StaticGateway}' }
            }, {
                fieldLabel: 'Primary DNS'.t(),
                allowBlank: false,
                bind: { value: '{wan.v4StaticDns1}' }
            }, {
                xtype: 'textfield',
                vtype: 'ipAddress',
                name: 'dns2',
                fieldLabel: 'Secondary DNS (optional)'.t(),
                allowBlank: true,
                bind: { value: '{wan.v4StaticDns2}' }
            }]
        }, {
            xtype: 'container',
            width: 200,
            margin: '0 10',
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            hidden: true,
            bind: {
                hidden: '{wan.v4ConfigType !== "PPPOE"}'
            },
            defaults: {
                xtype: 'textfield',
                labelAlign: 'top',
                allowBlank: false,
                disabled: true,
                bind: {
                    disabled: '{wan.v4ConfigType !== "PPPOE"}'
                }
            },
            items: [{
                fieldLabel: 'Username'.t(),
                bind: { value: '{wan.v4PPPoEUsername}' }
            }, {
                inputType: 'password',
                fieldLabel: 'Password'.t(),
                bind: { value: '{wan.v4PPPoEPassword}' }
            }]
        }]
    }, {
        xtype: 'container',
        margin: '50 20 0 0',
        width: 300,
        hidden: true,
        bind: {
            hidden: '{!wan}'
        },
        layout: {
            type: 'vbox',
            // align: 'stretch'
        },
        defaults: {
            xtype: 'displayfield',
            // labelWidth: 170,
            // labelAlign: 'right',
            // margin: 0
        },
        items: [{
            xtype: 'component',
            cls: 'sectionheader',
            margin: '0 0 10 0',
            html: 'Status'.t()
        }, {
            fieldLabel: 'IP Address'.t(),
            bind: { value: '{wanStatus.v4Address}' }
        }, {
            fieldLabel: 'Netmask'.t(),
            bind: { value: '/{wanStatus.v4PrefixLength} - {wanStatus.v4Netmask}' }
        }, {
            fieldLabel: 'Gateway'.t(),
            bind: { value: '{wanStatus.v4Gateway}' }
        }, {
            fieldLabel: 'Primary DNS'.t(),
            bind: { value: '{wanStatus.v4Dns1}' }
        }, {
            fieldLabel: 'Secondary DNS'.t(),
            bind: { value: '{wanStatus.v4Dns2}' }
        }, {
            xtype: 'button',
            text: 'Test Connectivity'.t(),
            margin: '10 0 0 0',
            iconCls: 'fa fa-globe',
            handler: 'onSave', // save is called because connectivity test is done inside of it
            // bind: {
            //     hidden: '{wan.v4ConfigType !== "AUTO" && wan.v4ConfigType !== "STATIC" }'
            // }
        }]
    }],


    listeners: {
        activate: 'getSettings',
        save: 'onSave',
    },

    controller: {

        getSettings: function () {
            var me = this, vm = me.getViewModel(),
                firstWan = null;

            rpc.networkManager.getNetworkSettings(function (settings, ex) {
                if (ex) { Util.handleException('Unable to fetch Network Settings.'.t()); return; }
                vm.set('networkSettings', settings);

                // get the first wan
                firstWan = Ext.Array.findBy(settings.interfaces.list, function (intf) {
                    return (intf.isWan && intf.configType !== 'DISABLED');
                });
                vm.set('wan', firstWan);
                if (!firstWan) { return; }
                me.getInterfaceStatus();
            });
        },

        getInterfaceStatus: function () {
            var me = this, vm = me.getViewModel(),
                wan = vm.get('wan');

            rpc.networkManager.getInterfaceStatus(function (status, ex) {
                if (ex) { Util.handleException('Unable to get WAN status.'.t()); return; }
                vm.set('wanStatus', status);
            }, wan.interfaceId);
        },

        renewDhcp: function () {
            var me = this, vm = me.getViewModel(),
                wan = vm.get('wan');
            // save settings before
            Ung.app.loading('Saving settings ...'.t());
            rpc.networkManager.setNetworkSettings(function (response, ex) {
                if (ex) { Util.handleException('Unable to set Network Settings.'.t()); return; }
                // then force the DHCP lease renew just in case
                // setNetworkSettings is not guaranteed to restart networking
                Ung.app.loading('Renewing DHCP Lease...'.t());
                rpc.networkManager.renewDhcpLease(function (r, e) {
                    if (e) { Util.handleException(e); return; }
                    Ung.app.loading(false);
                    me.getSettings();
                }, wan.interfaceId);
            }, vm.get('networkSettings'));
        },

        testConnectivity: function (testType, cb) {
            var me = this;
            Ung.app.loading('Testing Connectivity...'.t());
            rpc.connectivityTester.getStatus(function (result, ex) {
                Ung.app.loading(false);
                if (ex) {
                    Ext.MessageBox.show({
                        title: 'Network Settings'.t(),
                        msg: 'Unable to complete connectivity test, please try again.'.t(),
                        width: 300,
                        buttons: Ext.MessageBox.OK,
                        icon: Ext.MessageBox.INFO
                    });
                    return;
                }

                // build test fail message if any
                var message = null;
                if (result.tcpWorking === false  && result.dnsWorking === false) {
                    message = 'Warning! Internet tests and DNS tests failed.'.t();
                } else if (result.tcpWorking === false) {
                    message = 'Warning! DNS tests succeeded, but Internet tests failed.'.t();
                } else if (result.dnsWorking === false) {
                    message = 'Warning! Internet tests succeeded, but DNS tests failed.'.t();
                } else {
                    message = null;
                }

                if (testType === 'manual') {
                    // on manual test just show the message
                    Ext.MessageBox.show({
                        title: 'Internet Status'.t(),
                        msg: message || 'Success!'.t(),
                        width: 300,
                        buttons: Ext.MessageBox.OK,
                        icon: Ext.MessageBox.INFO
                    });
                } else {
                    // on next step just move forward if no failures
                    if (!message) { cb(); return; }

                    // otherwise show a warning message
                    var warningText = message + '<br/><br/>' + 'It is recommended to configure valid internet settings before continuing. Try again?'.t();
                    Ext.Msg.confirm('Warning:'.t(), warningText, function (btn) {
                        if (btn === 'yes') { return; }
                        cb();
                    });
                }

                me.getInterfaceStatus();
            });

        },

        onSave: function (cb) {
            var me = this, vm = this.getViewModel(),
                wan = vm.get('wan');

            if (!wan) { cb(); return; }

            // validate any current form first
            if (!me.getView().isValid()) {
                Ext.MessageBox.alert('Invalid settings'.t(), 'Some fields have invalid values.'.t());
                return;
            }

            if (wan.v4ConfigType === 'AUTO' || wan.v4ConfigType === 'PPPOE') {
                wan.v4StaticAddress = null;
                wan.v4StaticPrefix = null;
                wan.v4StaticGateway = null;
                wan.v4StaticDns1 = null;
                wan.v4StaticDns2 = null;
            }
            if (wan.v4ConfigType === 'STATIC') {
                wan.v4NatEgressTraffic = true;
            }
            if (wan.v4ConfigType === 'PPPOE') {
                wan.v4NatEgressTraffic = true;
                wan.v4PPPoEUsePeerDns = true;
            }

            // save
            Ung.app.loading('Saving settings ...'.t());
            rpc.networkManager.setNetworkSettings(function (response, ex) {
                if (ex) { Util.handleException(ex); return; }
                me.testConnectivity(Ext.isFunction(cb) ? 'auto' : 'manual', function () {
                    cb();
                });
            }, vm.get('networkSettings'));
        }
    }





});
