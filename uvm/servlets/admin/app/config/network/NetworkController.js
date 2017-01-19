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


    // editRule: function () {

    // }
});