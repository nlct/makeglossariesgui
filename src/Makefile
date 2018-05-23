APP_VERSION:=$(shell grep "public static final String APP_VERSION" java/MakeGlossariesInvoker.java | sed "s/public\sstatic\sfinal\sString\sAPP_VERSION=//" | tr -d "\"\; ")
IZPACKDIR=/usr/local/IzPack

test	: app
	bin/makeglossariesgui --debug

app	: lib lib/makeglossariesgui.jar lib/resources lib/makeglossariesgui-splash.png

dist	: ../dist/makeglossariesgui-installer.jar
	cp doc/makeglossariesgui-en.pdf ../dist
	cp doc/makeglossariesgui.1 ../dist

../dist/makeglossariesgui-installer.jar	: app installer/install.xml
	$(IZPACKDIR)/bin/compile installer/install.xml -b . \
	-o ../dist/makeglossariesgui-installer.jar -k standard

lib	:
	mkdir -p lib

lib/resources   :
	cd lib; ln -s ../resources

resources/icons	: 
	cd resources; ln -s ../images/icons

resources/LICENSE	:
	cd resources; ln -s ../doc/LICENSE

lib/makeglossariesgui-splash.png	: lib
		cd lib; ln -s -f ../images/makeglossariesgui-splash.png

lib/makeglossariesgui.jar	: java/Manifest.txt \
			resources/icons resources/LICENSE \
			classes/com/dickimawbooks/makeglossariesgui/MakeGlossariesInvoker.class \
			doc/createmakeglossariesguidocs doc/makeglossariesgui-*.xml doc/makeglossariesgui.dtd
	make -C doc
	cd classes; \
	jar cmf ../java/Manifest.txt ../lib/makeglossariesgui.jar \
	com/dickimawbooks/makeglossariesgui/*.class 

classes/com/dickimawbooks/makeglossariesgui/MakeGlossariesInvoker.class	: classes/com/dickimawbooks/makeglossariesgui \
	java/*.java \
	lib/javahelp2.jar lib/jlfgr-1_0.jar
	cd java; \
	javac -d ../classes \
	 -Xlint:unchecked -Xlint:deprecation \
	-cp ../lib/javahelp2.jar:../lib/jlfgr-1_0.jar *.java 

classes/com/dickimawbooks/makeglossariesgui	:
	mkdir -p classes/com/dickimawbooks/makeglossariesgui

clean	:
	\rm -f classes/com/dickimawbooks/makeglossariesgui/*.class
	\rm -f classes/com/dickimawbooks/makeglossariesgui/*/*.class

squeaky	:
	\rm -f lib/makeglossariesgui.jar
