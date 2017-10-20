Ext.define('Ung.view.reports.EntryModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.entry',

    data: {
        eEntry: null, // editable entry, copy of the selected entry
        _currentData: [],
        sqlFilterData: [],
        autoRefresh: false,

        f_startdate: null,
        f_enddate: null,

        tableColumns: [],

        textColumns: [],
        textColumnsCount: 0, // used for grids validation
        timeDataColumns: [],
        timeDataColumnsCount: 0, // used for grids validation

        validForm: true
    },

    stores: {
        textColumnsStore: {
            data: '{textColumns}',
            listeners: {
                datachanged: 'onTextColumnsChanged',
                update: 'onTextColumnsChanged'
            }
        },
        timeDataColumnsStore: {
            data: '{timeDataColumns}',
            listeners: {
                datachanged: 'onTimeDataColumnsChanged',
                update: 'onTimeDataColumnsChanged'
            }
        }
    },

    formulas: {
        // f_activeReportCard: function (get) {
        //     var reportCard = 'textreport', type;
        //     if (get('eEntry.type')) {
        //         type = get('eEntry.type');
        //     } else {
        //         type = get('entry.type');
        //     }
        //     console.log(type);
        //     switch(type) {
        //     case 'TEXT': reportCard = 'textreport'; break;
        //     case 'PIE_GRAPH': reportCard = 'graphreport'; break;
        //     case 'TIME_GRAPH': reportCard = 'graphreport'; break;
        //     case 'TIME_GRAPH_DYNAMIC': reportCard = 'graphreport'; break;
        //     case 'EVENT_LIST': reportCard = 'eventreport'; break;
        //     }
        //     return reportCard;
        // },

        f_tableColumns: function (get) {
            var table = get('eEntry.table'), tableConfig, defaultColumns;

            if (!table) { return []; }

            tableConfig = TableConfig.generate(table);

            if (get('eEntry.type') !== 'EVENT_LIST') {
                return tableConfig.comboItems;
            }

            // for EVENT_LIST setup the columns
            defaultColumns = Ext.clone(get('eEntry.defaultColumns'));

            // initially set none as default
            Ext.Array.each(tableConfig.comboItems, function (item) {
                item.isDefault = false;
            });

            Ext.Array.each(get('eEntry.defaultColumns'), function (defaultColumn) {
                var col = Ext.Array.findBy(tableConfig.comboItems, function (item) {
                    return item.value === defaultColumn;
                });
                // remove default columns if not in TableConfig
                if (!col) {
                    // vm.set('eEntry.defaultColumns', Ext.Array.remove(defaultColumns, defaultColumn));
                } else {
                    // otherwise set it as default
                    col.isDefault = true;
                }
            });
            console.log(tableConfig.comboItems);
            return tableConfig.comboItems;
            // me.fetchData();
        },
        // f_startdate: function (get) {
        //     if (!get('r_customRangeCk.value')) {
        //         return Util.serverToClientDate(new Date((Math.floor(rpc.systemManager.getMilliseconds()/600000) * 600000) - get('sinceDate.value') * 3600 * 1000));
        //     }
        // },
        // f_startdate: {
        //     get: function (get) {
        //         return Util.serverToClientDate(new Date((Math.floor(rpc.systemManager.getMilliseconds()/600000) * 600000) - get('sinceDate.value') * 3600 * 1000));
        //     },
        //     // set: function (date) {}
        // },
        // f_enddate: function (get) {
        //     if (!get('r_customRangeCk.value')) {
        //         return null;
        //     }
        //     // return Ext.Date.clearTime(get('endDate.value'));
        // },

        f_textColumnsCount: function (get) {
            return get('textColumns').length;
        },

        f_approximation: {
            get: function (get) {
                return get('eEntry.approximation') || 'sum';
            },
            set: function (value) {
                this.set('eEntry.approximation', value !== 'sum' ? value : null);
            }
        },

        _sqlConditions: {
            get: function (get) {
                return get('eEntry.conditions') || [];
            },
            set: function (value) {
                this.set('eEntry.conditions', value);
                this.set('_sqlTitle', '<i class="fa fa-filter"></i> ' + 'SQL Conditions:'.t() + ' (' + value.length + ')');
               // return get('entry.conditions') || [];
            },
        },

        _props: function (get) {
            return get('entry').getData();
        },

        // _colorsStr: {
        //     get: function (get) {
        //         if (get('entry.colors')) {
        //             return get('entry.colors').join(',');
        //         } else {
        //             return '';
        //         }
        //     },
        //     set: function (value) {
        //         var str = value.replace(/ /g, '');
        //         if (value.length > 0) {
        //             this.set('entry.colors', value.split(','));
        //         } else {
        //             this.set('entry.colors', null);
        //         }
        //     }
        // },

        f_tableColumnsSource: function (get) {
            var columns = get('tableColumns'), source = {};
            if (!columns || columns.length === 0) { return {}; }
            Ext.Array.each(columns, function (column) {
                source[column.text] = column.value;
            });
            return source;
        }
    }

});
