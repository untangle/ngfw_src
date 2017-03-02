Ext.define('Ung.apps.webfilter.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app.webfilter',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
        }
    },

    onBeforeRender: function (view) {
        // var me = this,
        //     vm = this.getViewModel();

        // rpc.nodeManager.node(function (nodeManager, ex) {
        //     if (ex) { Util.exceptionToast(ex); return false; }
        //     me.setNodeManager(nodeManager);
        //     // get node settings
        //     nodeManager.getSettings(function (settings) {
        //         // add the node settings view, based on node type and instance
        //         me.getView().add({
        //             xtype: 'ung.' + nodeInstance.nodeName,
        //             region: 'center',
        //             itemId: 'settings'
        //             //manager: nodeManager
        //         });
        //         console.log(settings);
        //         vm.set('settings', settings);
        //         mask.hide();
        //     });
        // }, vm.get('instance.id'));



        console.log(view.getViewModel().get('instance'));
        // view.getViewModel().set('instanceId', view.instance.id);
        // console.log('herere');
        // console.log(view.instance);
        // var vm = this.getViewModel();
        // var nodeInstance = policy.get('instances').list.filter(function (node) {
        //     return node.nodeName === vm.get('nodeName');
        // })[0];
        // console.log(vm.get('policyId'));
    }


});
