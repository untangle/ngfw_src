Ext.define('Ung.cmp.AppState', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.appstate',
    bind: {
        title: '{appStateTitle}'
    },

    padding: 10,
    margin: '20 0',
    cls: 'app-section',

    layout: {
        type: 'hbox',
        align: 'middle'
    },

    viewModel: {
        formulas: {
            appState: function (get) {
                var me = this;
                var targetState = get('targetState');
                var runState = get('runState');

                if ( ( targetState === 'RUNNING' ) &&
                     ( runState == 'RUNNING' ) ) {
                    return Ext.String.format('{0} is enabled.'.t(), get('props.displayName'));
                }else if( ( targetState == 'RUNNING' ) &&
                    ( runState != targetState ) ){
                    return Ext.String.format('{0} should be enabled but is not active.'.t(), get('props.displayName'));
                }
                return Ext.String.format('{0} is disabled.'.t(), get('props.displayName'));
            },
            appStateIcon: function (get) {
                var targetState = get('targetState');
                var runState = get('runState');
                if( !targetState ||
                    ( runState != targetState ) ){
                    return 'fa-orange';
                }
                if ( runState === 'RUNNING') {
                    return 'fa-green';
                }
                return 'fa-flip-horizontal fa-gray';
            },
            appStateTitle: function (get) {
                var targetState = get('targetState');
                var runState = get('runState');
                var icon = '<i class="fa fa-power-off fa-gray"></i>';
                if (!targetState) {
                    icon =  '<i class="fa fa-power-off fa-orange"></i>';
                }
                if ( ( runState === 'RUNNING' ) &&
                     ( targetState === 'RUNNING' ) ) {
                    icon = '<i class="fa fa-power-off fa-green"></i>';
                }
                return icon + ' ' + 'Power'.t();
            }
        }
    },

    controller: {
        runStateMaxWait: 10000,
        runStateWait: 0,
        runStateDelay: 100,
        runStateWantState: null,
        runStateWantButton: null,
        runStateTask: null,

        onPower: function (btn) {
            var me = this,
                appManager = me.getView().up('#appCard').appManager,
                vm = me.getViewModel(),
                targetState = vm.get('targetState'),
                runState = vm.get('runState');

            btn.setDisabled(true);

            if( !me.runStateTask ){
                me.runStateTask = new Ext.util.DelayedTask( Ext.bind(function(){
                    appManager.getRunState( Ext.bind( function (result, ex2) {
                        if (ex2) {
                            Util.handleException(ex2);
                            return false;
                        }
                        this.runStateWait = this.runStateWait - this.runStateDelay;
                        if(result != this.runStateWantState){
                            this.runStateTask.delay( this.runStateDelay );
                        }else{
                            this.getViewModel().set( 'runState', result );
                            this.getViewModel().set( 'targetState', this.runStateWantState );
                            this.runStateButton.setDisabled(false);
                            // force reload Apps after start/stop within App Settings
                            Ung.app.getGlobalController().getAppsView().getController().getApps();

                        }
                    }, this) );
                }, me) );
            }
            me.runStateWait = me.runStateMaxWait;
            me.runStateButton = btn;

            if ( ( targetState === 'RUNNING' ) &&
                 ( runState === 'RUNNING' ) ) {
                // stop app
                me.runStateWantState = 'INITIALIZED';
                appManager.stop(function (result, ex) {
                    if (ex) {
                        Util.handleException(ex);
                        return false;
                    }
                    me.runStateTask.delay( this.getRunStateDelay );
                });
            } else {
                // start app
                me.runStateWantState = 'RUNNING';
                appManager.start(function (result, ex) {
                    if (ex) {
                        Ext.Msg.alert('Error', ex.message);
                        // Likely due to an invalid licnese.
                        // Expect the app to shutdown
                        me.runStateWantState = 'INITIALIZED';
                        me.runStateTask.delay( this.getRunStateDelay );
                        btn.setDisabled(false);
                        return false;
                    }
                    me.runStateTask.delay( this.getRunStateDelay );
                });
            }
        }
    },

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
            hidden: '{targetState}'
        }
    }]
});
