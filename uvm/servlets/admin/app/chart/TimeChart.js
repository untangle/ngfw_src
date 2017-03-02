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
        entry: null
    },

    listeners: {
        afterrender: 'onAfterRender',
        resize: 'onResize',
        setseries: 'onSetSeries',
        //setstyle: 'onSetStyle',
        //setcolors: 'onSetColors',
        beginfetchdata: 'onBeginFetchData'
    },

    items: [{
        xtype: 'component',
        reference: 'timechart',
        cls: 'chart'
    }, {
        xtype: 'component',
        reference: 'loader',
        cls: 'loader',
        hideMode: 'visibility'
    }]
});
