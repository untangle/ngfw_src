Ext.define('Ung.config.network.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.network',

    control: {
        '#': { afterrender: 'loadSettings' },
        '#interfaces': { beforerender: 'onInterfaces' },
        '#portForwardRules': { beforerender: 'setPortForwardWarnings' },
        '#currentRoutes': { afterrender: 'refreshRoutes' },
        '#qosStatistics': { afterrender: 'refreshQosStatistics' },
        '#upnpStatus': { afterrender: 'refreshUpnpStatus' },
        '#dhcpLeases': { afterrender: 'refreshDhcpLeases' },
        'networktest': { afterrender: 'networkTestRender' }
    },

    additionalInterfaceProps: [{
        // interface status
        v4Address: null,
        v4Netmask: null,
        v4Gateway: null,
        v4Dns1: null,
        v4Dns2: null,
        v4PrefixLength: null,
        v6Address: null,
        v6Gateway: null,
        v6PrefixLength: null,
        // device status
        deviceName: null,
        macAddress: null,
        duplex: null,
        vendor: null,
        mbit: null,
        connected: null
    }],

    loadSettings: function () {
        var me = this,
            v = this.getView(),
            vm = this.getViewModel();
        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.networkManager.getNetworkSettings'),
            Rpc.asyncPromise('rpc.networkManager.getInterfaceStatus'),
            Rpc.asyncPromise('rpc.networkManager.getDeviceStatus'),
        ], this).then(function (result) {
            v.setLoading(false);
            var intfStatus, devStatus;
            result[0].interfaces.list.forEach(function (intf) {
                Ext.apply(intf, me.additionalInterfaceProps);
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

            // check if Allow SSH input filter rule is enabled
            var inputFilterRulesSshEnabled = me.isSshInputFilterRuleEnabled(vm.get('settings'));
            vm.set('inputFilterRulesSshEnabled', inputFilterRulesSshEnabled);

            var inputFilterRulesLength = me.getInputFilterRulesCount(vm.get('settings'));
            vm.set('inputFilterRulesLength', inputFilterRulesLength);

        }, function (ex) {
            v.setLoading(false);
            console.error(ex);
            Util.exceptionToast(ex);
        });
    },

    saveSettings: function () {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;

        if (!Util.validateForms(view)) {
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

        // check if Block All input filter rule is enabled
        var blockAllEnabled = me.isBlockAllInputFilterRuleEnabled(vm.get('settings'));
        if (!blockAllEnabled) {
            Ext.MessageBox.alert("Failed".t(), "The Block All rule in Input Filter Rules is disabled. This is dangerous and not allowed! Refer to the documentation.".t());
            view.setLoading(false);
            return;
        }

        // check to see if any input filter rules have been added/removed
        var inputFilterRulesLength = me.getInputFilterRulesCount(vm.get('settings'));
        if ( inputFilterRulesLength != vm.get('inputFilterRulesLength') ) {
            Ext.Msg.show({
                title: 'Input Filter Rules changed!'.t(),
                msg: "The Input Filter Rules have been changed!".t() + "<br/><br/>" +
                    "Improperly configuring the Input Filter Rules can be very dangerous.".t() + "<br/>" +
                    "Read the documentation for more details.".t() + "<br/><br/>" +
                    "Do you want to continue?".t(),
                buttons: Ext.Msg.YESNO,
                fn: function(btnId) {
                    if (btnId === 'yes') {
                        vm.set('inputFilterRulesLength', inputFilterRulesLength); // set this so it doesnt warning again
                        me.saveSettings(); // start over
                        return;
                    }
                    else {
                        view.setLoading(false);
                        return;
                    }
                },
                animEl: 'elId',
                icon: Ext.MessageBox.QUESTION
            });
            return;
        }

        // check if Allow SSH input filter rule has been enabled
        var inputFilterRulesSshEnabled = me.isSshInputFilterRuleEnabled(vm.get('settings'));
        if ( inputFilterRulesSshEnabled && !vm.get('inputFilterRulesSshEnabled') ) {
            Ext.Msg.show({
                title: 'SSH Access Enabled!'.t(),
                msg: "The 'Allow SSH' rule in Input Filter Rules has been enabled!".t() + "<br/><br/>" +
                    "If the admin/root password is poorly chosen, enabling SSH is very dangerous.".t() + "<br/><br/>" +
                    "Any changes made via the command line can be dangerous and destructive.".t() + "<br/>" +
                    "Any changes made via the command line are not supported and can limit your support options.".t() + "<br/><br/>" +
                    "Do you want to continue?".t(),
                buttons: Ext.Msg.YESNO,
                fn: function(btnId) {
                    if (btnId === 'yes') {
                        vm.set('inputFilterRulesSshEnabled', true); // set this so it doesnt warning again
                        me.saveSettings(); // start over
                        return;
                    }
                    else {
                        view.setLoading(false);
                        return;
                    }
                },
                animEl: 'elId',
                icon: Ext.MessageBox.QUESTION
            });
            return;
        }

        me.setNetworkSettings();
    },

    setNetworkSettings: function() {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;

        Rpc.asyncData('rpc.networkManager.setNetworkSettings', vm.get('settings'))
        .then(function(result) {
            view.setLoading(false);
            me.loadSettings();
            Util.successToast('Network'.t() + ' settings saved!');
        });
    },

    isSshInputFilterRuleEnabled: function(networkSettings) {
        var inputFilterRulesSshEnabled = false;
        if(networkSettings.inputFilterRules && networkSettings.inputFilterRules.list) {
            var i;
            for( i=0; i<networkSettings.inputFilterRules.list.length ; i++ ) {
                var rule = networkSettings.inputFilterRules.list[i];
                if ( rule.description == "Allow SSH" ) {
                    inputFilterRulesSshEnabled = rule.enabled;
                    break;
                }
            }
        }
        return inputFilterRulesSshEnabled;
    },

    getInputFilterRulesCount: function(networkSettings) {
        if(networkSettings.inputFilterRules && networkSettings.inputFilterRules.list) {
            return networkSettings.inputFilterRules.list.length;
        }
        return 0;
    },

    isBlockAllInputFilterRuleEnabled: function(networkSettings) {
        var blockAllEnabled = true; //assume true because we used different names in the past
        if(networkSettings.inputFilterRules && networkSettings.inputFilterRules.list) {
            var i;
            for( i=0; i<networkSettings.inputFilterRules.list.length ; i++ ) {
                var rule = networkSettings.inputFilterRules.list[i];
                if ( rule.description == "Block All" ) {
                    if ( !rule.enabled || !rule.ipv6Enabled )
                        blockAllEnabled = false;
                    else
                        blockAllEnabled = true;
                    break;
                }
            }
        }
        return blockAllEnabled;
    },

    onInterfaces: function () {
        var me = this,
            vm = this.getViewModel();

        vm.bind('{interfacesGrid.selection}', function(interface) {
            if (interface) {
                me.getInterfaceStatus();
                me.getInterfaceArp();
            }
        });
    },

    getInterfaceStatus: function () {
        var statusView = this.getView().down('#interfaceStatus'),
            vm = this.getViewModel(),
            symbolicDev = vm.get('interfacesGrid.selection').get('symbolicDev'),
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
        statusView.setLoading(true);
        Rpc.asyncData('rpc.execManager.execOutput', command1).then(function (result) {
            if (Ext.isEmpty(result) || result.search('Device not found') >= 0) {
                statusView.setLoading(false);
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

            Rpc.asyncData('rpc.execManager.execOutput', command2).then(function (result) {
                if (Ext.isEmpty(result)) {
                    statusView.setLoading(false);
                    return;
                }

                var linep = result.split(' ');
                Ext.apply(stat, {
                    address: linep[0].split(':')[1],
                    mask: linep[2].split(':')[1]
                });

                Rpc.asyncData('rpc.execManager.execOutput', command3).then(function (result) {
                    statusView.setLoading(false);
                    Ext.apply(stat, {
                        v6Addr: result
                    });
                    vm.set('siStatus', stat);
                });
            });
        });
    },

    getInterfaceArp: function () {
        var vm = this.getViewModel(),
            arpView = this.getView().down('#interfaceArp'),
            symbolicDev = vm.get('interfacesGrid.selection').get('symbolicDev'),
            arpCommand = 'arp -n | grep ' + symbolicDev + ' | grep -v incomplete > /tmp/arp.txt ; cat /tmp/arp.txt';

        arpView.setLoading(true);
        Rpc.asyncData('rpc.execManager.execOutput', arpCommand).then(function (result) {
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
            arpView.setLoading(false);
        });
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

    refreshRoutes: function (cmp) {
        var view = cmp.isXType('button') ? cmp.up('panel') : cmp;
        view.down('textarea').setValue('');
        view.setLoading(true);
        Rpc.asyncData('rpc.execManager.exec', '/usr/share/untangle/bin/ut-routedump.sh')
            .then(function (result) {
                view.down('textarea').setValue(result.output);
            }).always(function () {
                view.setLoading(false);
            });
    },

    refreshQosStatistics: function (cmp) {
        var view = cmp.isXType('button') ? cmp.up('grid') : cmp;
        view.setLoading(true);
        Rpc.asyncData('rpc.execManager.execOutput', '/usr/share/untangle-netd/bin/qos-status.py')
            .then(function (result) {
                var list = [];
                try {
                    list = eval(result);
                } catch (e) {
                    Util.exceptionToast('Unable to get QoS statistics');
                    console.error('Could not execute /usr/share/untangle-netd/bin/qos-status.py output: ', result, e);
                }
            }).always(function () {
                view.setLoading(false);
            });
    },

    refreshUpnpStatus: function (cmp) {
        var view = cmp.isXType('button') ? cmp.up('grid') : cmp;
        if (view.isDisabled()) {
            return;
        }
        view.setLoading(true);
        var vm = this.getViewModel();
        Rpc.asyncData('rpc.networkManager.getUpnpManager', '--status', '')
            .then(function(result) {
                console.log(result);
                vm.set('upnpStatus', Ext.decode(result).active);
            }).always(function () {
                view.setLoading(false);
            });
    },

    refreshDhcpLeases: function (cmp) {
        var view = cmp.isXType('button') ? cmp.up('grid') : cmp;
        view.setLoading(true);
        Rpc.asyncData('rpc.execManager.execOutput', 'cat /var/lib/misc/dnsmasq.leases')
            .then(function (result) {
                var lines = result.split('\n'),
                    leases = [], lineparts, i;
                for (i = 0 ; i < lines.length ; i++) {
                    if (lines[i] === null || lines[i] === '' ) {
                        continue;
                    }
                    lineparts = lines[i].split(/\s+/);
                    leases.push({
                        date: lineparts[0],
                        macAddress: lineparts[1],
                        address: lineparts[2],
                        hostname: lineparts[3],
                        clientId: lineparts[4]
                    });
                }
                //todo: handle leases data / store
            }).always(function () {
                view.setLoading(false);
            });
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
            if (ex) { console.error(ex); Util.exceptionToast(ex); return; }
            me.readOutput(result, text, output, btn);
        }, v.getViewModel().get('command'));

    },
    readOutput: function (resultReader, text, output, btn) {
        var me = this;

        if (!resultReader) {
            return;
        }
        resultReader.readFromOutput(function (res, ex) {
            if (ex) { console.error(ex); Util.exceptionToast(ex); return; }
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


    editInterface: function (btn) {
        var v = this.getView();
        // var win = Ext.create('config.interface');
        // this.editingRecord = btn.getWidgetRecord();
        this.dialog = v.add({
            xtype: 'config.interface',
            viewModel: {
                data: {
                    // si: btn.getWidgetRecord().copy(null)
                    si: btn.getWidgetRecord()
                },
                formulas: {
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
                    showWirelessPassword: function (get) { return get('si.wirelessEncryption') !== 'NONE' && get('si.wirelessEncryption') !== null; }
                }
            }
        });
        this.dialog.show();
    },
    cancelEdit: function () {
        this.dialog.close();
    },

    doneEdit: function () {
        this.dialog.close();
        // console.log(this.dialog.getViewModel());
        // // Ext.apply(this.editingRecord, this.dialog.getViewModel().get('si'));
        // var edited = this.dialog.getViewModel().get('si');
        // console.log(edited);
        // var store = this.getView().down('#interfacesGrid').getStore();
        // console.log(store);
        // var t = store.findRecord('interfaceId', edited.get('interfaceId'));
        // t = edited.copy();
        // console.log(t);
    }



});
