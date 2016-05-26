package com.dickimawbooks.makeglossariesgui;

import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class DiagnosticPanel extends JEditorPane
{
   public DiagnosticPanel(MakeGlossariesGUI application)
   {
      super("text/html", "");
      app = application;

      setEditable(false);
      addHyperlinkListener(app);
      addMouseListener(app);
      setFont(app.getFont());
      setTransferHandler(app.getTransferHandler());
   }

   public void updateDiagnostics()
   {
      Font font = app.getFont();

      StyleSheet stylesheet = ((HTMLDocument)getDocument()).getStyleSheet();

      stylesheet.addRule("body { font-size: "+font.getSize()+"pt; }");
      stylesheet.addRule("body { font-family: "+font.getName()+"; }");
      stylesheet.addRule("body { font-weight: "+(font.isBold()?"bold":"normal")+"; }");
      stylesheet.addRule("body { font-style:  "+(font.isItalic()?"italic":"normal")+"; }");

      MakeGlossariesInvoker invoker = app.getInvoker();

      if (invoker.getGlossaries() != null)
      {
         String diagnostics = invoker.getGlossaries().getDiagnostics();

         if (diagnostics == null)
         {
            setText(invoker.getLabel("diagnostics.no_errors"));
         }
         else
         {
            setText(diagnostics);
            app.selectDiagnosticComponent();
         }
      }
   }

   private MakeGlossariesGUI app;
}
