Ext.define('Ung.chart.PieChart', {
    extend: 'Ext.container.Container',
    alias: 'widget.piechart',
    /* requires-start */
    requires: [
        'Ung.chart.PieChartController'
    ],
    /* requires-end */
    controller: 'piechart',
    viewModel: true,

    config: {
        widget: null,
    },

    listeners: {
        afterrender: 'onAfterRender',
        resize: 'onResize'
    },

    items: [{
        xtype: 'component',
        reference: 'piechart',
        cls: 'chart'
    }]
});
