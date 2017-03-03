Ext.define('Ung.cmp.AppMetrics', {
    extend: 'Ext.grid.property.Grid',
    alias: 'widget.appmetrics',
    title: 'Metrics'.t(),
    border: false,

    nameColumnWidth: 250,

    viewModel: {
        data: {
            metrics: null
        }
    },

    disabled: true,
    bind: {
        disabled: '{instance.targetState !== "RUNNING"}',
        source: '{metrics}'
    },

    controller: {
        updateMetricsCount: 0,
        control: {
            '#': {
                afterrender: function () {
                    var me = this;
                    // me.updateMetrics();
                    me.getViewModel().bind('{instance.targetState}', function () {
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
            if (vm.get('instance.targetState') !== 'RUNNING' && this.updateMetricsCount > 0) {
                return;
            }
            this.updateMetricsCount++;
            var gridSource = {};
            var appMetrics = Ext.getStore('metrics').findRecord('nodeId', vm.get('instance.id'));
            if (appMetrics) {
                appMetrics.get('metrics').list.forEach(function (metric) {
                    gridSource[metric.displayName.t()] = metric.value;
                });
            }
            vm.set('metrics', gridSource);
            // if (this.getView().down('nodechart')) {
            //     this.getView().down('nodechart').fireEvent('addPoint');
            // }
        }
    },

    listeners: {
        beforeedit: function () {
            return false;
        }
    }

});
