Ext.define('Ung.apps.sslinspector.view.Configuration', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-ssl-inspector-configuration',
    itemId: 'configuration',
    title: 'Configuration'.t(),
    bodyPadding: 10,

    scrollable: true,

    viewModel: {
    },

    defaults: {
        border: false
    },

    items: [{
        title: "Description".t(),
        xtype: 'fieldset',
        padding: '10 0 10 0',
        html: "The SSL Inspector is an SSL decryption engine that allows other applications and services to process port 443 HTTPS and port 25 SMTPS traffic just like unencrypted port 80 HTTP and port 25 SMTP traffic. To do this, the application generates new SSL certificates on the fly which it uses to perform a the man-in-the-middle style inspection of traffic. To eliminate certificate security warnings on client computers and devices, you should download the root certificate and add it to the list of trusted authorities on each client connected to your network.".t()
    }, {
        xtype: 'fieldset',
        layout: 'column',
        items: [{
            xtype: 'button',
            margin: '0 5 0 5',
            width: 250,
            text: "Download Root Certificate".t(),
            iconCls: 'fa fa-download',
            handler: Ext.bind(function() {
                var downloadForm = document.getElementById('downloadForm');
                downloadForm["type"].value = "certificate_download";
                downloadForm["arg1"].value = "root";
                downloadForm.submit();
            }, this)
        },{
            xtype: 'displayfield',
            margin: '0 5 0 5',
            columnWidth: 1,
            value: "Click here to download the root certificate.".t(),
        }]
    }, {
        title: "Options".t(),
        xtype: 'fieldset',
        labelWidth: 230,
        padding: '10 0 0 0',
        items: [{
            xtype: 'checkbox',
            fieldLabel: "Enable SMTPS Traffic Processing".t(),
            labelWidth: 240,
            bind: '{settings.processEncryptedMailTraffic}'
        },{
            xtype: 'checkbox',
            fieldLabel: "Enable HTTPS Traffic Processing".t(),
            labelWidth: 240,
            bind: '{settings.processEncryptedWebTraffic}'
        },{
            xtype: 'checkbox',
            fieldLabel: "Block Invalid HTTPS Traffic".t(),
            labelWidth: 240,
            bind: '{settings.blockInvalidTraffic}'
        },{
            xtype: 'displayfield',
            padding: '0 0 0 20',
            value:  "When the SSL Inspector detects non-HTTPS traffic on port 443, it will normally ignore this traffic and allow it to flow unimpeded.  If you enable this checkbox, non-HTTPS traffic will instead be blocked.".t()
        }]
    }, {
        xtype: 'fieldset',
        padding: '10 0 10 0',
        layout: { type: 'hbox', align: 'left' },
        defaults: {
            margin: '0 0 0 30',
            labelWidth: 60
        },
        title: "Client Connection Protocols".t(),
        items: [{
            xtype: 'checkbox',
            fieldLabel: "SSLv2Hello".t(),
            labelWidth: 70,
            name: 'client_SSLv2Hello',
            bind: '{settings.client_SSLv2Hello}'
        },{
            xtype: 'checkbox',
            fieldLabel: "SSLv3".t(),
            labelWidth: 40,
            name: 'client_SSLv3',
            bind: '{settings.client_SSLv3}'
        },{
            xtype: 'checkbox',
            fieldLabel: "TLSv1".t(),
            labelWidth: 40,
            name: 'client_TLSv10',
            bind: '{settings.client_TLSv10}'
        },{
            xtype: 'checkbox',
            fieldLabel: "TLSv1.1".t(),
            name: 'client_TLSv11',
            bind: '{settings.client_TLSv11}'
        },{
            xtype: 'checkbox',
            fieldLabel: "TLSv1.2".t(),
            name: 'client_TLSv12',
            bind: '{settings.client_TLSv12}'
        }]
    }, {
        xtype: 'fieldset',
        padding: '10 0 10 0',
        layout: { type: 'hbox', align: 'left' },
        defaults: {
            margin: '0 0 0 30',
            labelWidth: 60
        },
        title: "Server Connection Protocols".t(),
        items: [{
            xtype: 'checkbox',
            fieldLabel: "SSLv2Hello".t(),
            labelWidth: 70,
            name: 'server_SSLv2Hello',
            bind: '{settings.server_SSLv2Hello}'
        },{
            xtype: 'checkbox',
            fieldLabel: "SSLv3".t(),
            labelWidth: 40,
            name: 'server_SSLv3',
            bind: '{settings.server_SSLv3}'
        },{
            xtype: 'checkbox',
            fieldLabel: "TLSv1".t(),
            labelWidth: 40,
            name: 'server_TLSv10',
            bind: '{settings.server_TLSv10}'
        },{
            xtype: 'checkbox',
            fieldLabel: "TLSv1.1".t(),
            name: 'server_TLSv11',
            bind: '{settings.server_TLSv11}'
        },{
            xtype: 'checkbox',
            fieldLabel: "TLSv1.2".t(),
            name: 'server_TLSv12',
            bind: '{settings.server_TLSv12}'
        }]
    }, {
        xtype: 'fieldset',
        padding: '10 0 10 0',
        title: "Server Trust".t(),
        labelWidth: 230,
        items: [{
            xtype: 'checkbox',
            fieldLabel: "Trust All Server Certificates".t(),
            labelWidth: 200,
            name: 'serverBlindTrust',
            bind: '{settings.serverBlindTrust}'
        },{
            xtype: 'displayfield',
            padding: '0 100 10 20',
            value:  "When this check box is enabled, the inspector will blindly trust all server certificates.  When clear, the inspector will only trust server certificates signed by a well known and trusted root certificate authority, or certificates that you have added to the list below.".t()
        }, {
            xtype: 'button',
            text: "Upload Trusted Certificate".t(),
            margin: '0 20 0 20',
            handler: 'uploadTrustedCertificate',
        }, {
            xtype: 'displayfield',
            margin: '10 20 0 20',
            value:  "<b>NOTE:</b> When uploading or deleting trusted certificates, changes are applied immediately.".t()
        }, {
            xtype: 'app-ssl-inspector-trusted-certs-grid',
            padding: '10 20 0 20'
        }]
    }]

});

Ext.define('Ung.apps.sslinspector.view.TrustedCertsGrid', {
    extend: 'Ung.cmp.Grid',
    alias: 'widget.app-ssl-inspector-trusted-certs-grid',
    itemId: 'trusted-certs-grid',
    title: "Trusted Certificates &nbsp;&nbsp; (click any cell to see details)".t(),
    viewModel: true,
    controller: 'app-sslinspector-special',
    bind: '{trustedCertList}',

    emptyText: 'No Trusted Certificates'.t(),

    columns: [{
        header: 'Alias'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'certAlias'
    }, {
        header: 'Issued To'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'issuedTo',
    }, {
        header: 'Issued By'.t(),
        width: Renderer.messageWidth,
        flex: 1,
        dataIndex: 'issuedBy'
    }, {
        header: 'Date Valid'.t(),
        width: Renderer.dateWidth,
        dataIndex: 'dateValid'
    }, {
        header: 'Date Expires'.t(),
        width: Renderer.dateWidth,
        dataIndex: 'dateExpire'
    }, {
        header: 'Delete'.t(),
        xtype: 'actioncolumn',
        width: Renderer.actionWidth,
        align: 'center',
        iconCls: 'fa fa-trash',
        handler: 'deleteTrustedCertificate'
    }]
});
