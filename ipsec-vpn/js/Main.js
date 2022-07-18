Ext.define('Ung.apps.ipsecvpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ipsec-vpn',
    controller: 'app-ipsec-vpn',

    viewModel: {
        data: {
            autoRefresh: false,
            tunnelStatusData: [],
            virtualUserData: [],
            wanListData: []
        },

        stores: {

            tunnelList: {
                data: '{settings.tunnels.list}'
            },

            networkList: {
                data: '{settings.networks.list}'
            },

            listenList: {
                data: '{settings.virtualListenList.list}'
            },

            tunnelStatusStore: {
                data: '{tunnelStatusData}',
                fields: [{
                    name: 'mode'
                }, {
                    name: 'src',
                    sortType: 'asIp'
                }, {
                    name: 'dst',
                    sortType: 'asIp'
                }, {
                    name: 'tmplSrc',
                }, {
                    name: 'tmplDst',
                }, {
                    name: 'proto',
                }, {
                    name: 'inBytes',
                    sortType: 'asInt'
                }, {
                    name: 'outBytes',
                    sortType: 'asInt'
                }]
            },

            virtualUserStore: {
                data: '{virtualUserData}',
                fields: [{
                    name: 'clientAddress',
                    sortType: 'asIp'
                }, {
                    name: 'clientProtocol',
                }, {
                    name: 'clientUsername',
                }, {
                    name: 'netInterface',
                }, {
                    name: 'sessionCreation',
                }, {
                    name: 'sessionElapsed',
                    sortType: 'asInt'
                }]
            },

            wanListStore: {
                fields: [ 'index' , 'address' , 'name' ],
                data: '{wanListData}'
            },

            activeWanAddress: '',

            P1CipherStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: '3DES', value: '3des' },
                    { name: 'AES-128', value: 'aes128' },
                    { name: 'AES-128 GCM w/ 8 bit ICV', value: 'aes128gcm8' },
                    { name: 'AES-128 GCM w/ 12 bit ICV', value: 'aes128gcm12' },
                    { name: 'AES-128 GCM w/ 16 bit ICV', value: 'aes128gcm16' },
                    { name: 'AES-128 GCM w/ 64 bit ICV', value: 'aes128gcm64' },
                    { name: 'AES-128 GCM w/ 96 bit ICV', value: 'aes128gcm96' },
                    { name: 'AES-128 GCM w/ 128 bit ICV', value: 'aes128gcm128' },
                    { name: 'AES-256', value: 'aes256' },
                    { name: 'AES-256 GCM w/ 8 bit ICV', value: 'aes256gcm8' },
                    { name: 'AES-256 GCM w/ 12 bit ICV', value: 'aes256gcm12' },
                    { name: 'AES-256 GCM w/ 16 bit ICV', value: 'aes256gcm16' },
                    { name: 'AES-256 GCM w/ 64 bit ICV', value: 'aes256gcm64' },
                    { name: 'AES-256 GCM w/ 96 bit ICV', value: 'aes256gcm96' },
                    { name: 'AES-256 GCM w/ 128 bit ICV', value: 'aes256gcm128' },
                    { name: 'Camellia', value: 'camellia' },
                    { name: 'Blowfish', value: 'blowfish' },
                    { name: 'Twofish', value: 'twofish' },
                    { name: 'Serpent', value: 'serpent' }
                ]
            },

            P1HashStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: 'MD5', value: 'md5' },
                    { name: 'SHA-1', value: 'sha1' },
                    { name: 'SHA-256', value: 'sha2_256' },
                    { name: 'SHA-384', value: 'sha2_384' },
                    { name: 'SHA-512', value: 'sha2_512' }
                ]
            },

            P1GroupStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: '1 (modp768)', value: 'modp768' },
                    { name: '2 (modp1024)', value: 'modp1024' },
                    { name: '5 (modp1536)', value: 'modp1536' },
                    { name: '14 (modp2048)', value: 'modp2048' },
                    { name: '15 (modp3072)', value: 'modp3072' },
                    { name: '16 (modp4096)', value: 'modp4096' },
                    { name: '17 (modp6144)', value: 'modp6144' },
                    { name: '18 (modp8192)', value: 'modp8192' },
                    { name: '22 (modp1024s160)', value:'modp1024s160' },
                    { name: '23 (modp2048s224)', value:'modp2048s224' },
                    { name: '24 (modp2048s256)', value: 'modp2048s256' },
                    { name: '25 (ecp192)', value: 'ecp192' },
                    { name: '26 (ecp224)', value: 'ecp224' },
                    { name: '19 (ecp256)', value: 'ecp256' },
                    { name: '20 (ecp384)', value: 'ecp384' },
                    { name: '21 (ecp521)', value: 'ecp521' },
                    { name: '27 (ecp224pb)', value: 'ecp224bp' },
                    { name: '28 (ecp256bp)', value: 'ecp256bp' },
                    { name: '29 (ecp384bp)', value: 'ecp384bp' },
                    { name: '30 (ecp512bp)', value: 'ecp512bp' }
                ]
            },

            P2CipherStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: '3DES', value: '3des' },
                    { name: 'AES-128', value: 'aes128' },
                    { name: 'AES-128 GCM w/ 8 bit ICV', value: 'aes128gcm8' },
                    { name: 'AES-128 GCM w/ 12 bit ICV', value: 'aes128gcm12' },
                    { name: 'AES-128 GCM w/ 16 bit ICV', value: 'aes128gcm16' },
                    { name: 'AES-128 GCM w/ 64 bit ICV', value: 'aes128gcm64' },
                    { name: 'AES-128 GCM w/ 96 bit ICV', value: 'aes128gcm96' },
                    { name: 'AES-128 GCM w/ 128 bit ICV', value: 'aes128gcm128' },
                    { name: 'AES-256', value: 'aes256' },
                    { name: 'AES-256 GCM w/ 8 bit ICV', value: 'aes256gcm8' },
                    { name: 'AES-256 GCM w/ 12 bit ICV', value: 'aes256gcm12' },
                    { name: 'AES-256 GCM w/ 16 bit ICV', value: 'aes256gcm16' },
                    { name: 'AES-256 GCM w/ 64 bit ICV', value: 'aes256gcm64' },
                    { name: 'AES-256 GCM w/ 96 bit ICV', value: 'aes256gcm96' },
                    { name: 'AES-256 GCM w/ 128 bit ICV', value: 'aes256gcm128' },
                    { name: 'Camellia', value: 'camellia' },
                    { name: 'Blowfish', value: 'blowfish' },
                    { name: 'Twofish', value: 'twofish' },
                    { name: 'Serpent', value: 'serpent' }
                ]
            },

            P2HashStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: 'MD5', value: 'md5' },
                    { name: 'SHA-1', value: 'sha1' },
                    { name: 'SHA-256', value: 'sha2_256' },
                    { name: 'SHA-384', value: 'sha2_384' },
                    { name: 'SHA-512', value: 'sha2_512' },
                    { name: 'AES XCBC', value: 'aesxcbc' },
                    { name: '128-bit AES-GMAC', value: 'aes128gmac' },
                    { name: '192-bit AES-GMAC', value: 'aes192gmac' },
                    { name: '256-bit AES-GMAC', value: 'aes256gmac' }
                ]
            },

            P2GroupStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: '0 (disabled)', value: 'disabled' },
                    { name: '1 (modp768)', value: 'modp768' },
                    { name: '2 (modp1024)', value: 'modp1024' },
                    { name: '5 (modp1536)', value: 'modp1536' },
                    { name: '14 (modp2048)', value: 'modp2048' },
                    { name: '15 (modp3072)', value: 'modp3072' },
                    { name: '16 (modp4096)', value: 'modp4096' },
                    { name: '17 (modp6144)', value: 'modp6144' },
                    { name: '18 (modp8192)', value: 'modp8192' },
                    { name: '22 (modp1024s160)', value:'modp1024s160' },
                    { name: '23 (modp2048s224)', value:'modp2048s224' },
                    { name: '24 (modp2048s256)', value: 'modp2048s256' },
                    { name: '25 (ecp192)', value: 'ecp192' },
                    { name: '26 (ecp224)', value: 'ecp224' },
                    { name: '19 (ecp256)', value: 'ecp256' },
                    { name: '20 (ecp384)', value: 'ecp384' },
                    { name: '21 (ecp521)', value: 'ecp521' },
                    { name: '27 (ecp224pb)', value: 'ecp224bp' },
                    { name: '28 (ecp256bp)', value: 'ecp256bp' },
                    { name: '29 (ecp384bp)', value: 'ecp384bp' },
                    { name: '30 (ecp512bp)', value: 'ecp512bp' }
                ]
            }
        }
    },

    items: [
        { xtype: 'app-ipsec-vpn-status' },
        { xtype: 'app-ipsec-vpn-ipsecoptions' },
        { xtype: 'app-ipsec-vpn-ipsectunnels' },
        { xtype: 'app-ipsec-vpn-vpnconfig' },
        { xtype: 'app-ipsec-vpn-grenetworks' },
        { xtype: 'app-ipsec-vpn-ipsecstate' },
        { xtype: 'app-ipsec-vpn-ipsecpolicy' },
        { xtype: 'app-ipsec-vpn-ipseclog' },
        { xtype: 'app-ipsec-vpn-l2tplog' }
    ],

    statics:{
        leftRenderer: function(value, meta, record, rowIndex, columnIndex, store, table){
            var vm = table.up('apppanel').getViewModel();
            if(value == 'active_wan_address'){
                return Ext.String.format('{0} [{1}]', 'Active WAN'.t(), vm.get('activeWanAddress'));
            }
            return value;
        },
        rightRenderer: function(value, meta, record, rowIndex, columnIndex, store, table){
            if(value == '%any'){
                return 'Any Remote Host'.t();
            }
            return value;
        }
    }
});
