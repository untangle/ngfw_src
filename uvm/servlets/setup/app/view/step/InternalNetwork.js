Ext.define('Ung.Setup.InternalNetwork', {
    extend: 'Ext.form.Panel',
    alias: 'widget.InternalNetwork',

    title: 'Internal Network'.t(),
    description: 'Configure the Internal Network Interface'.t(),

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    defaults: {
        hidden: true,
        bind: {
            hidden: '{!internal}'
        }
    },

    items: [{
        xtype: 'container',
        layout: {
            type: 'column',
        },
        items: [{
            xtype: 'container',
            columnWidth: 0.7,
            defaults: {
                padding: '0 0 0 10'
            },
            items: [{
                xtype: 'radio',
                reference: 'routerRadio',
                name: 'configType',
                inputValue: 'ROUTER',
                boxLabel: '<strong>' + 'Router'.t() + '</strong>',
                padding: 0,
                bind: {
                    value: '{internal.configType !== "BRIDGED"}'
                },
                listeners: {
                    change: 'setConfigType'
                }
            }, {
                xtype: 'component',
                margin: '0 0 10 0',
                html: 'This is recommended if the external port is plugged into the internet connection. This enables NAT and DHCP.'.t()
            }, {
                xtype: 'textfield',
                labelWidth: 150,
                width: 350,
                labelAlign: 'right',
                fieldLabel: 'Internal Address'.t(),
                vText: 'Please enter a valid Network  Address'.t(),
                vtype: 'ipAddress',
                allowBlank: false,
                msgTarget: 'side',
                maskRe: /(\d+|\.)/,
                disabled: true,
                value: '192.168.1.1',
                validationEvent: 'blur',
                bind: { value: '{internal.v4StaticAddress}', disabled: '{!routerRadio.checked}' }
            }, {
                labelWidth: 150,
                width: 350,
                labelAlign: 'right',
                fieldLabel: 'Internal Netmask'.t(),
                xtype: 'combo',
                store: Util.v4NetmaskList,
                queryMode: 'local',
                triggerAction: 'all',
                disabled: true,
                editable: false,
                bind: { value: '{internal.v4StaticPrefix}', disabled: '{!routerRadio.checked}' }
            }, {
                xtype: 'checkbox',
                margin: '0 0 0 155',
                disabled: true,
                boxLabel: 'Enable DHCP Server (default)'.t(),
                bind: { value: '{internal.dhcpEnabled}', disabled: '{!routerRadio.checked}' }
            }]
        }, {
            xtype: 'component',
            columnWidth: 0.3,
            margin: 20,
            html: '<img src="/skins/' + rpc.skinName + '/images/admin/wizard/router.png"/>'
        }]
    }, {
        xtype: 'container',
        margin: '10 0 0 0',
        layout: {
            type: 'column',
        },
        items: [{
            xtype: 'container',
            columnWidth: 0.7,
            defaults: {
                padding: '0 0 0 10'
            },
            items: [{
                xtype: 'radio',
                reference: 'bridgeRadio',
                name: 'configType',
                inputValue: 'BRIDGED',
                boxLabel: '<strong>' + 'Transparent Bridge'.t() + '</strong>',
                padding: 0,
                bind: {
                    value: '{internal.configType === "BRIDGED"}'
                },
                listeners: {
                    change: 'setConfigType'
                }
            }, {
                xtype: 'component',
                margin: '0 0 10 0',
                html: 'This is recommended if the external port is plugged into a firewall/router. This bridges Internal and External and disables DHCP.'.t()
            }]
        }, {
            xtype: 'component',
            columnWidth: 0.3,
            margin: 20,
            html: '<img src="/skins/' + rpc.skinName + '/images/admin/wizard/bridge.png"/>'
        }]
    }],

    listeners: {
        activate: 'getInterface',
        save: 'onSave'
    },

    controller: {
        getInterface: function () {
            var me = this, vm = me.getViewModel(),
                interfaces = vm.get('networkSettings.interfaces.list'),
                // first nonWAN is internal interface
                internal = Ext.Array.findBy(interfaces, function (intf) {
                    return !intf.isWan;
                });

            if (!internal) {
                Ext.Msg.show({
                    title: 'Warning!',
                    message: 'No internal interfaces found. Do you want to continue the setup?',
                    buttons: Ext.Msg.YESNO,
                    icon: Ext.Msg.QUESTION,
                    fn: function (btn) {
                        if (btn === 'yes') {
                            me.getView().up('setupwizard').down('#nextBtn').click();
                        } else {
                            // if no is pressed
                        }
                    }
                });
            } else {
                me.initialConfigType = internal.configType;
                me.initialv4Address = internal.v4StaticAddress;
                me.initialv4Prefix = internal.v4StaticPrefix;
                me.initialDhcpEnabled = internal.dhcpEnabled;
            }
            vm.set('internal', internal);
        },

        setConfigType: function (radio) {
            var me = this, vm = me.getViewModel();
            if (radio.checked) {
                if (radio.inputValue === 'BRIDGED') {
                    vm.set('internal.configType', 'BRIDGED');
                } else {
                    vm.set('internal.configType', 'ADDRESSED');
                    vm.set('internal.v4configType', 'STATIC');
                }
            }
        },

        warnAboutDisappearingAddress: function() {
            var me = this, vm = me.getViewModel();
            var firstWan, firstWanStatus, newSetupLocation;

            // get firstWan settings & status
            firstWan = Ext.Array.findBy(vm.get('networkSettings.interfaces.list'), function (intf) {
                return intf.isWan && intf.configType !== 'DISABLED';
            });

            // firstWan must exist
            if (!firstWan || !firstWan.interfaceId) { return; }

            try {
                firstWanStatus = rpc.networkManager.getInterfaceStatus(firstWan.interfaceId);
            } catch (e) {
                Util.handleException(e);
            }

            // and the first WAN has a address
            if (!firstWanStatus || !firstWanStatus.v4Address) { return; }

            //Use Internal Address instead of External Address
            newSetupLocation = window.location.href.replace(vm.get('internal.v4StaticAddress'), firstWanStatus.v4Address);
            rpc.keepAlive = function () {}; // prevent keep alive
            Ext.MessageBox.wait('Saving Internal Network Settings'.t() + '<br/><br/>' +
                                'The Internal Address is no longer accessible.'.t() + '<br/>' +
                                Ext.String.format('You will be redirected to the new setup address: {0}'.t(), '<a href="' + newSetupLocation + '">' + newSetupLocation + '</a>') + '<br/><br/>' +
                                'If the new location is not loaded after 30 seconds please reinitialize your local device network address and try again.'.t(), 'Please Wait'.t());
            Ext.defer(function () {
                window.location.href = newSetupLocation;
            }, 30000);
        },

        warnAboutChangingAddress: function() {
            var me = this, vm = me.getViewModel();
            var newSetupLocation;

            newSetupLocation = window.location.href.replace(me.initialv4Address, vm.get('internal.v4StaticAddress'));
            rpc.keepAlive = function () {}; // prevent keep alive
            Ext.MessageBox.wait('Saving Internal Network Settings'.t() + '<br/><br/>' +
                                Ext.String.format('The Internal Address is changed to: {0}'.t(), vm.get('internal.v4StaticAddress')) + '<br/>' +
                                Ext.String.format('The changes are applied and you will be redirected to the new setup address: {0}'.t(), '<a href="' + newSetupLocation + '">' + newSetupLocation + '</a>') + '<br/><br/>' +
                                'If the new location is not loaded after 30 seconds please reinitialize your local device network address and try again.'.t(), 'Please Wait'.t());
            Ext.defer(function () {
                window.location.href = newSetupLocation;
            }, 30000);

        },

        onSave: function (cb) {
            var me = this, vm = me.getViewModel();

            // if settings are invalid do not save
            if (!me.getView().isValid()) { return; }

            // no changes made - continue to next step
            if ( me.initialConfigType === vm.get('internal.configType') &&
                 me.initialv4Address === vm.get('internal.v4StaticAddress') &&
                 me.initialv4Prefix === vm.get('internal.v4StaticPrefix') &&
                 me.initialDhcpEnabled === vm.get('internal.dhcpEnabled')) {
                cb();
                return;
            }

            // BRIDGED (bridge mode)
            if (vm.get('internal.configType') === 'BRIDGED') {
                //If using internal address - redirect to external since internal address is vanishing
                if (window.location.hostname === vm.get('internal.v4StaticAddress')) {
                    me.warnAboutDisappearingAddress();
                }
            } else { // ADDRESSED (router)
                // set these to null so new values will automatically be calculated based on current address
                vm.set('internal.dhcpRangeStart', null);
                vm.set('internal.dhcpRangeEnd', null);
                //If using internal address and it is changed in this step redirect to new internal address
                if (window.location.hostname == me.initialv4Address && me.initialv4Address != vm.get('internal.v4StaticAddress')) {
                    me.warnAboutChangingAddress();
                }
            }

            // save settings and continue to next step
            Ung.app.loading('Saving Internal Network Settings'.t());
            rpc.networkManager.setNetworkSettings(function (result, ex) {
                Ung.app.loading(false);
                if (ex) { Util.handleException(ex); return; }
                cb();
            }, vm.get('networkSettings'));
        }
    }
});
