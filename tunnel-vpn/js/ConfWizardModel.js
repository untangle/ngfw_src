Ext.define('Ung.apps.bandwidthcontrol.ConfWizardModel', {
    extend: 'Ext.app.ViewModel',
    alias: 'viewmodel.app-tunnel-vpn-wizard',

    formulas: {
        selectedProvider: {
            get: function () {
                return;
            },
            set: function (val) {
                var details = this.get('providerConfigs')[val];

                this.set('provider', val);
                this.set('providerName', details.providerName);
                this.set('providerTitle', details.providerTitle);
                this.set('providerInstructions', details.providerInstructions);
            }
        }
    },
    
    data: {
        provider: null,
        providerName: null,
        providerTitle: null,
        providerInstructions: null,

        providerConfigs: {
            Untangle: {
                providerName: 'Untangle'.t(),
                providerTitle: 'Upload the Untangle OpenVPN config zip',
                providerInstructions: 'FIXME',
            },
            NordVPN: {
                providerName: 'NordVPN'.t(),
                providerTitle: 'Upload the NordVPN OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Log in to "My account" at nordvpn.com'.t() + '<br/>' +
                    '<li>' + 'Click on the "Download area"'.t() + '<br/>' +
                    '<li>' + 'Download the Linux ".OVPN configuration files" zip'.t() + '<br/>' +
                    '<li>' + 'Upload the downloaded zip file below'.t() + '<br/>'
            },
            ExpressVPN: {
                providerName: 'ExpressVPN'.t(),
                providerTitle: 'Upload the ExpressVPN OpenVPN config zip'.t(),
                providerInstructions: 'FIXME',
            },
            Custom: {
                providerName: 'Custom'.t(),
                providerTitle: 'Upload the Custom OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config File'.t() + '<br/>'
            },
        },

        trafficConfig: {
            allTraffic: false,
        }
    },

    
});
