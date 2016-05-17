package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

public class PropertiesDialog extends JDialog
      implements ActionListener
{
   public PropertiesDialog(MakeGlossariesGUI application)
   {
      super(application, application.getLabel("properties.title"), true);

      app = application;

      properties = app.getProperties();

      File file = new File(properties.getDefaultDirectory());

      String setting = properties.getDefaultDirectorySetting();

      if (setting == null) setting = "home";

      fileChooser = new JFileChooser(getDir());

      Box box = Box.createVerticalBox();
      getContentPane().add(box, "Center");

      JLabel dirLabel = new JLabel(app.getLabel("properties.start_dir"));
      dirLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(dirLabel);

      Dimension dim = dirLabel.getPreferredSize();
      int maxWidth = (int)dim.getWidth();

      Box startDirBox = Box.createVerticalBox();
      startDirBox.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(startDirBox);

      ButtonGroup bg = new ButtonGroup();

      homeButton = new JRadioButton(app.getLabel("properties.dir.home"));
      homeButton.setActionCommand("disablecustom");
      homeButton.addActionListener(this);
      homeButton.setMnemonic(app.getMnemonicInt("properties.dir.home"));
      homeButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      bg.add(homeButton);
      startDirBox.add(homeButton);

      dim = homeButton.getPreferredSize();
      maxWidth = (int)Math.max(maxWidth, dim.getWidth());

      lastButton = new JRadioButton(app.getLabel("properties.dir.last"));
      lastButton.setActionCommand("disablecustom");
      lastButton.setMnemonic(app.getMnemonicInt("properties.dir.last"));
      lastButton.addActionListener(this);
      lastButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      bg.add(lastButton);
      startDirBox.add(lastButton);

      dim = lastButton.getPreferredSize();
      maxWidth = (int)Math.max(maxWidth, dim.getWidth());

      Box panel = Box.createHorizontalBox();
      panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      startDirBox.add(panel);

      customButton = new JRadioButton(app.getLabel("properties.dir.custom"));
      customButton.setMnemonic(app.getMnemonicInt("properties.dir.custom"));
      customButton.setActionCommand("enablecustom");
      customButton.addActionListener(this);
      bg.add(customButton);
      panel.add(customButton);

      dim = customButton.getPreferredSize();
      maxWidth = (int)Math.max(maxWidth, dim.getWidth());

      customField = new FileField(this, fileChooser, JFileChooser.DIRECTORIES_ONLY);
      panel.add(customField);

      if (setting.equals("home"))
      {
         homeButton.setSelected(true);
         customField.setEnabled(false);
      }
      else if (setting.equals("last"))
      {
         homeButton.setSelected(true);
         customField.setEnabled(false);
      }
      else if (setting.equals("custom"))
      {
         customButton.setSelected(true);
         customField.setFileName(file.getAbsolutePath());
      }

      Box makeindexBox = Box.createVerticalBox();
      makeindexBox.setBorder(BorderFactory.createEtchedBorder());
      box.add(makeindexBox);

      panel = Box.createHorizontalBox();
      panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      makeindexBox.add(panel);

      JLabel makeindexLabel = new JLabel(app.getLabel("properties.makeindex"));
      makeindexLabel.setDisplayedMnemonic(app.getMnemonic("properties.makeindex"));
      panel.add(makeindexLabel);

      dim = makeindexLabel.getPreferredSize();
      maxWidth = (int)Math.max(maxWidth, dim.getWidth());

      makeindexField = new FileField(this, properties.getMakeIndexApp(), fileChooser);
      makeindexLabel.setLabelFor(makeindexField.getTextField());
      panel.add(makeindexField);

      germanWordOrderButton = new JCheckBox(app.getLabel("properties.german_word_order"),false);
      germanWordOrderButton.setMnemonic(app.getMnemonic("properties.german_word_order"));
      germanWordOrderButton.setSelected(properties.useGermanWordOrdering());

      // Need to change style to use something other than " as quote
      // character in order to implement this
      //makeindexBox.add(germanWordOrderButton);

      Box xindyBox = Box.createVerticalBox();
      xindyBox.setBorder(BorderFactory.createEtchedBorder());
      box.add(xindyBox);

      panel = Box.createHorizontalBox();
      panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      xindyBox.add(panel);

      JLabel xindyLabel = new JLabel(app.getLabel("properties.xindy"));
      xindyLabel.setDisplayedMnemonic(app.getMnemonic("properties.xindy"));
      panel.add(xindyLabel);

      JPanel xindyDefaultsPanel = new JPanel();
      xindyDefaultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      xindyBox.add(xindyDefaultsPanel);

      JLabel langLabel = new JLabel(app.getLabel("properties.language"));
      langLabel.setDisplayedMnemonic(app.getMnemonic("properties.language"));
      xindyDefaultsPanel.add(langLabel);

      languageBox = new JComboBox<String>(knownXindyLanguages);
      languageBox.setEditable(true);
      langLabel.setLabelFor(languageBox);

      languageBox.setSelectedItem(properties.getDefaultLanguage());

      xindyDefaultsPanel.add(languageBox);

      JLabel encodingLabel = new JLabel(app.getLabel("properties.encoding"));
      encodingLabel.setDisplayedMnemonic(app.getMnemonic("properties.encoding"));
      xindyDefaultsPanel.add(encodingLabel);

      encodingBox = new JComboBox<String>(knownEncodings);
      encodingBox.setEditable(true);
      encodingLabel.setLabelFor(encodingBox);
      encodingBox.setSelectedItem(properties.getDefaultCodePage());

      xindyDefaultsPanel.add(encodingBox);

      dim = xindyLabel.getPreferredSize();
      maxWidth = (int)Math.max(maxWidth, dim.getWidth());

      xindyField = new FileField(this, properties.getXindyApp(), fileChooser);
      xindyLabel.setLabelFor(xindyField.getTextField());
      panel.add(xindyField);

      dim = dirLabel.getPreferredSize();
      dim.width = maxWidth;
      dirLabel.setPreferredSize(dim);

      dim = homeButton.getPreferredSize();
      dim.width = maxWidth;
      homeButton.setPreferredSize(dim);

      dim = lastButton.getPreferredSize();
      dim.width = maxWidth;
      lastButton.setPreferredSize(dim);

      dim = customButton.getPreferredSize();
      dim.width = maxWidth;
      customButton.setPreferredSize(dim);

      dim = makeindexLabel.getPreferredSize();
      dim.width = maxWidth;
      makeindexLabel.setPreferredSize(dim);

      dim = xindyLabel.getPreferredSize();
      dim.width = maxWidth;
      xindyLabel.setPreferredSize(dim);

      JPanel buttonPanel = new JPanel();
      getContentPane().add(buttonPanel, "South");

      buttonPanel.add(app.createOkayButton(this));
      buttonPanel.add(app.createCancelButton(this));

      JButton helpButton = new JButton(app.getLabel("button.help"));
      helpButton.setMnemonic(app.getMnemonic("button.help"));
      app.enableHelpOnButton(helpButton, "sec:properties");
      buttonPanel.add(helpButton);

      pack();
      setLocationRelativeTo(app);

   }

   private File getDir()
   {
      String makeindexApp = app.getMakeIndexApp();
      String xindyApp = app.getXindyApp();

      if ((makeindexApp == null || makeindexApp.equals(""))
        &&(xindyApp == null || xindyApp.equals("")))
      {
         try
         {
            Process p = Runtime.getRuntime().exec("kpsewhich texmf.cnf");

            if (p.waitFor() == 0)
            {
               BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

               String line = in.readLine();

               in.close();

               if (line != null)
               {
                  File dir = (new File(line)).getParentFile();

                  if (dir.isDirectory())
                  {
                     return dir;
                  }
               }
            }
         }
         catch (Exception e)
         {
         }
      }

      return new File(properties.getDefaultDirectory());
   }

   public void actionPerformed(ActionEvent e)
   {
      String action = e.getActionCommand();

      if (action == null) return;

      if (action.equals("disablecustom"))
      {
         customField.setEnabled(false);
      }
      else if (action.equals("enablecustom"))
      {
         customField.setEnabled(true);
         customField.requestFocusInWindow();
      }
      else if (action.equals("cancel"))
      {
         setVisible(false);
      }
      else if (action.equals("okay"))
      {
         properties.setMakeIndexApp(makeindexField.getFileName());
         properties.setXindyApp(xindyField.getFileName());
         properties.setGermanWordOrdering(germanWordOrderButton.isSelected());
         properties.setDefaultLanguage((String)languageBox.getSelectedItem());
         properties.setDefaultCodePage((String)encodingBox.getSelectedItem());

         if (homeButton.isSelected())
         {
            properties.setDefaultHomeDir();
         }
         else if (lastButton.isSelected())
         {
            properties.setDefaultLastDir();
         }
         else
         {
            String fileName  = customField.getFileName();

            if (fileName.equals(""))
            {
               app.error(this, app.getLabel("error.missing_dir_name"));
               return;
            }

            File file = new File(fileName);

            if (!file.isDirectory())
            {
               app.error(this, app.getLabelWithValue("error.no_such_directory", fileName));
            }

            properties.setDefaultCustomDir(fileName);
         }
         
         setVisible(false);
      }
   }

   public void display()
   {
      setVisible(true);
   }

   private MakeGlossariesGUI app;

   private JRadioButton homeButton, lastButton, customButton;

   private JCheckBox germanWordOrderButton;

   private JComboBox<String> languageBox, encodingBox;

   private FileField customField, makeindexField, xindyField;

   private JFileChooser fileChooser;

   private MakeGlossariesProperties properties;

   private static final String[] knownXindyLanguages = new String[]
   {
      "albanian",
      "belarusian",
      "bulgarian",
      "croatian",
      "czech",
      "danish",
      "dutch",
      "english",
      "esperanto",
      "estonian",
      "finnish",
      "french",
      "general",
      "georgian",
      "german",
      "greek",
      "gypsy",
      "hausa",
      "hebrew",
      "hungarian",
      "icelandic",
      "italian",
      "klingon",
      "kurdish",
      "latin",
      "latvian",
      "lithuanian",
      "lower-sorbian",
      "macedonian",
      "mongolian",
      "norwegian",
      "polish",
      "portuguese",
      "romanian",
      "russian",
      "serbian",
      "slovak",
      "slovenian",
      "spanish",
      "swedish",
      "turkish",
      "ukrainian",
      "upper-sorbian",
      "vietnamese"
   };

   private static final String[] knownEncodings = new String[]
   {
     "cp1250",
     "cp1251",
     "cp1252",
     "ij-as-ij-latin1",
     "ij-as-ij-utf8",
     "ij-as-y-utf8",
     "iso88595",
     "iso88597",
     "isoir111",
     "koi8-r",
     "latin1",
     "latin2",
     "latin3",
     "latin4",
     "latin5",
     "latin9",
     "modern-latin1",
     "modern-utf8",
     "polytonic-utf8",
     "traditional-latin1",
     "traditional-utf8",
     "translit-latin4",
     "translit-utf8",
     "utf8"
   };
}
