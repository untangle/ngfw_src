Ext.define('Ung.view.main.IpmController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.ipm',

    control: {
        '#': { afterrender: 'onAfterRender'},
    },

    onAfterRender: function () {
        var me = this;

        //Rpc.directData('rpc.ipmMessages');
        var ipmMessages = [{
            msgType: 'alert',
            closure: false,
            message: '<strong>Unable to establish connection to the License Service!</strong> Installation of apps is disabled. Please ensure connectivity and <a href="">try again</a>'
        }, {
            msgType: 'info',
            closure: false,
            message: '<strong>Unable to establish connection to the License Service!</strong> Installation of apps is disabled. Please ensure connectivity and <a href="">try again</a>'
        }];
        
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