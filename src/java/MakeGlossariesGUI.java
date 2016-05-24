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
  implements ActionListener,MenuListener,GlossaryMessage
{
   public MakeGlossariesGUI(MakeGlossariesInvoker invoker)
   {
      super(invoker.appName);
      this.invoker = invoker;

      invoker.setMessageSystem(this);

      setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

      addWindowListener(new WindowAdapter()
      {
         public void windowClosing(WindowEvent evt)
         {
            quit();
         }
      });

      setIconImage(new ImageIcon(getClass().getResource(
        "/icons/makeglossariesgui-logosmall.png")).getImage());

      try
      {
         initTemplates();
      }
      catch (IOException e)
      {
         error(this, e.getMessage());
      }

      setTransferHandler(new GlsTransferHandler(this));

      toolBar = new JToolBar(invoker.getProperties().getToolBarOrientation());
      getContentPane().add(toolBar, 
        invoker.getProperties().getToolBarPosition());

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

      mainPanel.setTransferHandler(getTransferHandler());

      scrollPane = new JScrollPane(mainPanel);
      scrollPane.setPreferredSize(new Dimension(800,600));

      setFont(new Font(invoker.getProperties().getFontName(),
         invoker.getProperties().getFontStyle(),
         invoker.getProperties().getFontSize()));

      scrollPane.setName(getLabel("main.title"));
      tabbedPane.add(scrollPane, 0);
      tabbedPane.setMnemonicAt(0, getMnemonicInt("main.title"));

      diagnosticArea = new JTextArea();
      diagnosticArea.setEditable(false);
      diagnosticArea.setLineWrap(true);
      diagnosticArea.setWrapStyleWord(true);
      diagnosticArea.setFont(getFont());
      diagnosticArea.setTransferHandler(getTransferHandler());

      diagnosticSP = new JScrollPane(diagnosticArea);

      diagnosticSP.setName(getLabel("diagnostics.title"));
      tabbedPane.add(diagnosticSP, 1);
      tabbedPane.setMnemonicAt(1, getMnemonicInt("diagnostics.title"));

      auxFileFilter = new AuxFileFilter(getLabel("filter.aux"));

      fileChooser = new JFileChooser(
         invoker.getProperties().getDefaultDirectory());
      fileChooser.setFileFilter(auxFileFilter);

      propertiesDialog = new PropertiesDialog(this);

      appSelector = new AppSelector(this);

      pack();
      setLocationRelativeTo(null);
      setVisible(true);

      String xindy = getXindyApp();
      String makeindex = getMakeIndexApp();

      if ((xindy == null || xindy.equals(""))
       &&(makeindex == null || makeindex.equals("")))
      {
         File file = appSelector.findApp("makeindex");

         if (file != null)
         {
            invoker.getProperties().setMakeIndexApp(file);
            propertiesDialog.setMakeIndex(file);
         }

         file = appSelector.findApp("xindy");

         if (file != null)
         {
            invoker.getProperties().setXindyApp(file);
            propertiesDialog.setXindy(file);
         }

         propertiesDialog.display();
      }

      if (invoker.getFileName() != null)
      {
         load(invoker.getFile());
      }
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      try
      {
         int idx = Integer.parseInt(action);

         File file = new File(invoker.getProperties().getRecentFileName(idx));

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
         invoker.getProperties().setFontSize(size);

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
         invoker.getProperties().setFontSize(size);

         updateAll();
      }
      else if (action.equals("editproperties"))
      {
         propertiesDialog.display();
      }
      else if (action.equals("about"))
      {
         String[] str;

         String translator = getLabelOrDef("about.translator_info", null);

         if (translator == null || translator.equals(""))
         {
            str = new String[]
            {
               invoker.appName,
               getLabelWithValues("about.version", invoker.appVersion,
                  invoker.appDate),
               getLabelWithValues("about.copyright", "Nicola L. C. Talbot",
                  "2011/09/16"),
               "http://www.dickimaw-books.com/"
            };
         }
         else
         {
            str = new String[]
            {
               invoker.appName,
               getLabelWithValues("about.version", invoker.appVersion,
                invoker.appDate),
               getLabelWithValues("about.copyright", "Nicola L. C. Talbot",
                "2011/09/16"),
               "http://www.dickimaw-books.com/",
               translator
            };
         }

         JOptionPane.showMessageDialog(this, str,
            getLabelWithValue("about.title", invoker.appName),
            JOptionPane.PLAIN_MESSAGE);
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
         invoker.getProperties().setRecentFiles(recentM, this);
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
         MakeGlossariesProperties properties = invoker.getProperties();

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
      if (file.getName().toLowerCase().endsWith(".tex"))
      {
         int idx = file.getName().length()-4;

         file = new File(file.getParentFile(),
           file.getName().substring(0,idx)+".aux");
      }

      setTitle(getLabelWithValues("app.title", invoker.appName, file.getName()));

      invoker.setFile(file);
      invoker.getProperties().addRecentFile(file.getAbsolutePath());
      reload(file);
   }

   public void reload()
   {
      reload(invoker.getFile());
   }

   public void reload(final File file)
   {
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            invoker.reload(file);
         }
      });
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

      if (invoker.getGlossaries() != null)
      {
         String diagnostics = invoker.getGlossaries().getDiagnostics();

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
      String tooltip = getLabelOrDef(parentLabel+"."+label+".tooltip",
        null);
      String altText = getLabelOrDef(parentLabel+"."+label+".altText", null);

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

   public String getLabelOrDef(String label, String def)
   {
      return invoker.getLabelOrDef(label, def);
   }

   public String getLabel(String label)
   {
      return invoker.getLabel(label);
   }

   public String getLabel(String parent, String label)
   {
      return invoker.getLabel(parent, label);
   }

   public char getMnemonic(String label)
   {
      return invoker.getMnemonic(label);
   }

   public char getMnemonic(String parent, String label)
   {
      return invoker.getMnemonic(parent, label);
   }

   public int getMnemonicInt(String label)
   {
      return invoker.getMnemonicInt(label);
   }

   public int getMnemonicInt(String parent, String label)
   {
      return invoker.getMnemonicInt(parent, label);
   }

   public String getLabelWithValue(String label, String value)
   {
      return invoker.getLabelWithValue(label, value);
   }

   public String getLabelWithValues(String label, String value1,
      String value2)
   {
      return invoker.getLabelWithValues(label, value1, value2);
   }

   public String getDefaultLanguage()
   {
      return invoker.getDefaultLanguage();
   }

   public String getDefaultCodePage()
   {
      return invoker.getDefaultCodePage();
   }

   public JButton createActionButton(String label, ActionListener listener,
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

   public JButton createOkayButton(ActionListener listener)
   {
      return createActionButton("okay", listener, 
         KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), null);
   }

   public JButton createCancelButton(ActionListener listener)
   {
      return createActionButton("cancel", listener, 
         KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), null);
   }

   public void showMessages()
   {
      try
      {
         updateInfoPanel();
      }
      catch (Exception e)
      {
         error(e);
      }

      updateDiagnostics();
   }

   public void aboutToExec(String[] cmdArray, File dir)
   {
      if (invoker.isDebugMode())
      {
         for (int i = 0, n = cmdArray.length-1; i < cmdArray.length; i++)
         {
            if (i == n)
            {
               System.out.println(String.format("\"%s\"", cmdArray[i]));
            }
            else
            {
               System.out.print(String.format("\"%s\" ", cmdArray[i]));
            }
         }
      }
   }

   public void message(String msg)
   {
   }

   public void message(GlossaryException e)
   {
      error(e);
      selectDiagnosticComponent();
   }

   public void debug(String msg)
   {
      if (invoker.isDebugMode())
      {
         System.out.println(msg);
      }
   }

   public void debug(Throwable e)
   {
      if (invoker.isDebugMode())
      {
         e.printStackTrace();
      }
   }

   public void error(Exception e)
   {
      error(this, e.getMessage());
   }

   public void error(String message)
   {
      error(this, message);
   }

   public void error(Component parent, String message)
   {
      JOptionPane.showMessageDialog(parent, message, getLabel("error.title"),
         JOptionPane.ERROR_MESSAGE);
   }

   public void fatalError(Exception e)
   {
      JOptionPane.showMessageDialog(null,
        String.format("%s%n%s",
          getLabelWithValue("error.fatal.info", invoker.appName),
          e.getMessage()),
        getLabel("error.fatal.title"), JOptionPane.ERROR_MESSAGE);

      e.printStackTrace();
      System.exit(1);
   }

   private String loadTemplate(String templateName)
     throws IOException
   {
      InputStream in = 
         getClass().getResourceAsStream("/resources/"+templateName+"-template.html");

      if (in == null)
      {
         throw new IOException("Can't find resources template '"+templateName+"'");
      }

      BufferedReader reader = new BufferedReader(new InputStreamReader(in));

      String line;
      StringBuilder builder = new StringBuilder();

      while ((line = reader.readLine()) != null)
      {
         builder.append(line);
      }

      reader.close();

      in.close();

      return builder.toString();
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
      return invoker.getGlossaries();
   }

   public MakeGlossariesProperties getProperties()
   {
      return invoker.getProperties();
   }

   public String getFileName()
   {
      return invoker.getFileName();
   }

   public File getFile()
   {
      return invoker.getFile();
   }

   public File getCurrentDirectory()
   {
      return invoker.getCurrentDirectory();
   }

   public String getCurrentDirectoryName()
   {
      return invoker.getCurrentDirectoryName();
   }

   public String getXindyApp()
   {
      return invoker.getXindyApp();
   }

   public String getMakeIndexApp()
   {
      return invoker.getMakeIndexApp();
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
      return invoker.getLanguage(language);
   }

   public static void createAndShowGUI(final MakeGlossariesInvoker invoker)
   {
      SwingUtilities.invokeLater(new Runnable()
      {
         public void run()
         {
            try
            {
               new MakeGlossariesGUI(invoker);
            }
            catch (Exception e)
            {
               e.printStackTrace();
               JOptionPane.showMessageDialog(null, e.toString(),
                 "Error", JOptionPane.ERROR_MESSAGE);
            }
         }
      });
   }


   private JFileChooser fileChooser;

   private AuxFileFilter auxFileFilter;

   private GlossariesPanel mainPanel;

   private JTabbedPane tabbedPane;

   private JScrollPane scrollPane, diagnosticSP;

   private JTextArea diagnosticArea;

   private JMenu recentM;

   private PropertiesDialog propertiesDialog;

   private String mainInfoTemplate, glossaryInfoTemplate;

   private JToolBar toolBar;

   private AppSelector appSelector;

   private HelpBroker mainHelpBroker;
   private CSH.DisplayHelpFromSource csh;

   private MakeGlossariesInvoker invoker;
}
