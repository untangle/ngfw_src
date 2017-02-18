Ext.define('Ung.config.about.AboutController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.config.about',

    control: {
        '#': {
            beforerender: 'onBeforeRender',
        },
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
        rpc.UvmContext.licenseManager().reloadLicenses(function (result, ex) {
            // todo
        });
    }

});