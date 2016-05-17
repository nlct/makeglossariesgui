APP_VERSION:=$(shell grep "public static final String appVersion" java/MakeGlossariesGUI.java | sed "s/public\sstatic\sfinal\sString\sappVersion=//" | tr -d "\"\; ")
IZPACKDIR=/usr/local/IzPack

test	: app
	bin/makeglossariesgui

app	: lib lib/makeglossariesgui.jar lib/resources lib/makeglossariesgui-splash.png

dist	: ../dist/makeglossariesgui-installer.jar
	cp doc/makeglossariesgui-en-GB.pdf ../dist

../dist/makeglossariesgui-installer.jar	: app installer/install.xml
	$(IZPACKDIR)/bin/compile installer/install.xml -b . \
	-o ../dist/makeglossariesgui-installer.jar -k standard

lib	:
	mkdir -p lib

lib/resources   :
	cd lib; ln -s ../resources

resources/icons	: 
	cd resources; ln -s ../images/icons

lib/makeglossariesgui-splash.png	: lib
		cd lib; ln -s -f ../images/makeglossariesgui-splash.png

lib/makeglossariesgui.jar	: java/Manifest.txt \
			resources/icons \
			classes/com/dickimawbooks/makeglossariesgui/MakeGlossariesGUI.class \
			doc/createmakeglossariesguidocs doc/makeglossariesgui-*.xml doc/makeglossariesgui.dtd
	make -C doc
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/makeglossariesgui.jar \
	com/dickimawbooks/makeglossariesgui/*.class \
	com/dickimawbooks/makeglossariesgui/*/*.class 

classes/com/dickimawbooks/makeglossariesgui/MakeGlossariesGUI.class	: classes/com/dickimawbooks/makeglossariesgui \
	java/*.java java/*/*.java \
	lib/javahelp2.jar lib/jlfgr-1_0.jar
	cd java; \
	javac -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/javahelp2.jar:../lib/jlfgr-1_0.jar *.java */*.java

classes/com/dickimawbooks/makeglossariesgui	:
	mkdir -p classes/com/dickimawbooks/makeglossariesgui

clean	:
	\rm -f classes/com/dickimawbooks/makeglossariesgui/*.class
	\rm -f classes/com/dickimawbooks/makeglossariesgui/*/*.class

squeaky	:
	\rm -f lib/makeglossariesgui.jar
