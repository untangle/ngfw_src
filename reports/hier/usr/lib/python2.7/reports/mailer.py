#!/usr/bin/python
# $Id$

import sys

PREFIX = '@PREFIX@'
REPORTS_PYTHON_DIR = '%s/usr/lib/python%d.%d' % (PREFIX, sys.version_info[0], sys.version_info[1])
REPORTS_OUTPUT_BASE = '%s/usr/share/untangle/web/reports' % PREFIX
NODE_MODULE_DIR = '%s/reports/node' % REPORTS_PYTHON_DIR

BODY_TEMPLATE_PDF = u"""
The %(company)s PDF Summary Reports for %(date_start)s - %(date_end)s are attached.<br/>
<br/>
"""

BODY_TEMPLATE_LINK = u"""
Click %(link)s to view %(company)s Reports.<br/>
<br/>
"""

ATTACHMENT_TOO_BIG_TEMPLATE = u"""
The detailed reports were %sMB, which is too large to attach to this email:<br/>
the configured limit is currently %sMB.<br/>
<br/>
"""

HTML_LINK_TEMPLATE = u'<a href="%s">here</a>'

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
import reports.engine
import reports.sql_helper as sql_helper
import tempfile
import shutil
import uvm.i18n_helper

from reports.log import *
logger = getLogger(__name__)

from cStringIO import StringIO
from email import Charset, Encoders
from email.generator import Generator
from email.header import Header
from email.MIMEBase import MIMEBase
from email.MIMEImage import MIMEImage
from email.MIMEMultipart import MIMEMultipart
from email.MIMEText import MIMEText
from uvm.settings_reader import get_settings_item
from uvm.settings_reader import get_node_settings_item
from uvm.settings_reader import get_uvm_settings_item

_ = uvm.i18n_helper.get_translation('untangle').lgettext

def mail_reports(end_date, report_days, pdf_file, mail_reports, attach_csv, attachment_size_limit):
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
        try:
            has_web_access = receiver in report_users
            mail(pdf_file, zip_file, sender, receiver, end_date, company_name, has_web_access, url, report_days, attachment_size_limit)
        except:
            logger.warn("Failed to send email summary to '%s'" % (receiver), exc_info=True)

    if zip_file:
        shutil.rmtree(zip_dir)

def mail(pdf_file, zip_file, sender, receiver, date, company_name, has_web_access, url, report_days, attachment_size_limit):
    Charset.add_charset('utf-8', Charset.QP, Charset.QP, 'utf-8')
    msgRoot = MIMEMultipart('alternative')

    if pdf_file == None and zip_file == None and not has_web_access:
        logger.warn("Email has no content (no PDF, no ZIP, no link). Not sending...")
        return

    h = { 'company': company_name,
          'date_start': (date - datetime.timedelta(days=report_days+1)).strftime(locale.nl_langinfo(locale.D_FMT)),
          'date_end': (date - datetime.timedelta(days=1)).strftime(locale.nl_langinfo(locale.D_FMT)) }

    # build both the plain-text and html version of the text
    msg_plain = ""
    msg_html = ""
    if has_web_access and url:
        h.update({'link' : url})
        h.update({'link' : HTML_LINK_TEMPLATE % (url,)})
        msg_plain += BODY_TEMPLATE_LINK.replace("<br/>","\n") % h
        msg_html += BODY_TEMPLATE_LINK % h
    if pdf_file:
        msg_plain += BODY_TEMPLATE_PDF.replace("<br/>","\n") % h
        msg_html += BODY_TEMPLATE_PDF % h
    if zip_file:
      attachment_size = os.path.getsize(zip_file) / float(10**6)
      attachment_too_big = attachment_size > attachment_size_limit
      if attachment_too_big:
          msg_plain += ATTACHMENT_TOO_BIG_TEMPLATE.replace("<br/>","\n") % (attachment_size, attachment_size_limit)
          msg_html += ATTACHMENT_TOO_BIG_TEMPLATE % (attachment_size, attachment_size_limit)

    msgRoot.attach(MIMEText(msg_plain.encode('utf-8'), 'plain', 'utf-8'))
    msgRoot.attach(MIMEText((u"<HTML>" + msg_html + u"</HTML>").encode('utf-8'), 'html', 'utf-8'))

    tmpMsgRoot = msgRoot
    msgRoot = MIMEMultipart('related')
    msgRoot.attach(tmpMsgRoot)

    if report_days == 1:
        length_name = "Daily"
    elif report_days == 7:
        length_name = "Weekly"
    else:
        length_name = "Monthly"

    machine_name = ""
    try:
        machine_name = str(os.uname()[1])
    except:
        pass

    subject = ('%s %s' % (company_name, length_name)) + ' ' + _('Report Summary') + ' ' + ('[%s]' % machine_name)
    msgRoot['Subject'] = Header(subject, 'ascii') # falls back to utf-8 if ascii fails
    msgRoot['From'] = "%s" % Header(sender, 'ascii') # falls back to utf-8 if ascii fails
    msgRoot['To'] = "%s" % Header(receiver, 'ascii') # falls back to utf-8 if ascii fails

    if pdf_file:
        part = MIMEBase('application', "pdf")
        part.set_payload(open(pdf_file, 'rb').read())
        Encoders.encode_base64(part)
        part.add_header('Content-Disposition', 'attachment; filename="reports-%s-%s.pdf"'% (h['date_end'].replace('/', '_'),length_name))
        msgRoot.attach(part)

    if zip_file and not attachment_too_big:
        part = MIMEBase('application', "zip")
        part.set_payload(open(zip_file, 'rb').read())
        Encoders.encode_base64(part)
        part.add_header('Content-Disposition', 'attachment; filename="reports-%s-%s.zip"' % (h['date_end'].replace('/', '_'),length_name))
        msgRoot.attach(part)

    smtp = smtplib.SMTP()
    smtp.connect('localhost')
    logger.info("Emailing out: from '%s' to '%s'" % (sender, receiver))
    str_io = StringIO()
    g = Generator(str_io, False)
    g.flatten(msgRoot)
    smtp.sendmail(sender, receiver, str_io.getvalue())
    smtp.quit()

def __get_mail_info():
    report_email = None
    receivers = []

    try:
        report_email = get_uvm_settings_item('mail','fromAddress')
        receivers_list = get_node_settings_item('untangle-node-reports','reportingUsers')

        if receivers_list != None:
            for r in receivers_list['list']:
                if r['emailSummaries']:
                    receivers.append(r['emailAddress'])
    except:
        logger.warn('could not get mail info', exc_info=True)

    return (receivers, report_email)

def __get_branding_info():
    company = "Untangle"

    oemName = get_settings_item("/usr/share/untangle/conf/oem.js","oemName")
    if oemName != None:
        company = oemName

    brandco = get_node_settings_item('untangle-node-branding-manager','companyName')
    if (brandco != None):
        company = brandco

    return company

def __get_url(date):
    url = "unknown.ip"
    publicUrlMethod = get_uvm_settings_item('system','publicUrlMethod')
    httpsPort = get_uvm_settings_item('network','httpsPort')
    try:
        if publicUrlMethod == "external":
            url = reports.engine.get_wan_ip() + ":" + str(httpsPort)
        elif publicUrlMethod == "hostname":
            hostname = get_uvm_settings_item('network','hostName')
            domain = get_uvm_settings_item('network','domainName')
            url = hostname + "." + domain + ":" + str(httpsPort)
        elif publicUrlMethod == "address_and_port":
            publicUrlAddress = get_uvm_settings_item('system','publicUrlAddress')
            publicUrlPort = get_uvm_settings_item('system','publicUrlPort')
            url = str(publicUrlAddress) + ":" + str(publicUrlPort)
    except:
        logger.warn('Could not calculate reports URL', exc_info=True)

    return 'https://%s/reports?time=%s' % ( url, date.strftime("%Y-%m-%d") )

def __get_report_users():
    rv = []

    try:
        report_users = get_node_settings_item('untangle-node-reports','reportingUsers')

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

    logger.info("Creating zip file: %s/reports.zip" % tmp_dir)

    os.system(""" cd %s ; zip -r reports.zip ./reports """ % tmp_dir)

    return tmp_dir
