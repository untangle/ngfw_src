Ext.define('Ung.cmp.AppPanel', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.apppanel',
    layout: 'fit',

    dockedItems: [{
        xtype: 'toolbar',
        dock: 'top',
        weight: -10,
        border: false,
        items: [{
            text: 'Back'.t(),
            iconCls: 'fa fa-arrow-circle-left fa-lg',
            hrefTarget: '_self',
            bind: { href: '#apps/{policyId}' }
        }, '-', {
            xtype: 'component',
            padding: '0 5',
            bind: { html: '<img src="/skins/modern-rack/images/admin/apps/{props.name}_17x17.png" style="vertical-align: middle;" width="17" height="17"/> <strong>{props.displayName}</strong>' }
        }
        // '->', {
        //     xtype: 'button',
        //     text: 'View Reports'.t(),
        //     iconCls: 'fa fa-line-chart fa-lg'
        // }
        ],
    }, {
        xtype: 'toolbar',
        dock: 'bottom',
        // border: false,
        items: ['->', {
            text: '<strong>' + 'Save'.t() + '</strong>',
            // scale: 'large',
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    listeners: {
        // generic listener for all tabs in Apps, redirection
        beforetabchange: function (tab, newCard, oldCard) {
            var vm = this.getViewModel();
            Ung.app.redirectTo('#apps/' + vm.get('policyId') + '/' + vm.get('urlName') + '/' + newCard.getItemId());
        }
        // afterrender: function () {
        //     // get settings
        //     var me = this,
        //         vm = this.getViewModel();
        //     Rpc.asyncData('rpc.nodeManager.node', vm.get('instance.id'))
        //         .then(function (result) {
        //             me.appManager = result;
        //             console.log(me.appManager);
        //         });

        //     // if(handler==null) {handler=Ext.emptyFn;}
        //     // if (this.rpcNode === undefined) {
        //     //     rpc.nodeManager.node(Ext.bind(function(result, exception) {
        //     //         if(Ung.Util.handleException(exception)) return;
        //     //         this.rpcNode = result;
        //     //         handler.call(this);
        //     //     }, this), this.nodeId);
        //     // } else {
        //     //     handler.call(this);
        //     // }
        // }
    }
});
