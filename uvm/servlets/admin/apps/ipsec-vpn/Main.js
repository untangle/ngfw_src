Ext.define('Ung.apps.ipsecvpn.Main', {
    extend: 'Ung.cmp.AppPanel',
    alias: 'widget.app-ipsec-vpn',
    controller: 'app-ipsec-vpn',

    viewModel: {
        stores: {
            tunnelList: {
                data: '{settings.tunnels.list}'
            },

            P1CipherStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: '3DES', value: '3des' },
                    { name: 'AES128', value: 'aes128' },
                    { name: 'AES256', value: 'aes256' },
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
                    { name: 'SHA-512', value: 'sha2_512' }
                ]
            },

            P1GroupStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: '2 (1024 bit)', value: 'modp1024' },
                    { name: '5 (1536 bit)', value: 'modp1536' },
                    { name: '14 (2048 bit)', value: 'modp2048' },
                    { name: '15 (3072 bit)', value: 'modp3072' },
                    { name: '16 (4096 bit)', value: 'modp4096' },
                    { name: '17 (6144 bit)', value: 'modp6144' },
                    { name: '18 (8192 bit)', value: 'modp8192' }
                ]
            },

            P2CipherStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: '3DES', value: '3des' },
                    { name: 'AES128', value: 'aes128' },
                    { name: 'AES256', value: 'aes256' },
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
                    { name: 'SHA-512', value: 'sha2_512' },
                    { name: 'RIPEMD', value: 'ripemd' }
                ]
            },

            P2GroupStore: {
                fields: [ 'name', 'value' ],
                data: [
                    { name: '0 (disabled)', value: 'disabled' },
                    { name: '2 (1024 bit)', value: 'modp1024' },
                    { name: '5 (1536 bit)', value: 'modp1536' },
                    { name: '14 (2048 bit)', value: 'modp2048' },
                    { name: '15 (3072 bit)', value: 'modp3072' },
                    { name: '16 (4096 bit)', value: 'modp4096' },
                    { name: '17 (6144 bit)', value: 'modp6144' },
                    { name: '18 (8192 bit)', value: 'modp8192' }
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
    ]

});

Ext.define('Ung.apps.ipsecvpn.Data', {
    singleton: true,
    wanList: [],
    leftDefault: '0.0.0.0',
    leftSubnetDefault: '0.0.0.0/0',
});
