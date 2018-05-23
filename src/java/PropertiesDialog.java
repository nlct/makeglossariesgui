package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Arrays;

import javax.swing.*;
import javax.swing.event.*;

public class PropertiesDialog extends JDialog
      implements ActionListener,ItemListener,ChangeListener
{
   public PropertiesDialog(MakeGlossariesGUI application)
   {
      super(application, application.getLabel("properties.title"), true);

      app = application;

      JTabbedPane tabbedPane = new JTabbedPane();
      getContentPane().add(tabbedPane, "Center");

      properties = app.getProperties();

      File file = new File(properties.getDefaultDirectory());

      String setting = properties.getDefaultDirectorySetting();

      if (setting == null) setting = "home";

      fileChooser = new JFileChooser(getDir());

      Box box = Box.createVerticalBox();
      newTab(tabbedPane, box, "properties.start_dir");

      Box startDirBox = Box.createVerticalBox();
      startDirBox.setAlignmentX(Component.LEFT_ALIGNMENT);
      box.add(startDirBox);

      ButtonGroup bg = new ButtonGroup();

      homeButton = createRadioButton("properties.dir.home", "disablecustom",
        bg);
      startDirBox.add(homeButton);

      lastButton = createRadioButton("properties.dir.last", "disablecustom",
        bg);
      startDirBox.add(lastButton);

      Box panel = Box.createHorizontalBox();
      panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      panel.setAlignmentY(Component.TOP_ALIGNMENT);
      startDirBox.add(panel);

      customButton = createRadioButton("properties.dir.custom", "enablecustom",
        bg);
      customButton.setAlignmentY(Component.TOP_ALIGNMENT);
      panel.add(customButton);

      customField = new FileField(app, this, fileChooser,
        JFileChooser.DIRECTORIES_ONLY);
      customField.setOpaque(false);
      customField.setAlignmentY(Component.TOP_ALIGNMENT);
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

      box = Box.createVerticalBox();
      newTab(tabbedPane, box, "properties.diagnostics");

      docDefCheckBox = createCheckBox("properties", "docdefcheck", 
         properties.isDocDefsCheckOn());
      box.add(docDefCheckBox);

      missingLangModBox = createCheckBox("properties", "langcheck",
         properties.isMissingLangCheckOn());
      box.add(missingLangModBox);

      box = Box.createVerticalBox();
      newTab(tabbedPane, box, "properties.applications");

      Box makeindexBox = Box.createVerticalBox();
      makeindexBox.setBorder(BorderFactory.createEtchedBorder());
      box.add(makeindexBox);

      panel = Box.createHorizontalBox();
      panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      makeindexBox.add(panel);

      JLabel makeindexLabel = createLabel("properties.makeindex");
      panel.add(makeindexLabel);

      makeindexField = new FileField(app, this,
        properties.getMakeIndexApp(),
        fileChooser);
      makeindexLabel.setLabelFor(makeindexField.getTextField());
      panel.add(makeindexField);
      makeindexField.setOpaque(false);

      Box xindyBox = Box.createVerticalBox();
      xindyBox.setBorder(BorderFactory.createEtchedBorder());
      box.add(xindyBox);

      panel = Box.createHorizontalBox();
      panel.setAlignmentX(Component.LEFT_ALIGNMENT);
      xindyBox.add(panel);

      JLabel xindyLabel = createLabel("properties.xindy");
      panel.add(xindyLabel);

      JPanel xindyDefaultsPanel = new JPanel();
      xindyDefaultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
      xindyBox.add(xindyDefaultsPanel);
      xindyDefaultsPanel.setOpaque(false);

      langLabel = createLabel("properties.language");
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
      modulesPanel.setOpaque(false);

      initVariants((String)languageBox.getSelectedItem(),
        properties.getDefaultXindyVariant(),
        properties.getDefaultCodePage());

      xindyDefaultsPanel.add(modulesPanel);

      overrideBox = createCheckBox("properties", "override", 
         properties.isOverride());
      xindyBox.add(overrideBox);

      xindyField = new FileField(app, this,
        properties.getXindyApp(), fileChooser);
      xindyLabel.setLabelFor(xindyField.getTextField());
      panel.add(xindyField);
      xindyField.setOpaque(false);

      box = Box.createVerticalBox();
      newTab(tabbedPane, box, "properties.gui");

      JPanel lfPanel = new JPanel();
      lfPanel.setOpaque(false);
      box.add(lfPanel);

      JLabel lfLabel = createLabel("properties.look_and_feel");
      lfPanel.add(lfLabel);

      UIManager.LookAndFeelInfo[] lfInfo = UIManager.getInstalledLookAndFeels();

      lookAndFeelBox = new JComboBox<UIManager.LookAndFeelInfo>(lfInfo);
      lookAndFeelBox.setRenderer(new LookAndFeelCellRenderer());

      String currentLF = properties.getLookAndFeel();

      if (currentLF != null)
      {
         for (int i = 0; i < lfInfo.length; i++)
         {
            if (lfInfo[i].getClassName().equals(currentLF))
            {
               lookAndFeelBox.setSelectedIndex(i);
               break;
            }
         }
      }

      lfPanel.add(lookAndFeelBox);
      lfLabel.setLabelFor(lookAndFeelBox);

      JLabel restartLabel = new JLabel(app.getLabel("properties.restart"));
      lfPanel.add(restartLabel);

      JPanel fontPanel = new JPanel();
      fontPanel.setOpaque(false);
      box.add(fontPanel);

      JLabel fontLabel = createLabel("properties.font");
      fontPanel.add(fontLabel);

      GraphicsEnvironment env =
         GraphicsEnvironment.getLocalGraphicsEnvironment();

      fontBox = new JComboBox<String>(env.getAvailableFontFamilyNames());
      fontBox.setSelectedItem(properties.getFontName());
      fontBox.addItemListener(this);
      fontPanel.add(fontBox);

      int style = properties.getFontStyle();

      boldBox = createCheckBox("properties.bold", "font",
        (style & Font.BOLD) != 0);
      fontPanel.add(boldBox);

      italicBox = createCheckBox("properties.italic", "font", 
        (style & Font.ITALIC) != 0);
      fontPanel.add(italicBox);

      JLabel fontSizeLabel = createLabel("properties.size.font");
      fontPanel.add(fontSizeLabel);

      fontSizeBox = new JSpinner(
         new SpinnerNumberModel(properties.getFontSize(), 2,100,1));
      fontPanel.add(fontSizeBox);
      fontSizeLabel.setLabelFor(fontSizeBox);
      fontSizeBox.addChangeListener(this);

      JPanel p = new JPanel();
      box.add(p);

      fontSample = new JLabel(app.getLabel("properties.font.sample"));
      p.setBackground(Color.white);
      p.setOpaque(true);
      p.setBorder(BorderFactory.createEtchedBorder());
      p.add(fontSample);

      updateFontSample();

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

   private void newTab(JTabbedPane tabbedPane, JComponent comp,
     String label)
   {
      tabbedPane.add(app.getLabel(label), comp);
      tabbedPane.setMnemonicAt(tabbedPane.getTabCount()-1, 
         app.getMnemonic(label));
   }

   private JRadioButton createRadioButton(String label, String action,
     ButtonGroup bg)
   {
      JRadioButton button = new JRadioButton(app.getLabel(label));
      button.setMnemonic(app.getMnemonic(label));
      button.setActionCommand(action);
      button.addActionListener(this);
      bg.add(button);
      button.setOpaque(false);

      return button;
   }

   private JCheckBox createCheckBox(String parent, String label, boolean set)
   {
      JCheckBox box = new JCheckBox(app.getLabel(parent, label), set);
      box.setMnemonic(app.getMnemonic(parent, label));
      box.setActionCommand(label);
      box.addActionListener(this);
      box.setOpaque(false);

      return box;
   }

   private JLabel createLabel(String label)
   {
      JLabel comp = new JLabel(app.getLabel(label));
      comp.setDisplayedMnemonic(app.getMnemonic(label));

      return comp;
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
               BufferedReader in = null;

               try
               {
                  in = new BufferedReader(
                     new InputStreamReader(p.getInputStream(), 
                       app.getEncoding()));

                  String line = in.readLine();

                  if (line != null)
                  {
                     File dir = (new File(line)).getParentFile();

                     if (dir.isDirectory())
                     {
                        return dir;
                     }
                  }
               }
               finally
               {
                  if (in != null)
                  {
                     in.close();
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
      else if (action.equals("font"))
      {
         updateFontSample();
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
               app.error(this, 
                app.getLabelWithValues("error.no_such_directory", fileName));
            }

            properties.setDefaultCustomDir(fileName);
         }
         
         boolean fontChanged=false;
         String oldFont = properties.getFontName();
         String newFont = (String)fontBox.getSelectedItem();
         int oldStyle = properties.getFontStyle();
         int newStyle = Font.PLAIN;
         int oldSize = properties.getFontSize();
         int newSize = ((Number)fontSizeBox.getValue()).intValue();

         if (boldBox.isSelected())
         {
            newStyle = Font.BOLD;
         }

         if (italicBox.isSelected())
         {
            newStyle = newStyle | Font.ITALIC;
         }

         if (!oldFont.equals(newFont))
         {
            fontChanged = true;
            properties.setFontName(newFont);
         }

         if (oldStyle != newStyle)
         {
            fontChanged = true;
            properties.setFontStyle(newStyle);
         }

         if (oldSize != newSize)
         {
            fontChanged = true;
            properties.setFontSize(newSize);
         }

         if (fontChanged)
         {
            app.updateFont();
         }

         properties.setLookAndFeel(
           (UIManager.LookAndFeelInfo)lookAndFeelBox.getSelectedItem());

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
      Object source = e.getSource();

      if (source == languageBox)
      {
         updateXindyModule();
      }
      else if (source == fontBox)
      {
         updateFontSample();
      }
   }

   public void stateChanged(ChangeEvent e)
   {
      if (e.getSource() == fontSizeBox)
      {
         updateFontSample();
      }
   }

   private void updateFontSample()
   {
      int style = Font.PLAIN;

      if (boldBox.isSelected())
      {
         style = Font.BOLD;
      }

      if (italicBox.isSelected())
      {
         style = style | Font.ITALIC;
      }

      fontSample.setFont(new Font((String)fontBox.getSelectedItem(),
        style, ((Number)fontSizeBox.getValue()).intValue()));

      fontSample.repaint();
   }

   public void updateFontSize(int size)
   {
      fontSizeBox.setValue(size);
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

   private JLabel langLabel, fontSample;

   private XindyModulePanel currentModule;

   private JRadioButton homeButton, lastButton, customButton;

   private JComboBox<String> languageBox, fontBox;

   private JComboBox<UIManager.LookAndFeelInfo> lookAndFeelBox;

   private JSpinner fontSizeBox;

   private FileField customField, makeindexField, xindyField;

   private JFileChooser fileChooser;

   private JCheckBox overrideBox, docDefCheckBox, missingLangModBox,
     boldBox, italicBox;

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
      setOpaque(false);

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

class LookAndFeelCellRenderer extends DefaultListCellRenderer
{
   public Component getListCellRendererComponent(JList<?> list,
    Object value, int index, boolean isSelected, boolean cellHasFocus)
   {
      Component comp = super.getListCellRendererComponent(list, value, index,
        isSelected, cellHasFocus);

      if (value instanceof UIManager.LookAndFeelInfo)
      {
         setText(((UIManager.LookAndFeelInfo)value).getName());
      }

      return comp;
   }
}
