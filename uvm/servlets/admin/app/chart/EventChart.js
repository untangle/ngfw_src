Ext.define('Ung.chart.EventChart', {
    extend: 'Ext.grid.Panel',
    alias: 'widget.eventchart',

    /* requires-start */
    requires: [
        'Ung.chart.EventChartController',
        'Ung.util.TableConfig'
    ],
    /* requires-end */
    controller: 'eventchart',
    viewModel: {
        stores: {
            store: {
                data: '{customData}'
            }
        }
    },

    plugins: 'gridfilters',

    config: {
        entry: null
    },

    bind: {
        store: '{store}'
    },

    border: false,

    initComponent: function () {
        var me = this,
            col, columns = [],
            columnsConfig = Ung.TableConfig.getColumns();

        Ung.TableConfig.table[me.getEntry().get('table')].forEach(function (column) {
            // add columns from TableConfig and apply dataIndex
            col = columnsConfig[column] || { header: column };
            Ext.apply(col, {
                dataIndex: column,
                hidden: !Ext.Array.contains(me.getEntry().get('defaultColumns'), column)
            });
            columns.push(col);
        });

        // me.getEntry().get('defaultColumns')

        //var filterFeature = Ext.create('Ung.chart.EventFilter', {});


        Ext.apply(this, {
            columns: columns
        });
        this.callParent(arguments);

        //this.getStore().addFilter(filterFeature.globalFilter);
    }

});
