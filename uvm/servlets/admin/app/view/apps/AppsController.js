Ext.define('Ung.view.apps.AppsController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.apps',

    listen: {
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

    updateNodes: function () {
        var filtersRef = this.getView().lookupReference('filters');
        var servicesRef = this.getView().lookupReference('services');

        var i, node, instance, ref,
            policy = Ext.getStore('policies').findRecord('policyId', this.getViewModel().get('policyId')),
            nodes = policy.get('nodeProperties').list,
            instances = policy.get('instances').list;

        nodes.sort(function (a, b) {
            return a.viewPosition - b.viewPosition;
        });

        for (i = 0; i < nodes.length; i += 1) {
            node = nodes[i];
            instance = instances.filter(function (instance) {
                return instance.nodeName === node.name;
            })[0];

            ref = node.type === 'FILTER' ? filtersRef : servicesRef;
            if (!ref.down('#' + node.name)) {
                ref.insert(i, {
                    xtype: 'ung.appitem',
                    itemId: node.name,
                    cls: 'insert',
                    node: node,
                    state: instance.targetState === 'RUNNING' ? 'on' : '',
                    href: '#apps/' + this.getViewModel().get('policyId') + '/' + node.name
                });
            }
        }

        // remove uninstalled nodes
        filtersRef.query('button').forEach(function (nodeCmp) {
            if (instances.filter(function (instance) {
                return instance.nodeName === nodeCmp.itemId;
            }).length === 0) {
                nodeCmp.addCls('insert');
                Ext.defer(function () {
                    filtersRef.remove(nodeCmp);
                    Ung.Util.successToast(nodeCmp.node.displayName + ' removed successfully!');
                }, 500);
            }
        });

        servicesRef.query('button').forEach(function (nodeCmp) {
            if (instances.filter(function (instance) {
                return instance.nodeName === nodeCmp.itemId;
            }).length === 0) {
                nodeCmp.addCls('insert');
                Ext.defer(function () {
                    servicesRef.remove(nodeCmp);
                    Ung.Util.successToast(nodeCmp.node.displayName + ' removed successfully!');
                }, 500);
            }
        });
    },

    onPolicy: function () {
        this.getView().lookupReference('filters').removeAll();
        this.getView().lookupReference('services').removeAll();
        this.updateNodes();
    },

    setPolicy: function (combo, newValue, oldValue) {
        if (oldValue !== null) {
            this.redirectTo('#apps/' + newValue, false);
        }
    },

    onItemAfterRender: function (item) {
        Ext.defer(function () {
            item.removeCls('insert');
        }, 50);
    }

});
