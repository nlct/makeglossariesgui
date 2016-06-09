package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.net.*;
import java.util.*;

public class MakeGlossariesInvoker
{
   public MakeGlossariesInvoker()
   {
      setMessageSystem(new GlossaryBatchMessage(this));

      try
      {
         loadDictionary();
      }
      catch (IOException e)
      {
         getMessageSystem().error(
           String.format("Unable to load dictionary file:%n%s",
            e.getMessage()));
      }

      initLanguageMappings();

      try
      {
         properties = MakeGlossariesProperties.fetchProperties();
      }
      catch (IOException e)
      {
         getMessageSystem().error(
           getLabelWithValue("error.prop_io", e.getMessage()));
         properties = new MakeGlossariesProperties();
      }
   }

   public void setFile(File file)
   {
      setFile(file.getAbsolutePath());
   }

   public void setFile(String filename)
   {
      if (filename.toLowerCase().endsWith(".tex"))
      {
         int idx = filename.length()-4;

         filename = filename.substring(0, idx)+".aux";
      }
      else if (!filename.endsWith(".aux"))
      {
         filename = filename+".aux";
      }

      currentFileName = filename;
   }

   public void load(File file)
   {
      String lc = file.getName().toLowerCase();

      if (lc.endsWith(".tex") || lc.endsWith(".log"))
      {
         int idx = file.getName().length()-4;

         file = new File(file.getParentFile(),
                file.getName().substring(0, idx)+".aux");
      }

      setFile(file);
      reload(file);
   }

   public void reload()
   {
      reload(new File(currentFileName));
   }

   public void reload(File file) 
   {
      if (!file.exists())
      {
         // Does the log file exists?

         File log = null;

         String name = file.getName();

         if (!isBatchMode() && name.toLowerCase().endsWith(".aux"))
         {
            name = name.substring(0, name.length()-4);
            log = new File(file.getParentFile(), name+".log");

            if (!log.exists())
            {
               log = null;
            }
            else
            {
               glossaries = new Glossaries(this);
               glossaries.addDiagnosticMessage(
                 getLabel("diagnostics.no_aux"));
               glossaries.addErrorMessage(getLabelWithValue(
                 "error.io.file_doesnt_exist", file.toString()));

               try
               {
                  glossaries.parseLog(log.getParentFile(), name);
               }
               catch (Exception e)
               {
                  getMessageSystem().error(e);
               }

               getMessageSystem().showMessages();
            }
         }

         if (log == null)
         {
            getMessageSystem().error(getLabelWithValue(
              "error.io.file_doesnt_exist", file.getAbsolutePath()));
         }

         return;
      }

      try
      {
         glossaries = Glossaries.loadGlossaries(this, file);

         glossaries.process();
      }
      catch (InterruptedException e)
      {
         getMessageSystem().error(e);
      }
      catch (GlossaryException e)
      {
         String mess = e.getDiagnosticMessage();

         if (mess != null)
         {
            glossaries.addDiagnosticMessage(mess);
         }

         getMessageSystem().message(e);
      }
      catch (IOException e)
      {
         glossaries.addDiagnosticMessage(getLabel("diagnostics.io_error"));
         getMessageSystem().error(e);
      }

      String errMess = glossaries.getErrorMessages();

      if (errMess != null)
      {
         getMessageSystem().error(errMess);
      }

      getMessageSystem().showMessages();
   }

   public void setMessageSystem(GlossaryMessage msg)
   {
      messageSystem = msg;
   }

   public GlossaryMessage getMessageSystem()
   {
      return messageSystem;
   }

   public static String escapeHTML(String text)
   {
      if (text == null || text.isEmpty()) return "";

      int n = text.length();

      StringBuilder builder = new StringBuilder(n);

      for (int i = 0; i < n; i++)
      {
         char c = text.charAt(i);

         if (c == '&')
         {
            builder.append("&amp;");
         }
         else if (c == '<')
         {
            builder.append("&lt;");
         }
         else if (c == '>')
         {
            builder.append("&gt;");
         }
         else
         {
            builder.append(c);
         }
      }

      return builder.toString();
   }

   public String getLabelOrDef(String label, String alt)
   {
      String val = dictionary.getProperty(label);

      return val == null ? alt : val;
   }

   public String getLabel(String label)
   {
      return getLabel(null, label);
   }

   public String getLabel(String parent, String label)
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

   public char getMnemonic(String label)
   {
      return getMnemonic(null, label);
   }

   public char getMnemonic(String parent, String label)
   {
      String prop = getLabel(parent, label+".mnemonic");

      if (prop.equals(""))
      {
         System.err.println("Empty dictionary property '"+prop+"'");
         return label.charAt(0);
      }

      return prop.charAt(0);
   }

   public int getMnemonicInt(String label)
   {
      return getMnemonicInt(null, label);
   }

   public int getMnemonicInt(String parent, String label)
   {
      String prop = getLabel(parent, label+".mnemonic");

      if (prop.equals(""))
      {
         System.err.println("Empty dictionary property '"+prop+"'");
         return label.codePointAt(0);
      }

      return prop.codePointAt(0);
   }

   public String getLabelWithValue(String label, String value)
   {
      String prop = getLabel(label);

      if (!isBatchMode())
      {
         value = escapeHTML(value);
      }

      StringBuilder builder = new StringBuilder(prop.length());

      for (int i = 0; i < prop.length(); i++)
      {
         char c = prop.charAt(i);

         if (c == '$')
         {
            i++;

            if (i == prop.length())
            {
               builder.append(c);
            }
            else
            {
              char c2 = prop.charAt(i);

              if (c2 == '1')
              {
                 builder.append(value);
              }
              else
              {
                 builder.append(c);
                 builder.append(c2);
              }
            }
         }
         else
         {
            builder.append(c);
         }
      }

      return builder.toString();
   }

   public String getLabelWithValues(String label, String value1,
      String value2)
   {
      String prop = getLabel(label);

      if (!isBatchMode())
      {
         value1 = escapeHTML(value1);
         value2 = escapeHTML(value2);
      }

      StringBuilder builder = new StringBuilder(prop.length());

      for (int i = 0; i < prop.length(); i++)
      {
         char c = prop.charAt(i);

         if (c == '$')
         {
            i++;

            if (i == prop.length())
            {
               builder.append(c);
            }
            else
            {
              char c2 = prop.charAt(i);

              if (c2 == '1')
              {
                 builder.append(value1);
              }
              else if (c2 == '2')
              {
                 builder.append(value2);
              }
              else
              {
                 builder.append(c);
                 builder.append(c2);
              }
            }
         }
         else
         {
            builder.append(c);
         }
      }

      return builder.toString();
   }

   public String getLabelWithValues(String label, String value1,
      String value2, String value3)
   {
      String prop = getLabel(label);

      if (!isBatchMode())
      {
         value1 = escapeHTML(value1);
         value2 = escapeHTML(value2);
         value3 = escapeHTML(value3);
      }

      StringBuilder builder = new StringBuilder(prop.length());

      for (int i = 0; i < prop.length(); i++)
      {
         char c = prop.charAt(i);

         if (c == '$')
         {
            i++;

            if (i == prop.length())
            {
               builder.append(c);
            }
            else
            {
              char c2 = prop.charAt(i);

              if (c2 == '1')
              {
                 builder.append(value1);
              }
              else if (c2 == '2')
              {
                 builder.append(value2);
              }
              else if (c2 == '3')
              {
                 builder.append(value3);
              }
              else
              {
                 builder.append(c);
                 builder.append(c2);
              }
            }
         }
         else
         {
            builder.append(c);
         }
      }

      return builder.toString();
   }

   public MakeGlossariesProperties getProperties()
   {
      return properties;
   }

   public boolean isDocDefCheckOn()
   {
      return properties.isDocDefsCheckOn();
   }

   public boolean isMissingLangCheckOn()
   {
      return properties.isMissingLangCheckOn();
   }

   public String getDefaultLanguage()
   {
      return properties.getDefaultLanguage();
   }

   public String getDefaultXindyVariant()
   {
      return properties.getDefaultXindyVariant();
   }

   public String getDefaultCodePage()
   {
      return properties.getDefaultCodePage();
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

   public Glossaries getGlossaries()
   {
      return glossaries;
   }

   public String getFileName()
   {
      return currentFileName;
   }

   public File getFile()
   {
      return currentFileName == null ? null : new File(currentFileName);
   }

   public File getCurrentDirectory()
   {
      if (currentFileName == null)
      {
         return new File(properties.getDefaultDirectory());
      }

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
      languageMap.put("ngerman", "german");
      languageMap.put("norsk", "norwegian");
      languageMap.put("portuges", "portuguese");
      languageMap.put("russianb", "russian");
      languageMap.put("UKenglish", "english");
      languageMap.put("ukraineb", "ukrainian");
      languageMap.put("USenglish", "english");
      languageMap.put("usorbian", "upper-sorbian");
   }

   public String getLanguage(String language)
   {
      String map = languageMap.get(language);

      return map == null ? language : map;
   }

   public void help()
   {
      System.out.println(getLabel("syntax.cmdline"));
      System.out.println();
      System.out.println(getLabelWithValue("syntax.filename", "--batch"));
      System.out.println();
      System.out.println(getLabel("syntax.options"));
      System.out.println();
      System.out.println(getLabelWithValues("syntax.help", "--help", "-h"));
      System.out.println(getLabelWithValues("syntax.version", 
         "--version", "-v"));
      System.out.println(getLabelWithValue("syntax.debug", "--debug"));
      System.out.println(getLabelWithValues("syntax.dryrun", "--dry-run", 
        "-n"));
      System.out.println(getLabelWithValue("syntax.nodryrun", "--nodry-run"));
      System.out.println(getLabelWithValues("syntax.batch", "--batch", "-b"));
      System.out.println(getLabelWithValue("syntax.gui", "--gui"));
      System.out.println(getLabelWithValue("syntax.quiet", "--quiet"));
      
   }

   public void version()
   {
     String translator = dictionary.getProperty("about.translator_info");

     System.out.println(appName);
     System.out.println(getLabelWithValues("about.version",
       appVersion, appDate));
     System.out.println(getLabelWithValues("about.copyright", 
      "Nicola L. C. Talbot", "2011"));
     System.out.println("http://www.dickimaw-books.com/");

     if (translator != null && !translator.isEmpty())
     {
        System.out.println(translator);
     }

   }

   public boolean isQuiet()
   {
      return quiet;
   }

   public void setQuietMode(boolean isSet)
   {
      quiet = isSet;
   }

   public boolean isDebugMode()
   {
      return debug;
   }

   public void setDebugMode(boolean isSet)
   {
      debug = isSet;
   }

   public boolean isBatchMode()
   {
      return batchMode;
   }

   public void setDryRunMode(boolean isDryRun)
   {
      dryRun = isDryRun;
   }

   public boolean isDryRunMode()
   {
      return dryRun;
   }

   public static void main(String[] args)
   {
      MakeGlossariesInvoker invoker = new MakeGlossariesInvoker();

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--help") || args[i].equals("-h"))
         {
            invoker.batchMode = true;
            invoker.help();
            System.exit(0);
         }
         else if (args[i].equals("--version") || args[i].equals("-v"))
         {
            invoker.batchMode = true;
            invoker.version();
            System.exit(0);
         }
         else if (args[i].equals("--batch") || args[i].equals("-b"))
         {
            invoker.batchMode = true;
         }
         else if (args[i].equals("--gui"))
         {
            invoker.batchMode = false;
         }
         else if (args[i].equals("--quiet"))
         {
            invoker.setQuietMode(true);
         }
         else if (args[i].equals("--debug"))
         {
            invoker.setDebugMode(true);
         }
         else if (args[i].equals("--dry-run") || args[i].equals("-n"))
         {
            invoker.setDryRunMode(true);
         }
         else if (args[i].equals("--nodry-run"))
         {
            invoker.setDryRunMode(false);
         }
         else if (args[i].startsWith("-"))
         {
            System.err.println(invoker.getLabelWithValue("error.unknown_opt", args[i]));
            System.exit(1);
         }
         else if (invoker.getFileName() == null)
         {
            invoker.setFile(args[i]);

         }
         else
         {
            System.err.println(invoker.getLabel("error.one_input"));
            System.exit(1);
         }
      }

      if (invoker.batchMode)
      {
         if (invoker.getFileName() == null)
         {
            System.err.println(invoker.getLabel("error.input_required"));
            System.exit(1);
         }

         invoker.reload();
      }
      else
      {
         MakeGlossariesGUI.createAndShowGUI(invoker);
      }
   }

   private String currentFileName = null;

   private MakeGlossariesProperties properties;

   private static Properties dictionary;

   public static final String appName = "MakeGlossariesGUI";

   public static final String appVersion = "2.1";

   public static final String appDate = "2016-06-09";

   private boolean quiet=false, debug=false, batchMode=false, dryRun=false; 

   protected Glossaries glossaries;

   private Hashtable<String,String> languageMap;

   private GlossaryMessage messageSystem;
}
