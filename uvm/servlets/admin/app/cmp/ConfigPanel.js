Ext.define('Ung.cmp.ConfigPanel', {
    extend: 'Ext.tab.Panel',
    alias: 'widget.configpanel',
    layout: 'fit',

    isTogglePressed: false,

    showNetworkConfig: function () {
        return this.isTogglePressed;
    },

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'footer',
        dock: 'top',
        style: { background: '#D8D8D8' },
        items: [{
            text: 'Back to Config',
            iconCls: 'fa fa-arrow-circle-left',
            hrefTarget: '_self',
            href: '#config'
        }, {
            xtype: 'component',
            padding: '0 5',
            bind: { html: '<img src="/icons/config/{iconName}.svg" style="vertical-align: middle;" width="17" height="17"/> <strong>{title}</strong>' }
        },
        {
            xtype: 'button',
            text: 'New Layout',
            dock: 'top',
            iconCls: 'fa fa-toggle-on',
            enableToggle: true,
            toggleHandler: function (btn, pressed) {
    
                Ext.defer(function () {
                    var configPanel = btn.up('configpanel');

                    if (pressed) {
                        configPanel.isTogglePressed = true;
                        // var iframeContainer = configPanel.down('panel[reference=iframeContainer]');
                        
                        configPanel.fireEvent('toggleChanged', pressed);

                        configPanel.removeAll(true);
                        configPanel.update(''); 
                        
                        configPanel.fireEvent('afterRenderVue', configPanel);
                        // window.open('/vue/settings/network', '_blank');
                    } else {
                        configPanel.isTogglePressed = false;

                        configPanel.removeAll(true);
                        configPanel.update(''); 

                        // Add network configuration items
                        configPanel.add([
                            { xtype: 'config-network-interfaces' },
                            { xtype: 'config-network-hostname' },
                            { xtype: 'config-network-services' },
                            { xtype: 'config-network-port-forward-rules' },
                            { xtype: 'config-network-nat-rules' },
                            { xtype: 'config-network-bypass-rules' },
                            { xtype: 'config-network-filter-rules' },
                            { xtype: 'config-network-routes' },
                            { xtype: 'config-network-dynamicrouting' },
                            { xtype: 'config-network-dns-server' },
                            { xtype: 'config-network-dhcp-server' },
                            { xtype: 'config-network-advanced' },
                            { xtype: 'config-network-troubleshooting' }
                        ]);
                    }
            
                    // Update button appearance
                    btn.setIconCls(pressed ? 'fa fa-toggle-on' : 'fa fa-toggle-off');
                    btn.setText(pressed ? 'New Layout' : 'Click for new Layout');
                }, 50);
            }, 
        },]
    },
    {
        xtype: 'toolbar',
        dock: 'bottom',
        items: ['->', {
            text: '<strong>' + 'Save'.t() + '</strong>',
            bind:{
                disabled: '{panel.saveDisabled}'
            },
            iconCls: 'fa fa-floppy-o fa-lg',
            handler: 'saveSettings'
        }]
    }],

    listeners: {
        afterRenderVue: function (view) {

            view.removeAll(true);

            view.add({
                xtype: 'component',
                autoEl: {
                    tag: 'iframe',
                    src: 'vue/settings/network/dhcp',
                    style: 'position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none; min-width: 1000px; overflow: visible;'
                }
            });
        //     view.removeAll(true);

        // view.add({
        //     xtype: 'panel',
        //     layout: 'border', // Border layout for left panel + main content
        //     border: false,
        //     items: [
        //         // {
        //         //     region: 'west', // Left panel
        //         //     xtype: 'panel',
        //         //     width: 250, // Adjust width for visibility
        //         //     html: '<div style="padding: 10px; background: #f0f0f0;">Left Panel Placeholder</div>',
        //         //     hidden: false // Ensure it's visible
        //         // },
        //         {
        //             region: 'center', // Main content with iframe
        //             xtype: 'panel',
        //             layout: 'fit',
        //             items: [{
        //                 xtype: 'component',
        //                 autoEl: {
        //                     tag: 'iframe',
        //                     src: '/vue/settings/network/dhcp',
        //                     style: 'width: 100%; height: 100%; border: none;'
        //                 }
        //             }]
        //         }
        //     ]
        // });
            // view.removeAll(true); // Clear previous items

            // var iframe = view.add({
            //     xtype: 'component',
            //     autoEl: {
            //         tag: 'iframe',
            //         src: '/vue/settings/network/dhcp/',
            //         style: 'width: 100%; height: 100%; border: none;'
            //     }
            // });

            // // Force refresh after 100ms (to ensure visibility)
            // Ext.defer(function () {
            //     iframe.getEl().dom.contentWindow.location.reload();
            // }, 100);
            // view.add({
            //     xtype: 'panel', // Use a panel to ensure correct layout
            //     layout: 'fit',  // Ensure iframe stretches correctly
            //     border: false,
            //     items: [{
            //         xtype: 'component',
            //         autoEl: {
            //             tag: 'iframe',
            //             src: '/vue/',
            //             style: 'width: 100%; height: 100%; border: none;'
            //         }
            //     }]
                // xtype: 'component',
                // autoEl: {
                //     tag: 'iframe',
                //     src: '/vue/',
                //     style: 'position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;'
                // }
            // });
            // view.setHtml('<iframe src="/vue/" style="position: absolute; top: 0; left: 0; width: 100%; height: 100%; border: none;"></iframe>');
        },

        // generic listener for all tabs in Apps, redirection
        beforetabchange: function (tabPanel, newCard, oldCard) {
            Ung.app.redirectTo('#config/' + tabPanel.name + '/' + newCard.getItemId());
        },

        tabchange: function (tabPanel) {
            Ung.app.hashBackup = window.location.hash; // keep track of hash for changes detection
        },

        afterrender: function (configPanel) {
            // code used for detecting user manual data change
            Ung.app.hashBackup = window.location.hash; // keep track of hash for changes detection
            Ext.Array.each(configPanel.query('field'), function (field) {
                // setup the _initialValue of the field on focus
                field.on('focus', function () {
                    if (!field.hasOwnProperty('_initialValue')) {
                        field._initialValue = field.getValue();
                    }
                });
                field.on('blur', function () {
                    // on field blur check if new value is different than the initial one
                    // add an _isChanged prop which is true if field value is really changed by the user manually
                    if (field._initialValue !== field.getValue()) {
                        field._isChanged = true;
                    } else {
                        field._isChanged = false;
                    }
                });
            });
        },

        removed: function(){
            this.getViewModel().set('panel.saveDisabled', false);
        }
    }

});
