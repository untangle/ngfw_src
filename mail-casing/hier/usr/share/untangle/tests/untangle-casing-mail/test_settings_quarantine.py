from untangle.uvm_setup import UvmSetup

"""
Still to do:
  popTimeout, popEnabled
  smtpTimeout, smtpEnabled
  imapTimeout, imapEnabled
  User Quarantines ?
  Quarantinable Addressesa ?
  Quarantine Forwards ?
  ?
"""

class TestMailSettings(UvmSetup):
    DAY_IN_MILLISECONDS = 1440*1000*60
    @classmethod
    def setup_class( cls ):
        UvmSetup.setup_class.im_func( cls )
	
        node_manager = UvmSetup.node_manager.im_func( cls )
        
        cls.tid = cls.install_node.im_func( cls, "untangle-casing-mail" )
        
	## Stop the node so the settings don't take effect.
        try:
            node_manager.stop( cls.tid )
        except:
            ## No biggie if this fails
            pass
	
        cls.node_context = node_manager.nodeContext( cls.tid )
        cls.node = cls.node_context.node()

        cls.original_settings = cls.node.getMailNodeSettings()

    @classmethod
    def teardown_class( cls ):
        cls.node.setMailNodeSettings( cls.original_settings )

    # maxMailIntern -- Maximum Holding Time (max 36 days) 
    
    def test_set_max_holding_time(self):
	" sets maxMailIntern 1 day and check to see if is really set "
        settings = self.node.getMailNodeSettings()
	settings['quarantineSettings']['maxMailIntern'] = self.DAY_IN_MILLISECONDS
	self.node.setMailNodeSettings(settings)
	assert self.DAY_IN_MILLISECONDS == self.node.getMailNodeSettings()['quarantineSettings']['maxMailIntern'], \
		"Failed attempting to set Max Holding Time (maxMailIntern) to 1 day"

    """ -- comment out since plugins don't seem to work at the moment -->
    @py.test.mark.xfail
    def test_over_set_max_holding_time(self):
	print "This test fails only if can set maxMailIntern to a value greater than 36 days"
	bad_max = 37*self.DAY_IN_MILLISECONDS
        settings = self.node.getMailNodeSettings()
	settings['quarantineSettings']['maxMailIntern'] = bad_max
	self.node.setMailNodeSettings(settings)
	assert bad_max == self.node.getMailNodeSettings()['quarantineSettings']['maxMailIntern']
    """
    # sendDailyDigests -- Send Daily Quarantine Digest Emails (True|False)

    def check_set_send_daily_digests(self, do_send):
        " test setting sendDailyDigests - expect do_send as True | False "
        settings = self.node.getMailNodeSettings()
	settings['quarantineSettings']['sendDailyDigests'] = do_send
	self.node.setMailNodeSettings(settings)
	assert do_send == self.node.getMailNodeSettings()['quarantineSettings']['sendDailyDigests']
	
    def test_set_send_daily_digests(self):
	" generator sets sendDailyDigests to True / False "
	for do_send in (True, False):
	    yield self.check_set_send_daily_digests, do_send

    # digestHourOfDay, digestMinuteOfDay -- Quarantine Digest Sending Time (00:00 - 23:59)

    def check_set_sending_time(self, send_time):
        " test setting both digestHourOfDay and digestMinuteOfDay -- expect send_time in 'HH:MM' format "
	settings = self.node.getMailNodeSettings()
	send_hour, send_minute = send_time.split(':')
	settings['quarantineSettings']['digestHourOfDay'] = int(send_hour)
	settings['quarantineSettings']['digestMinuteOfDay'] = int(send_minute)
	self.node.setMailNodeSettings(settings)
	quarantine_settings = self.node.getMailNodeSettings()['quarantineSettings']
	assert int(send_hour) == quarantine_settings['digestHourOfDay']
	assert int(send_minute) == quarantine_settings['digestMinuteOfDay']
	
    def test_set_sending_time(self):
	for send_time in ('00:00', '12:01', '23:59'):
	   yield self.check_set_sending_time, send_time
   	 
    """ -- comment -- use for debugging, remove when done writing scripts -->
    def test_get_and_set_settings( self ):
        current_settings = self.node.getMailNodeSettings()
	self.node.setMailNodeSettings(current_settings)
	assert current_settings == self.node.getMailNodeSettings()
	print "settings: %s" % str(current_settings)
	assert False
    """
