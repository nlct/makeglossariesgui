package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;

import javax.swing.*;

public class PropertiesDialog extends JDialog
      implements ActionListener,ItemListener
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

      customField = new FileField(app, this, fileChooser,
        JFileChooser.DIRECTORIES_ONLY);
      panel.add(customField);

      if (setting.equals("home"))
      {
         homeButton.setSelected(true);
         customField.setEnabled(false);
      }
      else if (setting.equals("last"))
      {
         lastButton.setSelected(true);
         customField.setEnabled(false);
      }
      else if (setting.equals("custom"))
      {
         customButton.setSelected(true);
         customField.setFileName(file.getAbsolutePath());
      }

      box.add(new JLabel(app.getLabel("properties", "diagnostics")));

      docDefCheckBox = new JCheckBox(app.getLabel("properties", "docdefcheck"), 
         properties.isDocDefsCheckOn());
      docDefCheckBox.setMnemonic(app.getMnemonic("properties", "docdefcheck"));
      box.add(docDefCheckBox);

      missingLangModBox = new JCheckBox(app.getLabel("properties", "langcheck"),
         properties.isMissingLangCheckOn());
      missingLangModBox.setMnemonic(app.getMnemonic("properties", "langcheck"));
      box.add(missingLangModBox);

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

      makeindexField = new FileField(app, this,
        properties.getMakeIndexApp(),
        fileChooser);
      makeindexLabel.setLabelFor(makeindexField.getTextField());
      panel.add(makeindexField);

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

      langLabel = new JLabel(app.getLabel("properties.language"));
      langLabel.setDisplayedMnemonic(app.getMnemonic("properties.language"));
      xindyDefaultsPanel.add(langLabel);

      String[] languages = XindyModule.getKnownLanguages();
      Arrays.sort(languages);

      languageBox = new JComboBox<String>(languages);
      langLabel.setLabelFor(languageBox);

      languageBox.setSelectedItem(properties.getDefaultLanguage());
      languageBox.addItemListener(this);

      xindyDefaultsPanel.add(languageBox);

      xindyModuleLayout = new CardLayout();
      modulesPanel = new JPanel(xindyModuleLayout);

      initVariants((String)languageBox.getSelectedItem(),
        properties.getDefaultXindyVariant(),
        properties.getDefaultCodePage());

      xindyDefaultsPanel.add(modulesPanel);

      overrideBox = new JCheckBox(app.getLabel("properties", "override"), properties.isOverride());
      overrideBox.setMnemonic(app.getMnemonic("properties", "override"));
      overrideBox.setActionCommand("override");
      overrideBox.addActionListener(this);
      xindyBox.add(overrideBox);

      dim = xindyLabel.getPreferredSize();
      maxWidth = (int)Math.max(maxWidth, dim.getWidth());

      xindyField = new FileField(app, this,
        properties.getXindyApp(), fileChooser);
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
      app.enableHelpOnButton(helpButton, "properties");
      buttonPanel.add(helpButton);

      pack();

      setLocationRelativeTo(app);

      updateOverride();
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
      else if (action.equals("override"))
      {
         updateOverride();
      }
      else if (action.equals("okay"))
      {
         properties.setMakeIndexApp(makeindexField.getFileName());
         properties.setXindyApp(xindyField.getFileName());
         properties.setDefaultLanguage((String)languageBox.getSelectedItem());
         properties.setDefaultCodePage(currentModule.getSelectedCodePage());

         String variant = currentModule.getSelectedVariant();

         if (variant != null)
         {
            properties.setDefaultXindyVariant(variant);
         }

         properties.setOverride(overrideBox.isSelected());
         properties.setDocDefsCheck(docDefCheckBox.isSelected());
         properties.setMissingLangCheck(missingLangModBox.isSelected());

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

   private void updateOverride()
   {
      boolean enable = overrideBox.isSelected();

      langLabel.setEnabled(enable);
      languageBox.setEnabled(enable);
      currentModule.setEnabled(enable);
   }

   public void setXindy(File path)
   {
      xindyField.setFile(path);
   }

   public void setMakeIndex(File path)
   {
      makeindexField.setFile(path);
   }

   private void initVariants(String lang, String defVariant, String defCode)
   {
      HashMap<String,XindyModule> modules = XindyModule.getKnownModules();

      for (Iterator<String> it = modules.keySet().iterator(); it.hasNext();)
      {
         String key = it.next();
         XindyModule mod = modules.get(key);
         XindyModulePanel panel;

         if (lang.equals(key))
         {
            panel = new XindyModulePanel(app, mod, defVariant, defCode);
            currentModule = panel;
         }
         else
         {
            panel = new XindyModulePanel(app, mod);
         }

         modulesPanel.add(panel);
         xindyModuleLayout.addLayoutComponent(panel, key);
      }

      xindyModuleLayout.show(modulesPanel, lang);
   }

   public void itemStateChanged(ItemEvent e)
   {
      if (e.getSource() == languageBox)
      {
         updateXindyModule();
      }
   }

   private void updateXindyModule()
   {
      String language = (String)languageBox.getSelectedItem();

      for (int i = 0, n = modulesPanel.getComponentCount(); i < n; i++)
      {
         Component comp = modulesPanel.getComponent(i);

         if (comp.getName().equals(language))
         {
            currentModule = (XindyModulePanel)comp;
            xindyModuleLayout.show(modulesPanel, language);
            return;
         }
      }

      app.debug("Can't find module panel for language "+language);
      currentModule = (XindyModulePanel)modulesPanel.getComponent(0);
      xindyModuleLayout.first(modulesPanel);
   }

   private MakeGlossariesGUI app;

   private CardLayout xindyModuleLayout;

   private JPanel modulesPanel;

   private JLabel langLabel;

   private XindyModulePanel currentModule;

   private JRadioButton homeButton, lastButton, customButton;

   private JComboBox<String> languageBox;

   private FileField customField, makeindexField, xindyField;

   private JFileChooser fileChooser;

   private JCheckBox overrideBox, docDefCheckBox, missingLangModBox;

   private MakeGlossariesProperties properties;

}

class XindyModulePanel extends JPanel
{
   public XindyModulePanel(MakeGlossariesGUI app, XindyModule module)
   {
      this(app, module, null, null);
   }

   public XindyModulePanel(MakeGlossariesGUI app,
      XindyModule module, String defVar, String defCode)
   {
      super();
      setName(module.getLanguage());
      this.module = module;

      encodingLabel = new JLabel(app.getLabel("properties.encoding"));
      encodingLabel.setDisplayedMnemonic(app.getMnemonic("properties.encoding"));
      add(encodingLabel);

      codePageBox = new JComboBox<String>(module.getCodePages());
      add(codePageBox);

      if (defCode != null)
      {
         codePageBox.setSelectedItem(defCode);
      }
      else
      {
         codePageBox.setSelectedIndex(codePageBox.getItemCount()-1);
      }

      encodingLabel.setLabelFor(codePageBox);

      if (module.hasVariants())
      {
         variantBox = new JComboBox<String>(module.getVariants());
         variantBox.setSelectedItem(
           defVar == null ? module.getDefaultVariant() : defVar);
         add(variantBox);
      }
   }

   public String getSelectedVariant()
   {
      return variantBox == null ? null : 
       (String)variantBox.getSelectedItem();
   }

   public String getSelectedCodePage()
   {
      return (String)codePageBox.getSelectedItem();
   }

   public void setEnabled(boolean enabled)
   {
      if (variantBox != null)
      {
         variantBox.setEnabled(enabled);
      }

      encodingLabel.setEnabled(enabled);
      codePageBox.setEnabled(enabled);
   }

   private XindyModule module;
   private JComboBox<String> variantBox = null;
   private JComboBox<String> codePageBox = null;
   private JLabel encodingLabel;
}

