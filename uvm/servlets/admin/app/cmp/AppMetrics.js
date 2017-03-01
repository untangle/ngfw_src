Ext.define('Ung.cmp.AppMetrics', {
    extend: 'Ext.grid.property.Grid',
    alias: 'widget.appmetrics',
    //layout: 'fit',
    border: false,
    nameColumnWidth: 250,
    config: {
        instanceId: null
    },

    viewModel: {
        data: {
            metrics: null
        }
    },

    controller: {
        control: {
            '#': {
                afterrender: function (v) {
                    console.log('instance after render' + v.getViewModel().get('instanceId'));
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
            var gridSource = {};
            var vm = this.getViewModel();
            var nodeMetrics = Ext.getStore('metrics').findRecord('nodeId', vm.get('instanceId'));
            if (nodeMetrics) {
                nodeMetrics.get('metrics').list.forEach(function (metric) {
                    gridSource[metric.displayName.t()] = metric.value;
                });
            }
            vm.set('metrics', gridSource);
            // if (this.getView().down('nodechart')) {
            //     this.getView().down('nodechart').fireEvent('addPoint');
            // }
        }
    },

    title: 'Metrics'.t(),

    bind: {
        source: '{metrics}'
    },
    listeners: {
        beforeedit: function () {
            return false;
        }
    }

});
