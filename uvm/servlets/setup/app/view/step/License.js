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
            nextStep: null,
            remoteEulaSrc: '',
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
            html: '<p>' + "To continue installing and using this software, you must agree to the terms and conditions of the software license agreement. Please review the whole license agreement by navigating to the provided website link and scrolling through to the end of the agreement.".t()+'</p>'
        }, {
            xtype: 'component',
            style: { 
                "margin-top": '5px',
                "word-wrap": 'break-word',
                "text-align": "center"
            },
            // NOTE: These placeholder urls are filled in for uri translations in afterRender.
            html: '<p>' + Ext.String.format('The license is available at {0}'.t(), '<a style="color: blue;" id="licenseUrl" href="https://edge.arista.com/legal" target="_blank">https://edge.arista.com/legal</a>') + '</p>'
        }, {
            xtype: 'component',
            style: { 
                "margin-top": '5px',
                "word-wrap": 'break-word',
                "text-align": "center",
                "font-weight": "bold"
            },
            html: '<p>' + "After completing the setup, legal links can also be viewed from the About page." + '</p>'
        }, {
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
            Util.setRpcJsonrpc("setup");
            cb();
        },

        timer: null,
        clearTimer: function(){
            if (this.timer) {
                clearTimeout(this.timer);
                this.timer = null;
            }
        },

        handleFail: function(){
            this.onload = this.onabort = this.onerror = function() {};
            this.ownerCmp.clearTimer();
        },
        afterRender: function(){
            this.remoteEulaSrc = rpc.licenseAgreementUrl;
            this.remoteImage = rpc.licenseTestUrl;
            var me = this,
                vm = this.getViewModel(),
                hyperlink = document.getElementById('licenseUrl'),
                img = new Image(0,0);
            
            hyperlink.href = this.remoteEulaSrc;
            hyperlink.innerText = this.remoteEulaSrc;

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
        },
        onContinue: function(){
            this.getView().up('setupwizard').getController().onNext();
        },
        onDisagree: function(){
            window.location.reload();
        }
    }

});
