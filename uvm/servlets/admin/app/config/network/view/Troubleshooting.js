Ext.define('Ung.config.network.view.Troubleshooting', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.config.network.troubleshooting',

    title: 'Troubleshooting'.t(),

    layout: 'fit',

    tbar: [{
        xtype: 'displayfield',
        padding: '0 10',
        value: '<strong>' + 'Network Tests'.t() + '</strong>'
    }],


    // tabPosition: 'left',
    // tabRotation: 0,
    // align: 'left',
    // tabStretchMax: true,

    items: [{
        xtype: 'tabpanel',
        items: [{
            xtype: 'networktest',
            title: 'Connectivity Test'.t(),

            command: [
                '/bin/bash',
                '-c',
                ['echo -n "Testing DNS ... " ; success="Successful";',
                'dig updates.untangle.com > /dev/null 2>&1; if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                'echo -n "Testing TCP Connectivity ... ";',
                'echo "GET /" | netcat -q 0 -w 15 updates.untangle.com 80 > /dev/null 2>&1;',
                'if [ "$?" = "0" ]; then echo "OK"; else echo "FAILED"; success="Failure"; fi;',
                'echo "Test ${success}!"'].join('')
            ],
            viewModel: {
                data: {
                    description: 'The <b>Connectivity Test</b> verifies a working connection to the Internet.'.t(),
                    emptyText: 'Connectivity Test Output'.t()
                }
            }
        }, {
            xtype: 'networktest',
            title: 'Ping Test'.t(),

            command: 'ping -c 5 evz.ro',
            viewModel: {
                data: {
                    description: 'The <b>Ping Test</b> can be used to test that a particular host or client can be pinged'.t(),
                    emptyText: 'Ping Test Output'.t()
                }
            }
        }]
    }],
});