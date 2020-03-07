package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.nio.charset.Charset;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.event.*;
import javax.help.*;

public class MakeGlossariesGUI extends JFrame
  implements ActionListener,MenuListener,GlossaryMessage,
    HyperlinkListener,MouseListener
{
   public MakeGlossariesGUI(MakeGlossariesInvoker invoker)
   {
      super(invoker.APP_NAME);
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
        "/resources/icons/makeglossariesgui-logosmall.png")).getImage());

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

      dryRunItem = createCheckBoxMenuItem("settings", "dryrun", 
         invoker.isDryRunMode());

      settingsM.add(dryRunItem);

      settingsM.add(createMenuButtonItem("settings", "editproperties",
        KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_MASK),
        "general/Preferences24"));

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
      tabbedPane.setMnemonicAt(0, getMnemonic("main.title"));

      diagnosticArea = new DiagnosticPanel(this);

      diagnosticSP = new JScrollPane(diagnosticArea);

      diagnosticSP.setName(getLabel("diagnostics.title"));
      tabbedPane.add(diagnosticSP, 1);
      tabbedPane.setMnemonicAt(1, getMnemonic("diagnostics.title"));

      popupM = new JPopupMenu();

      popupM.add(createMenuItem("popup", "select_all"));

      copyItem = createMenuItem("popup", "copy");
      popupM.add(copyItem);

      addMouseListener(this);

      ActionMap actionMap = tabbedPane.getActionMap();
      InputMap inputMap = tabbedPane.getInputMap(
         JComponent.WHEN_IN_FOCUSED_WINDOW);

      
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTEXT_MENU, 0),
         "popup");
      inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 
         InputEvent.SHIFT_DOWN_MASK),
         "popup");
      actionMap.put("popup", new AbstractAction()
      {
         public void actionPerformed(ActionEvent evt)
         {
            showPopup();
         }
      });

      AbstractAction copyAction = new AbstractAction("copy")
      {
         public void actionPerformed(ActionEvent evt)
         {
            // omit html when copying

            JTextComponent textComp = getSelectedTextComponent();

            if (textComp != null)
            {
               StringSelection sel = new StringSelection(
                 textComp.getSelectedText());
               Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
               cb.setContents(sel, null);
            }
         }
      };

      actionMap.put("copy", copyAction);
      diagnosticArea.getActionMap().put("copy", copyAction);
      mainPanel.getActionMap().put("copy", copyAction);

      fileChooser = new JFileChooser(
         invoker.getProperties().getDefaultDirectory());

      auxFileFilter = new AuxFileFilter(getLabel("filter.aux"));

      try
      {
         // why does this sometimes throw a concurrent exception?
         // It's running on the EDT. Perhaps related to bug:
         // https://bugs.openjdk.java.net/browse/JDK-8068244 

         fileChooser.setFileFilter(auxFileFilter);
      }
      catch (java.util.ConcurrentModificationException e)
      {
         invoker.getMessageSystem().debug(e);
      }

      propertiesDialog = new PropertiesDialog(this);

      appSelector = new AppSelector(this);

      pack();
      diagnosticArea.updateDiagnostics();

      setLocationRelativeTo(null);
      setVisible(true);

      String xindy = getXindyApp();
      String makeindex = getMakeIndexApp();

      if ((xindy == null || xindy.equals(""))
       &&(makeindex == null || makeindex.equals("")))
      {
         File file = findApplication("makeindex");

         if (file != null)
         {
            invoker.getProperties().setMakeIndexApp(file);
            propertiesDialog.setMakeIndex(file);
         }

         file = findApplication("xindy");

         if (file != null)
         {
            invoker.getProperties().setXindyApp(file);
            propertiesDialog.setXindy(file);
         }
      }

      if (invoker.getFileName() != null)
      {
         load(invoker.getFile());
      }
   }

   public File findApplication(String name)
   {
      return invoker.findApp(name);
   }

   public File findApplication(String name, String altName, String altName2)
   {
      return invoker.findApp(name, altName, altName2);
   }

   public void hyperlinkUpdate(HyperlinkEvent evt)
   {
      Desktop desktop = Desktop.isDesktopSupported() ?
                        Desktop.getDesktop() : null;

      if (desktop == null)
      {
         debug("desktop not supported");
         return;
      }

      HyperlinkEvent.EventType type = evt.getEventType();

      if (evt instanceof FormSubmitEvent)
      {
         if (type == HyperlinkEvent.EventType.ACTIVATED)
         {
            // The data should only consist of one name=value
            // and only the name is important.

            String data = ((FormSubmitEvent)evt).getData();

            if (data.startsWith("makeglossaries-lite="))
            {
               testMakeGlossariesLite();
            }
            else if (data.startsWith("makeglossaries="))
            {
               testMakeGlossaries();
            }
            else if (data.startsWith("cleartestresults="))
            {
               makeglossariesTestResults=null;
               makeglossariesLiteTestResults=null;
               updateDiagnostics();
            }
            else
            {
               error(invoker.getLabel("error.invalid_query", data));
            }
         }

         return;
      }

      URL url = evt.getURL();

      if (url == null)
      {
         return;
      }

      Object source = evt.getSource();

      if (type == HyperlinkEvent.EventType.ACTIVATED)
      {
         if (desktop.isSupported(Desktop.Action.BROWSE))
         {
            try
            {
               desktop.browse(url.toURI());
            }
            catch (Exception e)
            {
               debug(e);
            }
         }
      }
      else if (type == HyperlinkEvent.EventType.ENTERED)
      {
         if (source instanceof JComponent)
         {
            ((JComponent)source).setToolTipText(url.toString());
         }
      }
      else if (type == HyperlinkEvent.EventType.EXITED)
      {
         if (source instanceof JComponent)
         {
            ((JComponent)source).setToolTipText(null);
         }
      }
   }

   public JTextComponent getSelectedTextComponent()
   {
      Component tab = tabbedPane.getSelectedComponent();

      if (tab instanceof JScrollPane)
      {
         tab = ((JScrollPane)tab).getViewport().getView();
      }

      if (tab instanceof JTextComponent)
      {
         return (JTextComponent)tab;
      }

      return null;
   }

   public void showPopup()
   {
      JTextComponent textComp = getSelectedTextComponent();

      if (textComp != null)
      {
         copyItem.setEnabled(textComp.getSelectedText() != null);

         int x = 0;
         int y = 0;

         try
         {
            Point p = textComp.getMousePosition();

            if (p != null)
            {
               x = (int)p.getX();
               y = (int)p.getY();
            }
         }
         catch (HeadlessException e)
         {
            debug(e);
         }

         popupM.show(textComp, x, y);
      }
   }

   public void showPopup(Component comp, int x, int y)
   {
      JTextComponent textComp = getSelectedTextComponent();

      if (textComp != null)
      {
         copyItem.setEnabled(textComp.getSelectedText() != null);

         popupM.show(comp, x, y);
      }
   }

   public void mousePressed(MouseEvent evt)
   {
      checkForPopupTrigger(evt);
   }

   public void mouseReleased(MouseEvent evt)
   {
      checkForPopupTrigger(evt);
   }

   public void mouseExited(MouseEvent evt)
   {
   }

   public void mouseEntered(MouseEvent evt)
   {
   }

   public void mouseClicked(MouseEvent evt)
   {
   }

   private boolean checkForPopupTrigger(MouseEvent evt)
   {
      if (evt.isPopupTrigger())
      {
         showPopup(evt.getComponent(), evt.getX(), evt.getY());

         return true;
      }

      return false;
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

         return;
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

         invoker.getProperties().setFontSize(size);
         propertiesDialog.updateFontSize(size);

         updateFont();
      }
      else if (action.equals("decsize"))
      {
         Font font = getFont();
         int size = font.getSize()-1;

         if (size < 2)
         {
            return;
         }

         invoker.getProperties().setFontSize(size);
         propertiesDialog.updateFontSize(size);

         updateFont();
      }
      else if (action.equals("dryrun"))
      {
         invoker.setDryRunMode(dryRunItem.isSelected());
      }
      else if (action.equals("editproperties"))
      {
         propertiesDialog.display();
      }
      else if (action.equals("select_all"))
      {
         JTextComponent textComp = getSelectedTextComponent();

         if (textComp != null)
         {
            textComp.requestFocusInWindow();
            textComp.selectAll();
         }
      }
      else if (action.equals("copy"))
      {
         JTextComponent textComp = getSelectedTextComponent();

         if (textComp != null)
         {
            textComp.requestFocusInWindow();
            textComp.copy();
         }
      }
      else if (action.equals("about"))
      {
         String[] str;

         String translator = getLabelOrDef("about.translator_info", null);

         if (translator == null || translator.equals(""))
         {
            str = new String[]
            {
               invoker.APP_NAME,
               getLabelWithValues("about.version", invoker.APP_VERSION,
                  invoker.APP_DATE),
               getLabelWithValues("about.copyright", "Nicola L. C. Talbot",
                  String.format("2011-%s", invoker.APP_DATE.substring(0,4))),
               "http://www.dickimaw-books.com/"
            };
         }
         else
         {
            str = new String[]
            {
               invoker.APP_NAME,
               getLabelWithValues("about.version", invoker.APP_VERSION,
                invoker.APP_DATE),
               getLabelWithValues("about.copyright", "Nicola L. C. Talbot",
                String.format("2011-%s", invoker.APP_DATE.substring(0,4))),
               "http://www.dickimaw-books.com/",
               translator
            };
         }

         JOptionPane.showMessageDialog(this, str,
            getLabelWithValues("about.title", invoker.APP_NAME),
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
      String lc = file.getName().toLowerCase();

      if (lc.endsWith(".tex") || lc.endsWith(".log"))
      {
         int idx = file.getName().length()-4;

         file = new File(file.getParentFile(),
           file.getName().substring(0,idx)+".aux");
      }

      setTitle(getLabelWithValues("app.title", 
        invoker.APP_NAME, file.getName()));

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

   public void updateFont()
   {
      MakeGlossariesProperties properties = invoker.getProperties();

      Font f = new Font(properties.getFontName(),
        properties.getFontStyle(), properties.getFontSize());

      setFont(f);

      updateAll();
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
         error(e);
      }
   }

   public void updateDiagnostics()
   {
      diagnosticArea.updateDiagnostics();
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

   private JCheckBoxMenuItem createCheckBoxMenuItem(String parentLabel,
      String label, boolean isSelected)
   {
      return createCheckBoxMenuItem(parentLabel, label, null, null, this,
        isSelected);
   }

   private JCheckBoxMenuItem createCheckBoxMenuItem(String parentLabel,
      String label, KeyStroke keyStroke, String tooltip,
      ActionListener listener, boolean isSelected)
   {
      JCheckBoxMenuItem item = new JCheckBoxMenuItem(
         getLabel(parentLabel, label), isSelected);
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

   public MakeGlossariesInvoker getInvoker()
   {
      return invoker;
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

   public int getMnemonic(String label)
   {
      return invoker.getMnemonic(label);
   }

   public int getMnemonic(String parent, String label)
   {
      return invoker.getMnemonic(parent, label);
   }

   public String getLabelWithValues(String label, Object... args)
   {
      return invoker.getLabelWithValues(label, args);
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

   private void testMakeGlossaries()
   {
      makeglossariesTestResults=null;

      StringBuilder results = new StringBuilder();

      try
      {
         int exitCode = testApplication("perl", "perl.exe", null, results);

         if (exitCode == -1)
         {
            results.append(invoker.getLabelWithValues("error.no_perl",
              "makeglossaries"));
         }
         else
         {
            results.append("<p>");

            exitCode = testApplication("makeglossaries", "makeglossaries.exe",
               "makeglossaries.bat", results);

            if (exitCode == -1)
            {
               results.append(invoker.getLabelWithValues(
                 "error.no_makeglossaries", "makeglossaries"));
            }
         }
      }
      catch (Exception e)
      {
         results.append("<p>");
         results.append(invoker.escapeHTML(e.getMessage()));
      }

      makeglossariesTestResults=results.toString();

      updateDiagnostics();
   }

   private void testMakeGlossariesLite()
   {
      makeglossariesLiteTestResults=null;
      StringBuilder results = new StringBuilder();

      try
      {
         int exitCode = testApplication("makeglossaries-lite", 
           "makeglossaries-lite.exe", "makeglossaries-lite.lua", results);

         if (exitCode == -1)
         {
            results.append(invoker.getLabelWithValues(
              "error.no_makeglossaries", "makeglossaries-lite"));
            results.append(invoker.getLabelWithValues(
              "diagnostics.at_least_version", "4.16", "glossaries.sty"));
         }
      }
      catch (Exception e)
      {
         results.append("<p>");
         results.append(invoker.escapeHTML(e.getMessage()));
      }

      makeglossariesLiteTestResults=results.toString();

      updateDiagnostics();
   }

   private int testApplication(String name, String altName1, String altName2,
     StringBuilder results)
    throws IOException,InterruptedException
   {
      File file = invoker.findApp(name, altName1, altName2);

      if (file == null)
      {
         results.append(invoker.getLabelWithValues("error.missing_application",
           name));
         results.append("<p>");
         return -1;
      }

      results.append(String.format("%s --version<p>", file));

      ProcessBuilder pb = new ProcessBuilder(file.getAbsolutePath(), 
        "--version");
      pb.redirectErrorStream(true);
      Process p = pb.start();

      int exitCode = p.waitFor();

      InputStream stream = p.getInputStream();

      if (stream == null)
      {
         results.append("Unable to open input stream from process.<p>");
         return exitCode;
      }

      results.append("<pre>");

      BufferedReader reader = null;

      try
      {
         reader = new BufferedReader(new InputStreamReader(stream));

         String line;

         while ((line = reader.readLine()) != null)
         {
            results.append(String.format("%s%n", invoker.escapeHTML(line)));
         }
      }
      finally
      {
         results.append("</pre>");

         if (reader != null)
         {
            reader.close();
         }
      }

      if (exitCode == 0)
      {
         results.append(invoker.getLabelWithValues(
           "diagnostics.test_successful", file.getName()));
         results.append("<p>");
      }

      return exitCode;
   }

   public String getScriptTestResults()
   {
      if (makeglossariesTestResults == null
         && makeglossariesLiteTestResults == null)
      {
         return null;
      }

      StringBuilder builder = new StringBuilder();

      builder.append("<dl>");

      if (makeglossariesTestResults != null)
      {
         builder.append(String.format(
            "<dt>makeglossaries</dt><dd>%s</dd>", 
            makeglossariesTestResults));
      }

      if (makeglossariesLiteTestResults != null)
      {
         builder.append(String.format(
            "<dt>makeglossaries-lite</dt><dd>%s</dd>", 
            makeglossariesLiteTestResults));
      }

      builder.append("</dl>");

      return builder.toString();
   }

   public String diagnosticsForm()
   {
      StringBuilder builder = new StringBuilder(String.format(
         "%s<p><form action=\"#\">", invoker.getLabel("diagnostics.build")));

      if (makeglossariesTestResults == null 
           && makeglossariesLiteTestResults == null)
      {
         builder.append(invoker.getLabelWithValues("diagnostics.query_two",
             diagnosticsActionButton("makeglossaries"),
             diagnosticsActionButton("makeglossaries-lite")));
      }
      else if (makeglossariesTestResults == null)
      {
         builder.append(invoker.getLabelWithValues("diagnostics.query_one",
             diagnosticsActionButton("makeglossaries")));
      }
      else if (makeglossariesLiteTestResults == null)
      {
         builder.append(invoker.getLabelWithValues("diagnostics.query_one",
             diagnosticsActionButton("makeglossaries-lite")));
      }

      if (makeglossariesTestResults != null 
           || makeglossariesLiteTestResults != null)
      {
          builder.append(String.format(
           "<p><input type=submit name=\"cleartestresults\" value=\"%s\">",
             invoker.getLabel("diagnostics.clear_test_results")
          ));
      }

      builder.append("</form>");

      return builder.toString();
   }

   private String diagnosticsActionButton(String name)
   {
      return String.format(
        "<input type=submit name=\"%s\" value=\"%s\">",
        name, invoker.getLabelWithValues("diagnostics.test_script", name)
      );
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

      if (invoker.isDryRunMode())
      {
        StringBuilder builder = new StringBuilder();

         for (int i = 0, n = cmdArray.length-1; i < cmdArray.length; i++)
         {
            if (i == n)
            {
               builder.append(String.format("\"%s\"", cmdArray[i]));
            }
            else
            {
               builder.append(String.format("\"%s\" ", cmdArray[i]));
            }
         }

         invoker.getGlossaries().addDiagnosticMessage(
            String.format("%s<pre>%s</pre>", invoker.getLabel(
            "diagnostics.dry_run"), builder.toString()));
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
      String msg = e.getMessage();

      if (msg == null || msg.isEmpty())
      {
         msg = e.getClass().getName();
      }

      error(this, msg);

      if (invoker.isDebugMode())
      {
         e.printStackTrace();
      }
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

   public void error(Component parent, Exception e)
   {
      String msg = e.getMessage();

      if (msg == null || msg.isEmpty())
      {
         msg = e.getClass().getName();
      }

      error(parent, msg);

      if (invoker.isDebugMode())
      {
         e.printStackTrace();
      }
   }

   public void fatalError(Exception e)
   {
      JOptionPane.showMessageDialog(null,
        String.format("%s%n%s",
          getLabelWithValues("error.fatal.info", invoker.APP_NAME),
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

   public Charset getEncoding()
   {
      return invoker.getEncoding();
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
      String lookAndFeel = invoker.getProperties().getLookAndFeel();

      if (lookAndFeel != null && !lookAndFeel.isEmpty())
      {
         try
         {
            UIManager.setLookAndFeel(lookAndFeel);
         }
         catch (Exception e)
         {
            invoker.getMessageSystem().debug(e);
         }
      }

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

   private DiagnosticPanel diagnosticArea;

   private JPopupMenu popupM;

   private JMenuItem copyItem;

   private JMenu recentM;

   private JCheckBoxMenuItem dryRunItem;

   private PropertiesDialog propertiesDialog;

   private String mainInfoTemplate, glossaryInfoTemplate;

   private String makeglossariesTestResults=null;
   private String makeglossariesLiteTestResults=null;

   private JToolBar toolBar;

   private AppSelector appSelector;

   private HelpBroker mainHelpBroker;
   private CSH.DisplayHelpFromSource csh;

   private MakeGlossariesInvoker invoker;
}
