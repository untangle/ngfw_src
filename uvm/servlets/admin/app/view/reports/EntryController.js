Ext.define('Ung.view.reports.EntryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports-entry',

    control: {
        '#': {
            afterrender: 'onAfterRender',
        }
    },

    colorPalette: [
        // red
        'B71C1C', 'C62828', 'D32F2F', 'E53935', 'F44336', 'EF5350', 'E57373', 'EF9A9A',
        // pink
        '880E4F', 'AD1547', 'C2185B', 'D81B60', 'E91E63', 'EC407A', 'F06292', 'F48FB1',
        // purple
        '4A148C', '6A1B9A', '7B1FA2', '8E24AA', '9C27B0', 'AB47BC', 'BA68C8', 'CE93D8',
        // blue
        '0D47A1', '1565C0', '1976D2', '1E88E5', '2196F3', '42A5F5', '64B5F6', '90CAF9',
        // teal`
        '004D40', '00695C', '00796B', '00897B', '009688', '26A69A', '4DB6AC', '80CBC4',
        // green
        '1B5E20', '2E7D32', '388E3C', '43A047', '4CAF50', '66BB6A', '81C784', 'A5D6A7',
        // limE
        '827717', '9E9D24', 'AFB42B', 'C0CA33', 'CDDC39', 'D4E157', 'DCE775', 'E6EE9C',
        // yellow
        'F57F17', 'F9A825', 'FBC02D', 'FDD835', 'FFEB3B', 'FFEE58', 'FFF176', 'FFF59D',
        // orange
        'E65100', 'EF6C00', 'F57C00', 'FB8C00', 'FF9800', 'FFA726', 'FFB74D', 'FFCC80',
        // brown
        '3E2723', '4E342E', '5D4037', '6D4C41', '795548', '8D6E63', 'A1887F', 'BCAAA4',
        // grey
        '212121', '424242', '616161', '757575', '9E9E9E', 'BDBDBD', 'E0E0E0', 'EEEEEE',
    ],

    onAfterRender: function () {
        var me = this, vm = this.getViewModel(),
            entryContainer = me.getView().down('#entryContainer'),
            dataGrid = this.getView().down('#currentData');

        vm.bind('{entry}', function (entry) {
            vm.set('_currentData', []);
            dataGrid.setColumns([]);
            dataGrid.setLoading(true);

            // hide current data side panel if event entry
            if (entry.get('type') === 'EVENT_LIST') {
                me.lookupReference('dataBtn').setPressed(false);

                // set defaultColumns for EVENT_LIST
                if (entry.get('table')) {
                    var tableConfig = TableConfig.getConfig(entry.get('table')),
                        checkboxes = [];
                    // console.log(me.getView().down('#tableColumns'));
                    Ext.Array.each(tableConfig.columns, function (column) {
                        checkboxes.push({ boxLabel: column.header, inputValue: column.dataIndex, name: 'cbGroup' });
                    });
                    me.getView().down('#tableColumns').add(checkboxes);
                    me.getView().down('#tableColumns').setValue(entry.get('defaultColumns').join());
                }
            } else {
                me.getView().down('#tableColumns').removeAll();
                me.getView().down('#tableColumns').setValue({});
            }
        });

        vm.bind('{_defaultColors}', function (val) {
            console.log('colors');
            var colors, colorBtns = [];

            if (val) {
                vm.set('entry.colors', null);
            } else {
                colors = vm.get('entry.colors') || Util.defaultColors;
                me.getView().down('#colors').removeAll();
                colors.forEach(function (color, i) {
                    colorBtns.push({
                        xtype: 'button',
                        margin: '0 1',
                        idx: i,
                        arrowVisible: false,
                        menu: {
                            plain: true,
                            xtype: 'colormenu',
                            colors: me.colorPalette,
                            height: 200,
                            listeners: {
                                select: 'updateColor'
                            },
                            dockedItems: [{
                                xtype: 'toolbar',
                                dock: 'bottom',
                                // ui: 'footer',
                                items: [{
                                    // text: 'Remove'.t(),
                                    iconCls: 'fa fa-ban',
                                    tooltip: 'Remove'.t()
                                }, {
                                    text: 'OK'.t(),
                                    iconCls: 'fa fa-check',
                                    listeners: {
                                        click: function (btn) {
                                            btn.up('button').hideMenu();
                                        }
                                    }
                                }]

                            }]
                        },
                        text: '<i class="fa fa-square" style="color: ' + color + '"></i>',
                    });
                });
                me.getView().down('#colors').add(colorBtns);
            }
        });

    },

    closeSide: function () {
        this.lookupReference('dataBtn').setPressed(false);
        this.lookupReference('settingsBtn').setPressed(false);
    },

    formatTimeData: function (data) {
        var entry = this.getViewModel().get('entry'),
            vm = this.getViewModel(),
            dataGrid = this.getView().down('#currentData'), i, column;

        dataGrid.setLoading(false);

        var storeFields = [{
            name: 'time_trunc'
        }];

        var reportDataColumns = [{
            dataIndex: 'time_trunc',
            header: 'Timestamp'.t(),
            width: 130,
            flex: 1,
            renderer: function (val) {
                return (!val) ? 0 : Util.timestampFormat(val);
            }
        }];
        var seriesRenderer = null, title;

        if (!Ext.isEmpty(entry.get('seriesRenderer'))) {
            // seriesRenderer =  Ung.panel.Reports.getColumnRenderer(this.entry.seriesRenderer);
        }

        for (i = 0; i < entry.get('timeDataColumns').length; i += 1) {
            column = entry.get('timeDataColumns')[i].split(' ').splice(-1)[0];
            // todo: deal with series renderer
            title = column;
            // title = seriesRenderer ? seriesRenderer(column) + ' [' + column + ']' : column;
            // storeFields.push({name: column, convert: zeroFn, type: 'integer'});
            reportDataColumns.push({
                dataIndex: column,
                header: title,
                width: entry.get('timeDataColumns').length > 2 ? 60 : 90,
                renderer: function (val) {
                    return val !== undefined ? val : '-';
                }
            });
        }

        dataGrid.setColumns(reportDataColumns);
        vm.set('_currentData', data);
    },

    formatTimeDynamicData: function (data) {
        // console.log('format dynamic');
        var dataGrid = this.getView().down('#currentData');
        dataGrid.setLoading(false);
    },

    formatPieData: function () {
        var dataGrid = this.getView().down('#currentData');
        dataGrid.setLoading(false);
    },

    formatTextData: function () {
        var dataGrid = this.getView().down('#currentData');
        dataGrid.setLoading(false);
    },

    formatData: function (data) {
        var entry = this.getViewModel().get('entry'),
            vm = this.getViewModel(),
            dataGrid = this.getView().down('#currentData'), i, column;

        if (entry.get('type') === 'TEXT') {
            dataGrid.setColumns([{
                dataIndex: 'data',
                header: 'data'.t(),
                width: 100,
                flex: 1
            }, {
                dataIndex: 'value',
                header: 'value'.t(),
                width: 100
            }]);

            var infos = [], reportData = [], value;
            if (data.length > 0 && entry.get('textColumns') !== null) {
                for (i = 0; i < entry.get('textColumns').length; i += 1) {
                    column = entry.get('textColumns')[i].split(' ').splice(-1)[0];
                    value = Ext.isEmpty(data[0][column]) ? 0 : data[0][column];
                    infos.push(value);
                    reportData.push({data: column, value: value});
                }
            }
            this.getView().down('#entry').setHtml(Ext.String.format.apply(Ext.String.format, [entry.get('textString').t()].concat(infos)));
            dataGrid.getStore().loadData(reportData);
        }

        if (entry.get('type') === 'PIE_GRAPH') {
            dataGrid.setColumns([{
                dataIndex: entry.get('pieGroupColumn'),
                header: entry.get('pieGroupColumn'),
                width: 100,
                flex: 1
            }, {
                dataIndex: 'value',
                header: 'value'.t(),
                width: 100
            }, {
                // xtype: 'actioncolumn',
                // menuDisabled: true,
                // width: 20,
                // items: [{
                //     iconCls: 'icon-row icon-filter',
                //     tooltip: i18n._('Add Condition'),
                //     handler: Ext.bind(function (view, rowIndex, colIndex, item, e, record) {
                //         this.buildWindowAddCondition();
                //         data = {
                //             column: this.entry.pieGroupColumn,
                //             operator: "=",
                //             value: record.get(this.entry.pieGroupColumn)
                //         };
                //         this.windowAddCondition.setCondition(data);
                //     }, this)
                // }]
            }]);
            dataGrid.getStore().loadData(data);
        }

        if (entry.get('type') === 'TIME_GRAPH' || entry.get('type') === 'TIME_GRAPH_DYNAMIC') {
            var zeroFn = function (val) {
                return val || 0;
            };
            var timeFn = function (val) {
                return val;
                // return (val === null) ? 0 : Util.timestampFormat(val);
            };

            var storeFields = [{
                name: 'time_trunc'
            }];

            var reportDataColumns = [{
                dataIndex: 'time_trunc',
                header: 'Timestamp'.t(),
                width: 130,
                flex: 1,
                renderer: function (val) {
                    // return val.time;
                    return (!val) ? 0 : Util.timestampFormat(val);
                }
                // flex: entry.get('timeDataColumns').length > 2 ? 0 : 1,
                // renderer: function (val) {
                //     return val.time;
                // }
                // sorter: function (rec1, rec2) {
                //     var t1, t2;
                //     console.log(rec1);
                //     if ( rec1.getData().time !== null && rec2.getData().time !== null ) {
                //         t1 = rec1.getData().time;
                //         t2 = rec2.getData().time;
                //     } else {
                //         t1 = rec1.getData();
                //         t2 = rec2.getData();
                //     }
                //     return (t1 > t2) ? 1 : (t1 === t2) ? 0 : -1;
                // }
            }];
            var seriesRenderer = null, title;
            if (!Ext.isEmpty(entry.get('seriesRenderer'))) {
                // seriesRenderer =  Ung.panel.Reports.getColumnRenderer(this.entry.seriesRenderer);
            }

            for (i = 0; i < entry.get('timeDataColumns').length; i += 1) {
                column = entry.get('timeDataColumns')[i].split(' ').splice(-1)[0];
                // todo: deal with series renderer
                title = column;
                // title = seriesRenderer ? seriesRenderer(column) + ' [' + column + ']' : column;
                // storeFields.push({name: column, convert: zeroFn, type: 'integer'});
                reportDataColumns.push({
                    dataIndex: column,
                    header: title,
                    width: entry.get('timeDataColumns').length > 2 ? 60 : 90,
                    renderer: function (val) {
                        return val !== undefined ? val : '-';
                    }
                });
            }

            // dataGrid.setStore(Ext.create('Ext.data.Store', {
            //     // fields: storeFields,
            //     data: []
            // }));
            dataGrid.setColumns(reportDataColumns);
            vm.set('_currentData', data);
            // dataGrid.getStore().loadData(data);
        }
    },


    filterData: function (min, max) {
        this.getView().down('#currentData').getStore().clearFilter();
        this.getView().down('#currentData').getStore().filterBy(function (point) {
            var t = point.get('time_trunc').time;
            return t >= min && t <= max ;
        });
    },

    addGraphEntry: function () {
        var me = this,
            vm = this.getViewModel(),
            entryContainer = me.getView().down('#entryContainer'),
            entry = vm.get('entry');

        if (entry.get('type') === 'TIME_GRAPH' || entry.get('type') === 'TIME_GRAPH_DYNAMIC') {
            entryContainer.add({
                xtype: 'timechart',
                itemId: 'entry',
                autoDestory: false
            });
        }

        if (entry.get('type') === 'PIE_GRAPH') {
            entryContainer.add({
                xtype: 'piechart',
                itemId: 'entry'
            });
        }

        if (entry.get('type') === 'EVENT_LIST') {
            // chartContainer.add({
            //     xtype: 'eventchart',
            //     itemId: 'chart',
            //     entry: this.entry
            // });
        }

        if (entry.get('type') === 'TEXT') {
            // chartContainer.add({
            //     xtype: 'component',
            //     itemId: 'chart',
            //     html: 'Not Implemented'
            // });
        }
    },

    addTextEntry: function () {
        this.getView().down('#entryContainer').add({
            xtype: 'textreport',
            itemId: 'entry',
            padding: 10
        });
    },

    updateColor: function (menu, color) {
        var vm = this.getViewModel(),
            newColors = vm.get('entry.colors') ? Ext.clone(vm.get('entry.colors')) : Ext.clone(Util.defaultColors);

        menu.up('button').setText('<i class="fa fa-square" style="color: #' + color + ';"></i>');
        newColors[menu.up('button').idx] = '#' + color;
        vm.set('entry.colors', newColors);
        return false;
    },

    // addColor: function (btn) {
    //     btn.up('grid').getStore().add({color: 'FF0000'});
    //     // var vm = this.getViewModel();
    //     // var colors = vm.get('report.colors');
    //     // colors.push('#FF0000');
    //     // vm.set('report.colors', colors);
    // },

    refreshData: function () {
        var vm = this.getViewModel();
        switch(vm.get('entry.type')) {
            case 'TEXT': this.getView().down('textreport').getController().fetchData(); break;
            case 'EVENT_LIST': this.getView().down('eventreport').getController().fetchData(); break;
            default: this.getView().down('graphreport').getController().fetchData();
        }
    },

    updateDefaultColumns: function (el, value) {
        this.getViewModel().set('entry.defaultColumns', value.split(','));
    },

    updateReport: function () {
        var me = this,
            v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('entry');

        v.setLoading(true);
        Rpc.asyncData('rpc.reportsManager.saveReportEntry', entry.getData())
            .then(function(result) {
                v.setLoading(false);
                vm.get('report').copyFrom(entry);
                vm.get('report').commit();
                Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('entry.title') + '</span> report updated!');
                Ung.app.redirectTo('#reports/' + entry.get('category').replace(/ /g, '-').toLowerCase() + '/' + entry.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase());

                me.refreshData();
            });
    },

    saveNewReport: function () {
        var v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('entry');

        entry.set('uniqueId', 'report-' + Math.random().toString(36).substr(2));
        entry.set('readOnly', false);

        v.setLoading(true);
        Rpc.asyncData('rpc.reportsManager.saveReportEntry', entry.getData())
            .then(function(result) {
                v.setLoading(false);
                Ext.getStore('reports').add(entry);
                entry.commit();
                Util.successToast('<span style="color: yellow; font-weight: 600;">' + entry.get('title') + ' report added!');
                Ung.app.redirectTo('#reports/' + entry.get('category').replace(/ /g, '-').toLowerCase() + '/' + entry.get('title').replace(/[^0-9a-z\s]/gi, '').replace(/\s+/g, '-').toLowerCase());
            });
    }

});
