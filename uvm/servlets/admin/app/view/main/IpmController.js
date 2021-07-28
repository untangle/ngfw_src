Ext.define('Ung.view.main.IpmController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ipm',

    listen: {
        global: {
            loadIpm: 'loadIpm'
        }
    },

    loadIpm: function () {
        var me = this, vm = me.getViewModel();
        var ipmMessages = [];

        Rpc.asyncData('rpc.UvmContext.licenseManager.getIpmMessages')
        .then(function(result){
            if(Util.isDestroyed(vm)) {
                return;
            }
            ipmMessages = result.list;
        });        

        console.log(ipmMessages);
        
        var messages = [], messagesCmps = [];

        Ext.Array.each(ipmMessages, function (message) {
            messages.push(message);
        });

        Ext.Array.each(messages, function(msg) {
            var html = msg.message;
            var color = 'black';
            switch(msg.msgType) {
                case 'alert':
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

        me.getView().down('#_ipmMessages').add(messagesCmps);


        
    },

});