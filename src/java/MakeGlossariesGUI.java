package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.help.*;

public class MakeGlossariesGUI extends JFrame
  implements ActionListener,MenuListener
{
   public MakeGlossariesGUI()
   {
      super(appName);

      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent evt)
         {
            quit();
         }
      });

      try
      {
         loadDictionary();
      }
      catch (IOException e)
      {
         error(this, "Unable to load dictionary file:\n"
            +e.getMessage());
      }

      setIconImage(new ImageIcon(getClass().getResource("/icons/makeglossariesgui-logosmall.png")).getImage());

      try
      {
         initTemplates();
      }
      catch (IOException e)
      {
         error(this, e.getMessage());
      }

      initLanguageMappings();

      try
      {
         properties = MakeGlossariesProperties.fetchProperties();
      }
      catch (IOException e)
      {
         error(this, "Unable to load properties:\n"+e.getMessage());
         properties = new MakeGlossariesProperties();
      }

      toolBar = new JToolBar(properties.getToolBarOrientation());
      getContentPane().add(toolBar, properties.getToolBarPosition());

      JMenuBar mBar = new JMenuBar();
      setJMenuBar(mBar);

      JMenu fileM = createMenu("file");
      mBar.add(fileM);

      recentM = createMenu("file", "recent");
      recentM.addMenuListener(this);
      fileM.add(recentM);

      fileM.add(createMenuButtonItem("file", "open",
         KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK),
         "general/Open24"));
      fileM.add(createMenuButtonItem("file", "reload",
         KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK),
         "general/Refresh24"));
      fileM.add(createMenuItem("file", "quit",
         KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK)));

      JMenu settingsM = createMenu("settings");
      mBar.add(settingsM);

      settingsM.add(createMenuButtonItem("settings", "incsize",
         KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_MASK),
         "general/ZoomIn24"));

      settingsM.add(createMenuButtonItem("settings", "decsize",
         KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK),
         "general/ZoomOut24"));

      settingsM.add(createMenuItem("settings", "editproperties"));

      initHelp();

      JMenu helpM = createMenu("help");
      mBar.add(helpM);

      helpM.add(createMenuItem("help", "about"));
      helpM.add(createMenuItem("help", "license"));
      helpM.add(createMenuButtonItem("help", "manual",
         KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0),
         "general/Help24", csh));

      tabbedPane = new JTabbedPane();
      getContentPane().add(tabbedPane, "Center");

      try
      {
         mainPanel = new GlossariesPanel(this);
      }
      catch (Exception e)
      {
         fatalError(e);
      }

      scrollPane = new JScrollPane(mainPanel);
      scrollPane.setPreferredSize(new Dimension(800,600));

      setFont(new Font(properties.getFontName(),
         properties.getFontStyle(),
         properties.getFontSize()));

      scrollPane.setName(getLabel("main.title"));
      tabbedPane.add(scrollPane, 0);
      tabbedPane.setMnemonicAt(0, getMnemonicInt("main.title"));

      diagnosticArea = new JTextArea();
      diagnosticArea.setEditable(false);
      diagnosticArea.setLineWrap(true);
      diagnosticArea.setWrapStyleWord(true);
      diagnosticArea.setFont(getFont());

      diagnosticSP = new JScrollPane(diagnosticArea);

      diagnosticSP.setName(getLabel("diagnostics.title"));
      tabbedPane.add(diagnosticSP, 1);
      tabbedPane.setMnemonicAt(1, getMnemonicInt("diagnostics.title"));

      auxFileFilter = new AuxFileFilter(getLabel("filter.aux"));

      fileChooser = new JFileChooser(properties.getDefaultDirectory());
      fileChooser.setFileFilter(auxFileFilter);

      propertiesDialog = new PropertiesDialog(this);

      pack();
      setLocationRelativeTo(null);
      setVisible(true);

      String xindy = getXindyApp();
      String makeindex = getMakeIndexApp();

      if ((xindy == null || xindy.equals(""))
       &&(makeindex == null || makeindex.equals("")))
      {
         propertiesDialog.display();
      }
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      try
      {
         int idx = Integer.parseInt(action);

         File file = new File(properties.getRecentFileName(idx));

         fileChooser.setCurrentDirectory(file.getParentFile());

         load(file);
      }
      catch (NumberFormatException e)
      {
      }

      if (action.equals("open"))
      {
         if (fileChooser.showOpenDialog(this)
           == JFileChooser.APPROVE_OPTION)
         {
            load(fileChooser.getSelectedFile());
         }
      }
      else if (action.equals("reload"))
      {
         reload();
      }
      else if (action.equals("quit"))
      {
         quit();
      }
      else if (action.equals("incsize"))
      {
         Font font = getFont();

         int size = font.getSize()+1;
         int style = font.getStyle();
         String name = font.getFontName();

         setFont(new Font(name, style, size));
         properties.setFontSize(size);

         updateAll();
      }
      else if (action.equals("decsize"))
      {
         Font font = getFont();
         int size = font.getSize()-1;
         int style = font.getStyle();
         String name = font.getFontName();

         if (size < 2)
         {
            return;
         }

         setFont(new Font(name, style, size));
         properties.setFontSize(size);

         updateAll();
      }
      else if (action.equals("editproperties"))
      {
         propertiesDialog.display();
      }
      else if (action.equals("about"))
      {
         String[] str;

         String translator = dictionary.getProperty("about.translator_info");

         if (translator == null || translator.equals(""))
         {
            str = new String[]
            {
               appName,
               getLabelWithValue("about.version", appVersion),
               getLabelWithValues("about.copyright", "Nicola L. C. Talbot", "2011/09/16"),
               "http://theoval.cmp.uea.ac.uk/~nlct/"
            };
         }
         else
         {
            str = new String[]
            {
               appName,
               getLabelWithValue("about.version", appVersion),
               getLabelWithValues("about.copyright", "Nicola L. C. Talbot", "2011/09/16"),
               "http://theoval.cmp.uea.ac.uk/~nlct/",
               translator
            };
         }

         JOptionPane.showMessageDialog(this, str,
            getLabelWithValue("about.title", appName), JOptionPane.PLAIN_MESSAGE);
      }
      else if (action.equals("license"))
      {
         URL url = getClass().getResource("/resources/LICENSE");

         if (url != null)
         {
            try
            {
               new ViewFile(url, getFont(), this).setVisible(true);
            }
            catch (Exception e)
            {
               error(this, e.getMessage());
            }
         }
         else
         {
            error(this, "Can't locate resources/LICENSE");
         }
      }
   }

   public void menuSelected(MenuEvent evt)
   {
      Object source = evt.getSource();

      if (source == recentM)
      {
         properties.setRecentFiles(recentM, this);
      }
   }

   public void menuDeselected(MenuEvent evt)
   {
   }

   public void menuCanceled(MenuEvent evt)
   {
   }

   public void quit()
   {
      try
      {
         properties.setToolBarPosition((String)
            ((BorderLayout)getContentPane().getLayout()).getConstraints(toolBar));
         properties.setToolBarOrientation(toolBar.getOrientation());
         properties.save(this);
      }
      catch (IOException e)
      {
         error(this, "Unable to save properties:\n"+e.getMessage());
      }

      System.exit(0);
   }

   public void load(File file)
   {
      currentFileName = file.getAbsolutePath();

      setTitle(appName+" - "+file.getName());

      properties.addRecentFile(currentFileName);

      reload(file);
   }

   public void reload()
   {
      reload(new File(currentFileName));
   }

   public void reload(File file)
   {
      Thread thread = new ProcessThread(this, file);

      thread.start();

      thread = null;
   }

   public void updateAll()
   {
      Component comp = tabbedPane.getSelectedComponent();
      updateDiagnostics();
      tabbedPane.setSelectedComponent(comp);

      try
      {
         mainPanel.updateInfo();
      }
      catch (Exception e)
      {
         error(this, e.getMessage());
      }
   }

   public void updateDiagnostics()
   {
      diagnosticArea.setFont(getFont());

      if (glossaries != null)
      {
         String diagnostics = glossaries.getDiagnostics();

         if (diagnostics == null)
         {
            diagnosticArea.setText(getLabel("diagnostics.no_errors"));
         }
         else
         {
            diagnosticArea.setText(diagnostics);
            tabbedPane.setSelectedComponent(diagnosticSP);
         }
      }
   }

   private JMenu createMenu(String label)
   {
      return createMenu(null, label);
   }

   private JMenu createMenu(String parentLabel, String label)
   {
      JMenu menu = new JMenu(getLabel(parentLabel, label));
      menu.setMnemonic(getMnemonic(parentLabel, label));

      return menu;
   }

   private JMenuItem createMenuItem(String parentLabel, String label)
   {
      return createMenuItem(parentLabel, label, null, null, this);
   }

   private JMenuItem createMenuItem(String parentLabel, String label, KeyStroke keyStroke)
   {
      return createMenuItem(parentLabel, label, keyStroke, null, this);
   }

   private JMenuItem createMenuItem(String parentLabel, String label, String tooltip)
   {
      return createMenuItem(parentLabel, label, null, tooltip, this);
   }

   private JMenuItem createMenuItem(String parentLabel, String label,
      KeyStroke keyStroke, String tooltip)
   {
      return createMenuItem(parentLabel, label, keyStroke, tooltip, this);
   }

   private JMenuItem createMenuItem(String parentLabel, String label,
      KeyStroke keyStroke, String tooltip, ActionListener listener)
   {
      JMenuItem item = new JMenuItem(getLabel(parentLabel, label));
      item.setMnemonic(getMnemonic(parentLabel, label));
      item.setActionCommand(label);

      if (listener != null)
      {
         item.addActionListener(listener);
      }

      if (keyStroke != null)
      {
         item.setAccelerator(keyStroke);
      }

      if (tooltip != null)
      {
         item.setToolTipText(tooltip);
      }

      return item;
   }

   private JMenuItem createMenuButtonItem(String parentLabel, String label,
      KeyStroke keyStroke, String imageName)
   {
      return createMenuButtonItem(parentLabel, label, keyStroke, imageName, this);
   }

   private JMenuItem createMenuButtonItem(String parentLabel, String label,
      KeyStroke keyStroke, String imageName, ActionListener listener)
   {
      String tooltip = dictionary.getProperty(parentLabel+"."+label+".tooltip");
      String altText = dictionary.getProperty(parentLabel+"."+label+".altText");

      JMenuItem item = createMenuItem(parentLabel, label, keyStroke, tooltip, listener);

      String imgLocation = "/toolbarButtonGraphics/"+imageName+".gif";

      URL imageURL = getClass().getResource(imgLocation);

      JButton button = new JButton();
      button.setActionCommand(label);

      if (listener != null)
      {
         button.addActionListener(listener);
      }

      if (tooltip != null)
      {
         button.setToolTipText(tooltip);
      }

      if (imageURL != null)
      {
         button.setIcon(new ImageIcon(imageURL, altText));
      }
      else
      {
         button.setText(altText);
         error(this, "Resource not found:\n"+imgLocation);
      }

      toolBar.add(button);

      return item;
   }

   public static String getLabel(String label)
   {
      return getLabel(null, label);
   }

   public static String getLabel(String parent, String label)
   {
      if (parent != null)
      {
         label = parent+"."+label;
      }

      String prop = dictionary.getProperty(label);

      if (prop == null)
      {
         System.err.println("No such dictionary property '"+label+"'");
         return "?"+label+"?";
      }

      return prop;
   }

   public static char getMnemonic(String label)
   {
      return getMnemonic(null, label);
   }

   public static char getMnemonic(String parent, String label)
   {
      String prop = getLabel(parent, label+".mnemonic");

      if (prop.equals(""))
      {
         System.err.println("Empty dictionary property '"+prop+"'");
         return label.charAt(0);
      }

      return prop.charAt(0);
   }

   public static int getMnemonicInt(String label)
   {
      return getMnemonicInt(null, label);
   }

   public static int getMnemonicInt(String parent, String label)
   {
      String prop = getLabel(parent, label+".mnemonic");

      if (prop.equals(""))
      {
         System.err.println("Empty dictionary property '"+prop+"'");
         return label.codePointAt(0);
      }

      return prop.codePointAt(0);
   }

   public static String getLabelWithValue(String label, String value)
   {
      String prop = getLabel(label);

      return prop.replaceAll("\\$1", value);
   }

   public static String getLabelWithValues(String label, String value1,
      String value2)
   {
      String prop = getLabel(label);

      return prop.replaceAll("\\$1", value1).replaceAll("\\$2", value2);
   }

   public int getFontSize()
   {
      return getFont().getSize();
   }

   public String getDefaultLanguage()
   {
      return properties.getDefaultLanguage();
   }

   public String getDefaultCodePage()
   {
      return properties.getDefaultCodePage();
   }

   public static JButton createActionButton(String label, ActionListener listener,
     KeyStroke keyStroke, String toolTipText)
   {
      JButton button = new JButton(getLabel("button", label));
      button.setMnemonic(getMnemonic("button", label));
      button.setActionCommand(label);

      if (listener != null)
      {
         button.addActionListener(listener);

         if (keyStroke != null)
         {
            button.registerKeyboardAction(listener, label, keyStroke,
               JComponent.WHEN_IN_FOCUSED_WINDOW);
         }
      }

      if (toolTipText != null)
      {
         button.setToolTipText(toolTipText);
      }

      return button;
   }

   public static JButton createOkayButton(ActionListener listener)
   {
      return createActionButton("okay", listener, 
         KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), null);
   }

   public static JButton createCancelButton(ActionListener listener)
   {
      return createActionButton("cancel", listener, 
         KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null);
   }

   public static void error(Component parent, String message)
   {
      JOptionPane.showMessageDialog(parent, message, getLabel("error.title"),
         JOptionPane.ERROR_MESSAGE);
   }

   public static void fatalError(Exception e)
   {
      JOptionPane.showMessageDialog(null,
        getLabelWithValue("error.fatal.info", appName)+"\n"+e.getMessage(),
        getLabel("error.fatal.title"), JOptionPane.ERROR_MESSAGE);

      e.printStackTrace();
      System.exit(1);
   }

   private void loadDictionary()
      throws IOException
   {
      Locale locale = Locale.getDefault();

      String lang = locale.getLanguage();

      InputStream in = 
         getClass().getResourceAsStream("/resources/dictionaries/makeglossariesgui-"+lang+".prop");

      if (in == null && !lang.equals("en"))
      {
         in = getClass().getResourceAsStream("/resources/dictionaries/makeglossariesgui-en.prop");
      }

      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      dictionary = new Properties();
      dictionary.load(reader);

      reader.close();

      in.close();
   }

   private String loadTemplate(String templateName)
     throws IOException
   {
      String templateContents = "";

      InputStream in = 
         getClass().getResourceAsStream("/resources/"+templateName+"-template.html");

      if (in == null)
      {
         throw new IOException("Can't find resources template '"+templateName+"'");
      }

      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      String line;

      while ((line = reader.readLine()) != null)
      {
         templateContents += line;
      }

      reader.close();

      in.close();

      return templateContents;
   }

   private void initTemplates()
      throws IOException
   {
      mainInfoTemplate = loadTemplate("information");
      glossaryInfoTemplate  = loadTemplate("glossary-info");
   }

   public String getMainInfoTemplate()
   {
      return mainInfoTemplate;
   }

   public String getGlossaryInfoTemplate()
   {
      return glossaryInfoTemplate;
   }

   public String getGlossaryInfoTemplate(String label)
   {
      return glossaryInfoTemplate.replaceAll("-LABEL", "-"+label);
   }

   public Glossaries getGlossaries()
   {
      return glossaries;
   }

   public MakeGlossariesProperties getProperties()
   {
      return properties;
   }

   public String getFileName()
   {
      return currentFileName;
   }

   public File getFile()
   {
      return new File(currentFileName);
   }

   public File getCurrentDirectory()
   {
      File file = getFile().getParentFile();

      if (file == null)
      {
         file = new File(properties.getDefaultDirectory());
      }

      return file;
   }

   public String getCurrentDirectoryName()
   {
      return getCurrentDirectory().getAbsolutePath();
   }

   public String getXindyApp()
   {
      return properties.getXindyApp();
   }

   public String getMakeIndexApp()
   {
      return properties.getMakeIndexApp();
   }

   public boolean useGermanWordOrdering()
   {
      return properties.useGermanWordOrdering();
   }

   public void selectDiagnosticComponent()
   {
      tabbedPane.setSelectedComponent(diagnosticSP);
   }

   public void updateInfoPanel()
      throws BadLocationException,IOException
   {
      mainPanel.updateInfo();
   }

   private void initLanguageMappings()
   {
      languageMap = new Hashtable<String,String>();

      languageMap.put("american", "english");
      languageMap.put("british", "english");
      languageMap.put("francais", "french");
      languageMap.put("frenchb", "french");
      languageMap.put("germanb", "german");
      languageMap.put("magyar", "hungarian");
      languageMap.put("ngermanb", "german");
      languageMap.put("norsk", "norwegian");
      languageMap.put("portuges", "portuguese");
      languageMap.put("russianb", "russian");
      languageMap.put("UKenglish", "english");
      languageMap.put("ukraineb", "ukrainian");
      languageMap.put("USenglish", "english");
      languageMap.put("usorbian", "upper-sorbian");
   }

   private void initHelp()
   {
      if (mainHelpBroker == null)
      {
         HelpSet mainHelpSet = null;

         String helpsetLocation = "/resources/helpsets/makeglossariesgui";

         String lang = Locale.getDefault().getLanguage();

         try
         {
            URL hsURL = getClass().getResource(helpsetLocation+"-"+lang+"/makeglossariesgui.hs");

            if (hsURL == null && !lang.equals("en"))
            {
               hsURL = getClass().getResource(helpsetLocation+"-en/makeglossariesgui.hs");
            }

            mainHelpSet = new HelpSet(null, hsURL);
         }
         catch (Exception e)
         {
            error(this, "/resources/helpsets/makeglossariesgui.hs\n"+
              getLabel("error.io.helpset")+":\n" +e.getMessage());
         }

         if (mainHelpSet != null)
         {
            mainHelpBroker = mainHelpSet.createHelpBroker();
         }

         if (mainHelpBroker != null)
         {
            csh = new CSH.DisplayHelpFromSource(mainHelpBroker);
         }
      }
   }

   public void enableHelpOnButton(JComponent comp, String id)
   {
      if (mainHelpBroker != null)
      {
         try
         {
            mainHelpBroker.enableHelpOnButton(comp, id,
               mainHelpBroker.getHelpSet());
         }
         catch (BadIDException e)
         {
            error(this, e.getMessage());
            e.printStackTrace();
         }
      }
   }

   public String getLanguage(String language)
   {
      String map = languageMap.get(language);

      return map == null ? language : map;
   }

   public static void main(String[] args)
   {
      new MakeGlossariesGUI();
   }

   private JFileChooser fileChooser;

   private AuxFileFilter auxFileFilter;

   private String currentFileName = null;

   private MakeGlossariesProperties properties;

   private static Properties dictionary;

   private static final String appName = "MakeGlossariesGUI";

   private static final String appVersion = "1.0";

   private static final String appDate = "2016-05-17";

   private GlossariesPanel mainPanel;

   private JTabbedPane tabbedPane;

   private JScrollPane scrollPane, diagnosticSP;

   private JTextArea diagnosticArea;

   protected Glossaries glossaries;

   private JMenu recentM;

   private PropertiesDialog propertiesDialog;

   private String mainInfoTemplate, glossaryInfoTemplate;

   private Hashtable<String,String> languageMap;

   private JToolBar toolBar;

   private HelpBroker mainHelpBroker;
   private CSH.DisplayHelpFromSource csh;
}
