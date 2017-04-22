Ext.define('Ung.cmp.AppRemove', {
    extend: 'Ext.button.Button',
    alias: 'widget.appremove',

    // title: '<i class="fa fa-trash fa-red"></i> ' + 'Remove'.t(),

    // padding: 10,
    // margin: '20 0',
    // cls: 'app-section',
    // style: {
    //     borderColor: 'red'
    // },
    viewModel: true,

    bind: {
        text: 'Remove'.t() + ' {props.displayName}',
    },
    iconCls: 'fa fa-minus-circle fa-red',
    handler: function (btn) {
        var vm = this.getViewModel(),
            mainView = btn.up('#appCard'),
            // settingsView = this.getView();
            message = Ext.String.format('{0} will be uninstalled from this policy.'.t(), vm.get('appName')) + '<br/>' +
            'All of its settings will be lost.'.t() + '\n' + '<br/>' + '<br/>' +
            'Would you like to continue?'.t();

        if (vm.get('instance.appName') === 'policy-manager') { // rebuild policies tree
            if (Ext.getStore('policiestree').getCount() > 1) {
                Ext.Msg.alert('Warning!', 'Policy Manager cannot be removed if there are more than one default policy!');
                return;
            }
        }

        Ext.Msg.confirm('Warning:'.t(), message, function(btn) {
            if (btn === 'yes') {
                // var appItem = settingsView.up('#main').down('#apps').down('#' + vm.get('appInstance.appName'));
                mainView.setLoading(true);

                Rpc.asyncData('rpc.appManager.destroy', vm.get('instance.id'))
                    .then(function (result) {

                        if (vm.get('instance.appName') === 'reports') { // just reload the dashboard
                            window.location.href = '/admin/index.do';
                            return;
                        }

                        if (vm.get('instance.appName') === 'policy-manager') { // rebuild policies tree
                            Ext.getStore('policiestree').build();
                        }

                        if (rpc.reportsManager) {
                            Rpc.asyncData('rpc.reportsManager.getUnavailableApplicationsMap')
                                .then(function (unavailApps) {
                                    Ext.getStore('unavailableApps').loadRawData(unavailApps.map);
                                });
                        }

                        vm.set('instance.targetState', null);

                        Ung.app.redirectTo('#apps/' + vm.get('policyId'));

                        // remove card
                        if (Ung.app.getMainView().down('#appCard')) {
                            Ung.app.getMainView().remove('appCard');
                        }

                        // Rpc.asyncData('rpc.appManager.getAppsViews')
                        //     .then(function (policies) {
                        //         Ext.getStore('policies').loadData(policies);
                        //         Ung.app.redirectTo('#apps/');
                        //     });

                        // todo: fire global event
                        // Ext.GlobalEvents.fireEvent('appinstall', 'remove', appItem.app);
                    }, function (ex) {
                        Util.handleException(ex);
                    });
            }
        });
    }
});
