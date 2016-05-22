package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.net.*;
import java.util.*;

public class MakeGlossariesInvoker implements GlossaryMessage
{
   public MakeGlossariesInvoker()
   {
      setMessageSystem(this);

      try
      {
         loadDictionary();
      }
      catch (IOException e)
      {
         error(String.format("Unable to load dictionary file:%n%s",
            e.getMessage()));
      }

      initLanguageMappings();

      try
      {
         properties = MakeGlossariesProperties.fetchProperties();
      }
      catch (IOException e)
      {
         error(getLabelWithValue("error.prop_io", e.getMessage()));
         properties = new MakeGlossariesProperties();
      }
   }

   public void setFile(File file)
   {
      currentFileName = file.getAbsolutePath();
   }

   public void load(File file)
   {
      setFile(file);
      reload(file);
   }

   public void reload()
   {
      reload(new File(currentFileName));
   }

   public void reload(File file) 
   {
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
         error(errMess);
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

      return prop.replaceAll("\\$1", value);
   }

   public String getLabelWithValues(String label, String value1,
      String value2)
   {
      String prop = getLabel(label);

      return prop.replaceAll("\\$1", value1).replaceAll("\\$2", value2);
   }

   public MakeGlossariesProperties getProperties()
   {
      return properties;
   }

   public String getDefaultLanguage()
   {
      return properties.getDefaultLanguage();
   }

   public String getDefaultCodePage()
   {
      return properties.getDefaultCodePage();
   }

   public void error(String message)
   {
      System.err.println(message);
   }

   public void error(Exception e)
   {
      System.err.println(e.getMessage());
   }

   public void message(GlossaryException e)
   {
      if (!quiet)
      {
         System.out.println(e.getMessage());
         System.out.println(e.getDiagnosticMessage());
      }
   }

   public void message(String msg)
   {
      if (!quiet)
      {
         System.out.println(msg);
      }
   }

   public void aboutToExec(String[] cmdArray, File dir)
   {
      if (!quiet)
      {
         for (int i = 0, n = cmdArray.length-1; i < cmdArray.length; i++)
         {
            if (i == n)
            {
               System.out.println(cmdArray[i]);
            }
            else
            {
               System.out.print(cmdArray[i]);
               System.out.print(" ");
            }
         }
      }
   }

   public void showMessages()
   {
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

   public String getLanguage(String language)
   {
      String map = languageMap.get(language);

      return map == null ? language : map;
   }

   public void help()
   {
   }

   public void version()
   {
     String translator = dictionary.getProperty("about.translator_info");

     System.out.println(appName);
     System.out.println(getLabelWithValues("about.version",
       appVersion, appDate));
     System.out.println(getLabelWithValues("about.copyright", 
      "Nicola L. C. Talbot", "2011/09/16"));
     System.out.println("http://www.dickimaw-books.com/");

     if (translator != null && !translator.isEmpty())
     {
        System.out.println(translator);
     }

   }

   public static void main(String[] args)
   {
      MakeGlossariesInvoker invoker = new MakeGlossariesInvoker();

      boolean doBatch = false;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("--help") || args[i].equals("-h"))
         {
            doBatch = true;
            invoker.help();
            System.exit(0);
         }
         else if (args[i].equals("--version") || args[i].equals("-v"))
         {
            doBatch = true;
            invoker.version();
            System.exit(0);
         }
         else if (args[i].equals("--batch"))
         {
            doBatch = true;
         }
         else if (args[i].equals("--gui"))
         {
            doBatch = false;
         }
         else if (args[i].equals("--quiet"))
         {
            invoker.quiet = false;
         }
         else if (args[i].startsWith("-"))
         {
            System.err.println(invoker.getLabelWithValue("error.unknown_opt", args[i]));
            System.exit(1);
         }
         else if (invoker.getFileName() == null)
         {
            invoker.currentFileName = args[i];
         }
         else
         {
            System.err.println(invoker.getLabel("error.one_input"));
            System.exit(1);
         }
      }

      if (doBatch)
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

   public static final String appVersion = "1.0";

   public static final String appDate = "2016-05-17";

   protected Glossaries glossaries;

   private Hashtable<String,String> languageMap;

   private GlossaryMessage messageSystem;

   private boolean quiet = false;
}
