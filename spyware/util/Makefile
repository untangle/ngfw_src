TARGET_DIR=../impl/resources/com/untangle/tran/spyware

all: clean build
	cp ./urlblacklist.txt $(TARGET_DIR)

	cp ./cookie-diff-* $(TARGET_DIR) || true
	cp $(shell ls cookie-init-* | sort -n -t- -k2 -r | head -n1) $(TARGET_DIR)/cookie.txt

	cp ./subnet-diff-* $(TARGET_DIR) || true
	cp $(shell ls subnet-init-* | sort -n -t- -k2 -r | head -n1) $(TARGET_DIR)/subnet.txt

build: url cookie subnet

clean:
	rm -f ./cookie.txt
	rm -f ./blocklist.reg
	rm -f ./urlblacklist.txt
	rm -f ./subnet.txt
	rm -f ./files/*

cookie: cookie-diff-$(shell expr `ls cookie-init-* | cut -d- -f3 | sort -r -n | head -n1` + 1)
subnet: subnet-diff-$(shell expr `ls subnet-init-* | cut -d- -f3 | sort -r -n | head -n1` + 1)

url:
	mkdir -p ./files
	@( cd ./files ; wget http://www.geocities.com/spywarekilla/hosts.zip && unzip -a -o hosts.zip )
	@( cd ./files ; wget http://www.mvps.org/winhelp2002/hosts.txt )
	@( cd ./files ; wget http://www.spywarewarrior.com/uiuc/res/agnis-sites.zip && unzip -a -o agnis-sites.zip ag-sites-full.txt )
	@cat ./files/hosts > ./files/hostslist
	@cat ./files/hosts.txt >> ./files/hostslist
	@cat ./files/hostslist | grep -v "^#.*" | grep "^127.0.0.1" | awk '{print $$2}' | sed -e 's/\#.*//' -e 's///' > ./files/urlblacklist
	@cat ./files/ag-sites-full.txt | egrep -v '^[0-9]{1,3}\.[0-9]{1,3}\.' | grep -v "^\." >> ./files/urlblacklist
	@cat ./files/urlblacklist | grep -v '^$$' | sed -e "/[\x80-\xff]/d" | sort | uniq > ./files/urlblacklist.txt
	@cat ./files/urlblacklist.txt | grep -vwf domains.clean > urlblacklist.txt

cookie-init-%: domains.clean
	mkdir -p ./files
	@( cd ./files ; wget http://www.geocities.com/spywarekilla/autoinstaller.zip  && unzip autoinstaller.zip)
	@cat ./files/Spyware\ blacklist\ auto-installer.reg | tr -d '\377' | tr -d '\376' | tr -d '\000' >| ./files/cookies
	@cat ./files/cookies | grep HKEY | sed 's/^.*History\\\(.*\)\]/\1/' | grep -v HKEY > ./files/cookie.txt.tmp
	@cat domains.clean | grep -vwf cookies.dirty > ./files/clean.tmp
	@cat ./files/cookie.txt.tmp | grep -vwf files/clean.tmp | sort > $@

cookie-diff-%: cookie-init-%
	if diff cookie-init-$(shell expr $* - 1) $< >$@; then \
		echo "cookies has not changed"; \
		rm -f $< $@; \
	fi

subnet-init-%:
	mkdir -p ./files
	@( cd ./files ; wget http://www.geocities.com/yosponge/blockips.txt)
	@cat ./files/blockips.txt | tr -d '\r' | egrep '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' | awk '{print $$1 " " $$2 " " $$3}' > ./files/blockips2.txt
	@cat ./files/blockips2.txt | egrep -v "single IP" | egrep -v "Use a" | sed "s/^ //" > ./files/blockips3.txt
	@cat ./files/blockips3.txt | egrep '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}.*[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' > ./files/blockips-mask.txt
	@cat ./files/blockips3.txt | egrep -v '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}.*[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' > ./files/blockips-single.txt
	@rm -f ./files/subnet.txt
	@cat ./files/blockips-single.txt | awk '{print $$1 " mask 255.255.255.255,Malware," $$2 }' >> ./files/subnet.txt
	@cat ./files/blockips-mask.txt | awk '{print $$1 " mask " $$2 ",Malware," $$3 }' >> ./files/subnet.txt
	@cat ./files/subnet.txt | egrep '^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3} mask [0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}' > ./files/subnet.txt.tmp
	@cat ./files/subnet.txt.tmp | egrep -v -i "(Microsoft|MSN|Netscape)" | sort > $@

subnet-diff-%: subnet-init-%
	if diff subnet-init-$(shell expr $* - 1) $< >$@; then \
		echo "subnets has not changed"; \
		rm -f $< $@; \
	fi

.PHONY: all build clean cookie subnet url
.SECONDARY:

