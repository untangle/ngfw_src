Ext.define('Ung.view.reports.ReportWizardModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.newreport',

    data: {
        newEntry: {
            approximation: 'sum',
            category: '',
            colors: [],
            conditions: [],
            defaultColumns: [],
            description: '',
            displayOrder: null,
            enabled: true,
            javaClass: '',

            orderByColumn: '',
            orderDesc: false,

            pieGroupColumn: '',
            pieNumSlices: 10,
            pieStyle: 'PIE',
            pieSumColumn: '',

            readOnly: false,
            seriesRenderer: '',
            table: 'sessions',
            textColumns: [],
            textString: 'some text',

            timeDataColumns: [],
            timeDataDynamicAggregationFunction: 'avg',
            timeDataDynamicAllowNull: false,
            timeDataDynamicColumn: '',
            timeDataDynamicLimit: '',
            timeDataDynamicValue: '',
            timeDataInterval: 'MINUTE',
            timeStyle: 'LINE',
            title: '',
            type: 'TEXT', // TEXT, PIE_GRAPH, TIME_GRAPH, TIME_GRAPH_DYNAMIC, EVENT_LIST
            units: ''
        },


    },

    formulas: {
        newEntrySqlConditions: {
            get: function (get) {
                return get('newEntry.conditions') || [];
            },
            set: function (value) {
                this.set('newEntry.conditions', value);
                this.set('newEntrySqlTitle', '<i class="fa fa-filter"></i> ' + 'Sql Conditions:'.t() + ' (' + value.length + ')');
            },
        },
    },

    // stores: {
    //     timeDataColumnsStore: {
    //         data: '{timeDataColumns}', // defined as a formula
    //         proxy: {
    //             type: 'memory',
    //             reader: { type: 'json' }
    //         }
    //     },
    //     textDataColumnsStore: {
    //         data: '{textDataColumns}', // defined as a formula
    //         proxy: {
    //             type: 'memory',
    //             reader: { type: 'json' }
    //         }
    //     }
    // }
});
