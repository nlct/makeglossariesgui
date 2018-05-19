package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.awt.datatransfer.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class GlossariesPanel extends JEditorPane
   implements HyperlinkListener
{
   public GlossariesPanel(MakeGlossariesGUI application)
      throws IOException,BadLocationException
   {
      super();

      app = application;

      setContentType("text/html");

      setEditable(false);
      addHyperlinkListener(this);
      addMouseListener(app);

      HTMLDocument doc = (HTMLDocument)getDocument();
   }

   public void updateInfo()
      throws BadLocationException,IOException
   {
      StyleSheet stylesheet = ((HTMLDocument)getDocument()).getStyleSheet();

      Font font = app.getFont();

      stylesheet.addRule(String.format("body { font-size: %dpt; }",
        font.getSize()));
      stylesheet.addRule(String.format("body { font-family: %s; }",
        font.getName()));
      stylesheet.addRule(String.format("body { font-weight: %s; }",
        (font.isBold()?"bold":"normal")));
      stylesheet.addRule(String.format("body { font-style: %s; }",
        (font.isItalic()?"italic":"normal")));

      setText(app.getMainInfoTemplate());
      updateHeaders();
      updateFields();
      updateGlossaries();
   }

   private void updateHeaders()
      throws BadLocationException,IOException
   {
      HTMLDocument doc = (HTMLDocument)getDocument();
      Glossaries glossaries = app.getGlossaries();

      if (glossaries == null)
      {
         return;
      }

      for (int i = 0, n = Glossaries.getNumFields(); i < n; i++)
      {
         Element e = Glossaries.getFieldLabelElement(doc, i);
         String tag = e.getName();

	 doc.setOuterHTML(e, String.format("<%s>%s</%s>",
            tag, glossaries.getFieldLabel(i), tag));
      }
   }

   private void updateFields()
      throws BadLocationException,IOException
   {
      HTMLDocument doc = (HTMLDocument)getDocument();
      Glossaries glossaries = app.getGlossaries();

      if (glossaries == null) return;

      for (int i = 0, n = glossaries.getNumFields(); i < n; i++)
      {
         Element e = glossaries.getFieldElement(doc, i);
         String field = glossaries.getField(i);
         String err = glossaries.getFieldError(i);
         String tag = glossaries.getFieldTag(i);

         String content = "";

         if (err == null)
         {
            if (field != null)
            {
               content = field;
            }
         }
         else
         {
            if (field == null)
            {
               content = String.format("<font class=errormess>%s</font>", err);
            }
            else
            {
               content = String.format(
                 "<font class=error>%s <font class=errormess>%s</font></font>",
                  field, err);
            }
         }

         doc.setOuterHTML(e, String.format("<td id=%s>%s</td>", tag, content));
      }
   }

   private void updateGlossaries()
      throws BadLocationException,IOException
   {
      HTMLDocument doc = (HTMLDocument)getDocument();
      Glossaries glossaries = app.getGlossaries();

      if (glossaries == null) return;

      if (glossaries.getIstName() == null) return;

      DateFormat df = DateFormat.getDateTimeInstance();

      File file = app.getFile();
      File dir = file.getParentFile();
      String baseName = file.getName();

      int idx = baseName.lastIndexOf(".");

      if (idx != -1)
      {
         baseName = baseName.substring(0, idx);
      }

      String viewLabel = app.getLabel("main.view");
      String detailsLabel = app.getLabel("main.details");

      for (int i = 0, n = glossaries.getNumGlossaries(); i < n; i++)
      {
         Glossary g = glossaries.getGlossary(i);
         String glossaryInfoTemplate = app.getGlossaryInfoTemplate(g.label);

         doc.insertBeforeEnd(doc.getElement("glossaries"),
            glossaryInfoTemplate);

         doc.setInnerHTML(doc.getElement("glossaryheader-"+g.label), 
            app.getLabelWithValues("main.glossary", g.label));

         File transFile = new File(dir, baseName+"."+g.transExt);
         File gloFile = new File(dir, baseName+"."+g.gloExt);
         File glsFile = new File(dir, baseName+"."+g.glsExt);

         Element e = doc.getElement("loglabel-"+g.label);
         String tag = e.getName();

	 doc.setOuterHTML(e, String.format("<%s>%s</%s>",
            tag, app.getLabel("main.glossary.log"), tag));

         if (transFile.exists())
         {
            doc.setOuterHTML(doc.getElement("logname-"+g.label),
                transFile.getName());
            doc.setOuterHTML(doc.getElement("logmod-"+g.label),
                transFile.lastModified() < gloFile.lastModified() ?
                app.getLabel("main.out_of_date"):
                app.getLabel("main.up_to_date"));
            doc.setOuterHTML(doc.getElement("logview-"+g.label),
                String.format("<a href=\"%s\">%s</a>", 
                   transFile.toURI(), viewLabel));
         }
         else
         {
            doc.setInnerHTML(doc.getElement("loginfo-"+g.label),
               String.format("<font class=error>%s</font>", 
                 app.getLabel("error.no_such_file")));
         }

         e = doc.getElement("glslabel-"+g.label);
         tag = e.getName();

	 doc.setOuterHTML(e, String.format("<%s>%s</%s>",
            tag, app.getLabel("main.glossary.gls"), tag));

         if (glsFile.exists())
         {
            doc.setOuterHTML(doc.getElement("glsname-"+g.label),
                glsFile.getName());
            doc.setOuterHTML(doc.getElement("glsmod-"+g.label),
                glsFile.length() == 0 ?
                app.getLabel("main.empty") :
                (glsFile.lastModified() < gloFile.lastModified() ?
                app.getLabel("main.out_of_date"):
                app.getLabel("main.up_to_date")));
            doc.setOuterHTML(doc.getElement("glsview-"+g.label),
                String.format("<a href=\"%s\">%s</a>",
                   glsFile.toURI(), viewLabel));
         }
         else
         {
            doc.setInnerHTML(doc.getElement("glsinfo-"+g.label),
               String.format("<font class=error>%s</font>", 
                  app.getLabel("error.no_such_file")));
         }

         e = doc.getElement("glolabel-"+g.label);
         tag = e.getName();

	 doc.setOuterHTML(e, String.format("<%s>%s</%s>",
            tag, app.getLabel("main.glossary.glo"), tag));


         if (gloFile.exists())
         {
            doc.setOuterHTML(doc.getElement("gloname-"+g.label),
                gloFile.getName());
            doc.setOuterHTML(doc.getElement("glomod-"+g.label),
                df.format(new Date(gloFile.lastModified())));
            doc.setOuterHTML(doc.getElement("gloview-"+g.label),
               String.format("<a href=\"%s\">%s</a>",
                  gloFile.toURI(), viewLabel));
         }
         else
         {
            doc.setInnerHTML(doc.getElement("gloinfo-"+g.label),
               String.format("<font class=error>%s</font>",
                  app.getLabel("error.no_such_file")));
         }


         if (glossaries.useXindy())
         {
            e = doc.getElement("langlabel-"+g.label);
            tag = e.getName();

	    doc.setOuterHTML(e, String.format("<%s>%s</%s>",
               tag, app.getLabel("main.glossary.language"), tag));

            doc.setInnerHTML(doc.getElement("langinfo-"+g.label),
               g.displayLanguage());

            e = doc.getElement("codelabel-"+g.label);
            tag = e.getName();

	    doc.setOuterHTML(e, String.format("<%s>%s</%s>",
               tag, app.getLabel("main.glossary.codepage"), tag));

            doc.setInnerHTML(doc.getElement("codeinfo-"+g.label),
               g.displayCodePage());
         }
         else
         {
            doc.setOuterHTML(doc.getElement("xindy"), "");
         }

         e = doc.getElement("entrieslabel-"+g.label);
         tag = e.getName();

	 doc.setOuterHTML(e, String.format("<%s>%s</%s>",
            tag, app.getLabel("main.num_entries"), tag));

         e = doc.getElement("entriesinfo-"+g.label);
         tag = e.getName();

         String detailsLink = "";

         if (g.getNumEntries() > 0)
         {
            detailsLink = String.format(
              " <a description=\"%s\" href=\"%s\">%s</a>",
              g.label, g.label, detailsLabel);
         } 

	 doc.setOuterHTML(e, String.format("<%s>%s%s</%s>",
            tag, g.getNumEntries(), detailsLink, tag));

         String errMess = g.getErrorMessages();

         if (errMess != null)
         {
            doc.setOuterHTML(doc.getElement("error-"+g.label), 
                String.format("<font class=error>%s</font>", errMess));
         }
      }
   }

   public void hyperlinkUpdate(HyperlinkEvent evt)
   {
      HyperlinkEvent.EventType type = evt.getEventType();
      URL url = evt.getURL();

      if (type == HyperlinkEvent.EventType.ACTIVATED)
      {
         if (url == null)
         {
            Glossary g = app.getGlossaries().getGlossary(evt.getDescription());

            if (g != null)
            {
               new ViewEntries(app, g, app.getFont()).setVisible(true);
            }
         }
         else
         {
            try
            {
               new ViewFile(url, app.getFont(), app).setVisible(true);
            }
            catch (Exception e)
            {
               app.error(e);
            }
         }
      }
   }

   private MakeGlossariesGUI app;
}
