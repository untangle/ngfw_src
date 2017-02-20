Ext.define('Ung.chart.NodeChart', {
    extend: 'Ext.container.Container',
    alias: 'widget.nodechart',
    requires: [
        'Ung.chart.NodeChartController'
    ],

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
