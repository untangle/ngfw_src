Ext.define('Ung.config.network.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-network',

    control: {
        '#': { afterrender: 'loadSettings' },
        '#interfaces': { beforerender: 'onInterfaces' },
        '#routes': { afterrender: 'refreshRoutes' },
        '#qosStatistics': { afterrender: 'refreshQosStatistics' },
        '#upnpStatus': { afterrender: 'refreshUpnpStatus' },
        '#dhcpLeases': { afterrender: 'refreshDhcpLeases' },
        'networktest': { afterrender: 'networkTestRender' }
    },

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
                // Ext.apply(intf, me.additionalInterfaceProps);
                intfStatus = Ext.Array.findBy(result[1].list, function (intfSt) {
                    return intfSt.interfaceId === intf.interfaceId;
                });


                if(intfStatus != null){
                    delete intfStatus.javaClass;
                }
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

            me.setPortForwardWarnings();

        }, function (ex) {
            v.setLoading(false);
            console.error(ex);
            Util.handleException(ex);
        });
    },

    saveSettings: function () {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;

        if (!Util.validateForms(view)) {
            return;
        }

        view.setLoading(true);

        // update interfaces data
        var interfacesStore = view.down('#interfacesGrid').getStore();
        if (interfacesStore.getModifiedRecords().length > 0 ||
            interfacesStore.getNewRecords().length > 0 ||
            interfacesStore.getRemovedRecords().length > 0) {
            vm.set('settings.interfaces.list', Ext.Array.pluck(interfacesStore.getRange(), 'data'));
        }


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

        if( vm.get('settings').qosSettings.qosEnabled === true ){
            var bandwidthFound = false;
            vm.get('wanInterfaces').each( function(interface){
                if( interface.get('downloadBandwidthKbps') != null &&
                    interface.get('uploadBandwidthKbps') != null){
                    bandwidthFound = true;
                }
            });
            if(bandwidthFound === false){
                Ext.MessageBox.alert(
                        "Failed".t(),
                        "QoS is Enabled. Please set valid Download Bandwidth and Upload Bandwidth limits in WAN Bandwidth for all WAN interfaces.".t()
                );
                view.setLoading(false);
                return;
            }
        }

        me.setNetworkSettings();
    },

    setNetworkSettings: function() {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;

        Rpc.asyncData('rpc.networkManager.setNetworkSettings', vm.get('settings'))
        .then(function(result) {
            me.loadSettings();
            Util.successToast('Network'.t() + ' settings saved!');
        }).always(function () {
            view.setLoading(false);
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
                    if ( !rule.enabled || !rule.ipv6Enabled ) {
                        blockAllEnabled = false;
                    }
                    else {
                        blockAllEnabled = true;
                        break;
                    }
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
                    Util.handleException('Unable to get QoS statistics');
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
                view.getStore().loadData(leases);
            }).always(function () {
                view.setLoading(false);
            });

    },
    addStaticDhcpLease: function (view, rowIndex, colIndex, item, e, record) {
        var me = this; staticDhcpGrid = me.getView().down('#dhcpEntries');
        var newDhcpEntry = {
            macAddress: record.get('macAddress'),
            address: record.get('address'),
            description: record.get('hostname'),
            javaClass: 'com.untangle.uvm.network.DhcpStaticEntry'
        };
        staticDhcpGrid.getStore().add(newDhcpEntry);
    },

    // Network Tests
    networkTestRender: function (view) {
        view.down('form').insert(0, view.commandFields);
    },
    runTest: function (btn) {
        var v = btn.up('networktest'),
            vm = v.getViewModel(),
            output = v.down('textarea'),
            text = [],
            me = this;

        btn.setDisabled(true);

        text.push(output.getValue());
        text.push('' + (new Date()) + ' - ' + 'Test Started'.t() + '\n');

        rpc.execManager.execEvil(function (result, ex) {
            if (ex) { console.error(ex); Util.handleException(ex); return; }
            // Save the filename.
            me.readOutput(result, text, output, btn, vm);
        }, v.getViewModel().get('command'));

    },
    readOutput: function (resultReader, text, output, btn, vm) {
        var me = this;

        if (!resultReader) {
            return;
        }

        resultReader.readFromOutput(function (res, ex) {
            if (ex) { console.error(ex); Util.handleException(ex); return; }
            if (res !== null) {
                text.push(res);
                Ext.Function.defer(me.readOutput, 1000, me, [resultReader, text, output, btn, vm]);
            } else {
                btn.setDisabled(false);
                text.push('' + (new Date()) + ' - ' + 'Test Completed'.t());
                text.push('\n\n--------------------------------------------------------\n\n');
                if(vm.get('exportRunFilename') !== '' ){
                    vm.set('exportFilename', vm.get('exportRunFilename'));
                    vm.set('exportRunFilename', '');
                }
            }
            output.setValue(text.join(''));
            output.getEl().down('textarea').dom.scrollTop = 99999;
        });
    },

    clearOutput: function (btn) {
        var me = this,
            vm = btn.up('networktest').getViewModel();

        var v = btn.up('networktest');
        v.down('textarea').setValue('');
        vm.set('exportRunFilename', '');
        vm.set('exportFilename', '');
    },

    exportOutput: function(btn){
        var vm = btn.up('networktest').getViewModel();

        Ext.MessageBox.wait( "Exporting Packet Dump...".t(), "Please wait".t());
        var downloadForm = document.getElementById('downloadForm');
        downloadForm["type"].value = "NetworkTestExport";
        downloadForm["arg1"].value = vm.get('exportFilename');
        downloadForm.submit();
        Ext.MessageBox.hide();
    },

    editInterface: function (btn) {
        var me = this;
        if (Ext.isFunction(btn.getWidgetRecord)) {
            // editing existing interface from the grid
            me.editIntf = btn.getWidgetRecord();
        } else {
            // adding a VLAN Interface
            me.editIntf = Ext.create('Ung.model.Interface', {
                isVlanInterface: true,
                isWirelessInterface: false,
                vlanTag: 1,
                configType: 'ADDRESSED',
                v4ConfigType: 'STATIC',
                v6ConfigType: 'DISABLED'
            });
        }

        var configTypesArr,
            configTypesRadios = {
                ADDRESSED: { boxLabel: '<i class="fa fa-file-text fa-gray"></i> <strong>' + 'Addressed'.t() + '</strong>', inputValue: 'ADDRESSED' },
                BRIDGED: { boxLabel: '<i class="fa fa-link fa-gray"></i> <strong>' + 'Bridged'.t() + '</strong>', inputValue: 'BRIDGED' },
                DISABLED: { boxLabel: '<i class="fa fa-ban fa-gray"></i> <strong>' + 'Disabled'.t() + '</strong>', inputValue: 'DISABLED' }
            }, configTypes = [];


        configTypesArr = me.editIntf.get('supportedConfigTypes') || ['ADDRESSED', 'BRIDGED', 'DISABLED'];

        Ext.Array.each(configTypesArr, function (confType) {
            configTypes.push(configTypesRadios[confType]);
        });

        me.dialog = me.getView().add({
            xtype: 'config.interface',
            configTypesRadios: configTypes, // holds the radio buttons based on interface supported config types
            title: me.editIntf.get('interfaceId') > 0 ? 'Edit Interface'.t() : 'Add VLAN Interface'.t(),
            viewModel: {
                data: {
                    // intf: btn.getWidgetRecord().copy(null)
                    intf: me.editIntf
                },
                formulas: {
                    isAddressed: function (get) { return get('intf.configType') === 'ADDRESSED'; },
                    isDisabled: function (get) { return get('intf.configType') === 'DISABLED'; },
                    isBridged: function (get) { return get('intf.configType') === 'BRIDGED'; },
                    isStaticv4: function (get) { return get('intf.v4ConfigType') === 'STATIC'; },
                    isAutov4: function (get) { return get('intf.v4ConfigType') === 'AUTO'; },
                    isPPPOEv4: function (get) { return get('intf.v4ConfigType') === 'PPPOE'; },
                    isDisabledv6: function (get) { return get('intf.v6ConfigType') === 'DISABLED'; },
                    isStaticv6: function (get) { return get('intf.v6ConfigType') === 'STATIC'; },
                    isAutov6: function (get) { return get('intf.v6ConfigType') === 'AUTO'; },
                    showRouterWarning: function (get) { return get('intf.v6StaticPrefixLength') !== 64; },
                    showWireless: function (get) { return get('intf.isWirelessInterface') && get('intf.configType') !== 'DISABLED'; },
                    showWirelessPassword: function (get) { return get('intf.wirelessEncryption') !== 'NONE' && get('intf.wirelessEncryption') !== null; }
                },
                stores: {
                    v4Aliases: { data: '{intf.v4Aliases.list}' },
                    v6Aliases: { data: '{intf.v6Aliases.list}' },
                    dhcpOptions: { data: '{intf.dhcpOptions.list}' },
                    vrrpAliases: { data: '{intf.vrrpAliases.list}' }
                }
            }
        });
        me.dialog.show();
    },
    cancelEdit: function (button) {
        this.editIntf.reject();
        this.dialog.close();
    },

    doneEdit: function (btn) {
        var me = this, dialogVm = me.dialog.getViewModel(), intf = dialogVm.get('intf');
        this.dialog.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                if (grid.listProperty) {
                    // console.log(intf);
                    intf.set(grid.listProperty, {
                        javaClass: 'java.util.LinkedList',
                        list: Ext.Array.pluck(store.getRange(), 'data')
                    });
                }
            }
        });

        // new VLAN interface
        if (intf.get('interfaceId') === -1) {
            me.lookup('interfacesGrid').getStore().add(intf);
        }

        this.dialog.close();
    },

    onBridgedInteface: function( combo ){
        var me = this,
            vm = me.getViewModel();

        var record = combo.up('window').getViewModel().get('intf');

        var fields = [];
        vm.get('settings').interfaces.list.forEach( function(interface){
            if( ( interface.interfaceId == record.get('interfaceId') ) ||
                ( interface.bridged !== false ) ||
                ( interface.disabled !== false ) ||
                ( interface.v4ConfigType == 'ADDRESSED') ){
                return;
            }
            fields.push([interface.interfaceId, interface.name]);
        });

        combo.setStore(Ext.create('Ext.data.ArrayStore', {
            fields: [ 'id', 'name' ],
            sorters: [{
                property: 'name',
                direction: 'ASC'
            }],
            data: fields
        }));
    },

    onRenewDhcpLease: function () {
        var me = this, interfaceId = me.editIntf.get('interfaceId');
        // Ext.MessageBox.wait('Renewing DHCP Lease...'.t(), 'Please wait'.t());
        me.dialog.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.asyncPromise('rpc.networkManager.renewDhcpLease', interfaceId),
            Rpc.asyncPromise('rpc.networkManager.getInterfaceStatus'),
        ], this).then(function (result) {
            var intfStatus = Ext.Array.findBy(result[1].list, function (intfSt) {
                return intfSt.interfaceId === interfaceId;
            });

            if (intfStatus != null) {
                delete intfStatus.javaClass;
                delete intfStatus.interfaceId;
            }
            console.log(intfStatus);
            Ext.apply(me.editIntf, intfStatus);
        }, function (ex) {
            console.error(ex);
            Util.handleException(ex);
        }).always(function () {
            me.dialog.setLoading(false);
        });
    },

    addVlanInterface: function () {
        console.log('add');
    }
});
