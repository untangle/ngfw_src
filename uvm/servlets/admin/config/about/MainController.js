Ext.define('Ung.config.about.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config-about',

    control: {
        '#': {
            beforerender: 'onBeforeRender'
        },
        '#licenses': {
            afterrender: 'reloadLicenses'
        }
    },

    onBeforeRender: function (view) {
        try {
            view.getViewModel().set({
                kernelVersion: rpc.adminManager.getKernelVersion(),
                modificationState: rpc.adminManager.getModificationState(),
                rebootCount: rpc.adminManager.getRebootCount(),
                activeSize: rpc.hostTable.getCurrentActiveSize(),
                maxActiveSize: rpc.hostTable.getMaxActiveSize()
            });
            console.log(view.getViewModel());
        } catch (ex) {

        }
    },

    reloadLicenses: function () {
        rpc.licenseManager = rpc.UvmContext.licenseManager();
        Rpc.asyncData('rpc.licenseManager.reloadLicenses', true).then(function(result) {
            if (ex) { Util.exceptionToast(ex); return; }
            Rpc.asyncData('rpc.licenseManager.getLicenses').then(function(result) {
                if (ex) { Util.exceptionToast(ex); return; }
                vm.set('licenses', result.list);
            });
        });
    }

});
