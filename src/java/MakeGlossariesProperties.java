package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.util.*;
import java.awt.Font;

import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JToolBar;

public class MakeGlossariesProperties extends Properties
{
   public MakeGlossariesProperties(File propfile, File recentFile)
     throws IOException
   {
      super();

      this.propFile = propfile;
      this.recentFile = recentFile;

      recentList = new Vector<String>();

      BufferedReader in;

      if (propfile != null && propfile.exists())
      {
         in = new BufferedReader(new FileReader(propFile));

         load(in);

         in.close();
      }
      else
      {
         setDefaults();
      }

      if (recentFile != null && recentFile.exists())
      {
         in = new BufferedReader(new FileReader(recentFile));

         loadRecentFiles(in);

         in.close();
      }
   }

   public MakeGlossariesProperties()
   {
      super();

      propFile = null;
      recentFile = null;

      setDefaults();
   }

   protected void setDefaults()
   {
      setDefaultHomeDir();
      setProperty("language", Locale.getDefault().getDisplayLanguage().toLowerCase());
      setProperty("codepage", "utf8");
      setProperty("override", "false");
      setProperty("docdefscheck", "true");
      setProperty("langcheck", "true");
      setProperty("fontsize", "12");
      setProperty("fontname", "Serif");
      setProperty("fontstyle", ""+Font.PLAIN);
      setProperty("toolbar.position", "North");
      setProperty("toolbar.orientation", ""+JToolBar.HORIZONTAL);
   }

   public String getToolBarPosition()
   {
      String prop = getProperty("toolbar.position");

      return prop == null ? "North" : prop;
   }

   public void setToolBarPosition(String position)
   {
      setProperty("toolbar.position", position == null ? "North" : position);
   }

   public int getToolBarOrientation()
   {
      int orientation = JToolBar.HORIZONTAL;

      String prop = getProperty("toolbar.orientation");

      if (prop != null)
      {
         try
         {
            orientation = Integer.parseInt(prop);
         } 
         catch (NumberFormatException e)
         {
         }
      }

      return orientation;
   }

   public void setToolBarOrientation(int orientation)
   {
      setProperty("toolbar.orientation", ""+orientation);
   }

   public int getFontSize()
   {
      String prop = getProperty("fontsize");

      if (prop == null) return 12;

      try
      {
         return Integer.parseInt(prop);
      }
      catch (NumberFormatException e)
      {
      }

      return 12;
   }

   public int getFontStyle()
   {
      String prop = getProperty("fontstyle");

      if (prop == null) return Font.PLAIN;

      try
      {
         return Integer.parseInt(prop);
      }
      catch (NumberFormatException e)
      {
      }

      return Font.PLAIN;
   }

   public String getFontName()
   {
      String prop = getProperty("fontname");

      return prop == null ? "Serif" : prop;
   }

   public void setFontSize(int size)
   {
      setProperty("fontsize", ""+size);
   }

   public void setFontStyle(int style)
   {
      setProperty("fontstyle", ""+style);
   }

   public void setFontName(String name)
   {
      setProperty("fontname", name);
   }

   public String getDefaultXindyVariant()
   {
      String prop = getProperty("variant");

      return "".equals(prop) ? null : prop;
   }

   public void setDefaultXindyVariant(String variant)
   {
      setProperty("variant", variant==null?"":variant);
   }

   public String getDefaultLanguage()
   {
      String prop = getProperty("language");

      return prop == null ? "english" : prop;
   }

   public void setDefaultLanguage(String language)
   {
      setProperty("language", language);
   }

   public String getDefaultCodePage()
   {
      String prop = getProperty("codepage");

      return prop == null ? "utf8" : prop;
   }

   public void setDefaultCodePage(String codepage)
   {
      setProperty("codepage", codepage);
   }

   public void setOverride(boolean override)
   {
      setProperty("override", override?"true":"false");
   }

   public boolean isOverride()
   {
      String prop = getProperty("override");

      return prop == null ? false : Boolean.parseBoolean(prop);
   }

   public void setDocDefsCheck(boolean check)
   {
      setProperty("docdefscheck", check?"true":"false");
   }

   public boolean isDocDefsCheckOn()
   {
      String prop = getProperty("docdefscheck");

      return prop == null ? true : Boolean.parseBoolean(prop);
   }

   public void setMissingLangCheck(boolean check)
   {
      setProperty("langcheck", check?"true":"false");
   }

   public boolean isMissingLangCheckOn()
   {
      String prop = getProperty("langcheck");

      return prop == null ? true : Boolean.parseBoolean(prop);
   }

   public static MakeGlossariesProperties fetchProperties()
      throws IOException
   {
      File parent = getOrMakeConfigDir();
      File settings = null;
      File recent = null;

      if (parent != null)
      {
         settings = new File(parent, propName);
         recent = new File(parent, recentName);
      }

      return new MakeGlossariesProperties(settings, recent);
   }

   public void save(MakeGlossariesGUI app) throws IOException
   {
      File parent = getConfigDir();

      if (parent == null)
      {
         parent = getOrMakeConfigDir();
      }

      if (parent == null)
      {
         throw new IOException("Unable to create properties directory");
      }

      if (propFile == null)
      {
         propFile = new File(parent, propName);
      }

      if (getProperty("directory.setting").equals("last"))
      {
         setProperty("directory", app.getCurrentDirectoryName());
      }

      PrintWriter out = new PrintWriter(new FileWriter(propFile));

      store(out, "makeglossaries-gui properties");

      out.close();

      if (recentFile == null)
      {
         recentFile = new File(parent, recentName);
      }

      out = new PrintWriter(new FileWriter(recentFile));

      for (int i = 0, n = recentList.size(); i < n; i++)
      {
         out.println(recentList.get(i));
      }

      out.close();
   }

   public String getDefaultDirectory()
   {
      String settings = getProperty("directory.setting");

      if (settings == null) settings = "home";

      if (settings.equals("home"))
      {
         setProperty("directory", System.getProperty("user.home"));
      }

      return getProperty("directory");
   }

   public String getDefaultDirectorySetting()
   {
      return getProperty("directory.setting");
   }

   public String getMakeIndexApp()
   {
      String prop = getProperty("makeindex");

      if (prop == null || prop.equals("")) return null;

      return prop;
   }

   public String getXindyApp()
   {
      String prop = getProperty("xindy");

      if (prop == null || prop.equals("")) return null;

      return prop;
   }

   public void setDefaultCustomDir(String dir)
   {
      setProperty("directory", dir);
      setProperty("directory.setting", "custom");
   }

   public void setDefaultHomeDir()
   {
      setProperty("directory", System.getProperty("user.home"));
      setProperty("directory.setting", "home");
   }

   public void setDefaultLastDir()
   {
      setProperty("directory.setting", "last");
   }

   public void setMakeIndexApp(File pathToApp)
   {
      setMakeIndexApp(pathToApp.getAbsolutePath());
   }

   public void setMakeIndexApp(String pathToApp)
   {
      setProperty("makeindex", pathToApp);
   }

   public void setXindyApp(File pathToApp)
   {
      setXindyApp(pathToApp.getAbsolutePath());
   }

   public void setXindyApp(String pathToApp)
   {
      setProperty("xindy", pathToApp);
   }

   private void loadRecentFiles(BufferedReader in)
     throws IOException
   {
      String line;

      while ((line = in.readLine()) != null)
      {
         recentList.add(line);
      }
   }

   public void addRecentFile(String fileName)
   {
      if (fileName == null)
      {
         throw new NullPointerException();
      }

      recentList.remove(fileName); // just in case it's already in the list
      recentList.add(fileName);
   }

   public void setRecentFiles(JMenu menu, ActionListener listener)
   {
      if (recentList == null) return;

      if (menu.getMenuComponentCount() > 0)
      {
         menu.removeAll();
      }

      int lastIdx = recentList.size()-1;

      int n = Math.min(MAX_RECENT_FILES-1, lastIdx);

      for (int i = 0; i <= n; i++)
      {
         File file = new File(recentList.get(lastIdx-i));
         String num = ""+i;
         JMenuItem item = new JMenuItem(num+": "+file.getName());
         item.setMnemonic(num.charAt(0));
         item.setToolTipText(file.getAbsolutePath());
         item.setActionCommand(num);
         item.addActionListener(listener);

         menu.add(item);
      }
   }

   public String getRecentFileName(int i)
   {
      return recentList.get(recentList.size()-1-i);
   }

   public static File getConfigDir()
   {
      File dir = null;

      String env = System.getenv("MAKEGLOSSARIES-SETTINGS");

      if (env != null)
      {
         dir = new File(env);

         if (dir.isDirectory()) return dir;
      }

      String home = System.getProperty("user.home");

      if (home == null) return null;

      dir = new File(home, ".makeglossaries");

      if (dir.isDirectory()) return dir;

      dir = new File(home, "makeglossaries-settings");

      if (dir.isDirectory()) return dir;

      return null;
   }

   public static File getOrMakeConfigDir()
   {
      File dir = null;

      String env = System.getenv("MAKEGLOSSARIES-SETTINGS");

      if (env != null)
      {
         dir = new File(env);

         if (!dir.exists())
         {
            if (dir.mkdir()) return dir;
         }

         if (dir.isDirectory()) return dir;
      }

      String home = System.getProperty("user.home");

      if (home == null) return null;

      File dir2;

      if (System.getProperty("os.name").toLowerCase().indexOf("windows") != -1)
      {
         dir = new File(home, "makeglossaries-settings");
         dir2 = new File(home, ".makeglossaries");
      }
      else
      {
         dir = new File(home, ".makeglossaries");
         dir2 = new File(home, "makeglossaries-settings");
      }

      if (dir.isDirectory()) return dir;

      if (dir2.isDirectory()) return dir2;

      if (dir.mkdir()) return dir;

      if (dir2.mkdir()) return dir2;

      return null;
   }

   private File propFile, recentFile;
   private static String propName = "makeglossaries.prop";
   private static String recentName = "recentfiles";

   private Vector<String> recentList;

   public static final int MAX_RECENT_FILES = 10;
}
