Ext.define('Ung.view.main.SupportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.support',

    cancelHandler: function (btn) {
        btn.up('window').close();
    },

    noSupportHandler: function (btn) {
        Ung.app.getMainView().getController().supportLaunch();
        btn.up('window').close();
    },

    yesSupportHandler: function (btn) {
        var me = this;
        Ung.app.getMainView().getController().supportLaunch(); // open support before saving to prevent popup blocker
        me.getView().setLoading(true);
        Rpc.asyncData('rpc.systemManager.getSettings')
            .then(function (result) {
                result.supportEnabled = true;
                result.cloudEnabled = true;
                Rpc.asyncData('rpc.systemManager.setSettings', result)
                .then(function() {
                    Util.successToast('Remote Support Access enabled!');
                    btn.up('window').close();
                }, function (ex) {
                    console.error(ex);
                    Util.handleException(ex);
                });
            });
    }
});
