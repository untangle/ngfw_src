Ext.define('Ung.apps.livesupport.MainController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.app-live-support',

    control: {
        '#': {
            afterrender: 'onAfterRender'
        }
    },

    onAfterRender: function(){
        var me = this, v = me.getView(), vm = me.getViewModel();

        // There's nothing to save on this form.
        vm.set('panel.saveDisabled', true);

        v.setLoading(true);
        Ext.Deferred.sequence([
            Rpc.directPromise('rpc.companyName'),
            Rpc.directPromise('rpc.companyURL'),
            Rpc.directPromise('rpc.serverUID'),
            Rpc.directPromise('rpc.fullVersionAndRevision')
        ], this)
        .then(function (result) {
            if(Util.isDestroyed(v, vm)){
                return;
            }
            vm.set({
                companyName: result[0],
                companyURL: result[1],
                serverUID: result[2],
                fullVersionAndRevision: result[3],
            });

            var paidFrame = v.down('[itemId=paidFrame]');
            var freeFrame = v.down('[itemId=freeFrame]');

            // get the license and show either the Get Support or Learn More section
            Rpc.asyncData('rpc.UvmContext.licenseManager.getLicense', 'live-support').then(function(license) {
                if ((license) && (license.valid)) {
                    paidFrame.setHidden(false);
                } else {
                    freeFrame.setHidden(false);
                }
            });

            v.setLoading(false);
        });
    },

    supportHandler: function(btn){
        this.getView().up('[itemId=main]').getController().supportHandler(btn);
    },

    // When the license is not valid we want the Learn More button open the companyURL configured
    // in the branding manager. If it is not configured or is our Untangle default, we open
    // our Live-Support app page.
    learnMoreHandler: function(btn){
        var target = this.getViewModel().get('companyURL');
        if ((target) && (target !== '') && (! target.includes('edge.arista.com'))) {
            window.open(target);
        } else {
            window.open(rpc.uriManager.getUriWithPath("https://edge.arista.com/shop/Live-Support"));
        }
    }
});
