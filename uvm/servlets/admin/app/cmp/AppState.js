Ext.define('Ung.cmp.AppState', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.appstate',
    title: '<i class="fa fa-power-off"></i> ' + 'Power'.t(),

    padding: 10,
    margin: '20 0',
    cls: 'app-section',

    layout: {
        type: 'vbox',
        align: 'stretch'
    },

    viewModel: {
        formulas: {
            appState: function (get) {
                return Ext.String.format('{0} is {1}.', get('props.displayName'), get('state.status').toLowerCase());
            },
            appStateIcon: function (get) {
                var cls = get('state.colorCls');
                if(!get('state.inconsistent') &&
                    !get('state.power') &&
                    !get('state.on')){
                    cls = 'fa-flip-horizontal ' + cls;
                }
                return cls;
            },
            appStatus: function(get){
                return this.getView().up('#appCard').appManager.getStatus();
            }
        }
    },

    controller: {
        reload: function(){
            var me = this,
                appManager = me.getView().up('#appCard').appManager,
                appView = me.getView().up('#appCard');

            appView.getViewModel().get('state').detect();

            rpc.appsViews = rpc.appManager.getAppsViews();
            Ext.getStore('policies').loadData(rpc.appsViews);
            Ung.app.getGlobalController().getAppsView().getController().getApps();

            if (appManager.getAppProperties().name === 'reports') {
                Ung.app.reportscheck();
            }

        },

        onPower: function (btn) {
            var me = this,
                appManager = me.getView().up('#appCard').appManager,
                appView = me.getView().up('#appCard');

            btn.setDisabled(true);

            appView.getViewModel().get('state').set('power', true);
            if ( appView.getViewModel().get('state').get('on') ){
                appView.getViewModel().get('state').set('on', false);
                appView.getViewModel().set('instance.targetState', 'INITIALIZED');
                Rpc.asyncData(appManager, 'stop')
                .then( function(result){
                    if(Util.isDestroyed(me, btn)){
                        return;
                    }
                    btn.setDisabled(false);
                    me.reload();
                },function(ex){
                    Util.handleException(ex);
                });
            } else {
                appView.getViewModel().get('state').set('on', true);
                appView.getViewModel().set('instance.targetState', 'RUNNING');
                Rpc.asyncData(appManager, 'start')
                .then( function(result){
                    if(Util.isDestroyed(me, btn)){
                        return;
                    }
                    btn.setDisabled(false);
                    me.reload();
                },function(ex){
                    Ext.Msg.alert('Error', ex.message);
                    if(Util.isDestroyed(btn)){
                        return;
                    }
                    // Likely due to an invalid licnese.
                    // Expect the app to shutdown
                    btn.setDisabled(false);
                });
            }
        }
    },

    items: [{
        xtype: 'container',
        layout: 'hbox',
        items: [{
            xtype: 'button',
            cls: 'power-btn',
            bind: {
                iconCls: 'fa fa-toggle-on {appStateIcon} fa-2x'
            },
            handler: 'onPower'
        }, {
            xtype: 'component',
            padding: '3 5',
            bind: {
                html: '<strong>' + '{appState}' + '</strong>'
            }
        }, {
            xtype: 'component',
            html: '<i class="fa fa-spinner fa-spin fa-lg fa-fw"></i>',
            hidden: true,
            bind: {
                hidden: '{!state.power}',
            }
        }]
    },{
        xtype: 'container',
        layout: 'hbox',
        flex: 1,
        hidden: true,
        bind: {
            hidden: '{!state.inconsistent}',
        },
        items:[{
            fieldLabel: 'Status'.t(),
            xtype: 'textarea',
            height: 100,
            flex: 1,
            fieldStyle: {
                fontFamily: 'monospace'
            },
            bind: {
                value: '{appStatus}'
            }
        }]
    }]
});
