#!/usr/bin/python
# $Id: mailer.py,v 1.00 2012/05/30 11:51:31 dmorris Exp $


PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python2.5' % PREFIX
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

BODY_TEMPLATE_SIMPLE = """
The %(company)s Summary Reports for %(date_start)s - %(date_end)s are attached.
The PDF file requires Adobe Reader to view.
"""

BODY_TEMPLATE_LINK = BODY_TEMPLATE_SIMPLE + """
For more in-depth online reports, click %(link)s to view
Online %(company)s Reports.
"""

ATTACHMENT_TOO_BIG_TEMPLATE = """
The detailed reports were %sMB, which is too large to attach to this email:
the user-defined limit is currently %sMB.
"""

HTML_LINK_TEMPLATE = '<a href="%s">here</a>'

import sys

if PREFIX != '':
    sys.path.insert(0, REPORTS_PYTHON_DIR)

import datetime
import gettext
import locale
import mx
import os.path
import os
import smtplib
import reports
import reports.i18n_helper
import reports.sql_helper as sql_helper
import tempfile
import shutil

from reports.log import *
logger = getLogger(__name__)

from email import Encoders
from email.MIMEBase import MIMEBase
from email.MIMEImage import MIMEImage
from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText
from uvm.settings_reader import get_settings_item
from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_uvm_settings_item

_ = reports.i18n_helper.get_translation('untangle-vm').lgettext

def mail_reports(end_date, report_days, file, mail_reports,
                 attach_csv, attachment_size_limit):
    if attach_csv:
        zip_dir = __make_zip_file(end_date, report_days, mail_reports)
        zip_file = '%s/reports.zip' % zip_dir
    else:
        zip_file = None

    receivers, sender = __get_mail_info()
    company_name = __get_branding_info()

    url = __get_url(end_date)
    report_users = __get_report_users()

    for receiver in receivers:
        has_web_access = receiver in report_users
        mail(file, zip_file, sender, receiver, end_date, company_name, has_web_access, url, report_days, attachment_size_limit)

    if zip_file:
        shutil.rmtree(zip_dir)

def mail(file, zip_file, sender, receiver, date, company_name, has_web_access, url, report_days, attachment_size_limit):
    msgRoot = MIMEMultipart('alternative')

    h = { 'company': company_name,
          'date_start': (date - datetime.timedelta(days=report_days+1)).strftime(locale.nl_langinfo(locale.D_FMT)),
          'date_end': (date - datetime.timedelta(days=1)).strftime(locale.nl_langinfo(locale.D_FMT)) }

    if has_web_access and url:
        h.update({'link' : url})
        msg_plain = BODY_TEMPLATE_LINK % h
        h.update({'link' : HTML_LINK_TEMPLATE % (url,)})
        msg_html = BODY_TEMPLATE_LINK % h
    else:
        msg_plain = msg_html = BODY_TEMPLATE_SIMPLE % h

    if zip_file:
      attachment_size = os.path.getsize(zip_file) / float(10**6)
      attachment_too_big = attachment_size > attachment_size_limit
      if attachment_too_big:
          note = ATTACHMENT_TOO_BIG_TEMPLATE % (attachment_size,
                                                attachment_size_limit)
          msg_plain += note
          msg_html += note

    msgRoot.attach(MIMEText(msg_plain, 'plain'))
    msgRoot.attach(MIMEText("<HTML>" + msg_html + "</HTML>", 'html'))

    tmpMsgRoot = msgRoot
    msgRoot = MIMEMultipart('related')
    msgRoot.attach(tmpMsgRoot)

    if report_days == 1:
        a = "Daily"
    elif report_days == 7:
        a = "Weekly"
    else:
        a = "Monthly"
    msgRoot['Subject'] = _('New %s %s Reports Available') % (company_name, a)
    msgRoot['From'] = sender
    msgRoot['To'] = receiver

    part = MIMEBase('application', "pdf")
    part.set_payload(open(file, 'rb').read())
    Encoders.encode_base64(part)
    part.add_header('Content-Disposition', 'attachment; filename="reports-%s-%s.pdf"'
                    % (h['date_end'].replace('/', '_'),a))
    msgRoot.attach(part)

    if zip_file and not attachment_too_big:
        part = MIMEBase('application', "zip")
        part.set_payload(open(zip_file, 'rb').read())
        Encoders.encode_base64(part)
        part.add_header('Content-Disposition', 'attachment; filename="reports-%s-%s.zip"'
                        % (h['date_end'].replace('/', '_'),a))
        msgRoot.attach(part)

    smtp = smtplib.SMTP()
    smtp.connect('localhost')
    logger.info("Emailing out: from '%s' to '%s'" % (sender, receiver))
    smtp.sendmail(sender, receiver, msgRoot.as_string())
    smtp.quit()

def __get_mail_info():
    report_email = None
    receivers = []

    try:
        report_email = get_uvm_settings_item('mail','fromAddress')
        receivers_list = get_node_settings_item('untangle-node-reporting','reportingUsers')

        if receivers_list != None:
            for r in receivers_list['list']:
                receivers.append(r['emailAddress'])
    except:
        logger.warn('could not get mail info', exc_info=True)

    return (receivers, report_email)

def __get_branding_info():
    company = "Untangle"

    oemName = get_settings_item("/usr/share/untangle/conf/oem.js","oemName")
    if oemName != None:
        company = oemName

    brandco = get_node_settings_item('untangle-node-branding','companyName')
    if (brandco != None):
        company = brandco

    return company

def __get_url(date):
    url = "unknown.ip"
    publicUrlMethod = get_node_settings_item('system','publicUrlMethod')
    httpsPort = get_node_settings_item('system','httpsPort')
    if publicUrlMethod == "external":
        url = get_wan_ip() + ":" + httpsPort
    elif publicUrlMethod == "hostname":
        url = os.uname()[1] + ":" + httpsPort
    elif publicUrlMethod == "address_and_port":
        publicUrlAddress = get_node_settings_item('system','publicUrlAddress')
        publicUrlPort = get_node_settings_item('system','publicUrlPort')
        url = publicUrlAddress + ":" + publicUrlPort
        
    return 'https://%s/reports?time=%s' % ( url, date.strftime(locale.nl_langinfo(locale.D_FMT)), )

def __get_report_users():
    rv = []

    try:
        report_users = get_node_settings_item('untangle-node-reporting','reportingUsers')

        if report_users != None:
            for r in report_users['list']:
                if (r['onlineAccess']):
                    rv.append(r['emailAddress'])
    except:
        logger.warn('could not get reporting users', exc_info=True)

    return rv

def __make_zip_file(end_date, report_days, mail_reports):
    start_date = end_date - mx.DateTime.DateTimeDelta(report_days)

    tmp_dir = tempfile.mkdtemp()
    base_dir = '%s/reports' % tmp_dir
    os.mkdir(base_dir)

    for r in mail_reports:
        report_name = r.name
        report_dir = '%s/%s' % (base_dir, report_name)
        os.mkdir(report_dir)

        empty = True

        for s in r.sections:
            if isinstance(s, reports.DetailSection):
                filename = '%s/%s.csv' % (report_dir, s.name)
                s.write_csv(filename, start_date, end_date)
                empty = False

        if empty:
            f = open('%s/no-reports' % report_dir, 'w')
            try:
                f.write(_('This report has no detail data.'))
            finally:
                f.close()

    os.system("""
{ pushd %s ;
  zip -r reports.zip ./reports ;
  popd ; } > /dev/null 2>&1"""
              % tmp_dir)

    return tmp_dir
