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
            message = Ext.String.format('{0} will be uninstalled from this policy.'.t(), vm.get('props.displayName')) + '<br/>' +
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
                    .then(function () {

                        if (vm.get('instance.appName') === 'reports') {
                            Ung.app.reportscheck(); // check reports
                            Ung.app.redirectTo('#apps');
                            return;
                        }

                        if (vm.get('instance.appName') === 'policy-manager') { // rebuild policies tree
                            Ext.getStore('policiestree').build();
                        }

                        Ext.fireEvent('appremove', vm.get('props.displayName'));

                        vm.set('instance.targetState', null);
                        vm.set('instance.runState', null);

                        Ung.app.redirectTo('#apps/' + vm.get('policyId'));

                        // remove card
                        if (Ung.app.getMainView().down('#appCard')) {
                            Ung.app.getMainView().remove('appCard');
                        }
                    }, function (ex) {
                        Util.handleException(ex);
                    });
            }
        });
    }
});
