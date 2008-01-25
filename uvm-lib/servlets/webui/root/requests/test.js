{
 'data':[ 
	  {'category':'Streaming Video','protocol':'RTSP tunneled within HTTP','block':false,'log':false,'description':'RTSP tunneled within HTTP','signature':'^(get[\x09-\x0d -~]* Accept: application/x-rtsp-tunnelled|http/(0\.9|1\.0|1\.1) [1-5][0-9][0-9] [\x09-\x0d -~]*a=control:rtsp://)'},
	  {'category':'Streaming Video','protocol':'Quicktime HTTP','block':false,'log':false,'description':'Quicktime HTTP','signature':'user-agent: quicktime \(qtver=[0-9].[0-9].[0-9];os=[\x09-\x0d -~]+\)\x0d\x0a'}
	]
}
	