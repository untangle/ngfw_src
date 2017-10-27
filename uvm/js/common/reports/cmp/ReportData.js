Ext.define('Ung.reports.cmp.ReportData', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.reportdata',

    viewModel: true,

    border: false,
    store: { data: [] },
    columns: [],
    bbar: ['->', {
        itemId: 'exportGraphData',
        text: 'Export Data'.t(),
        iconCls: 'fa fa-external-link-square',
        handler: 'exportGraphData'
    }],

    controller: {
        control: {
            '#': {
                afterrender: 'onAfterRender',
                deactivate: 'onDeactivate'
            }
        },

        viewModel: true,

        onAfterRender: function () {
            var me = this, vm = me.getViewModel();
            vm.bind('{entry}', function () {
                me.getView().setColumns([]);
                me.getView().getStore().loadData([]);
            });

            vm.bind('{fetching}', function (value) {
                me.getView().setLoading(value);
            });
            vm.bind('{reportData}', function (data) {
                switch (vm.get('entry.type')) {
                case 'TEXT':               me.formatTextData(data); break;
                case 'PIE_GRAPH':          me.formatPieData(data); break;
                case 'TIME_GRAPH':         me.formatTimeData(data); break;
                case 'TIME_GRAPH_DYNAMIC': me.formatTimeDynamicData(data); break;
                }
            });
        },

        onDeactivate: function () {
            // this.getView().setHtml('');
        },

        formatTextData: function (data) {
            var vm = this.getViewModel(),
                entry = vm.get('eEntry') || vm.get('entry'), i, column;

            this.getView().setColumns([{
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
            this.getView().getStore().loadData(reportData);
        },

        formatTimeData: function (data) {
            var me = this, vm = this.getViewModel(),
                entry = vm.get('eEntry') || vm.get('entry'), i, column;

            var reportDataColumns = [{
                dataIndex: 'time_trunc',
                header: 'Timestamp'.t(),
                width: 130,
                flex: 1,
                renderer: function (val) {
                    return (!val) ? 0 : Util.timestampFormat(val);
                }
            }];
            var title;

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

            me.getView().setColumns(reportDataColumns);
            me.getView().getStore().loadData(data);
        },

        formatTimeDynamicData: function (data) {
            var vm = this.getViewModel(),
                entry = vm.get('entry'),
                timeDataColumns = [], i, column;


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

            this.getView().setColumns(reportDataColumns);
            this.getView().getStore().loadData(data);
            // vm.set('_currentData', data);
        },

        formatPieData: function (data) {
            var me = this, vm = me.getViewModel(),
                entry = vm.get('eEntry') || vm.get('entry');

            var header = '<strong>' + TableConfig.getColumnHumanReadableName(entry.get('pieGroupColumn')) + '</strong> <span style="float: right;">[' + entry.get('pieGroupColumn') + ']</span>';

            me.getView().setColumns([{
                dataIndex: entry.get('pieGroupColumn'),
                header: header,
                flex: 1,
                renderer: Renderer[entry.get('pieGroupColumn')] || null
            }, {
                dataIndex: 'value',
                header: 'value'.t(),
                width: 200,
                renderer: function (value) {
                    if (entry.get('units') === 'bytes' || entry.get('units') === 'bytes/s') {
                        return Util.bytesToHumanReadable(value, true);
                    } else {
                        return value;
                    }
                }
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
            me.getView().getStore().loadData(data);
        },

        addPieFilter: function (view, rowIndex, colIndex, item, e, record) {
            var me = this, vm = me.getViewModel(),
                // gridFilters =  me.getView().down('#sqlFilters'),
                col = vm.get('entry.pieGroupColumn');

            if (col) {
                me.getView().up('entry').down('globalconditions').getStore().add({
                    column: col,
                    operator: '=',
                    value: record.get(col),
                    javaClass: 'com.untangle.app.reports.SqlCondition'
                });
            } else {
                console.log('Issue with pie column!');
                return;
            }
        }

    }

});
