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
                handler: 'onDisagree',
                bind: {
                    disabled: '{!eulaLoaded}'
                }
            }, {
                text: 'Agree',
                handler: 'onContinue',
                bind: {
                    disabled: '{!eulaLoaded}'
                }
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

        timer: null,
        remoteEulaSrc: 'https://www.untangle.com/legal',
        localEulaSrc: '/setup/legal.html',
        remoteImage: 'https://www.untangle.com/favicon.ico',
        clearTimer: function(){
            if (this.timer) {
                clearTimeout(this.timer);
                this.timer = null;
                var iframe = document.getElementById('eula-src');
                iframe.src = this.remoteEulaSrc;
            }
        },

        handleFail: function(){
            this.onload = this.onabort = this.onerror = function() {};
            this.ownerCmp.clearTimer();
            var iframe = document.getElementById('eula-src');
            iframe.src = this.ownerCmp.localEulaSrc;
        },

        afterRender: function( view ){
            var me = this,
                vm = this.getViewModel(),
                iframe = document.getElementById('eula-src'),
                iframeCmp = view.down('[itemId=eula]'),
                timer = null,
                img = new Image(0,0);

            iframeCmp.mask();
            img.ownerCmp = me;
            vm.set('nextStep', "");

            img.onerror = img.onabort = me.handleFail;
            img.onload = function(){
                me.clearTimer();
            };
            img.src = me.remoteImage;
            me.timer = setTimeout( function(image){
                return function(){
                    me.handleFail.call(image);
                };
            }(img), 1000);

            iframe.addEventListener('load', function () {
                iframeCmp.unmask();
                vm.set('eulaLoaded', true);
            });
        },
        onContinue: function(){
            this.getView().up('setupwizard').getController().onNext();
        },
        onDisagree: function(){
            window.location.reload();
        }
    }

});
