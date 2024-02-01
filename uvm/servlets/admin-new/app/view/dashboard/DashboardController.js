/**
 * Dashboard Controller which displays and manages the Dashboard Widgets
 * Widgets can be affected by following actions:
 * - remove/add/modify widget entry itself;
 * - install/uninstall Reports or start/stop Reports service
 * - install/uninstall Apps which can lead in a report widget to be available or not;
 * - modifying a report that is used by a widget, which requires reload of that affected widget
 */
Ext.define('Ung.view.dashboard.DashboardController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.dashboard',
    viewModel: true,
    control: {
        '#': {
            afterrender: 'onAfterRender',
            activate: 'onActivate',
            deactivate: 'onDeactivate',
        },
        '#dashboard': {
            resize: 'onResize'
        }
    },

    widgetsRendered: false,

    listen: {
        store: {
            '#stats': {
                datachanged: 'onStatsUpdate'
            }
        }
    },

    onAfterRender: function () {
        var me = this, vm = me.getViewModel(),
            dashboardCmp = me.getView().down('#dashboard'),
            text;


        // add scroll/resize events which will triger widgets update
        dashboardCmp.body.on('scroll', me.debounce(me.updateVisibleWidgets, 300));
        dashboardCmp.getEl().on('resize', me.debounce(me.updateVisibleWidgets, 300));

        /**
         * Fetch dashboard settings
         */
        Rpc.asyncData('rpc.dashboardManager.getSettings')
            .then(function (result) {
                // initially the timeframe could be null
                if (!result.timeframe) {
                    result.timeframe = 1;
                }
                Ung.dashboardSettings = result;
                Ext.getStore('widgets').loadData(result.widgets.list);

                if (result.timeframe === 1) {
                    text = '1 Hour ago'.t();
                } else {
                    text = result.timeframe + ' ' + 'Hours ago'.t();
                }

                me.getView().down('#since > button').setText(text);

                Ung.app.reportscheck();
            });

        /**
         * On global conditions change refetch data based on new conditions
         * Using {query.string} because it fires only when the value changes, unlike {query} only
         */
        vm.bind('{query.string}', function () {
            Ext.defer(function() {
                me.updateVisibleWidgets();
            }, 1000);
        });

    },

    /**
     * Helper method used to slowfire resize or scroll events
     */
    debounce: function (fn, delay) {
        var timer = null;
        var me = this;
        return function () {
            clearTimeout(timer);
            timer = setTimeout(function () {
                fn.apply(me, arguments);
            }, delay);
        };
    },

    /**
     * Checks if widget is visible or not in viewport
     * Based on it's visibility it will be added to queue for fetching data
     */
    updateVisibleWidgets: function () {
        var dashboard = this.lookup('dashboard'),
            widgets = dashboard.query('reportwidget'),
            graphReport;
        DashboardQueue.isVisible(dashboard.down('networklayoutwidget'));
        DashboardQueue.isVisible(dashboard.down('mapdistributionwidget'));
        DashboardQueue.isVisible(dashboard.down('networkinformationwidget'));
        DashboardQueue.isVisible(dashboard.down('policyoverviewwidget'));
        DashboardQueue.isVisible(dashboard.down('notificationswidget'));
        Ext.Array.each(widgets, function (widget) {
            if (widget) {
                graphReport = widget.down('graphreport');
                // important to reflow the chart when widget changes size
                if (graphReport && graphReport.getController().chart) {
                    graphReport.getController().chart.reflow();
                }
                DashboardQueue.isVisible(widget);
            }
        });
    },

    updateSince: function (menu, item) {
        var me = this, dashboard = me.lookup('dashboard');
        menu.up('button').setText(item.text);
        Ung.dashboardSettings.timeframe = item.value;

        Rpc.asyncData('rpc.dashboardManager.setSettings', Ung.dashboardSettings)
            .then(function() {
                Ext.Array.each(dashboard.query('reportwidget'), function (widgetCmp) {
                    widgetCmp.lastFetchTime = null;
                });
                me.updateVisibleWidgets();
            });

    },


    onResize: function (view) {
        if (view.down('window')) {
            view.down('window').close();
        }
    },

    toggleManager: function () {
        var me = this, vm = me.getViewModel(),
            columns = me.lookup('dashboardManager').getColumns();

        vm.set('managerVisible', !vm.get('managerVisible'));
        if (!vm.get('managerVisible')) {
            columns[0].setHidden(true);
        }
    },

    onStatsUpdate: function() {
        var vm = this.getViewModel();
        vm.set('stats', Ext.getStore('stats').first());

        Rpc.asyncData('rpc.deviceTable.getDevices')
            .then( function(result){
                if(Util.isDestroyed(vm)){
                    return;
                }
                vm.set('deviceCount', result.list.length);
            },function(ex){
                Util.handleException(ex);
            });
    },

    onActivate: function () {
        DashboardQueue.paused = false;
        this.updateVisibleWidgets();
    },

    onDeactivate: function () {
        DashboardQueue.paused = true;
        var vm = this.getViewModel();
        if (vm.get('managerVisible')) {
            this.toggleManager();
        }
    }

});
