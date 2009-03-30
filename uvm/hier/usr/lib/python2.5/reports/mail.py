# Send an HTML email with an embedded image and a plain text message for
# email clients that don't want to display the HTML.

import os

from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText
from email.MIMEImage import MIMEImage

INDIR = '/home/amread/reports-email-template'

# Define these once; use them twice!
strFrom = 'amread@untangle.com'
strTo = 'karthiknram@gmail.com'

# Create the root message and fill in the from, to, and subject headers
msgRoot = MIMEMultipart('related')
msgRoot['Subject'] = 'test message'
msgRoot['From'] = strFrom
msgRoot['To'] = strTo
msgRoot.preamble = 'This is a multi-part message in MIME format.'

# Encapsulate the plain and HTML versions of the message body in an
# 'alternative' part, so message agents can decide which they want to display.
msgAlternative = MIMEMultipart('alternative')
msgRoot.attach(msgAlternative)

msgText = MIMEText('This is the alternative plain text message.')
msgAlternative.attach(msgText)

# We reference the image in the IMG SRC attribute by the ID we give it below
msgText = MIMEText(open('%s/%s' % (INDIR, 'reports-test.htm'), 'rb').read(), 'html')
#msgText = MIMEText('<b>Some <i>HTML</i> text</b> and an image.<br><img src="cid:image1"><br>Nifty!', 'html')
msgAlternative.attach(msgText)

for f in os.listdir(INDIR):
    if f.endswith('.png'):
        # This example assumes the image is in the current directory
        fp = open('%s/%s' % (INDIR, f), 'rb')
        msgImage = MIMEImage(fp.read())
        fp.close()
        # Define the image's ID as referenced above
        msgImage.add_header('Content-ID', f)
        msgRoot.attach(msgImage)

import smtplib
smtp = smtplib.SMTP()
smtp.connect('mail.untangle.com')
smtp.sendmail(strFrom, strTo, msgRoot.as_string())
smtp.quit()
