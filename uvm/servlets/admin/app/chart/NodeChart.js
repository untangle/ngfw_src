Ext.define('Ung.chart.NodeChart', {
    extend: 'Ext.container.Container',
    alias: 'widget.nodechart',
    /* requires-start */
    requires: [
        'Ung.chart.NodeChartController'
    ],
    /* requires-end */
    controller: 'nodechart',
    viewModel: true,

    listeners: {
        afterrender: 'onAfterRender',
        addPoint: 'onAddPoint'
        //resize: 'onResize',
        //setseries: 'onSetSeries',
        //beginfetchdata: 'onBeginFetchData'
    },

    items: [{
        xtype: 'component',
        reference: 'nodechart',
        width: 400,
        height: 150
    }]
});
