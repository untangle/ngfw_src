Ext.define('Ung.chart.TimeChart', {
    extend: 'Ext.container.Container',
    alias: 'widget.timechart',
    /* requires-start */
    requires: [
        'Ung.chart.TimeChartController'
    ],
    /* requires-end */
    controller: 'timechart',
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
        reference: 'timechart',
        cls: 'chart'
    }]
});
