Ext.define('Ung.Setup.Complete', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.Complete',

    title: 'Finish'.t(),
    description: '',

    layout: 'center',
    items: [{
        xtype: 'container',
        itemId: 'complete',
        layout: {
            type: 'vbox',
            align: 'middle'
        },
        items: []
    }],
    listeners: {
        afterrender: 'onAfterRender',
        activate: 'onActivate'
    },
    controller: {

        onAfterRender: function () {
            var me = this,
                view = me.getView(),
                vm = me.getViewModel(),
                items = [];

            // Configure main based on remote.
            if(!rpc.remote){
                // Local Setup Wizard configuration
                items.push({
                    xtype: 'component',
                    style: { textAlign: 'center' },
                    html: '<h1 style="margin: 0;">' + Ext.String.format('The {0} is now configured.', rpc.oemProductName) + '</h1><br/><br/>You are now ready to configure the applications.'.t()
                });
                items.push({
                    xtype: 'button',
                    margin: '30 0 0 0',
                    scale: 'medium',
                    text: 'Go to Dashboard'.t(),
                    iconCls: 'fa fa-check',
                    handler: function () {
                        Ext.MessageBox.wait('Loading User Interface...'.t(), 'Please Wait'.t());
                        //and set a flag so the wizard wont run again
                        rpc.jsonrpc.UvmContext.wizardComplete(function (result, ex) {
                            if (ex) { Util.handleException(ex); return; }
                            window.location.href = '/admin/index.do';
                        });
                    }
                });
            }else{
                // Can get to remote server
                items.push({
                    xtype: 'component',
                    style: { textAlign: 'center' },
                    html: '<img src="images/BrandingLogo.png?' + (new Date()).getTime() + '" height=48/><h1>' + Ext.String.format('Thanks for choosing {0}!'.t(), rpc.oemName) + '</h1>'
                });
                items.push({
                    xtype: 'component',
                    margin: '0 0 20 0',
                    style: { textAlign: 'center' },
                    html: 'To continue, you must log in using your ETM Dashboard account.  If you do not have one, you can create a free account.'.t()
                });
                items.push({
                    xtype: 'container',
                    items: [{
                        xtype: 'button',
                        width: 332,
                        margin: '0 0 10 0',
                        text: '<div style="color:white;">' + 'Log In'.t() + '</div>',
                        baseCls: 'command-center-login-button',
                        handler: function(){
                            window.location = rpc.remoteUrl + "appliances/add/" + rpc.serverUID;
                        }
                    },{
                        xtype: 'button',
                        width: 332,
                        text: '<div style="color:white;">' + 'Create Account'.t() + '</div>',
                        baseCls: 'command-center-create-button',
                        handler: function(){
                            window.location = rpc.remoteUrl + "login/create-account/add-appliance/" + rpc.serverUID;
                        }
                    }]
                });
            }
            view.down('[itemId=complete]').add(items);
        },

        onActivate: function(){
            if(!rpc.remote){
                // In local mode, mark wizard completed so apps can start being populated.
                rpc.jsonrpc.UvmContext.wizardComplete();
            }
        }
    }
});
