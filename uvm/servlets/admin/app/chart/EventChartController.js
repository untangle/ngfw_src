Ext.define('Ung.chart.EventChartController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.eventchart',

    control: {
        '#': { setdata: 'onSetData', beginfetchdata: 'onBeforeData' }
    },

    onBeforeData: function () {
        this.getView().setLoading('Querying Database...'.t());
    },

    onSetData: function (data) {
        this.getView().setLoading(false);
        this.getViewModel().set('customData', data);
        //console.log(data);
    }
});
