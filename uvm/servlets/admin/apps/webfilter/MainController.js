Ext.define('Ung.apps.webfilter.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app.webfilter',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
        }
    },

    onBeforeRender: function (view) {
        view.getViewModel().set('instanceId', view.instance.id);
        console.log('herere');
        console.log(view.instance);
        // var vm = this.getViewModel();
        // var nodeInstance = policy.get('instances').list.filter(function (node) {
        //     return node.nodeName === vm.get('nodeName');
        // })[0];
        // console.log(vm.get('policyId'));
    }


});
