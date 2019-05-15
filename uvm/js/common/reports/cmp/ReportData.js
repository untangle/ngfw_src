Ext.define('Ung.reports.cmp.ReportData', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.reportdata',

    viewModel: true,

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
                deactivate: 'onDeactivate',
                cellclick: 'onCellClick'
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
                entry = vm.get('eEntry') || vm.get('entry'), i, column, title;

            var reportDataColumns = [{
                dataIndex: 'time_trunc',
                header: 'Timestamp'.t(),
                width: 130,
                flex: 1,
                renderer: Renderer.timestamp,
                sortable: true
            }];
            var reportDataFields = [
                { name: 'time_trunc', sortType: 'asTimestamp' }
            ];

            for (i = 0; i < entry.get('timeDataColumns').length; i += 1) {
                column = entry.get('timeDataColumns')[i].split(' ').splice(-1)[0];
                title = column;
                reportDataColumns.push({
                    dataIndex: column,
                    header: title,
                    width: entry.get('timeDataColumns').length > 2 ? 60 : 90,
                    renderer: function (val) {
                        return val !== undefined ? val : '-';
                    },
                    sortable: true
                });
                reportDataFields.push({
                    name: column, sortType: 'asFloat'
                });
            }

            me.getView().setColumns(reportDataColumns);
            me.getView().getStore().setFields(reportDataFields);
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
                renderer: Renderer.timestamp
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
                tdCls: 'anchor',
                field: entry.get('pieGroupColumn'),
                table: entry.get('table'),
                renderer: Renderer[entry.get('pieGroupColumn')] || TableConfig.getDisplayValue
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
            }]);
            me.getView().getStore().loadData(data);
        },

        /**
         * exports graph data
         */
        exportGraphData: function (btn) {
            var me = this, vm = me.getViewModel(), entry = vm.get('entry').getData(), columns = [], headers = [], j;
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

            grid.getStore().each(function (row) {
                var r = [];
                for (j = 0; j < columns.length; j += 1) {
                    if (columns[j] === 'time_trunc') {
                        r.push(Renderer.timestamp(row.get('time_trunc')));
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
        /**
         * used for applying/replace new global condition
         */
        onCellClick: function (cell, td, cellIndex, record, tr, rowIndex) {
            var me = this, entry = me.getViewModel().get('entry'), grid = me.getView(), column, value;

            // works only for PIE_GRAPH Data View
            if (entry.get('type') !== 'PIE_GRAPH') {
                return;
            }

            if (cellIndex === 0) {
                // fire event to add global condition, implemented in GlobalConditions.js controller section
                Ext.fireEvent('addglobalcondition', entry.get('table'), entry.get('pieGroupColumn'), record.get(entry.get('pieGroupColumn')));
            }
        }

    }

});
