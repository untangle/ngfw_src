Ext.define('Ung.view.main.UserLicenseMessagesController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.userLicenseMessages',

    listen: {
        global: {
            loadUserLicenseMessages: 'loadUserLicenseMessages'
        }
    },

    loadUserLicenseMessages: function () {
        var me = this, vm = me.getViewModel();

        Rpc.asyncData('rpc.UvmContext.licenseManager.getUserLicenseMessages')
        .then(function(result){
            if(Util.isDestroyed(vm)) {
                return;
            }
            var userLicenseMessages = result.list;

            messagesCmps = [];

            Ext.Array.each(userLicenseMessages, function(msg) {
                var html = msg.message;
                var color = 'black';
                switch(msg.type) {
                    case 'ALERT':
                        html = '<i class="fa fa-exclamation-triangle"></i> ' + html;
                        color = '#F44336';
                        break;
                    default: //info
                        html = '<i class="fa fa-info-circle"></i> ' + html;
                        break;
                }
                var msgItem = {
                    xtype: 'container',
                    items: [{
                        xtype: 'component',
                        style: { 
                            fontSize: '12px', 
                            color: color, 
                            padding: '15px',
                            textAlign: 'center'
                        },
                        html: html
                    }]
                };

                messagesCmps.push(msgItem);
            });

            // remove all current UserLicenseMessages messages before adding new ones
            me.getView().down('#_userLicenseMessages').removeAll();
            me.getView().down('#_userLicenseMessages').add(messagesCmps);
        }, function(ex){
            handleException(ex);
        }); 
    },

});