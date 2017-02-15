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
        'networktest': {
            afterrender: 'networkTestRender'
        }
        // '#apply': {
        //     click: 'saveSettings'
        // }
    },

    saveSettings: function () {
        var view = this.getView();
        var vm = this.getViewModel();
        var me = this;
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

    /**
     * Loads netowrk settings
     */
    loadSettings: function () {
        var me = this;
        rpc.networkManager.getNetworkSettings(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
            me.getViewModel().set('settings', result);
            me.loadInterfaceStatusAndDevices();
            console.log(result);
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
            if (ex) {
                console.log(ex);
                Ung.Util.exceptionToast(ex);
                return;
            }
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
            me.setPortForwardWarnings();
        });

        rpc.networkManager.getDeviceStatus(function (result, ex) {
            if (ex) { console.error(ex); Ung.Util.exceptionToast(ex); return; }
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
    }


});