Ext.define('Ung.Setup.License', {
    extend: 'Ext.form.Panel',
    alias: 'widget.License',

    title: 'License'.t(),
    description: 'License'.t(),

    layout: {
        type: 'vbox',
        align: 'middle'
    },

    defaults: {
        layout: {
            type: 'vbox',
            align: 'stretch',
        }
    },

    viewModel: {
        data: {
            eulaLoaded: false,
            nextStep: null
        }
    },

    items: [{
        xtype: 'container',
        width: 600,
        layout: {
            type: 'vbox',
            align: 'stretch'
        },
        items: [{
            xtype: 'component',
            style: { 
                "word-wrap": 'break-word',
                "text-align": "center",
                "margin-bottom": '5px'
            },
            html: '<p>' + "To continue installing and using this software, you must agree to the terms and conditions of the software license agreement. Please review the whole license agreement by scrolling through to the end of the agreement".t()+'</p>'
        }, {
            xtype: 'container',
            itemId: 'eula',
            style: 'background: #FFF; border-radius: 3px; border: 1px #EEE solid; line-height: 0;',
            html: '<iframe id="eula-src" style="border: none; width: 100%; height: 350px;"></iframe>',
            masked: {
                xtype: 'loadmask',
                message: 'Loading ...'
            },
        },{
            xtype: 'component',
            style: { 
                "margin-top": '5px',
                "word-wrap": 'break-word',
                "text-align": "center"
            },
            html: '<p>' + Ext.String.format('After installation, this license is available at {0}'.t(), '<a style="color: blue;" href="https://www.untangle.com/legal" target="_blank">https://www.untangle.com/legal</a>') + '</p>'
        },{
            xtype: 'container',
            margin: '8 0',
            layout: {
                type: 'hbox',
                pack: 'middle'
            },
            defaults: {
                xtype: 'button',
                margin: 8
            },
            items: [{
                text: 'Disagree',
                handler: 'onDisagree'
            }, {
                text: 'Agree',
                handler: 'onContinue'
            }]
        }]
    }],

    listeners: {
        save: 'onSave'
    },

    controller: {
        onSave: function(cb){
            Util.setRpcJsonrpc();
            cb();
        },
        afterRender: function( view ){
            var vm = this.getViewModel(),
                remoteEulaSrc = 'https://www.untangle.com/legal',
                localEulaSrc = '/setup/legal.html',
                iframe = document.getElementById('eula-src'),
                iframeCmp = view.down('[itemId=eula]'),
                img = new Image(0,0); // 0 width and height
                
            iframeCmp.mask();

            vm.set('nextStep', "");

            // if eula already loaded no connection check required
            if (iframe.src) {
                vm.set('eulaLoaded', true);
                return;
            }

            img.src = 'https://www.untangle.com/favicon.ico';

            img.addEventListener('error', function () {
                iframe.src = localEulaSrc;
            });
            img.addEventListener('load', function () {
                iframe.src = remoteEulaSrc;
            });

            // unmask eula container, remove image and show agreement buttons after license content loaded
            iframe.addEventListener('load', function () {
                // img.parentNode.removeChild(img);
                iframeCmp.unmask();
                vm.set('eulaLoaded', true);
            });
            // append the test image wich will trigger the load/error events
            document.body.appendChild(img);
        },
        onContinue: function(){
            this.getView().up('setupwizard').getController().onNext();
        },
        onDisagree: function(){
            window.location.reload();
        }
    }

});
