Ext.define('Ung.cmp.AppState', {
    extend: 'Ext.form.FieldSet',
    alias: 'widget.appstate',
    // border: false,
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
                var expectedState = get('instance.targetState');
                var actualState = get('runState');
                if ( ( expectedState === 'RUNNING' ) &&
                     ( actualState == 'RUNNING' ) ) {
                    return Ext.String.format('{0} is enabled.'.t(), get('props.displayName'));
                }else if( ( expectedState == 'RUNNING' ) &&
                    ( actualState != expectedState ) ){
                    return Ext.String.format('{0} should be enabled but is not active.'.t(), get('props.displayName'));
                }
                return Ext.String.format('{0} is disabled.'.t(), get('props.displayName'));
            },
            appStateIcon: function (get) {
                var expectedState = get('instance.targetState');
                var actualState = get('runState');
                if( !expectedState ||
                    ( actualState != expectedState ) ){
                    return 'fa-orange';
                }
                if ( actualState === 'RUNNING') {
                    return 'fa-green';
                }
                return 'fa-flip-horizontal fa-gray';
            },
            appStateTitle: function (get) {
                var expectedState = get('instance.targetState');
                var actualState = get('runState');
                var icon = '<i class="fa fa-power-off fa-gray"></i>';
                if (!expectedState) {
                    icon =  '<i class="fa fa-power-off fa-orange"></i>';
                }
                if ( ( actualState === 'RUNNING' ) &&
                     ( expectedState === 'RUNNING' ) ) {
                    icon = '<i class="fa fa-power-off fa-green"></i>';
                }
                return icon + ' ' + 'Power'.t();
            }
        }
    },

    controller: {
        onPower: function (btn) {
            var appManager = this.getView().up('#appCard').appManager,
                vm = this.getViewModel(),
                expectedState = vm.get('instance.targetState'),
                actualState = vm.get('runState');

            btn.setDisabled(true);

            if ( ( expectedState === 'RUNNING' ) &&
                 ( actualState === 'RUNNING' ) ) {
                vm.set('instance.targetState', null);
                // stop app
                appManager.stop(function (result, ex) {
                    if (ex) { Util.exceptionToast(ex); return false; }
                    appManager.getRunState(function (result2, ex2) {
                        if (ex2) { Util.exceptionToast(ex2); return false; }
                        vm.set('instance.targetState', result2);
                        // vm.notify();
                        btn.setDisabled(false);

                        // if (appManager.getAppProperties().name === 'reports') {
                        //     vm.getParent().set('reportsRunning', false);
                        // }

                        // Util.successToast(vm.get('powerMessage'));

                        // Ext.GlobalEvents.fireEvent('appstatechange', result2, vm.get('appInstance'));
                    });
                });
            } else {
                vm.set('instance.targetState', null);
                // start app
                appManager.start(function (result, ex) {
                    if (ex) {
                        Ext.Msg.alert('Error', ex.message);
                        btn.setDisabled(false);
                        return false;
                    }
                    appManager.getRunState(function (result2, ex2) {
                        if (ex2) { Util.exceptionToast(ex2); return false; }
                        vm.set('instance.targetState', result2);
                        // vm.notify();
                        btn.setDisabled(false);

                        // if (appManager.getAppProperties().name === 'reports') {
                        //     vm.getParent().set('reportsRunning', true);
                        // }

                        // Util.successToast(vm.get('powerMessage'));
                        // Ext.GlobalEvents.fireEvent('appstatechange', result2, vm.get('appInstance'));
                    });
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
            hidden: '{instance.targetState}'
        }
    }]
});
