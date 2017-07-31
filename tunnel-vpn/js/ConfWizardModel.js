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

                switch (val) {
                  case 'ExpressVPN':
                  case 'NordVPN':
                  case 'CustomZipPass':
                  case 'CustomConfPass':
                  case 'CustomOvpnPass':
                    this.set('usernameHidden', false);
                    this.set('passwordHidden', false);
                    break;
                default:
                    this.set('usernameHidden', true);
                    this.set('passwordHidden', true);
                    break;
                }
            }
        }
    },
    
    data: {
        provider: null,
        providerName: null,
        providerTitle: null,
        providerInstructions: null,

        username: null,
        password: null,
        usernameHidden: true,
        passwordHidden: true,

        tunnelId: -1,
        
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
                    '<li>' + 'Choose an ovpn file for a server (in your region)'.t() + '<br/>' +
                    '<li>' + 'Upload the chosen ovpn file below'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password of your NordVPN account'.t() + '<br/>'
            },
            ExpressVPN: {
                providerName: 'ExpressVPN'.t(),
                providerTitle: 'Upload the ExpressVPN OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Log in to "My account" at expressvpn.com'.t() + '<br/>' +
                    '<li>' + 'FIXME"'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password provided by ExpressVPN'.t() + '<br/>' + 
                    '<li>' + 'NOTE: This is not your ExpressVPN account username/password'.t() + '<br/>'
            },
            Custom_zip: {
                providerName: 'CustomZip'.t(),
                providerTitle: 'Upload the Custom OpenVPN config zip'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .zip File'.t() + '<br/>'
            },
            Custom_zip_password: {
                providerName: 'CustomZipPass'.t(),
                providerTitle: 'Upload the Custom OpenVPN config zip with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .zip File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
            Custom_ovpn: {
                providerName: 'CustomOvpn'.t(),
                providerTitle: 'Upload the Custom OpenVPN .ovpn file'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .ovpn File'.t() + '<br/>'
            },
            Custom_ovpn_password: {
                providerName: 'CustomOvpnPass'.t(),
                providerTitle: 'Upload the Custom OpenVPN .ovpn file with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .ovpn File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
            Custom_conf: {
                providerName: 'CustomConf'.t(),
                providerTitle: 'Upload the Custom OpenVPN .conf file'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .conf File'.t() + '<br/>'
            },
            Custom_conf_password: {
                providerName: 'CustomConfPass'.t(),
                providerTitle: 'Upload the Custom OpenVPN .conf file with username/password'.t(),
                providerInstructions: '<li>' + 'Upload the Custom OpenVPN Config .conf File'.t() + '<br/>' +
                    '<li>' + 'Provide the username/password'.t() + '<br/>'
            },
        },

        trafficConfig: {
            allTraffic: false,
        }
    },

    
});
