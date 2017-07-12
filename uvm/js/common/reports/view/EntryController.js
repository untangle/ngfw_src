Ext.define('Ung.view.reports.EntryController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reports-entry',

    control: {
        '#': {
            afterrender: 'onAfterRender',
        }
    },

    refreshTimeout: null,

    // colorPalette: [
    //     // red
    //     'B71C1C', 'C62828', 'D32F2F', 'E53935', 'F44336', 'EF5350', 'E57373', 'EF9A9A',
    //     // pink
    //     '880E4F', 'AD1547', 'C2185B', 'D81B60', 'E91E63', 'EC407A', 'F06292', 'F48FB1',
    //     // purple
    //     '4A148C', '6A1B9A', '7B1FA2', '8E24AA', '9C27B0', 'AB47BC', 'BA68C8', 'CE93D8',
    //     // blue
    //     '0D47A1', '1565C0', '1976D2', '1E88E5', '2196F3', '42A5F5', '64B5F6', '90CAF9',
    //     // teal`
    //     '004D40', '00695C', '00796B', '00897B', '009688', '26A69A', '4DB6AC', '80CBC4',
    //     // green
    //     '1B5E20', '2E7D32', '388E3C', '43A047', '4CAF50', '66BB6A', '81C784', 'A5D6A7',
    //     // limE
    //     '827717', '9E9D24', 'AFB42B', 'C0CA33', 'CDDC39', 'D4E157', 'DCE775', 'E6EE9C',
    //     // yellow
    //     'F57F17', 'F9A825', 'FBC02D', 'FDD835', 'FFEB3B', 'FFEE58', 'FFF176', 'FFF59D',
    //     // orange
    //     'E65100', 'EF6C00', 'F57C00', 'FB8C00', 'FF9800', 'FFA726', 'FFB74D', 'FFCC80',
    //     // brown
    //     '3E2723', '4E342E', '5D4037', '6D4C41', '795548', '8D6E63', 'A1887F', 'BCAAA4',
    //     // grey
    //     '212121', '424242', '616161', '757575', '9E9E9E', 'BDBDBD', 'E0E0E0', 'EEEEEE',
    // ],

    onAfterRender: function () {
        var me = this, vm = this.getViewModel(), widget,
            entryContainer = me.getView().down('#entryContainer'),
            dataGrid = this.getView().down('#currentData');

        vm.set('context', Ung.app.servletContext);

        vm.bind('{entry}', function (entry) {
            vm.set('_currentData', []);
            vm.set('autoRefresh', false); // reset auto refresh
            if (me.refreshTimeout) {
                clearInterval(me.refreshTimeout);
            }

            dataGrid.setColumns([]);
            dataGrid.setLoading(true);

            me.tableConfig = TableConfig.generate(entry.get('table'));

            if (entry.get('type') === 'EVENT_LIST') {
                me.lookupReference('dataBtn').setPressed(false);
                me.getView().down('#tableColumns').removeAll();
                me.getView().down('#tableColumns').add(me.tableConfig.checkboxes);
                me.getView().down('#tableColumns').setValue(entry.get('defaultColumns') ? entry.get('defaultColumns').join() : '');
                // me.lookup('filterfield').fireEvent('change');
                me.lookup('filterfield').setValue('');
            } else {
                me.getView().down('#tableColumns').removeAll();
                me.getView().down('#tableColumns').setValue({});
            }

            // check if widget in admin context
            if (Ung.app.servletContext === 'admin') {
                widget = Ext.getStore('widgets').findRecord('entryId', entry.get('uniqueId')) || null;
                vm.set('widget', Ext.getStore('widgets').findRecord('entryId', entry.get('uniqueId')));
            }




            // set the _sqlConditions data as for the sql conditions grid store
            vm.set('_sqlConditions', entry.get('conditions') || []);
            // set combo store conditions
            me.getView().down('#sqlConditionsCombo').getStore().setData(me.tableConfig.comboItems);
            me.getView().down('#sqlConditionsCombo').setValue(null);

            me.getView().down('#sqlFilterCombo').getStore().setData(me.tableConfig.comboItems);
            me.getView().down('#sqlFilterCombo').setValue(null);

        });

        // vm.bind('{_defaultColors}', function (val) {
        //     console.log('colors');
        //     var colors, colorBtns = [];

        //     if (val) {
        //         vm.set('entry.colors', null);
        //     } else {
        //         colors = vm.get('entry.colors') || Util.defaultColors;
        //         me.getView().down('#colors').removeAll();
        //         colors.forEach(function (color, i) {
        //             colorBtns.push({
        //                 xtype: 'button',
        //                 margin: '0 1',
        //                 idx: i,
        //                 arrowVisible: false,
        //                 menu: {
        //                     plain: true,
        //                     xtype: 'colormenu',
        //                     colors: me.colorPalette,
        //                     height: 200,
        //                     listeners: {
        //                         select: 'updateColor'
        //                     },
        //                     dockedItems: [{
        //                         xtype: 'toolbar',
        //                         dock: 'bottom',
        //                         // ui: 'footer',
        //                         items: [{
        //                             // text: 'Remove'.t(),
        //                             iconCls: 'fa fa-ban',
        //                             tooltip: 'Remove'.t()
        //                         }, {
        //                             text: 'OK'.t(),
        //                             iconCls: 'fa fa-check',
        //                             listeners: {
        //                                 click: function (btn) {
        //                                     btn.up('button').hideMenu();
        //                                 }
        //                             }
        //                         }]

        //                     }]
        //                 },
        //                 text: '<i class="fa fa-square" style="color: ' + color + '"></i>',
        //             });
        //         });
        //         me.getView().down('#colors').add(colorBtns);
        //     }
        // });

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
        dataGrid.getStore().loadData(data);
        // vm.set('_currentData', data);
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
            seriesRenderer = Renderer[entry.get('seriesRenderer')];
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
        dataGrid.getStore().loadData(data);
        // vm.set('_currentData', data);
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
                tooltip: 'Add Condition'.t(),
                handler: 'addPieFilter'
            }]
        }]);
        dataGrid.getStore().loadData(data);
        // vm.set('_currentData', data);

    },

    addPieFilter: function (view, rowIndex, colIndex, item, e, record) {
        var me = this, vm = me.getViewModel(),
            gridFilters =  me.getView().down('#sqlFilters'),
            col = vm.get('entry.pieGroupColumn');

        if (col) {
            vm.get('sqlFilterData').push({
                column: col,
                operator: '=',
                value: record.get(col),
                javaClass: 'com.untangle.app.reports.SqlCondition'
            });
        } else {
            console.log('Issue with pie column!');
            return;
        }

        gridFilters.setCollapsed(false);
        gridFilters.setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('sqlFilterData').length));
        gridFilters.getStore().reload();
        me.refreshData();
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
        // vm.set('_currentData', reportData);
        dataGrid.getStore().loadData(reportData);
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
        var me = this, vm = me.getViewModel(), ctrl;
        switch(vm.get('entry.type')) {
            case 'TEXT': ctrl = this.getView().down('textreport').getController(); break;
            case 'EVENT_LIST': ctrl = this.getView().down('eventreport').getController(); break;
            default: ctrl = this.getView().down('graphreport').getController();
        }

        if (!ctrl) {
            console.error('Entry controller not found!');
            return;
        }

        var tb = me.getView().down('#actionsToolbar');
        tb.setDisabled(true); // disable toolbar actions while fetching data

        ctrl.fetchData(false, function () {
            tb.setDisabled(false);
            if (vm.get('autoRefresh')) {
                me.refreshTimeout = setTimeout(function () {
                    me.refreshData();
                }, 5000);
            } else {
                clearTimeout(me.refreshTimeout);
            }
        });

    },

    resetView: function(){
        var grid = this.getView().down('grid');
        Ext.state.Manager.clear(grid.stateId);
        grid.reconfigure(null, grid.tableConfig.columns);
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

        me.getView().down('#sqlConditionsCombo').setValue(null);

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
        return '<strong>' + TableConfig.getColumnHumanReadableName(val) + '</strong> <span style="float: right;">[' + val + ']</span>';
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

        _filterComboCmp.setValue(null);
        _operatorCmp.setValue('=');

        me.getView().down('#filtersToolbar').remove('sqlFilterValue');

        me.getView().down('#sqlFilters').setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('sqlFilterData').length));
        me.getView().down('#sqlFilters').getStore().reload();
        me.refreshData();
    },

    removeSqlFilter: function (table, rowIndex) {
        var me = this, vm = me.getViewModel();
        Ext.Array.removeAt(vm.get('sqlFilterData'), rowIndex);

        me.getView().down('#filtersToolbar').remove('sqlFilterValue');

        me.getView().down('#sqlFilters').setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('sqlFilterData').length));
        me.getView().down('#sqlFilters').getStore().reload();
        me.refreshData();
    },

    onColumnChange: function (cmp, newValue) {
        var me = this;

        cmp.up('toolbar').remove('sqlFilterValue');

        if (!newValue) { return; }
        var column = Ext.Array.findBy(me.tableConfig.columns, function (column) {
            return column.dataIndex === newValue;
        });

        if (column.widgetField) {
            column.widgetField.itemId = 'sqlFilterValue';
            cmp.up('toolbar').insert(4, column.widgetField);
        } else {
            cmp.up('toolbar').insert(4, {
                xtype: 'textfield',
                itemId: 'sqlFilterValue',
                value: ''
            });
        }
    },

    onFilterKeyup: function (cmp, e) {
        if (e.keyCode === 13) {
            this.addSqlFilter();
        }
    },

    sqlFilterQuickItems: function (btn) {
        var me = this, menuItem, menuItems = [], col;
        Rpc.asyncData('rpc.reportsManager.getConditionQuickAddHints').then(function (result) {
            Ext.Object.each(result, function (key, vals) {
                menuItem = {
                    text: TableConfig.getColumnHumanReadableName(key),
                    disabled: vals.length === 0
                };
                if (vals.length > 0) {
                    menuItem.menu = {
                        plain: true,
                        items: Ext.Array.map(vals, function (val) {
                            return {
                                text: val,
                                column: key
                            };
                        }),
                        listeners: {
                            click: 'selectQuickFilter'
                        }
                    };
                }
                menuItems.push(menuItem);


            });
            btn.getMenu().removeAll();
            btn.getMenu().add(menuItems);
        });
    },

    selectQuickFilter: function (menu, item) {
        var me = this, vm = this.getViewModel(),
            _filterComboCmp = me.getView().down('#sqlFilterCombo'),
            _operatorCmp = me.getView().down('#sqlFilterOperator');

        vm.get('sqlFilterData').push({
            column: item.column,
            operator: '=',
            value: item.text,
            javaClass: 'com.untangle.app.reports.SqlCondition'
        });

        _filterComboCmp.setValue(null);
        _operatorCmp.setValue('=');

        me.getView().down('#filtersToolbar').remove('sqlFilterValue');

        me.getView().down('#sqlFilters').setTitle(Ext.String.format('Conditions: {0}'.t(), vm.get('sqlFilterData').length));
        me.getView().down('#sqlFilters').getStore().reload();
        me.refreshData();

    },

    // END FILTERS


    // // DASHBOARD ACTION
    dashboardAddRemove: function (btn) {
        var vm = this.getViewModel(), widget = vm.get('widget'), entry = vm.get('entry'), action;

        if (!widget) {
            action = 'add';
            widget = Ext.create('Ung.model.Widget', {
                displayColumns: entry.get('displayColumns'),
                enabled: true,
                entryId: entry.get('uniqueId'),
                javaClass: 'com.untangle.uvm.DashboardWidgetSettings',
                refreshIntervalSec: 60,
                timeframe: '',
                type: 'ReportEntry'
            });
        } else {
            action = 'remove';
        }

        Ext.fireEvent('widgetaction', action, widget, entry, function (wg) {
            vm.set('widget', wg);
            Util.successToast('<span style="color: yellow; font-weight: 600;">' + vm.get('entry.title') + '</span> ' + (action === 'add' ? 'added to' : 'removed from') + ' Dashboard!');
        });
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

                Ext.getStore('reportstree').build(); // rebuild tree after save new
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

                Ext.getStore('reportstree').build(); // rebuild tree after save new
            });
    },

    removeReport: function () {
        var me = this, v = this.getView(),
            vm = this.getViewModel(),
            entry = vm.get('entry');

        if (vm.get('widget')) {
            Ext.MessageBox.confirm('Warning'.t(),
                'Deleting this report will remove also the Widget from Dashboard!'.t() + '<br/><br/>' +
                'Do you want to continue?'.t(),
                function (btn) {
                    if (btn === 'yes') {
                        // remove it from dashboard first
                        Ext.fireEvent('widgetaction', 'remove', vm.get('widget'), entry, function (wg) {
                            vm.set('widget', wg);
                            me.removeReportAction(entry.getData());
                        });
                    }
                });
        } else {
            me.removeReportAction(entry.getData());
        }

    },

    removeReportAction: function (entry) {
        Rpc.asyncData('rpc.reportsManager.removeReportEntry', entry)
            .then(function (result) {
                Ung.app.redirectTo('#reports/' + entry.category.replace(/ /g, '-').toLowerCase());
                Util.successToast(entry.title + ' ' + 'deleted successfully'.t());

                var removableRec = Ext.getStore('reports').findRecord('uniqueId', entry.uniqueId);
                if (removableRec) {
                    Ext.getStore('reports').remove(removableRec); // remove record
                    Ext.getStore('reportstree').build(); // rebuild tree after save new
                }
            }, function (ex) {
                Util.handleException(ex);
            });
    },

    downloadGraph: function () {
        var view = this.getView(), vm = this.getViewModel(), now = new Date();
        try {
            this.getView().down('#graphreport').getController().chart.exportChart({
                filename: (vm.get('entry.category') + '-' + vm.get('entry.title') + '-' + Ext.Date.format(now, 'd.m.Y-Hi')).replace(/ /g, '_'),
                type: 'image/png'
            });
        } catch (ex) {
            console.log(ex);
            Util.handleException('Unable to download!');
        }
    },

    exportEventsHandler: function () {
        var me = this, vm = me.getViewModel(), entry = vm.get('entry').getData(), columns = [];
        if (!entry) { return; }

        var grid = me.getView().down('eventreport > ungrid');

        if (!grid) {
            console.log('Grid not found');
            return;
        }

        Ext.Array.each(grid.getColumns(), function (col) {
            if (col.dataIndex && !col.hidden) {
                columns.push(col.dataIndex);
            }
        });

        var conditions = [];
        Ext.Array.each(Ext.clone(vm.get('sqlFilterData')), function (cnd) {
            delete cnd._id;
            conditions.push(cnd);
        });

        Ext.MessageBox.wait('Exporting Events...'.t(), 'Please wait'.t());
        var downloadForm = document.getElementById('downloadForm');
        downloadForm['type'].value = 'eventLogExport';
        downloadForm['arg1'].value = (entry.category + '-' + entry.title + '-' + Ext.Date.format(new Date(), 'd.m.Y-Hi')).replace(/ /g, '_');
        downloadForm['arg2'].value = Ext.encode(entry);
        downloadForm['arg3'].value = conditions.length > 0 ? Ext.encode(conditions) : '';
        downloadForm['arg4'].value = columns.join(',');
        downloadForm['arg5'].value = vm.get('_sd') ? vm.get('_sd').getTime() : -1;
        downloadForm['arg6'].value = vm.get('_ed') ? vm.get('_ed').getTime() : -1;
        downloadForm.submit();
        Ext.MessageBox.hide();
    },

    exportGraphData: function (btn) {
        var me = this, vm = me.getViewModel(), entry = vm.get('entry').getData(), columns = [], headers = [];
        if (!entry) { return; }

        var grid = btn.up('grid'), csv = [];

        if (!grid) {
            console.log('Grid not found');
            return;
        }

        var processRow = function (row) {
            var data = [], j, innerValue;
            for (j = 0; j < row.length; j += 1) {
                innerValue = !row[j] ? '' : row[j].toString();
                data.push('"' + innerValue.replace(/"/g, '""') + '"');
            }
            return data.join(',') + '\r\n';
        };

        Ext.Array.each(grid.getColumns(), function (col) {
            if (col.dataIndex && !col.hidden) {
                columns.push(col.dataIndex);
                headers.push(col.text);
            }
        });
        csv.push(processRow(headers));

        grid.getStore().each(function (row, idx) {
            var r = [];
            for (j = 0; j < columns.length; j += 1) {
                if (columns[j] === 'time_trunc') {
                    r.push(Util.timestampFormat(row.get('time_trunc')));
                } else {
                    r.push(row.get(columns[j]));
                }
            }
            csv.push(processRow(r));
        });

        me.download(csv.join(''), (entry.category + '-' + entry.title + '-' + Ext.Date.format(new Date(), 'd.m.Y-Hi')).replace(/ /g, '_') + '.csv', 'text/csv');

    },

    download: function(content, fileName, mimeType) {
        var a = document.createElement('a');
        mimeType = mimeType || 'application/octet-stream';

        if (navigator.msSaveBlob) { // IE10
            return navigator.msSaveBlob(new Blob([ content ], {
                type : mimeType
            }), fileName);
        } else if ('download' in a) { // html5 A[download]
            a.href = 'data:' + mimeType + ',' + encodeURIComponent(content);
            a.setAttribute('download', fileName);
            document.body.appendChild(a);
            setTimeout(function() {
                a.click();
                document.body.removeChild(a);
            }, 100);
            return true;
        } else { //do iframe dataURL download (old ch+FF):
            var f = document.createElement('iframe');
            document.body.appendChild(f);
            f.src = 'data:' + mimeType + ',' + encodeURIComponent(content);
            setTimeout(function() {
                document.body.removeChild(f);
            }, 400);
            return true;
        }
    },

    setAutoRefresh: function (btn) {
        var me = this,
            vm = this.getViewModel();
        vm.set('autoRefresh', btn.pressed);

        if (btn.pressed) {
            me.refreshData();
        } else {
            if (me.refreshTimeout) {
                clearInterval(me.refreshTimeout);
            }
        }

    },

});
