Ext.namespace('Ung');
// The location of the blank pixel image
Ung.Wizard = Ext.extend(Ext.Panel, {
    currentPage : 0,
    constructor : function( config )
    {
        this.cards = config.cards;
		var logo_container = Ext.get('extraDiv1');
		logo_container.addClass( 'logo-container');
        var logo = document.createElement('img');
		logo.src= '../images/BrandingLogo.gif';		
		logo_container.appendChild(logo);		
        /* Build a panel to hold the headers on the left */
        this.headerPanel = new Ext.Panel( {
            cls : 'wizard-steps',
            items : this.buildHeaders( this.cards ),
            defaults : { border : false },
            layout : 'table',
            layoutConfig : { columns : 1 },
            region : "west",
            width : 200,
			bodyStyle:{background:'none','padding':'20 0 0 0'},
			border:false
        } );

        var panels = [];

        var length = this.cards.length;
        for ( c = 0 ;c < length ; c++ ) panels.push(this.cards[c].panel );

        this.previousButton = new Ext.Button({
            id : 'card-prev',
            text : i18n._( '&laquo; Previous' ),
            handler : this.goPrevious.createDelegate( this )
        });

        this.nextButton = new Ext.Button({
            id : 'card-next',
            text : i18n._( 'Next &raquo;' ),
            handler : this.goNext.createDelegate( this ),
			cls:'x-btn-over',
			overCls :'x-btn-'
        });
        
        this.cardDefaults = config.cardDefaults;
        if ( this.cardDefaults == null ) this.cardDefaults = {};
        
        /* Append some necessary defaults */
        this.cardDefaults.autoHeight = true;
        this.cardDefaults.border = false;

        /* Build a card to hold the wizard */
        this.contentPanel = new Ext.Panel({
            layout : "card",
            items : panels,
            activeItem : 0,
            region : "center",
            title : "&nbsp;",
			header:false,
            defaults : this.cardDefaults, 
            bbar : [ '->', this.previousButton, this.nextButton ],
			border:false
        });

        config.layout = "border";

        config.items = [ this.headerPanel, this.contentPanel ];

        Ung.Wizard.superclass.constructor.apply(this, arguments);
    },
    
    buildHeaders : function( cards )
    {
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
                title = i18n.sprintf( i18n._( '<span class="count">%d</span> '), c  ) + title;
            }
            var id = this.getStepId( c );
            items.push({ 
                html : title,
                cls : 'step'
			});
        }
        
        return items;
    },
    
    getStepId : function( index )
    {
        return "wizard-step-" + index;
    },

    goPrevious : function()
    {
        this.goToPage( this.currentPage - 1 );
    },

    goNext : function()
    {
        this.goToPage( this.currentPage + 1 );
    },

    goToPage : function( index )
    {
        if ( index >= this.cards.length ) index = this.cards.length - 1;
        if ( index < 0 ) index = 0;

        var hasChanged = false;
        var handler = null;
		var validationPassed = true;

		if(validationPassed === true){
	        if ( this.currentPage < index ) {
				if(this.cards[this.currentPage].onValidate){
					validationPassed = this.cards[this.currentPage].onValidate();
				}			
				if(validationPassed === true){
		            /* moving forward, call the forward handler */
		            hasChanged = true;
		            handler = this.cards[this.currentPage].onNext;				
				}else{
					return false;
				}
	        } else if ( this.currentPage > index ) {
	            hasChanged = true;
	            handler = this.cards[this.currentPage].onPrevious;
	        }
		}else{
			return;
		}

        if ( this.disableNext == true ) handler = null;

        /* If the page has changed and it is defined, then call the handler */
        if ( handler ) {
            handler( this.afterChangeHandler.createDelegate( this, [ index, hasChanged ] ));
        } else {
			//where are we going if there is no handler? - karthik
            this.afterChangeHandler( index, hasChanged );
        }
    },

    /* This function must be called once the the onPrevious or onNext handler completes,
     * broken into its own function so the handler can run asynchronously */
    afterChangeHandler : function( index, hasChanged )
    {        
        this.currentPage = index;
        var card = this.cards[this.currentPage];
        handler = card.onLoad;

        //this.contentPanel.setTitle( this.getCardTitle( this.currentPage, card ));//removing title - karthik
        //this.contentPanel.setTitle( "" );
        
        if ( hasChanged && ( handler )) {
            handler( this.afterLoadHandler.createDelegate( this ));
        } else {
            this.afterLoadHandler();
        }
    },

    afterLoadHandler : function()
    {
        this.contentPanel.getLayout().setActiveItem( this.currentPage );

        /* You have to force the layout for components that need to recalculate their heights */
        this.contentPanel.doLayout();
        
        /* retrieve all of the items */
        var items = this.headerPanel.find();
        var length = items.length;
        var isComplete = true;
        for ( var c = 0 ; c < length ; c++ ) {
            var item = items[c];
            if ( c == this.currentPage ) {
                item.removeClass( "incomplete" );
                item.removeClass( "completed" );
                item.addClass( "current" );
                isComplete = false;
            } else {
                item.removeClass( "current" );
                if ( isComplete ) {
                    item.removeClass( "incomplete" );
                    item.addClass( "completed" );
                } else {
                    item.removeClass( "completed" );
                    item.addClass( "incomplete" );
                }
            }
        }
        
        if ( this.currentPage == 0 ) {
            this.previousButton.hide();
        } else {
            this.previousButton.show();
        }

        if ( this.currentPage == ( length - 1 )) {
            this.nextButton.setText( "Finish" );
        } else {
            this.nextButton.setText( "Next &raquo;" );
        }

    },

    getCardTitle : function( index, card )
    {
        var title = card.cardTitle;
        if ( title == null ) title = card.title;

        if (( index > 0 ) && ( index < ( this.cards.length - 1 ))) {
            if ( title == null ) title = i18n.sprintf( i18n._( 'Step %d'), index );
            else title = i18n.sprintf( i18n._( 'Step %d - '), index ) + title;
        }
        
        return title;
    }
});
function _validateinvalidate(items,methodname){
	var rv = true;
	for(var i=0;i<items.length;i++){
		switch(items[i].getXType()){
			case 'fieldset':
				if(!_validateinvalidate(items[i].items.items,methodname)){
					rv = false;
				}
			break;
			default:
				if(items[i].validate){
					if(!items[i][methodname].call(items[i])){
						rv = false;
					}
				}			
			break;		
		}
	}
	return rv;
}
function _invalidate(items){
	return _validateinvalidate(items,'clearInvalid')
}
function _validate(items){
	return _validateinvalidate(items,'validate')

}
