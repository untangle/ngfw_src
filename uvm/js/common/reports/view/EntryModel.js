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
            return tableConfig.comboItems;
        },

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

        // _props: function (get) {
        //     return get('entry').getData();
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
