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
import java.io.*;
import java.net.*;

import javax.swing.*;
import javax.swing.text.*;

public class ViewFile extends JFrame
{
   public ViewFile(URL source, Font font, MakeGlossariesGUI application)
      throws IOException,BadLocationException
   {
      super(source.getFile());

      app = application;

      setIconImage(app.getIconImage());

      url = source;

      area = new JTextPane();

      StyledDocument doc = area.getStyledDocument();

      Style def = StyleContext.getDefaultStyleContext()
         .getStyle(StyleContext.DEFAULT_STYLE);

      doc.addStyle("regular", def);

      area.setEditable(false);

      area.setFont(font);

      scrollPane = new JScrollPane(area);

      getContentPane().add(scrollPane, "Center");

      scrollPane.setPreferredSize(new Dimension(500, 400));

      pack();

      setLocationRelativeTo(null);

      reload();
   }

   public void reload()
      throws IOException,BadLocationException
   {
      BufferedReader reader = null;

      StringBuilder content = new StringBuilder();

      try
      {
         reader = new BufferedReader(new InputStreamReader(
           url.openStream(), app.getEncoding()));

         String line;

         while ((line = reader.readLine()) != null)
         {
            content.append(String.format("%s%n", line));
         }
      }
      finally
      {
         if (reader != null)
         {
            reader.close();
         }
      }

      StyledDocument doc = area.getStyledDocument();
      doc.insertString(0, content.toString(), doc.getStyle("regular"));

      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            area.setCaretPosition(0);
         }
      }
      );
   }

   private URL url;

   private JTextPane area;

   private JScrollPane scrollPane;

   private MakeGlossariesGUI app;
}
