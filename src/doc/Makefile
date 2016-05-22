TARGETDIR=../resources/helpsets
DICTDIR=../resources/dictionaries
TARGETBACKREF=../../doc
MAINCLASS=../java/MakeGlossariesInvoker.java

all	: en

en	: $(TARGETDIR)/makeglossariesgui-en/makeglossariesgui.html \
	  makeglossariesgui-en.pdf

$(TARGETDIR)/makeglossariesgui-en/makeglossariesgui.html	: $(TARGETDIR) \
	  createmakeglossariesguidocs \
	  makeglossariesgui-en.xml makeglossariesgui.dtd \
          ../tests/basic-sample/basic-sample.tex \
	  images/*.png \
	  $(MAINCLASS) \
	  $(DICTDIR)/makeglossariesgui-en.prop \
	  $(TARGETDIR)/makeglossariesgui-en/images/
	createmakeglossariesguidocs en 
	cd $(TARGETDIR)/makeglossariesgui-en; \
	  \rm -r -f JavaHelpSearch ; \
	  jh2indexer .

$(TARGETDIR)	:
	mkdir $(TARGETDIR)

makeglossariesgui-en.pdf	: makeglossariesgui-en.tex
	arara makeglossariesgui-en

makeglossariesgui-en.tex	: createmakeglossariesguidocs makeglossariesgui-en.xml \
	  $(MAINCLASS) \
	  $(DICTDIR)/makeglossariesgui-en.prop
	createmakeglossariesguidocs en 

$(TARGETDIR)/makeglossariesgui-en/images/	: 
	mkdir -p $(TARGETDIR)/makeglossariesgui-en
	cd $(TARGETDIR)/makeglossariesgui-en/ ; \
	ln -s ../$(TARGETBACKREF)/images

clean	:
	\rm -f makeglossariesgui-en.{tex,aux,log,pdf,glg,glo,gls,ist,ilg,ind,idx,acr,acn,alg}

