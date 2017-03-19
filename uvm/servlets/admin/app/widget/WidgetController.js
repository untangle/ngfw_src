Ext.define('Ung.widget.WidgetController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.widget',

    control: {
        '#': {
            afterrender: 'onAfterRender',
            afterdata: 'onAfterData',
            beforedestroy: 'onBeforeRemove'
            // show: 'onShow'
        },
        '#header': {
            render: 'headerRender'
        }
    },

    listen: {
        // store: {
        //     '#stats': {
        //         datachanged: 'onStatsUpdate'
        //     }
        // }
    },

    headerRender: function (cmp) {
        var me = this;
        cmp.getEl().on({
            click: function (e) {
                if (e.target.dataset.action === 'refresh') {
                    me.addToQueue();
                }
                // if (e.target.dataset.action === 'style') {
                //     var vm = me.getViewModel();
                //     var styles = ['LINE', 'AREA', 'BAR', 'BAR_OVERLAPPED', 'BAR_STACKED'];
                //     var style = vm.get('entry.timeStyle');
                //     var idx = Ext.Array.indexOf(styles, style);
                //     if (idx < styles.length - 1) {
                //         vm.set('entry.timeStyle', styles[idx + 1]);
                //     } else {
                //         vm.set('entry.timeStyle', styles[0]);
                //     }
                // }
            }
        });
    },

    init: function (view) {
        var vm = view.getViewModel(), entryType;
        if (vm.get('entry')) {
            entryType = vm.get('entry.type');
            if (entryType === 'TIME_GRAPH' || entryType === 'TIME_GRAPH_DYNAMIC') {
                view.add({ xtype: 'graphreport', itemId: 'report-widget',  reference: 'chart', height: 260, widgetDisplay: true });
            }

            if (entryType === 'PIE_GRAPH') {
                view.add({ xtype: 'graphreport', itemId: 'report-widget', reference: 'chart',  height: 260, widgetDisplay: true });
            }

            if (entryType === 'TEXT') {
                view.add({ xtype: 'textreport', itemId: 'report-widget', height: 260 });
            }


            if (entryType === 'EVENT_LIST') {
                view.add({ xtype: 'component', html: 'Not Implemented',  height: 260 });
            }
        }
    },

    onAfterRender: function (widget) {
        setTimeout(function () {
            widget.removeCls('adding');
        }, 100);


        widget.getViewModel().bind('{widget.enabled}', function (enabled) {
            if (enabled && Ext.isFunction(widget.fetchData)) {
                Ung.view.dashboard.Queue.add(widget);
            }
        });
        widget.getViewModel().notify();
    },

    onAfterData: function () {
        var widget = this.getView();
        Ung.view.dashboard.Queue.next();
        if (widget.refreshIntervalSec && widget.refreshIntervalSec > 0) {
            widget.refreshTimeoutId = setTimeout(function () {
                Ung.view.dashboard.Queue.add(widget);
            }, widget.refreshIntervalSec * 1000);
        }
    },

    onBeforeRemove: function (widget) {
        // remove widget from queue if; important if removal is happening while fetching data
        Ung.view.dashboard.Queue.remove(widget);
    },

    onShow: function (widget) {
        console.log('onShow');
        widget.removeCls('adding');
        // console.log('on show', widget.getViewModel().get('widget.type'));
        // if (Ext.isFunction(widget.fetchData)) {
        //     Ung.view.dashboard.Queue.add(widget);
        // }
    },

    addToQueue: function () {
        var widget = this.getView();
        if (widget.refreshTimeoutId) {
            clearTimeout(widget.refreshTimeoutId);
        }
        Ung.view.dashboard.Queue.addFirst(widget);
    },


    // not used
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
