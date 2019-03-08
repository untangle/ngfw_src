Ext.define('Ung.apps.reports.SendFixedReportController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-reports-sendfixedreport',

    control: {
        '#': {
            afterrender: 'afterrender'
        }
    },

    afterrender: function(view){
        var vm = this.getViewModel();

        var record = view.record;
        var dbRetention = view.up('[itemId=appCard]').getViewModel().get('settings.dbRetention');
        var intervalWeekStart = record.get('intervalWeekStart');

        var interval = record.get('interval');

        var showStopDate = interval <= 2;
        vm.set('disableStopDate', !showStopDate);

        var maxDate = new Date(Util.getMilliseconds());
        maxDate.setHours(0,0,0,0);
        var maxYear = maxDate.getFullYear();
        var maxMonth = maxDate.getMonth();
        var maxDateOfWeek = maxDate.getDay();

        var minDate = new Date(Util.getMilliseconds() - (86400000 * dbRetention) );
        minDate.setHours(0,0,0,0);
        var minYear = minDate.getFullYear();
        var minMonth = minDate.getMonth();
        var minDateOfWeek = minDate.getDay();

        var startDateValue = Util.serverToClientDate(maxDate);
        var stopDateValue = Util.serverToClientDate(maxDate);

        var startDateDisabledDates = [];
        var stopDateDisabledDates = [];

        var disabledDate = null;
        var disabledMonth = null;

        switch(interval){
            case 2419200:
            case 2:
                minDate = new Date(minYear, minMonth, 1);
                if(interval == 2419200){
                    startDateValue = new Date(maxYear, maxMonth > 0 ? maxMonth - 1 : 11, 1);
                    maxDate = new Date(startDateValue.getTime());
                }else{
                    startDateValue = new Date(maxYear, maxMonth, 1);
                }
                vm.set('minDate', minDate);
                vm.set('maxDate', maxDate);

                startDateDisabledDates = this.getMonthDisabledDates();

                if(interval == 2){
                    stopDateDisabledDates = this.getMonthDisabledDates(startDateValue);
                }
                break;

            case 604800:
            case 1:
                minDate.setHours( -24 * ( minDateOfWeek  - intervalWeekStart));

                startDateValue = new Date(maxDate.getTime());
                startDateValue.setHours( -24 * ( maxDateOfWeek  - intervalWeekStart));
                
                if(interval == 604800 || maxDateOfWeek < intervalWeekStart){
                    startDateValue.setHours(-24 * 7);
                    stopDateValue = new Date( startDateValue.getTime());
                    stopDateValue.setHours(24 * 7);
                }else{
                    stopDateValue = new Date(maxDate.getTime());
                }

                if(interval == 604800){
                    maxDate = new Date( startDateValue.getTime() );
                }
                vm.set('minDate', minDate);
                vm.set('maxDate', maxDate);

                startDateDisabledDates = this.getWeekDisabledDates();
                if(interval == 1){
                    stopDateDisabledDates = this.getWeekDisabledDates(startDateValue);
                }
                break;
            default:
                vm.set('minDate', minDate);
                vm.set('maxDate', maxDate);
        }
        var startDate = view.down('[itemId=startDate]');
        var stopDate = view.down('[itemId=stopDate]');

        if(startDateDisabledDates.length){
            startDate.setDisabledDates(startDateDisabledDates);
        }
        startDate.setValue(startDateValue);
        startDate.todayCls = '';

        if(stopDateDisabledDates.length){
            stopDate.setDisabledDates(stopDateDisabledDates);
        }
        stopDate.setValue(stopDateValue);
        stopDate.todayCls = '';

    },

    getMonthDisabledDates: function(startDay){
        var vm = this.getViewModel();
        var minDate = vm.get('minDate');
        var maxDate = vm.get('maxDate');

        var disabledDates = [];
        var disabledDate = new Date(minDate.getTime());
        var disabledMonth = disabledDate.getMonth();
        while(disabledDate < maxDate){
            if( typeof(startDay) !== 'undefined' ){
                if( startDay.getTime() != disabledDate.getTime()){
                    disabledDates.push( (disabledDate.getMonth() + 1) + '/[0-3][0-9]/' + disabledDate.getFullYear() );
                }else{
                    disabledDates.push( (disabledDate.getMonth() + 1) + '/01/' + disabledDate.getFullYear() );                    
                }
            }else{
                disabledDates.push( (disabledDate.getMonth() + 1) + '/[1-3][0-9]/' + disabledDate.getFullYear() );
                disabledDates.push( (disabledDate.getMonth() + 1) + '/[0][2-9]/' + disabledDate.getFullYear() );
            }
            disabledMonth++;
            if(disabledMonth == 12){
                disabledMonth = 0;
                disabledDate.setYear(disabledDate.getYear());
            }
            disabledDate.setMonth(disabledMonth);
        }

        return disabledDates;
    },

    getWeekDisabledDates: function(startDay){
        var vm = this.getViewModel();
        var minDate = vm.get('minDate');
        var maxDate = vm.get('maxDate');

        var disabledDates = [];
        var disabledDate = new Date(minDate.getTime());
        var i;
        var day;
        while(disabledDate <= maxDate){
            if( typeof(startDay) !== 'undefined' ){
                if( ( disabledDate.getTime() < ( startDay.getTime() + 86400000 ) ||
                    disabledDate.getTime() > ( startDay.getTime() + ( 86400000 * 7 ) ) ) ){
                    day = disabledDate.getDate();
                    disabledDates.push( (disabledDate.getMonth() + 1) + '/' + ( day < 10 ? '0' : '')  + day  + '/' + disabledDate.getFullYear() );
                }
            }else{
                for( i = 0; i < 6; i++){
                    disabledDate.setHours(24);
                    day = disabledDate.getDate();
                    disabledDates.push( (disabledDate.getMonth() + 1) + '/' + ( day < 10 ? '0' : '')  + day  + '/' + disabledDate.getFullYear() );
                }
            }
            disabledDate.setHours(24);
        }
        return disabledDates;
    },

    datepickerselect: function(el, startDate){
        var vm = el.up('window').getViewModel();
        var record = el.up('window').record;
        var interval = record.get('interval');
        var intervalWeekStart = record.get('intervalWeekStart');
        var stopDate = el.up('window').down('[itemId=stopDate]');
        var startDateValue = el.up('window').down('[itemId=startDate]').getValue();
        var maxDate = vm.get('maxDate');

        var stopDateValue = startDateValue;
        if(interval == 1){
            stopDate.setDisabledDates(this.getWeekDisabledDates(startDate));
            var maxDateOfWeek = maxDate.getDay();
            if( ( startDateValue.getTime() + ( 86400000 * 7) < maxDate.getTime() ) ||
                ( maxDateOfWeek < intervalWeekStart) ){
                stopDateValue = new Date( startDateValue.getTime());
                stopDateValue.setHours(24 * 7);
            }else{
                stopDateValue = new Date(maxDate.getTime());
            }
        }else if(interval === 2){
            stopDate.setDisabledDates(this.getMonthDisabledDates(startDate));
            var month = stopDate.getMonth();
            stopDateValue.setMonth( month == 0 ? 11 : month - 1);
            stopDateValue.setDay(-24);
        }
        stopDate.setValue(stopDateValue);
    },

    closeWindow: function (button) {
        button.up('window').close();
    },

    send: function (button) {
        var startDate = button.up('window').down('[itemId=startDate]').getValue().getTime(),
            stopDate = button.up('window').down('[itemId=stopDate]').getValue().getTime(),
            templateId = button.up('window').record.get('templateId');

        var dialog = button.up('window');
        dialog.setLoading(true);
        var app = button.up('window').up('[itemId=appCard]');
        Ext.Deferred.sequence([
            Rpc.asyncPromise(app.appManager, 'runFixedReport', templateId, startDate, stopDate),
            Rpc.asyncPromise(app.appManager, 'getFixedReportQueueSize')
        ]).then(function(result){
            if(Util.isDestroyed(button, dialog)){
                return;
            }
            app.getViewModel().set('reportQueueSize', result[1]);
            button.up('window').up('[itemId=email-templates]').getController().updateReportQueueSize(button.up('window').up('[itemId=email-templates]').down('[itemId=reportQueueSize]'));
            button.up('window').close();
        }, function(ex) {
            Ext.MessageBox.close();
            Util.handleException(ex);
            if(Util.isDestroyed(button)){
                return;
            }
            button.up('window').close();
        });
    }
});
