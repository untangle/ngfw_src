Ext.define('Ung.apps.sslinspector.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-sslinspector',

    control: {
        '#status': {
            afterrender: 'statusAfterRender'
        }
    },

    statusAfterRender: function () {
        var vm = this.getViewModel();

        if (!rpc.certificateManager) {
            rpc.certificateManager = rpc.UvmContext.certificateManager();
        }
        Rpc.asyncData('rpc.certificateManager.validateActiveInspectorCertificates')
            .then(function (result) {
                vm.set('serverCertificateVerification', result);
            }, function (ex) {
                Util.exceptionToast(ex);
            });
    }
});
