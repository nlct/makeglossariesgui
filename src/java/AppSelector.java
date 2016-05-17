/*
    Copyright (C) 2013 Nicola L.C. Talbot
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

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class AppSelector extends JDialog
   implements ActionListener
{
   public AppSelector(MakeGlossariesGUI application)
   {
      super(application, application.getLabel("appselect.title"), true);

      message = new JLabel(application.getLabel("appselect.pathlabel"));
      message.setDisplayedMnemonic(application.getMnemonic("appselect.pathlabel"));

      getContentPane().add(message, "North");

      fileChooser = new JFileChooser();

      fileField = new FileField(this, fileChooser);

      getContentPane().add(fileField, "Center");

      JPanel buttonPanel = new JPanel();
      add(buttonPanel, "South");

      buttonPanel.add(app.createOkayButton(this));
      buttonPanel.add(app.createCancelButton(this));

      String arch = System.getProperty("os.name").toLowerCase();
      path = System.getenv("PATH");

      if (arch.indexOf("win") >= 0)
      {
         pathEnvSep = ";";
         exeSuffix = ".exe";
      }
      else
      {
         pathEnvSep = ":";
         exeSuffix = "";
      }

      pack();
      Dimension dim = getSize();

      dim.width += 50;
      dim.height += 10;

      setSize(dim);

      setLocationRelativeTo(application);
   }

   public File findApp(String name)
   {
      return findApp(name, null, null);
   }

   public File findApp(String name, String altName, String altName2)
   {
      String filename = name + exeSuffix;
      String filename2 = (altName == null ? null : altName + exeSuffix);
      String filename3 = (altName2 == null ? null : altName2 + exeSuffix);

      String[] split = path.split(pathEnvSep);

      for (int i = 0; i < split.length; i++)
      {
         File file = new File(split[i], filename);

         if (file.exists())
         {
            return file;
         }

         if (filename2 != null)
         {
            file = new File(split[i], filename2);

            if (file.exists())
            {
               return file;
            }
         }

         if (filename3 != null)
         {
            file = new File(split[i], filename3);

            if (file.exists())
            {
               return file;
            }
         }
      }

      return null;
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("choose"))
      {
         if (fileChooser.showOpenDialog(this)
           == JFileChooser.APPROVE_OPTION)
         {
            fileField.setFileName(fileChooser.getSelectedFile().getAbsolutePath());
         }
      }
      else if (action.equals("okay"))
      {
         selectedFile = fileField.getFile();

         if (selectedFile == null || selectedFile.equals(""))
         {
            app.error(this,
               MakeGlossariesGUI.getLabel("error.no_file"));
         }
         else
         {
            setVisible(false);
         }
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
   }

   public File fetchApplicationPath(String appName, String messageText)
   {
      return fetchApplicationPath(appName, null, null, messageText);
   }

   public File fetchApplicationPath(String appName, String altAppName,
      String altAppName2, String messageText)
   {
      selectedFile = null;

      File file = findApp(appName, altAppName, altAppName2);

      if (file != null)
      {
         fileChooser.setCurrentDirectory(file.getParentFile());
         fileChooser.setSelectedFile(file);

         fileField.setFileName(file.getAbsolutePath());
      }
      else
      {
         fileField.setFileName(appName+exeSuffix);
      }

      message.setText(messageText);

      setVisible(true);

      return selectedFile;
   }

   public File fetchApplicationPath(String messageText)
   {
      selectedFile = null;

      fileChooser.setSelectedFile(null);
      fileField.setFileName("");

      message.setText(messageText);

      setVisible(true);

      return selectedFile;
   }

   private File selectedFile = null;

   private JLabel message;

   private FileField fileField;

   private JFileChooser fileChooser;
   
   private MakeGlossariesGUI app;

   private String pathEnvSep = ":", exeSuffix = "", path;
}
