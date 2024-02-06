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
                var icon = '<i class="fa fa-info-circle"></i> ';
                var color = 'black';
                switch(msg.type) {
                    case 'ALERT':
                        icon = '<i class="fa fa-exclamation-triangle"></i> ';
                        color = '#F44336';
                        break;
                    case 'WARNING':
                        icon = '<i class="fa fa-exclamation"></i> ';
                        color = "#FF6600";
                        break;
                }
                var hasClosure = msg.hasClosure;
                var message = msg.message;
                var msgItem = {
                    xtype: 'container',
                    layout: 'hbox',
                    border: 1,
                    style: {
                        borderColor: 'black',
                        borderStyle: 'solid'
                    },
                    items: [
                        {
                            xtype: 'component',
                            style: { 
                                fontSize: '12px', 
                                color: color, 
                                padding: '15px',
                                textAlign: 'center',
                            },
                            html: icon + message.t(),
                            flex: 1
                        },
                        {
                            xtype: 'button',
                            iconCls: 'fa fa-window-close',
                            hidden: !hasClosure,
                            handler: 'removeUserLicenseMessage'
                        }
                    ]
                };

                if (msg.showAsBanner) {
                    messagesCmps.push(msgItem);
                }
            });

            // remove all current UserLicenseMessages messages before adding new ones
            me.getView().down('#_userLicenseMessages').removeAll();
            me.getView().down('#_userLicenseMessages').add(messagesCmps);
        }, function(ex){
            handleException(ex);
        }); 
    },

    removeUserLicenseMessage: function(cmp) {
        cmp.up().destroy();  
    }

});