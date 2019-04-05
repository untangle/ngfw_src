Ext.define('Ung.config.network.view.Hostname', {
    extend: 'Ext.form.Panel',
    alias: 'widget.config-network-hostname',
    itemId: 'hostname',
    scrollable: true,

    withValidation: true, // requires validation on save
    viewModel: true,

    title: 'Hostname'.t(),
    bodyPadding: 10,

    items: [{
        xtype: 'fieldset',
        title: 'Hostname'.t(),
        padding: 10,
        items: [{
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Hostname'.t(),
                labelAlign: 'right',
                emptyText: 'hostname',
                allowBlank: false,
                blankText: 'Hostname must be specified.'.t(),
                name: 'HostName',
                bind: '{settings.hostName}',
                maskRe: /[a-zA-Z0-9\-]/
            }, {
                xtype: 'displayfield',
                value: '(eg: gateway)'.t(),
                margin: '0 0 0 5'
                // cls: 'boxlabel'
            }]
        },{
            xtype: 'container',
            layout: 'column',
            margin: '0 0 5 0',
            items: [{
                xtype: 'textfield',
                fieldLabel: 'Domain Name'.t(),
                labelAlign: 'right',
                emptyText: 'example.com',
                allowBlank: false,
                blankText: 'Domain Name must be specified.'.t(),
                name: 'DomainName',
                bind: '{settings.domainName}',
                vtype: 'domainName'
            }, {
                xtype: 'displayfield',
                value: '(eg: example.com)'.t(),
                margin: '0 0 0 5'
                // cls: 'boxlabel'
            }]
        }]
    }, {
        xtype: 'fieldset',
        title: 'Dynamic DNS Service Configuration'.t(),
        checkboxToggle: true,
        collapsible: true,
        collapsed: true,
        padding: 10,
        checkbox: {
            bind: {
                value: '{settings.dynamicDnsServiceEnabled}'
            }
        },
        defaults: {
            labelAlign: 'right'
        },
        items: [{
            xtype: 'combo',
            editable: false,
            fieldLabel: 'Service'.t(),
            bind: '{settings.dynamicDnsServiceName}',
            store: [['easydns','EasyDNS'],
                    ['zoneedit','ZoneEdit'],
                    ['dyndns','DynDNS'],
                    ['namecheap','Namecheap'],
                    ['dslreports','DSL-Reports'],
                    ['dnspark','DNSPark'],
                    ['no-ip','No-IP'],
                    ['dnsomatic','DNS-O-Matic'],
                    ['freedns','FreeDNS'],
                    ['google','Google'],
                    ['googledomains','Google Domains'],
                    //['cloudflare','Cloudflare'],
                    ['duckdns','DuckDNS']
                   ]
        }, {
            xtype: 'textfield',
            fieldLabel: 'Username'.t(),
            bind: '{settings.dynamicDnsServiceUsername}'
        }, {
            xtype: 'textfield',
            fieldLabel: 'Password'.t(),
            bind: '{settings.dynamicDnsServicePassword}',
            inputType: 'password'
        }, {
            xtype: 'textfield',
            fieldLabel: 'Hostname(s)'.t(),
            bind: '{settings.dynamicDnsServiceHostnames}',
        }]
    }, {
        xtype: 'radiogroup',
        title: 'Public Address Configuration'.t(),
        columns: 1,
        simpleValue: true,
        bind: '{settings.publicUrlMethod}',
        items: [{
            xtype: 'component',
            margin: '0 0 10 0',
            bind:{
                html: Ext.String.format('The Public Address is the address/URL that provides a public location for the {0} Server. This address will be used in emails sent by the {0} Server to link back to services hosted on the {0} Server such as Quarantine Digests and OpenVPN Client emails.'.t(), '{companyName}')
            }
        }, {
            xtype: 'radio',
            boxLabel: '<strong>' + 'Use IP address from External interface (default)'.t() + '</strong>',
            name: 'publicUrl',
            inputValue: 'external'
        }, {
            xtype: 'component',
            margin: '0 0 10 25',
            bind:{
                html: Ext.String.format('This works if your {0} Server has a routable public static IP address.'.t(), '{companyName}')
            }
        }, {
            xtype: 'radio',
            boxLabel: '<strong>' + 'Use Hostname'.t() + '</strong>',
            name: 'publicUrl',
            inputValue: 'hostname'
        }, {
            xtype: 'component',
            margin: '0 0 5 25',
            bind:{
                html: Ext.String.format('This is recommended if the {0} Server\'s fully qualified domain name looks up to its IP address both internally and externally.'.t(), '{companyName}')
            }
        }, {
            xtype: 'component',
            margin: '0 0 10 25',
            bind: {
                html: 'Current Hostname'.t() + ':<i> {fullHostName} </i>'
            }
        }, {
            xtype: 'radio',
            boxLabel: '<strong>' + 'Use Manually Specified Address'.t() + '</strong>',
            name: 'publicUrl',
            inputValue: 'address_and_port'
        }, {
            xtype: 'component',
            margin: '0 0 10 25',
            bind:{
                html: Ext.String.format('This is recommended if the {0} Server is installed behind another firewall with a port forward from the specified hostname/IP that redirects traffic to the {0} Server.'.t(), '{companyName}')
            }
        }, {
            xtype: 'textfield',
            margin: '0 0 5 25',
            fieldLabel: 'IP/Hostname'.t(),
            name: 'publicUrlAddress',
            allowBlank: false,
            width: 400,
            blankText: 'You must provide a valid IP Address or hostname.'.t(),
            disabled: true,
            bind: {
                value: '{settings.publicUrlAddress}',
                disabled: '{settings.publicUrlMethod != "address_and_port"}',
            }
        }, {
            xtype: 'numberfield',
            margin: '0 0 5 25',
            fieldLabel: 'Port'.t(),
            name: 'publicUrlPort',
            allowDecimals: false,
            minValue: 0,
            allowBlank: false,
            width: 210,
            blankText: 'You must provide a valid port.'.t(),
            vtype: 'port',
            disabled: true,
            bind: {
                value: '{settings.publicUrlPort}',
                disabled: '{settings.publicUrlMethod != "address_and_port"}',
            }
        }]
    }]
});
