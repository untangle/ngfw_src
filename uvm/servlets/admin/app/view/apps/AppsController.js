Ext.define('Ung.view.apps.AppsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.apps',

    control: {
        '#': {
            beforeactivate: 'updateNodes'
        }
    },

    listen: {
        global: {
            nodestatechange: 'updateNodes'
        },
        store: {
            '#policies': {
                datachanged: 'updateNodes'
            }
        }
    },

    init: function (view) {
        view.getViewModel().bind({
            bindTo: '{policyId}'
        }, this.onPolicy, this);
    },

    onNodeStateChange: function (state, instance) {
        console.log(instance);
    },

    updateNodes: function () {
        var vm = this.getViewModel(),
            nodeInstance, i;

        rpc.nodeManager.getAppsViews(function(result, exception) {
            var policy = result.filter(function (p) {
                return parseInt(p.policyId) === parseInt(vm.get('policyId'));
            })[0];

            var nodes = policy.nodeProperties.list,
                instances = policy.instances.list;

            for (i = 0; i < nodes.length; i += 1) {
                nodeInstance = instances.filter(function (instance) {
                    return instance.nodeName === nodes[i].name;
                })[0];
                // console.log(nodeInstance.targetState);
                nodes[i].policyId = vm.get('policyId');
                nodes[i].state = nodeInstance.targetState.toLowerCase();
            }
            vm.set('nodes', nodes);
        });
    },

    onPolicy: function () {
        // this.getView().lookupReference('filters').removeAll();
        // this.getView().lookupReference('services').removeAll();
        // this.updateNodes();
    },

    setPolicy: function (combo, newValue, oldValue) {
        if (oldValue !== null) {
            this.redirectTo('#apps/' + newValue, false);
            this.updateNodes();
        }
    },

    onItemAfterRender: function (item) {
        Ext.defer(function () {
            item.removeCls('insert');
        }, 50);
    }

});
