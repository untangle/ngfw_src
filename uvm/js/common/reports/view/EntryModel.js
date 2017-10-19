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

        textColumns: [],
        textColumnsCount: 0, // used for grids validation
        timeDataColumns: [],
        timeDataColumnsCount: 0, // used for grids validation
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
        // f_startdate: function (get) {
        //     if (!get('customRange.value')) {
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
        //     if (!get('customRange.value')) {
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
