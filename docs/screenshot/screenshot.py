#!/usr/bin/python3

import copy
import getopt
import json
import os
import re
import sys
import time
import traceback
from selenium import webdriver
import selenium.webdriver.chrome.service as ChromeDriverService
import selenium.webdriver.chrome.options as ChromeDriverOptions
from selenium.webdriver.support.ui import WebDriverWait
from selenium.common.exceptions import TimeoutException

from timeit import default_timer as timer

from PIL import Image

Debug = False

class WebBrowser:
    """ 
    Start X, the chrome driver, and chromium
    """
    service = None
    driver = None

    temp_directory = "/tmp/webbrowser"
    chrome_driver = "/usr/bin/chromedriver"
    chrome_browser = "/usr/bin/chromium"

    xvfb_command = None

    cropped_image = None

    def __init__(self, resolution="1024x768", sequence="1", screen="7", temp_directory="/tmp/webbrowser"):
        self.resolution = resolution
        self.sequence = sequence
        self.screen = screen
        self.temp_directory = temp_directory

        self.xvfb_command = "Xvfb" + " :" + self.sequence + " -screen " + self.screen + " " + resolution + "x8"
        # print(self.xvfb_command)

        os.system("/usr/bin/pkill -f \"" + self.xvfb_command + "\"")
        os.system("nohup " + self.xvfb_command + " >/dev/null 2>&1 &")
        try: os.makedirs(self.temp_directory)
        except: pass

        self.service = ChromeDriverService.Service( 
            executable_path=self.chrome_driver, 
            service_args=[
                "--log-path=" + self.temp_directory + "/log.txt", 
                "--verbose"
            ], 
            env={"DISPLAY": ":" + self.sequence + "." + self.screen} 
        )
        self.service.start()

#                    "--ash-host-window-bounds=" + resolution,
        self.capabilities = {
            'chrome.binary': self.chrome_browser,
            'chromeOptions': {
                "args" : [
                    "--no-sandbox",
                    "--kiosk",
                    "--user-data-dir=" + self.temp_directory
                ]
            }
        }
        try:
            self.driver = webdriver.Remote( self.service.service_url, self.capabilities)
        except Exception as e:
            print("Unable to initialize webdriver.Remote")
            print(e)

        r = resolution.split("x")
        self.driver.set_window_size( int(r[0]), int(r[1]) )

    def __del__(self):
        """
        Shut down chromium, chrome driver, and X.
        """
        self.driver.quit()
        self.service.stop()
        os.system("/usr/bin/pkill -f \"" + self.xvfb_command + "\"")
        time.sleep(5)

    def auth_cookie_exists(self):
        """
        Check that authentication cookie exists
        """
        for cookie in self.driver.get_cookies():
            if "auth-" in cookie["name"]:
                return cookie
        return None

    def authenticate(self, url, username, password):
        """
        Authenticate
        """
        self.go(url)
        usernameInput = self.driver.find_element_by_id("username")
        passwordInput = self.driver.find_element_by_id("password")
        button = self.driver.find_element_by_tag_name("button")

        usernameInput.clear()
        usernameInput.send_keys(username)
        passwordInput.clear()
        passwordInput.send_keys(password)
        button.click()

        try:
            WebDriverWait(self.driver,10).until(
                lambda x: self.auth_cookie_exists()
            )
        except TimeoutException:
            print("Unable to obtain authentication cookie")

    def go(self, url):
        """
        Open the specified URL
        """
        if Debug:
            print("go: url=" + url)
        self.driver.get(url)

    def wait_for_load(self):
        """
        Wait for everything to be available:
        -   Progressbars hidden
        -   Image loads completed
        """
        last_progressbar_count = 0
        max_sleep = 20
        while max_sleep > 0:
            if self.errors_detected() is True:
                print("Found error!")
                return False

            try:
                progressbars = self.driver.find_elements_by_css_selector("div[id^=loadmask][role=progressbar]")
            except Exception:
                pass
                max_sleep -= 1
                continue

            if len(progressbars) == 0:
                # Wait until we see at least one progress bar.
                time.sleep(.5)
                max_sleep -= 1
                continue

            if len(progressbars) != last_progressbar_count:
                last_progressbar_count = len(progressbars)
                max_sleep -= 1
                continue

            all_none_displayed = True
            for progressbar in progressbars:
                try:
                    style = progressbar.get_attribute("style")
                    if not "display: none;" in style:
                        all_none_displayed = False
                except Exception:
                    if Debug:
                        print("Problem getting progressbar attribute")
                    all_none_displayed = False
                    pass

            all_images_complete=True
            images = self.driver.find_elements_by_css_selector("img")
            for image in images:
                try:
                    if image.get_attribute("complete") is None:
                        if Debug:
                            print("Waiting for image " + image_get_attribute("name"))
                        all_images_complete = False
                except Exception:
                    if Debug:
                        print("Problem getting image attribute")
                    all_images_complete = False
                    pass

            if Debug:
                print("")
                print("all_none_displayed=" + str(all_none_displayed) + ", all_images_complete=" + str(all_images_complete))

            if all_none_displayed is True and all_images_complete:
                return True

            time.sleep(.5)
            max_sleep -= 1
        return False

    def errors_detected(self):
        for entry in self.driver.get_log("browser"):
            if entry['level'] == 'SEVERE':
                if entry['source'] == 'network':
                    continue
                if entry['source'] == 'console-api':
                    print(entry)
                    return True
                print(entry)
        return False

    def crop_by_dimension(self, x, y, height, width):
        """
        Crop an image by dimensions.
        If height and/or width are "auto", use the image's resolution
        """
        self.driver.get_screenshot_as_file(self.temp_directory + "/temp_crop.png")
        img = Image.open(self.temp_directory + "/temp_crop.png")

        if height == "auto":
            height = img.size[1]

        if width == "auto":
            width = img.size[0]

        self.cropped_image = img.crop( ( x, y, width, height ) )
        return True

    def crop_by_elements(self, searches, matches):
        """
        Crop by:
        -   Searching for elements and pulling thsoe from screen shot.
        -   Pasting together a new image from the cropped images.
        """
        url = screen["url"]
        replacements = {}
        if "path" in screen:
            replacements["%path%"] = screen["path"]
        else:
            path = url.replace("-", "");
            replacements["%path%"] = url[url.find('#') + 1:].replace("_", "").replace("/", "-").replace("apps-1","app")

        images = {}
        for index, value in enumerate(matches):
            images[index] = None

            for key in list(value.keys()):
                if key == "height":
                    continue

                for rkey in list(replacements.keys()):
                    if rkey in value[key]:
                        value[key] = value[key].replace(rkey, replacements[rkey]) 

                if type(value[key]) in [str, str]:
                    value[key] = re.compile( r''+value[key].replace("\\\\", "\\"), re.UNICODE )

            for rkey in list(replacements.keys()):
                if rkey in searches[index]:
                        searches[index] = searches[index].replace(rkey, replacements[rkey]) 

        max_width = 0
        max_height = 0

        self.driver.get_screenshot_as_file(self.temp_directory + "/temp_crop.png")
        img = Image.open(self.temp_directory + "/temp_crop.png")

        for search in searches:
            for element in self.driver.find_elements_by_css_selector(search):
                try:
                    width = element.size['width']
                    height = element.size['height']
                    if width == 0 and height == 0:
                        continue

                    for index, value in enumerate(matches):
                        if not images[index] is None:
                            # Already found it
                            continue

                        match = True
                        for key in list(value.keys()):
                            if key[0] == "!":
                                if value[key].search( element.get_attribute(key[1:]) ) is not None:
                                    match = False
                                    break
                            elif key == "height":
                                if height != value[key]:
                                    match = False
                                    break
                            else:
                                if value[key].search( element.get_attribute(key) ) is None:
                                    match = False
                                    break

                        if match is True:
                            if Debug:
                                print("matched element=")
                                for key in list(value.keys()):
                                    if key[0] == "!":
                                        print(key[1:] + "=" + str(element.get_attribute(key[1:])))
                                    else:
                                        print(key + "=" + str(element.get_attribute(key)))
                            x = int( element.location['x'] )
                            y = int( element.location['y'] )
                            # print( x, y, x + width, y + height )
                            images[index] = img.crop( ( x, y, x + width, y + height ) )
                            # save piece for debugging....
                            # images[index].save(self.temp_directory + "/cropped" + str(index) + ".png")
                            if width > max_width:
                                max_width = width

                            max_height = max_height + height
                            break

                except Exception:
                    pass


        all_cropped_exists = True
        for id in images:
            if images[id] is None:
                print("Cannot create - missing for " + matches[id]["id"].pattern)
                all_cropped_exists = False
                continue
        if all_cropped_exists is False:
            return False

        if Debug:
            print("images=")
            print(images)
        # print(max_width)
        # print(max_height)

        self.cropped_image = Image.new("RGB", (max_width, max_height), "white" )
        x = 0
        y = 0;
        for id in images:
            if images[id] is None:
                print("Cannot create - missing for " + str(id))
                continue
            # print(id)
            # print(images[id].size)
            # print( x, y, x + images[id].size[0], y + images[id].size[1])
            # print("pasting " + str(id))
            self.cropped_image.paste(images[id], ( x, y, x + images[id].size[0], y + images[id].size[1]) )
            y = y + images[id].size[1]

        del img

        return True

    def build_file_name_path(self, name):
        """
        Create a filename based on the specified name and resolution
        """
        return self.temp_directory + "/" + self.resolution + "_" + name + ".png"

    def screenshot(self, name):
        """
        Create named image from current screenshot or cropped image if it exists.
        """
        if self.cropped_image != None:
            self.cropped_image.save( self.build_file_name_path(name) )
            self.cropped_image = None
        else:
            self.driver.get_screenshot_as_file( self.build_file_name_path(name) )

def usage():
    """
    Show usage
    """
    print("usage")

def main(argv):
    global Debug
    want_screen_name = None
    want_resolution = None
    tmp_dir = "/tmp/webbrowser"
    try:
        opts, args = getopt.getopt(argv, "hdr:s:t:", ["help", "debug", "resolution=", "screen=", "tmpdir="] )
    except getopt.GetoptError:
        usage()
        sys.exit(2)

    for opt, arg in opts:
        if opt in ( "-h", "--help"):
            usage()
            sys.exit()
        if opt in ( "-d", "--debug"):
            Debug = True
        if opt in ( "-s", "--screen"):
            want_screen_name = arg
        if opt in ( "-r", "--resolution"):
            want_resolution = arg
        if opt in ( "-t", "--tmpdir"):
            tmp_dir = arg

    settings_file = open( "screenshot.json" )
    settings = json.load( settings_file)
    settings_file.close()

    if want_resolution is not None:
        settings["resolutions"].append(want_resolution)

    total_screens = 0
    total_screens_failed = 0
    total_screens_created = 0
    total_time = 0
    for resolution in settings["resolutions"]:
        if ( want_resolution is not None ) and ( resolution != want_resolution ):
            if Debug is True:
                print("Skipping resolution: "+resolution)
            continue
        print("Using resolution: "+resolution)

        web_browser = WebBrowser(resolution=resolution, temp_directory=tmp_dir)

        base_url = settings["authentication"]["url"]
        if Debug:
            print("base_url=" + base_url)

        if "user" in settings["authentication"]:
            web_browser.authenticate(url=base_url, username=settings["authentication"]["user"], password=settings["authentication"]["password"])

        last_screen_url = None
        for screen in settings["screens"]:
            screen['name'] = screen['url'].replace("/admin/index.do#","")
            screen['name'] = screen['name'].replace("/","_")
            screen['name'] = screen['name'].replace("(","_")
            screen['name'] = screen['name'].replace(")","_")
            screen['name'] = screen['name'].replace("=","_")
            screen['name'] = screen['name'].replace("?","_")
            screen['name'] = screen['name'].replace("&","_")
            screen['name'] = screen['name'].replace("_1_","_")

            total_screens += 1
            if want_screen_name is not None and want_screen_name not in screen["url"]:
                # if Debug is True:
                #     print("Skipping screen: " + screen["name"] )
                continue

            fname = web_browser.build_file_name_path(screen['name'])
            if os.path.exists(fname):
                print("File exists: " + fname + " Skipping ...")
                continue

            start = timer()
            print("Capturing: " + base_url + screen['url'] + " -> " + web_browser.build_file_name_path(screen["name"]),)
            sys.stdout.flush()

            web_browser.go(base_url + screen["url"])
            if web_browser.wait_for_load() is False:
                print("Could not load")
                total_screens_failed += 1
                del web_browser
                web_browser = WebBrowser(resolution=resolution, temp_directory=tmp_dir)
                if "user" in settings["authentication"]:
                    web_browser.authenticate(url=base_url, username=settings["authentication"]["user"], password=settings["authentication"]["password"])
                continue

            if web_browser.errors_detected() is True:
                print("Errors on url")
                total_screens_failed += 1
                del web_browser
                web_browser = WebBrowser(resolution=resolution, temp_directory=tmp_dir)
                if "user" in settings["authentication"]:
                    web_browser.authenticate(url=base_url, username=settings["authentication"]["user"], password=settings["authentication"]["password"])
                continue

            if last_screen_url is None:
                time.sleep(settings["default_sleep_seconds"])
            else:
                last_slash_pos=last_screen_url.rfind('/')
                last_pieces=last_screen_url.split('/')
                current_slash_pos=screen["url"].rfind('/')
                current_pieces=screen["url"].split('/')

                url_base_match=last_slash_pos > -1 and current_slash_pos > -1 and last_screen_url[0:last_slash_pos] == screen["url"][0:current_slash_pos]
                reports = ( len(last_pieces) == len(current_pieces) ) and ( current_pieces[2] == "index.do#reports" ) and last_pieces[2] == current_pieces[2]
                subtab = ( len(last_pieces) == len(current_pieces) ) and len(current_pieces) > 5

                if url_base_match and ( reports or subtab ):
                    time.sleep(settings["default_sleep_seconds"])

            if screen.get('extra_delay') != None:
                time.sleep(screen.get('extra_delay'))

            success = True
            if "type" in screen:
                for operation in settings["types"][screen["type"]]:
                    if operation["action"] == "crop_by_elements":
                        success = web_browser.crop( screen, copy.deepcopy( operation["searches"] ), copy.deepcopy( operation["matches"] ) )
                    elif operation["action"] == "crop_by_dimension":
                        success = web_browser.crop_by_dimension( operation["x"], operation["y"], operation["height"], operation["width"] )

                    if success is False:
                        break

            if success is True:
                web_browser.screenshot(screen["name"])
                total_screens_created += 1
            else:
                total_screens_failed += 1
                print("Cannot capture screen",)

            last_screen_url= screen["url"]
            total_time += timer() - start
            print(str(round(timer() - start,2)) + " (" + str(round(total_time,2)) + ")")

        del web_browser

    print("Total screens available: %d, failed: %d, created: %d, %ds" % ( total_screens, total_screens_failed, total_screens_created, total_time))

if __name__ == "__main__":
    main( sys.argv[1:] )
