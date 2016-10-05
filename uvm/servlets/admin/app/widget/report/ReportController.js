Ext.define('Ung.widget.report.ReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.reportwidget',
    //stores: ['widgetsStore'],
    init: function (view) {
        this.getViewModel().set('widget', view.getWidget());
        this.getViewModel().set('entry', view.getEntry());

        var type = view.getEntry().get('type');

        if (type === 'TIME_GRAPH' || type === 'TIME_GRAPH_DYNAMIC') {
            view.add({ xtype: 'timechart', reference: 'chart', height: 250 });
        }

        if (type === 'PIE_GRAPH') {
            view.add({ xtype: 'piechart', reference: 'chart',  height: 250 });
        }

        if (type === 'EVENT_LIST') {
            view.add({ xtype: 'component', html: 'Not Implemented',  height: 250 });
        }
    },

    fetchData: function () {
        var me = this,
            entry = me.getViewModel().get('entry'),
            timeframe = me.getViewModel().get('widget.timeframe');

        if (entry.get('type') === 'EVENT_LIST') {
            // fetch event data
            //console.log('Event List');
        } else {
            // fetch chart data
            me.getView().lookupReference('chart').fireEvent('beginfetchdata');
            Rpc.getReportData(entry.getData(), timeframe)
                .then(function (response) {
                    me.getView().lookupReference('chart').fireEvent('setseries', response.list);
                }, function (exception) {
                    console.log(exception);
                });
        }
    },

    /*
    showEditor: function () {
        this.getView().up('dashboardmain').fireEvent('showwidgeteditor', this.getView().getWidget());
    },
    */

    resizeWidget: function () {
        var view = this.getView();
        if (view.hasCls('small')) {
            view.removeCls('small').addCls('medium');
        } else {
            if (view.hasCls('medium')) {
                view.removeCls('medium').addCls('large');
            } else {
                if (view.hasCls('large')) {
                    view.removeCls('large').addCls('x-large');
                } else {
                    if (view.hasCls('x-large')) {
                        view.removeCls('x-large').addCls('small');
                    }
                }
            }
        }
        view.updateLayout();
    }

});
