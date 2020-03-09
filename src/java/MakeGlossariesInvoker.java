/*
    Copyright (C) 2013-2020 Nicola L.C. Talbot
    www.dickimaw-books.com

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.nio.charset.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
           getLabelWithValues("error.prop_io", e.getMessage()));
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
               glossaries.addErrorMessage(getLabelWithValues(
                 "error.io.file_doesnt_exist", file));

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
            getMessageSystem().error(getLabelWithValues(
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
      if (messages == null)
      {
         return alt;
      }

      String val = messages.getMessageIfExists(label);

      return val == null ? alt : val;
   }

   public String getLabel(String label)
   {
      return getLabel(null, label);
   }

   public String getLabel(String parent, String label)
   {
      if (messages == null)
      {
         messageSystem.debug("Dictionary not loaded.");
         return null;
      }

      String propLabel;

      if (parent == null)
      {
         propLabel = label;
      }
      else
      {
         propLabel = String.format("%s.%s", parent, label);
      }

      return messages.getMessage(propLabel);
   }

   public int getMnemonic(String label)
   {
      return getMnemonic(null, label);
   }

   public int getMnemonic(String parent, String label)
   {
      if (messages == null)
      {
         messageSystem.debug("Dictionary not loaded.");
         return -1;
      }

      String propLabel = label+".mnemonic";

      if (parent != null)
      {
         propLabel = String.format("%s.%s", parent, propLabel);
      }

      String val = messages.getMessageIfExists(propLabel);

      if (val == null || val.isEmpty())
      {
         return -1;
      }

      return val.codePointAt(0);
   }

   public String getLabelWithValues(String label, Object... values)
   {
      if (messages == null)
      {
         return null;
      }

      return messages.getMessage(label, values);
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

   // Convert xindy and inputenc encoding labels to Charset
   // Uses String.contains rather than String.equals as
   // the xindy codepage may contain additional information,
   // such as din5007-
   public static Charset getCharset(String encodingLabel)
    throws IllegalCharsetNameException,
           IllegalArgumentException,
           UnsupportedCharsetException
   {
      if (encodingLabel == null)
      {
         throw new IllegalArgumentException(
          "Illegal encoding label (null)");
      }

      if (encodingLabel.contains("utf8"))
      {
         return StandardCharsets.UTF_8;
      }

      if (encodingLabel.contains("ascii"))
      {
         return Charset.forName("US-ASCII");
      }

      if (encodingLabel.contains("latin10"))
      {
         return Charset.forName("ISO-8859-16");
      }

      if (encodingLabel.contains("latin1"))
      {
         return StandardCharsets.ISO_8859_1;
      }

      if (encodingLabel.contains("latin2"))
      {
         return Charset.forName("ISO-8859-2");
      }

      if (encodingLabel.contains("latin3"))
      {
         return Charset.forName("ISO-8859-3");
      }

      if (encodingLabel.contains("latin4"))
      {
         return Charset.forName("ISO-8859-4");
      }

      if (encodingLabel.contains("latin5"))
      {
         return Charset.forName("ISO-8859-9");
      }

      if (encodingLabel.contains("latin6"))
      {
         return Charset.forName("ISO-8859-10");
      }

      if (encodingLabel.contains("latin7"))
      {
         return Charset.forName("ISO-8859-13");
      }

      if (encodingLabel.contains("latin8"))
      {
         return Charset.forName("ISO-8859-14");
      }

      if (encodingLabel.contains("latin9"))
      {
         return Charset.forName("ISO-8859-15");
      }

      if (encodingLabel.contains("cp1250"))
      {
         return Charset.forName("Cp1250");
      }

      if (encodingLabel.contains("cp1252")
           || encodingLabel.contains("ansinew"))
      {
         return Charset.forName("Cp1252");
      }

      if (encodingLabel.contains("cp1257"))
      {
         return Charset.forName("Cp1257");
      }

      if (encodingLabel.contains("cp437"))
      {
         return Charset.forName("Cp437");
      }

      if (encodingLabel.contains("cp850"))
      {
         return Charset.forName("Cp850");
      }

      if (encodingLabel.contains("cp852"))
      {
         return Charset.forName("Cp852");
      }

      if (encodingLabel.contains("cp858"))
      {
         return Charset.forName("Cp858");
      }

      if (encodingLabel.contains("cp865"))
      {
         return Charset.forName("Cp865");
      }

      if (encodingLabel.contains("decmulti"))
      {
         return Charset.forName("DEC-MCS");
      }

      if (encodingLabel.contains("applemac"))
      {
         return Charset.forName("MacRoman");
      }

      if (encodingLabel.contains("macce"))
      {
         return Charset.forName("MacCentralEurope");
      }

      // don't know appropriate Java name for inputenc's 'next'

      return null;
   }

   // indexer character encoding
   public Charset getEncoding()
   {
      return defaultCharset;
   }

   public void setEncoding(Charset charset)
   {
      defaultCharset = charset;
   }

   private void loadDictionary()
      throws IOException
   {
      Locale locale = Locale.getDefault();

      String lang = locale.getLanguage();

      String resource = "/resources/dictionaries/makeglossariesgui-"
                        +lang+".prop";

      InputStream in = getClass().getResourceAsStream(resource);

      if (in == null && !lang.equals("en"))
      {
         resource = "/resources/dictionaries/makeglossariesgui-en.prop";
         in = getClass().getResourceAsStream(resource);
      }

      BufferedReader reader = null;

      try
      {
         Charset defCharset = Charset.defaultCharset();

         reader = new BufferedReader(new InputStreamReader(in, defCharset));
   
         // First line should be # Encoding: <encoding name>
   
         Charset charset = null;
         String encoding=null;
   
         try
         {
            String line = reader.readLine();
   
            Pattern p = Pattern.compile("# Encoding: (.+)");
   
            Matcher m = p.matcher(line);
   
            if (!m.matches())
            {
               throw new InvalidSyntaxException(
                 "Missing encoding comment on line 1 of dictionary file: !"
                 + resource);
            }
   
            encoding = m.group(1);
            charset = Charset.forName(encoding);
         }
         catch (UnsupportedCharsetException|IllegalCharsetNameException e)
         {
            throw new InvalidSyntaxException(String.format(
              "Invalid encoding '%s' on line 1 of dictionary file: !%s",
               encoding, resource), e);
         }
   
         if (!charset.equals(defCharset))
         {
            reader.close();
            reader = new BufferedReader(new InputStreamReader(in, charset));
         }
   
         Properties dictionary = new Properties();
         dictionary.load(reader);
         messages = new MakeGlossariesDictionary(dictionary);
   
      }
      finally
      {
         if (reader != null)
         {
            reader.close();
         }

         in.close();
      }
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

   public File findApp(String name)
   {
      return findApp(name, name+".exe", null);
   }

   public File findApp(String name, String altName, String altName2)
   {
      String filename = name;
      String filename2 = (altName == null ? null : altName);
      String filename3 = (altName2 == null ? null : altName2);

      String path = System.getenv("PATH");
      String[] split = path.split(File.pathSeparator);

      for (int i = 0; i < split.length; i++)
      {
         File file = new File(split[i], filename);

         if (file.exists())
         {
            return file;
         }

         if (filename2 != null)
         {
            file = new File(split[i], filename2);

            if (file.exists())
            {
               return file;
            }
         }

         if (filename3 != null)
         {
            file = new File(split[i], filename3);

            if (file.exists())
            {
               return file;
            }
         }
      }

      return null;
   }



   public void help()
   {
      System.out.println(getLabel("syntax.cmdline"));
      System.out.println();
      System.out.println(getLabelWithValues("syntax.filename", "--batch"));
      System.out.println();
      System.out.println(getLabel("syntax.options"));
      System.out.println();
      System.out.println(getLabelWithValues("syntax.help", "--help", "-h"));
      System.out.println(getLabelWithValues("syntax.version", 
         "--version", "-v"));
      System.out.println(getLabelWithValues("syntax.debug", "--debug"));
      System.out.println(getLabelWithValues("syntax.dryrun", "--dry-run", 
        "-n"));
      System.out.println(getLabelWithValues("syntax.nodryrun", "--nodry-run"));
      System.out.println(getLabelWithValues("syntax.batch", "--batch", "-b"));
      System.out.println(getLabelWithValues("syntax.gui", "--gui"));
      System.out.println(getLabelWithValues("syntax.quiet", "--quiet"));
      
   }

   public void version()
   {
     String translator = getLabelOrDef("about.translator_info", null);

     System.out.println(APP_NAME);
     System.out.println(getLabelWithValues("about.version",
       APP_VERSION, APP_DATE));
     System.out.println(getLabelWithValues("about.copyright", 
      "Nicola L. C. Talbot", 
      String.format("2011-%s", APP_DATE.substring(0, 4))));
     System.out.println("https://www.dickimaw-books.com/");

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
            System.err.println(invoker.getLabelWithValues("error.unknown_opt", 
              args[i]));
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

   private MakeGlossariesDictionary messages;

   public static final String APP_NAME = "MakeGlossariesGUI";

   public static final String APP_VERSION = "2.2";

   public static final String APP_DATE = "2020-03-09";

   private boolean quiet=false, debug=false, batchMode=false, dryRun=false; 

   protected Glossaries glossaries;

   private Charset defaultCharset=Charset.defaultCharset();

   private Hashtable<String,String> languageMap;

   private GlossaryMessage messageSystem;
}
