Ext.define('Ung.cmp.AppMetrics', {
    extend: 'Ext.grid.property.Grid',
    alias: 'widget.appmetrics',
    title: 'Metrics'.t(),
    border: false,
    scrollable: true,

    nameColumnWidth: 250,
    hideHeaders: true,

    viewModel: {
        data: {
            metrics: null
        }
    },

    disabled: true,
    bind: {
        disabled: '{!state.on}',
        source: '{metrics}'
    },

    controller: {
        updateMetricsCount: 0,
        control: {
            '#': {
                afterrender: function () {
                    var me = this;
                    me.getViewModel().bind('{instance.runState}', function () {
                        me.updateMetrics();
                    });
                }
            }
        },
        listen: {
            store: {
                '#metrics': {
                    datachanged: 'updateMetrics'
                }
            }
        },

        updateMetrics: function () {
            var vm = this.getViewModel();
            if( !vm.get('state.on') && this.updateMetricsCount > 0) {
                return;
            }
            this.updateMetricsCount++;
            var gridSource = {};
            var appMetrics = Ext.getStore('metrics').findRecord('appId', vm.get('instance.id'));
            if (appMetrics) {
                appMetrics.get('metrics').list.forEach(function (metric) {
                    gridSource[metric.displayName.t()] = metric.value;
                });
            }
            vm.set('metrics', gridSource);
        }
    },

    listeners: {
        beforeedit: function () {
            return false;
        }
    }

});
