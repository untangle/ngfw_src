Ext.define('Ung.config.email.EmailTestController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.config.email.test',

    sendMail: function (btn) {
        var v = this.getView(), vm = this.getViewModel();
        btn.setDisabled(true);
        vm.set({
            processing: true,
            processingIcon: '<i class="fa fa-spinner fa-spin fa-3x fa-fw"></i>'
        });
        rpc.UvmContext.mailSender().sendTestMessage(function (result, ex) {
            btn.setDisabled(false);
            if (ex) { console.error(ex); Util.handleException(ex); return; }
            vm.set({
                processing: null,
                processingIcon: '<i class="fa fa-check fa-3x fa-fw" style="color: green;"></i> <br/>' + 'Success'.t()
            });
        }, v.down('textfield').getValue());
    },
    cancel: function () {
        this.getView().close();
    }

});
