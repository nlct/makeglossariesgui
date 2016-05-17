package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class FileField extends JPanel
  implements ActionListener
{
   public FileField(Container parent, JFileChooser fileChooser)
   {
      this(parent, null, fileChooser, JFileChooser.FILES_ONLY);
   }

   public FileField(Container parent, JFileChooser fileChooser, int mode)
   {
      this(parent, null, fileChooser, mode);
   }

   public FileField(Container parent, String fileName, JFileChooser fileChooser)
   {
      this(parent, fileName, fileChooser, JFileChooser.FILES_ONLY);
   }

   public FileField(Container parent, String fileName, JFileChooser fileChooser, int mode)
   {
      super();

      this.fileChooser = fileChooser;
      this.parent = parent;
      this.mode = mode;

      textField = new JTextField(fileName == null ? "" : fileName, 20);

      add(textField);

      button = new JButton("...");

      button.setActionCommand("choose");
      button.addActionListener(this);

      add(button);
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("choose"))
      {
         File file = getFile();

         if (file != null)
         {
            fileChooser.setCurrentDirectory(file.getParentFile());
         }

         fileChooser.setSelectedFile(file);

         fileChooser.setFileSelectionMode(mode);

         fileChooser.setApproveButtonMnemonic(MakeGlossariesGUI.getMnemonic("button.select"));

         if (fileChooser.showDialog(parent, MakeGlossariesGUI.getLabel("button.select"))
            == JFileChooser.APPROVE_OPTION)
         {
            textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
         }
      }
   }

   public boolean requestFocusInWindow()
   {
      return textField.requestFocusInWindow();
   }

   public JTextField getTextField()
   {
      return textField;
   }

   public File getFile()
   {
      String fileName = getFileName();

      if (fileName == null || fileName.equals("")) return null;

      return new File(fileName);
   }

   public String getFileName()
   {
      return textField.getText();
   }

   public void setFileName(String name)
   {
      textField.setText(name);
   }

   public void setEnabled(boolean flag)
   {
      super.setEnabled(flag);

      textField.setEnabled(flag);
      button.setEnabled(flag);
   }

   private JTextField textField;

   private JButton button;

   private JFileChooser fileChooser;

   private Container parent;

   private int mode;
}
