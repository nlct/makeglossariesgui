/*
    Copyright (C) 2013-2020 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
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
      ((HTMLEditorKit)getEditorKit()).setAutoFormSubmission(false);
   }

   public void updateDiagnostics()
   {
      Font font = app.getFont();

      StyleSheet stylesheet = ((HTMLDocument)getDocument()).getStyleSheet();

      stylesheet.addRule("body { font-size: "+font.getSize()+"pt; }");
      stylesheet.addRule("body { font-family: "+font.getName()+"; }");
      stylesheet.addRule("body { font-weight: "+(font.isBold()?"bold":"normal")+"; }");
      stylesheet.addRule("body { font-style:  "+(font.isItalic()?"italic":"normal")+"; }");

      stylesheet.addRule("input { vertical-align: baseline; }");


      MakeGlossariesInvoker invoker = app.getInvoker();

      String message = app.diagnosticsForm();

      String results = app.getScriptTestResults();

      if (results != null)
      {
         message += results;
      }
            
      if (invoker.getGlossaries() == null)
      {
         message = String.format("%s<p>%s",
            invoker.getLabelWithValues("diagnostics.no_file",
              invoker.getLabel("file"), invoker.getLabel("file.open")),
            message);
      }
      else
      {
         String advisory = invoker.getGlossaries().getAdvisoryMessages();
         String diagnostics = invoker.getGlossaries().getDiagnostics();

         if (diagnostics == null)
         {
            message = String.format("%s<p>%s", 
               invoker.getLabel("diagnostics.no_errors"),
               message);
         }
         else
         {
            app.selectDiagnosticComponent();
            message = String.format("%s<p>%s", diagnostics,
               message);
         }

         if (advisory != null)
         {
            message = String.format("%s<h2>%s</h2>%s", 
                 message,
                 invoker.getLabel("diagnostics.advisory"), 
                 advisory);
         }

      }

      setText(message);
   }

   private MakeGlossariesGUI app;
}
