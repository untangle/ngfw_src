Ext.define('Ung.Setup.Interfaces', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.Interfaces',
    title: 'Network Cards'.t(),
    description: 'Identify Network Cards'.t(),

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    items: [{
        xtype: 'component',
        html: 'This step identifies the external, internal, and other network cards.'.t() + '<br/><br/>' +
            '<strong>' + 'Step 1:'.t() + '</strong> ' +
            'Plug an active cable into one network card to determine which network card it is.'.t() + '<br/>' +
            '<strong>' + 'Step 2:'.t() + '</strong> ' +
            '<em>' + 'Drag and drop'.t() + '</em> ' + 'the network card to map it to the desired interface.'.t() + '<br/>' +
            '<strong>' + 'Step 3:'.t() + '</strong> ' +
            'Repeat steps 1 and 2 for each network card and then click <i>Next</i>.'.t()
    }, {
        xtype: 'grid',
        margin: '20 0 0 0',
        flex: 1,
        sortableColumns: false,
        enableColumnResize: true,
        enableColumnHide: false,
        enableColumnMove: false,
        plugins: {
            ptype: 'cellediting',
            clicksToEdit: 1
        },
        store: {
            data: []
        },

        viewConfig: {
            plugins: {
                ptype: 'gridviewdragdrop',
                dragText: 'Drag and drop to reorganize'.t(),
                dragZone: {
                    onBeforeDrag: function (data, e) {
                        return Ext.get(e.target).hasCls('fa-arrows');
                    }
                }
            },
            listeners: {
                beforedrop: 'onBeforeDrop'
            }
        },
        columns: [{
            header: 'Name'.t(),
            dataIndex: 'name',
            sortable: false,
            width: 110,
            renderer: function (value) {
                return value.t();
            }
        }, {
            xtype: 'gridcolumn',
            header: '<i class="fa fa-sort"></i>',
            align: 'center',
            width: 30,
            resizable: false,
            tdCls: 'action-cell',
            renderer: function() {
                return '<i class="fa fa-arrows" style="cursor: move;"></i>';
            }
        }, {
            header: 'Device'.t(),
            tooltip: 'Click on a Device to open a combo and choose the desired Device from a list. When anoter Device is selected the 2 Devices are swithced.'.t(),
            tooltipType: 'title',
            width: 80,
            dataIndex: 'deviceName',
            editor: {
                xtype: 'combo',
                bind: {
                    store: '{deviceStore}'
                },
                // store: physicalDevsStore,
                editable: false,
                valueField: 'physicalDev',
                displayField: 'physicalDev',
                queryMode: 'local',
                listeners: {
                    change: 'setInterfacesMap'
                }
            }
        }, {
            dataIndex: 'connected',
            sortable: false,
            resizable: false,
            width: 30,
            align: 'center',
            renderer: function (value) {
                switch (value) {
                case 'CONNECTED': return '<i class="fa fa-circle fa-green"></i>';
                case 'DISCONNECTED': return '<i class="fa fa-circle fa-gray"></i>';
                case 'MISSING': return '<i class="fa fa-exclamation-triangle fa-orange"></i>';
                default: return '<i class="fa fa-question-circle fa-gray"></i>';
                }
            }
        }, {
            header: 'Status'.t(),
            dataIndex: 'connected',
            sortable: false,
            flex: 1,
            renderer: function (value, metadata, record) {
                var connected = record.get('connected'),
                    mbit = record.get('mbit'),
                    duplex = record.get('duplex'),
                    vendor = record.get('vendor'),
                    connectedStr = (connected == 'CONNECTED') ? 'connected'.t() : (connected == 'DISCONNECTED') ? 'disconnected'.t() : 'unknown'.t(),
                    duplexStr = (duplex == 'FULL_DUPLEX') ? 'full-duplex'.t() : (duplex == 'HALF_DUPLEX') ? 'half-duplex'.t() : 'unknown'.t();
                return connectedStr + ' ' + mbit + ' ' + duplexStr + ' ' + vendor;
            }
        }, {
            header: 'MAC Address'.t(),
            dataIndex: 'macAddress',
            sortable: false,
            width: 120
            // renderer: function (value, metadata, record, rowIndex, colIndex, store, view) {
            //     var text = '';
            //     if (value && value.length > 0) {
            //         // Build the link for the mac address
            //         text = '<a target="_blank" href="http://standards.ieee.org/cgi-bin/ouisearch?' +
            //             value.substring(0, 8).replace(/:/g, '') + '">' + value + '</a>';
            //     }
            //     return text;
            // }
        }]
    }, {
        xtype: 'container',
        layout: {
            type: 'hbox',
            align: 'stretch'
        },
        cls: 'inline-warning',
        margin: '10 0 0 0',
        padding: '20 20 10 20',

        // publishes: 'hidden',
        hidden: true,
        bind: {
            hidden: '{networkSettings.interfaces.list.length > 1}'
        },

        items: [{
            xtype: 'component',
            margin: '0 20 0 0',
            html: '<i class="fa fa-exclamation-triangle fa-orange fa-3x"></i>'
        }, {
            xtype: 'container',
            flex: 1,
            layout: {
                type: 'vbox',
                align: 'stretch'
            },
            items: [{
                xtype: 'component',
                html: '<p style="float: left; margin: 0;">' + 'Untangle must be installed "in-line" as a gateway. <br/>This usually requires at least 2 network cards (NICs) and fewer than 2 NICs were detected.'.t() + '</p>'
            }, {
                xtype: 'checkbox',
                reference: 'forcecontinue',
                margin: '5 0',
                boxLabel: '<strong>' + 'Continue anyway'.t() + '</strong>'
            }]
        }]
    }],

    listeners: {
        activate: 'onActivate',
        deactivate: 'onDeactivate',
        save: 'onSave'
    },

    controller: {

        onActivate: function () {
            var me = this;
            me.getSettings();
            me.enableAutoRefresh = true;
            Ext.defer(me.autoRefreshInterfaces, 3000, me); // refreshes interfaces every 3 seconds
        },

        onDeactivate: function () {
            var me = this;
            me.enableAutoRefresh = false;
        },

        getSettings: function () {
            var me = this, vm = me.getViewModel(),
                grid = me.getView().down('grid');

            me.physicalDevsStore = [];
            me.intfOrderArr = [];

            Ung.app.loading('Loading interfaces...'.t());
            rpc.networkManager.getNetworkSettings(function (result, ex) {
                Ung.app.loading(false);
                if (ex) { Util.handleException('Unable to load interfaces.'.t()); return; }

                vm.set({
                    networkSettings: result,
                    intfListLength: result.interfaces.list.length
                });

                var interfaces = [], devices = [];

                Ext.Array.each(result.interfaces.list, function (intf) {
                    if (!intf.isVlanInterface) {
                        interfaces.push(intf);
                        devices.push({physicalDev: intf.physicalDev});
                    }
                });

                rpc.networkManager.getDeviceStatus(function (result2, ex2) {
                    if (ex2) { Util.handleException(ex2); return; }
                    var deviceStatusMap = Ext.Array.toValueMap(result2.list, 'deviceName');
                    Ext.Array.forEach(interfaces, function (intf) {
                        Ext.applyIf(intf, deviceStatusMap[intf.physicalDev]);
                    });

                    // store data is not binded, so grid changes are not affecting the network settings
                    grid.getStore().loadData(Ext.clone(interfaces));
                    grid.getStore().commitChanges(); // so the grid is not dirty after initial data load

                    Ext.Array.each(interfaces, function (intf) {
                        me.physicalDevsStore.push([intf.physicalDev, intf.physicalDev]);
                        // me.intfOrderArr.push(Ext.clone(intf));
                    });
                    vm.set('deviceStore', me.physicalDevsStore);
                });

                // update the steps based on interfaces
                me.getView().up('setupwizard').fireEvent('syncsteps');

            });
        },

        autoRefreshInterfaces: function () {
            var me = this, store = me.getView().down('grid').getStore(),
                vm = me.getViewModel();

            if (!me.enableAutoRefresh) { return; }

            rpc.networkManager.getNetworkSettings(function (result, ex) {
                if (ex) { Util.handleException('Unable to refresh the interfaces.'.t()); return; }
                var interfaces = [];

                vm.set('intfListLength', result.interfaces.list.length);

                Ext.Array.each(result.interfaces.list, function (intf) {
                    if (!intf.isVlanInterface) {
                        interfaces.push(intf);
                    }
                });

                if (interfaces.length !== store.getCount()) {
                    Ext.MessageBox.alert('New interfaces'.t(), 'There are new interfaces, please restart the wizard.', '');
                    return;
                }

                rpc.networkManager.getDeviceStatus(function (result2, ex2) {
                    if (ex2) { Util.handleException(ex); return; }
                    if (result === null) { return; }

                    var deviceStatusMap = Ext.Array.toValueMap(result2.list, 'deviceName');
                    store.each(function (row) {
                        var deviceStatus = deviceStatusMap[row.get('physicalDev')];
                        if (deviceStatus !== null) {
                            row.set('connected', deviceStatus.connected);
                        }
                    });
                    if (me.enableAutoRefresh) {
                        Ext.defer(me.autoRefreshInterfaces, 3000, me);
                    }
                });

            });
        },

        // use the same mechanism as for drop downs
        onBeforeDrop: function (node, data, overModel, dropPosition, dropHandlers) {
            dropHandlers.wait = true;

            var sourceRecord = data.records[0],
                targetRecord = overModel;

            if (sourceRecord === null || targetRecord === null) {
                dropHandlers.cancelDrop();
                return;
            }

            // clone phantom records to manipulate (switch) data properly
            var sourceRecordCopy = sourceRecord.copy(null),
                targetRecordCopy = targetRecord.copy(null);

            sourceRecord.set({
                deviceName: targetRecordCopy.get('deviceName'),
                physicalDev: targetRecordCopy.get('physicalDev'),
                // systemDev:   targetRecordCopy.get('systemDev'),
                // symbolicDev: targetRecordCopy.get('symbolicDev'),
                macAddress:  targetRecordCopy.get('macAddress'),
                duplex:      targetRecordCopy.get('duplex'),
                vendor:      targetRecordCopy.get('vendor'),
                mbit:        targetRecordCopy.get('mbit'),
                connected:   targetRecordCopy.get('connected')
            });
            targetRecord.set({
                deviceName: sourceRecordCopy.get('deviceName'),
                physicalDev: sourceRecordCopy.get('physicalDev'),
                // systemDev:   sourceRecordCopy.get('systemDev'),
                // symbolicDev: sourceRecordCopy.get('symbolicDev'),
                macAddress:  sourceRecordCopy.get('macAddress'),
                duplex:      sourceRecordCopy.get('duplex'),
                vendor:      sourceRecordCopy.get('vendor'),
                mbit:        sourceRecordCopy.get('mbit'),
                connected:   sourceRecordCopy.get('connected')
            });
            dropHandlers.cancelDrop(); // cancel drop as we do not want to reorder rows but just to set physicalDev
        },

        // used when mapping from comboboxes
        setInterfacesMap: function (elem, newValue, oldValue) {
            var sourceRecord = null, targetRecord = null, grid = this.getView().down('grid');

            grid.getStore().each( function( currentRow ) {
                if (oldValue === currentRow.get('physicalDev')) {
                    sourceRecord = currentRow;
                } else if (newValue === currentRow.get('physicalDev')) {
                    targetRecord = currentRow;
                }
            });
            // make sure sourceRecord & targetRecord are defined
            if (sourceRecord === null || targetRecord === null || sourceRecord === targetRecord) {
                return;
            }

            // clone phantom records to manipulate (switch) data properly
            var sourceRecordCopy = sourceRecord.copy(null),
                targetRecordCopy = targetRecord.copy(null);

            // switch data between records (interfaces) - remapping
            sourceRecord.set({
                deviceName: newValue,
                physicalDev: targetRecordCopy.get('physicalDev'),
                // systemDev:   targetRecordCopy.get('systemDev'),
                // symbolicDev: targetRecordCopy.get('symbolicDev'),
                macAddress:  targetRecordCopy.get('macAddress'),
                duplex:      targetRecordCopy.get('duplex'),
                vendor:      targetRecordCopy.get('vendor'),
                mbit:        targetRecordCopy.get('mbit'),
                connected:   targetRecordCopy.get('connected')
            });
            targetRecord.set({
                deviceName: oldValue,
                physicalDev: sourceRecordCopy.get('physicalDev'),
                // systemDev:   sourceRecordCopy.get('systemDev'),
                // symbolicDev: sourceRecordCopy.get('symbolicDev'),
                macAddress:  sourceRecordCopy.get('macAddress'),
                duplex:      sourceRecordCopy.get('duplex'),
                vendor:      sourceRecordCopy.get('vendor'),
                mbit:        sourceRecordCopy.get('mbit'),
                connected:   sourceRecordCopy.get('connected')
            });
        },

        onSave: function (cb) {
            var me = this, vm = me.getViewModel(),
                grid = me.getView().down('grid'), interfacesMap = {};

            // if no changes/remapping skip this step
            if (grid.getStore().getModifiedRecords().length === 0) { cb(); return; }

            grid.getStore().each(function (currentRow) {
                interfacesMap[currentRow.get('interfaceId')] = currentRow.get('physicalDev');
            });

            // apply new physicalDev for each interface from initial Network Settings
            Ext.Array.each(vm.get('networkSettings.interfaces.list'), function (intf) {
                if (!intf.isVlanInterface) {
                    intf.physicalDev = interfacesMap[intf.interfaceId];
                }
            });

            Ung.app.loading('Saving Settings ...'.t());
            rpc.networkManager.setNetworkSettings(function (result, ex) {
                Ung.app.loading(false);
                if (ex) { Util.handleException(ex); return; }
                cb();
            }, vm.get('networkSettings'));
        }
    }
});
