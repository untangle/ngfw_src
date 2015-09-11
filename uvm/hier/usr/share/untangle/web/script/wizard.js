Ext.define('Ung.Wizard', {
    extend:'Ext.panel.Panel',
    layout: { type: 'hbox', align: 'stretch' },
    name: 'wizard',
    currentPage: null,
    hasCancel: false,
    modalFinish: false, //can not go back or cancel on finish step
    finished: false,
    showLogo: false,
    initComponent : function() {
        if(this.showLogo) {
            Ext.DomHelper.append(this.renderTo,{
                tag: 'div',
                cls: 'logo-container',
                children: [{tag: 'img', src:'../images/BrandingLogo.png'}]
            });
        }
        // Build a panel to hold the headers on the left
        this.headerPanel = Ext.create('Ext.container.Container', {
            cls: 'wizard-steps',
            layout: { type: 'vbox', align: 'right'},
            flex: 0,
            width: 200,
            defaults: { border : false, width : 200 },
            items: this.buildHeaders( this.cards )
        } );

        var items = [];
        for (var i = 0; i < this.cards.length; i++ ) {
            items.push(this.cards[i].panel );
        }
        this.previousButton = Ext.create('Ext.button.Button', {
            text : Ext.String.format(i18n._( '{0} Previous' ),'&laquo;'),
            handler : Ext.bind(this.goPrevious, this )
        });
        this.nextButton = Ext.create('Ext.button.Button',{
            text : Ext.String.format(i18n._( 'Next {0}' ),'&raquo;'),
            handler : Ext.bind(this.goNext, this )
        });

        var bbarArr=[ '->', this.previousButton, { xtype: 'tbspacer', width: 10 },this.nextButton , { xtype: 'tbspacer', width: 15 }];
        if(this.hasCancel) {
            this.cancelButton = Ext.create('Ext.button.Button',{
                iconCls: 'cancel-icon',
                text : i18n._( 'Cancel' ),
                handler : Ext.bind(function() {
                    this.cancelAction();
                },this)
            });
            bbarArr.unshift(this.cancelButton);
        }
        
        if ( this.cardDefaults == null ) { this.cardDefaults = {}; }
        Ext.apply(this.cardDefaults, { border: true, autoScroll: true });

        // Build a card to hold the wizard
        this.contentPanel = Ext.create('Ext.panel.Panel',{
            layout : "card",
            flex: 1,
            items : items,
            activeItem : 0,
            defaults : this.cardDefaults,
            bbar : bbarArr,
            border:false
        });
        this.items = [ this.headerPanel, this.contentPanel ];
        this.callParent(arguments);
    },
    buildHeaders : function( cards ) {
        var items = [];
        var length = cards.length;
        for ( var c = 0 ; c < length ; c++ ) {
            var card = cards[c];
            var addnlclass = '';
            if(c === 0 || c == length -1){
                addnlclass = ' nostep ';
            }
            var title = '<span class="text'+addnlclass+'">' + card.title + '</span>';
            if (( c > 0 ) && ( c < ( length - 1 ))) {
                title = Ext.String.format( '<span class="count">{0}</span> ', c  ) + title;
            }
            items.push({
                xtype: 'component',
                html : title,
                cls : 'step'
            });
        }
        return items;
    },
    goPrevious : function() {
        this.goToPage( this.currentPage - 1 );
    },
    goNext : function() {
        this.goToPage( this.currentPage + 1 );
    },
    goToPage : function( index ) {
        if(this.currentPage == null || this.currentPage == index) {
            this.loadPage(index);
            return;
        }
        var handler = null;
        if ( this.currentPage <= index ) {
            if(Ext.isFunction(this.cards[this.currentPage].onValidate)){
                if(!this.cards[this.currentPage].onValidate()){
                    return;
                }
            }
            handler = this.cards[this.currentPage].onNext;
        } else if ( this.currentPage > index ) {
            handler = this.cards[this.currentPage].onPrevious;
        }

        // Call the handler if it is defined
        if ( Ext.isFunction(handler) ) {
            handler( Ext.bind(this.loadPage, this, [index] ));
        } else {
            this.loadPage(index);
        }
    },

    loadPage : function( index) {
        if ( index < 0 || index >= this.cards.length ) {
            return;
        }
        this.currentPage = index;
        var card = this.cards[this.currentPage];
        if ( Ext.isFunction(card.onLoad)) {
            card.onLoad( Ext.bind(this.syncWizard, this ));
        } else {
            this.syncWizard();
        }
    },

    syncWizard : function() {
        this.contentPanel.getLayout().setActiveItem( this.currentPage );
        // update steps classes
        var items = this.headerPanel.query('component');
        var length = items.length;
        var isComplete = true;
        for ( var c = 0 ; c < length ; c++ ) {
            var item = items[c];
            if ( c == this.currentPage ) {
                item.removeCls( "incomplete" );
                item.removeCls( "completed" );
                item.addCls( "current" );
                isComplete = false;
            } else {
                item.removeCls( "current" );
                if ( isComplete ) {
                    item.removeCls( "incomplete" );
                    item.addCls( "completed" );
                } else {
                    item.removeCls( "completed" );
                    item.addCls( "incomplete" );
                }
            }
        }

        if ( this.currentPage == 0 || (this.modalFinish && this.currentPage == ( length - 1 ))) {
            this.previousButton.hide();
        } else {
            this.previousButton.show();
        }

        if ( this.currentPage == ( length - 1 )) {
            if(this.modalFinish) {
                this.nextButton.setText( i18n._('Close') );
                if(this.hasCancel) {
                   this.cancelButton.hide();
                }
                this.finished=true;
            } else {
                this.nextButton.setText( i18n._('Finish') );
            }
        } else {
            this.nextButton.setText( Ext.String.format(i18n._('Next {0}'),"&raquo;"));
            if(this.hasCancel) {
                this.cancelButton.show();
            }
        }
    },
    cancelAction: Ext.emptyFn
});