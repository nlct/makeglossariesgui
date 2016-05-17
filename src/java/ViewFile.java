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
      BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));

      String content = null;

      String line;

      while ((line = reader.readLine()) != null)
      {
         if (content == null)
         {
            content = line;
         }
         else
         {
            content += "\n" + line;
         }
      }

      reader.close();

      StyledDocument doc = area.getStyledDocument();
      doc.insertString(0, content, doc.getStyle("regular"));

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
