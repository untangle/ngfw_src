Ext.define('Ung.chart.PieChart', {
    extend: 'Ext.container.Container',
    alias: 'widget.piechart',
    requires: [
        'Ung.chart.PieChartController'
    ],

    controller: 'piechart',
    viewModel: true,

    config: {
        widget: null,
        entry: null
    },

    listeners: {
        afterrender: 'onAfterRender',
        resize: 'onResize',
        setseries: 'onSetSeries',
        //setstyle: 'onSetStyle',
        beginfetchdata: 'onBeginFetchData'
    },

    items: [{
        xtype: 'component',
        reference: 'piechart',
        cls: 'chart'
    }, {
        xtype: 'component',
        reference: 'loader',
        cls: 'loader',
        hideMode: 'visibility',
        html: '<div class="spinner"><div class="bounce1"></div><div class="bounce2"></div><div class="bounce3"></div></div>'
    }]
});
