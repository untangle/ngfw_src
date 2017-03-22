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

            me.tableConfig = TableConfig.generate(entry.get('table'));

            if (entry.get('type') === 'EVENT_LIST') {
                me.lookupReference('dataBtn').setPressed(false);
                me.getView().down('#tableColumns').removeAll();
                me.getView().down('#tableColumns').add(me.tableConfig.checkboxes);
                me.getView().down('#tableColumns').setValue(entry.get('defaultColumns') ? entry.get('defaultColumns').join() : '');
            } else {
                me.getView().down('#tableColumns').removeAll();
                me.getView().down('#tableColumns').setValue({});
            }

            // set the _sqlConditions data as for the sql conditions grid store
            vm.set('_sqlConditions', entry.get('conditions') || []);
            // set combo store conditions
            me.getView().down('#sqlConditionsCombo').getStore().setData(me.tableConfig.comboItems);
            me.getView().down('#sqlConditionsCombo').setValue('');

            me.getView().down('#sqlFilterCombo').getStore().setData(me.tableConfig.comboItems);
            me.getView().down('#sqlFilterCombo').setValue('');

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

        // var storeFields = [{
        //     name: 'time_trunc'
        // }];

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

        for (i = 0; i < entry.get('timeDataColumns').length; i += 1) {
            column = entry.get('timeDataColumns')[i].split(' ').splice(-1)[0];
            title = column;
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
        var vm = this.getViewModel(),
            entry = vm.get('entry'),
            timeDataColumns = [],
            dataGrid = this.getView().down('#currentData'), i, column;

        dataGrid.setLoading(false);

        for (i = 0; i < data.length; i += 1) {
            for (var _column in data[i]) {
                if (data[i].hasOwnProperty(_column) && _column !== 'time_trunc' && _column !== 'time' && timeDataColumns.indexOf(_column) < 0) {
                    timeDataColumns.push(_column);
                }
            }
        }

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
            seriesRenderer = ColumnRenderer[entry.get('seriesRenderer')];
        }

        for (i = 0; i < timeDataColumns.length; i += 1) {
            column = timeDataColumns[i];
            title = seriesRenderer ? seriesRenderer(column) + ' [' + column + ']' : column;
            // storeFields.push({name: timeDataColumns[i], type: 'integer'});
            reportDataColumns.push({
                dataIndex: column,
                header: title,
                width: timeDataColumns.length > 2 ? 60 : 90
            });
        }

        dataGrid.setColumns(reportDataColumns);
        vm.set('_currentData', data);
    },

    formatPieData: function (data) {
        var me = this, entry = me.getViewModel().get('entry'),
            vm = me.getViewModel(),
            dataGrid = me.getView().down('#currentData');

        dataGrid.setLoading(false);

        dataGrid.setColumns([{
            dataIndex: entry.get('pieGroupColumn'),
            header: me.sqlColumnRenderer(entry.get('pieGroupColumn')),
            flex: 1
        }, {
            dataIndex: 'value',
            header: 'value'.t(),
            width: 200
        }, {
            xtype: 'actioncolumn',
            menuDisabled: true,
            width: 30,
            align: 'center',
            items: [{
                iconCls: 'fa fa-filter',
                tooltip: 'Add Condition'.t()
                // handler: Ext.bind(function (view, rowIndex, colIndex, item, e, record) {
                //     this.buildWindowAddCondition();
                //     data = {
                //         column: this.entry.pieGroupColumn,
                //         operator: "=",
                //         value: record.get(this.entry.pieGroupColumn)
                //     };
                //     this.windowAddCondition.setCondition(data);
                // }, this)
            }]
        }]);
        vm.set('_currentData', data);

    },

    formatTextData: function (data) {
        var entry = this.getViewModel().get('entry'),
            vm = this.getViewModel(),
            dataGrid = this.getView().down('#currentData');
        dataGrid.setLoading(false);

        dataGrid.setColumns([{
            dataIndex: 'data',
            header: 'data'.t(),
            flex: 1
        }, {
            dataIndex: 'value',
            header: 'value'.t(),
            width: 200
        }]);

        var reportData = [], value;
        if (data.length > 0 && entry.get('textColumns') !== null) {
            for (i = 0; i < entry.get('textColumns').length; i += 1) {
                column = entry.get('textColumns')[i].split(' ').splice(-1)[0];
                value = Ext.isEmpty(data[0][column]) ? 0 : data[0][column];
                reportData.push({data: column, value: value});
            }
        }
        vm.set('_currentData', reportData);
    },

    filterData: function (min, max) {
        // aply filtering only on timeseries
        if (this.getViewModel().get('entry.type').indexOf('TIME_GRAPH') >= 0) {
            this.getView().down('#currentData').getStore().clearFilter();
            this.getView().down('#currentData').getStore().filterBy(function (point) {
                var t = point.get('time_trunc').time;
                return t >= min && t <= max ;
            });
        }
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


    // TABLE COLUMNS / CONDITIONS
    updateDefaultColumns: function (el, value) {
        this.getViewModel().set('entry.defaultColumns', value.split(','));
    },

    addSqlCondition: function (btn) {
        var me = this, vm = me.getViewModel(),
            conds = vm.get('_sqlConditions') || [];

        conds.push({
            autoFormatValue: false,
            column: me.getView().down('#sqlConditionsCombo').getValue(),
            javaClass: 'com.untangle.app.reports.SqlCondition',
            operator: '=',
            value: ''
        });

        vm.set('_sqlConditions', conds);
        me.getView().down('#sqlConditions').getStore().reload();
    },

    removeSqlCondition: function (table, rowIndex) {
        var me = this, vm = me.getViewModel(),
            conds = vm.get('_sqlConditions');
        Ext.Array.removeAt(conds, rowIndex);
        vm.set('_sqlConditions', conds);
        me.getView().down('#sqlConditions').getStore().reload();
    },

    sqlColumnRenderer: function (val) {
        return Ext.Array.findBy(this.tableConfig.columns, function (col) {
            return col.dataIndex === val;
        }).header;
    },
    // TABLE COLUMNS / CONDITIONS END


    // FILTERS
    addSqlFilter: function () {
        var me = this, vm = me.getViewModel(),
            _filterComboCmp = me.getView().down('#sqlFilterCombo'),
            _operatorCmp = me.getView().down('#sqlFilterOperator'),
            _filterValueCmp = me.getView().down('#sqlFilterValue');

        vm.get('sqlFilterData').push({
            column: _filterComboCmp.getValue(),
            operator: _operatorCmp.getValue(),
            value: _filterValueCmp.getValue(),
            javaClass: 'com.untangle.app.reports.SqlCondition'
        });

        _filterComboCmp.setValue('');
        _operatorCmp.setValue('=');
        _filterValueCmp.setValue('');

        me.getView().down('#sqlFilters').setTitle('Sql Filters'.t() + ' (' + vm.get('sqlFilterData').length + ')');
        me.getView().down('#sqlFilters').getStore().reload();
        me.refreshData();
    },

    removeSqlFilter: function (table, rowIndex) {
        var me = this, vm = me.getViewModel();
        Ext.Array.removeAt(vm.get('sqlFilterData'), rowIndex);
        me.getView().down('#sqlFilters').setTitle('Sql Filters'.t() + ' (' + vm.get('sqlFilterData').length + ')');
        me.getView().down('#sqlFilters').getStore().reload();
        me.refreshData();
    },

    onFilterKeyup: function (cmp, e) {
        if (e.keyCode === 13) {
            this.addSqlFilter();
        }
    },

    // END FILTERS


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
