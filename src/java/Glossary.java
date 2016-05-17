package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class Glossary
{
   public Glossary(MakeGlossariesGUI application, String label, String transExt,
      String glsExt, String gloExt)
   {
      app = application;
      this.label = label;
      this.transExt = transExt;
      this.glsExt = glsExt;
      this.gloExt = gloExt;

      entryTable = new Hashtable<String,Integer>();
   }

   public void xindy(File dir, String baseName, 
      boolean isWordOrder, String istName)
      throws IOException,InterruptedException,GlossaryException
   {
      File xindyApp = new File(app.getXindyApp());

      if (!xindyApp.exists())
      {
         throw new GlossaryException(app.getLabelWithValues(
            "error.no_indexer_app", "xindy", xindyApp.getAbsolutePath()),
            app.getLabelWithValue("diagnostics.no_indexer", "xindy"));
      }

      String transFileName = baseName+"."+transExt;

      if (language == null || language.equals(""))
      {
         language = app.getDefaultLanguage();
      }

      if (codepage == null || codepage.equals(""))
      {
         codepage = app.getDefaultCodePage();
      }

      String style = istName;

      int idx = istName.lastIndexOf(".");

      if (idx != -1)
      {
         style = style.substring(0, idx);
      }

      File gloFile = new File(dir, baseName+"."+gloExt);

      String[] cmdArray;

      if (isWordOrder)
      {
         cmdArray = new String[]
         {
            xindyApp.getAbsolutePath(),
            "-L", language,
            "-C", codepage,
            "-I", "xindy",
            "-M", style,
            "-t", transFileName,
            "-o", baseName+"."+glsExt,
            gloFile.getName()
         };
      }
      else
      {
         cmdArray = new String[]
         {
            xindyApp.getAbsolutePath(),
            "-L", language,
            "-C", codepage,
            "-I", "xindy",
            "-M", style,
            "-M", "ord/letorder",
            "-t", transFileName,
            "-o", baseName+"."+glsExt,
            gloFile.getName()
         };
      }

      Process p = Runtime.getRuntime().exec(cmdArray, null, dir);

      int exitCode = p.waitFor();

      String line;

      BufferedReader in = new BufferedReader(new InputStreamReader(p.getErrorStream()));

      String processErrors = null;

      String unknownMod = null;

      while ((line = in.readLine()) != null)
      {
         if (processErrors == null)
         {
            processErrors = "\n" + line;
         }
         else
         {
            processErrors += "\n" + line;
         }

         Matcher matcher = xindyModulePattern.matcher(line);

         if (matcher.matches())
         {
            unknownMod = app.getLabelWithValues("diagnostics.unknown_language_or_codepage",
               matcher.group(1), matcher.group(2));
         }
      }

      in.close();

      String unknownVar = null;

      File transFile = new File(dir, transFileName);

      if (transFile.exists())
      {
         in = new BufferedReader(new FileReader(transFile));

         while ((line = in.readLine()) != null)
         {
            Matcher matcher = xindyIstPattern.matcher(line);

            if (matcher.matches())
            {
               unknownVar = matcher.group();
            }
         }

         in.close();
      }

      if (gloFile.exists())
      {
         in = new BufferedReader(new FileReader(gloFile));

         while ((line = in.readLine()) != null)
         {
            Matcher matcher = entryPattern.matcher(line);

            if (matcher.matches())
            {
               String key = matcher.group(1);

               Integer n = entryTable.get(key);

               if (n == null)
               {
                  n = new Integer(1);
               }
               else
               {
                  n = new Integer(n.intValue()+1);
               }

               entryTable.put(key, n);
            }
         }

         in.close();
      }

      if (exitCode > 0)
      {
         addErrorMessage(app.getLabelWithValues("error.app_failed",
            "Xindy", ""+exitCode));

         if (unknownVar != null)
         {
            addDiagnosticMessage(app.getLabelWithValues
               ("diagnostics.bad_attributes", istName, "xindy"));
         }
         else if (unknownMod != null)
         {
            addDiagnosticMessage(unknownMod);
         }
         else if (processErrors != null)
         {
            addDiagnosticMessage(app.getLabelWithValues("diagnostics.app_err",
               "Xindy", processErrors));
         }
         else
         {
            addDiagnosticMessage(app.getLabel("diagnostics.app_err_null"));
         }
      }
      else if (entryTable.size() == 0)
      {
         addDiagnosticMessage(app.getLabelWithValue("diagnostics.no_entries",
            label));
      }
   }

   public void makeindex(File dir, String baseName, boolean isWordOrder, String istName)
      throws IOException,InterruptedException,GlossaryException
   {
      File makeindexApp = new File(app.getMakeIndexApp());

      if (!makeindexApp.exists())
      {
         throw new GlossaryException(app.getLabelWithValues(
            "error.no_indexer_app", "makeindex", makeindexApp.getAbsolutePath()),
            app.getLabelWithValue("diagnostics.no_indexer", "makeindex"));
      }

      String transFileName = baseName+"."+transExt;

      File gloFile = new File(dir, baseName+"."+gloExt);

      String[] cmdArray;

      if (app.useGermanWordOrdering())
      {
         if (isWordOrder)
         {
            cmdArray = new String[]
            {
               makeindexApp.getAbsolutePath(),
               "-g",
               "-s", istName,  
               "-t", transFileName,
               "-o", baseName+"."+glsExt,
                gloFile.getName()
            };
         }
         else
         {
            cmdArray = new String[]
            {
               makeindexApp.getAbsolutePath(),
               "-l", "-g",
               "-s", istName,  
               "-t", transFileName,
               "-o", baseName+"."+glsExt,
                gloFile.getName()
            };
         }
      }
      else if (isWordOrder)
      {
         cmdArray = new String[]
         {
            makeindexApp.getAbsolutePath(),
            "-s", istName,  
            "-t", transFileName,
            "-o", baseName+"."+glsExt,
             gloFile.getName()
         };
      }
      else
      {
         cmdArray = new String[]
         {
            makeindexApp.getAbsolutePath(),
            "-l",
            "-s", istName,  
            "-t", transFileName,
            "-o", baseName+"."+glsExt,
             gloFile.getName()
         };
      }

      Process p = Runtime.getRuntime().exec(cmdArray, null, dir);

      int exitCode = p.waitFor();

      String line;

      BufferedReader in = new BufferedReader(new InputStreamReader(p.getErrorStream()));

      String processErrors = null;

      while ((line = in.readLine()) != null)
      {
         if (processErrors == null)
         {
            processErrors = "\n" + line;
         }
         else
         {
            processErrors += "\n" + line;
         }
      }

      in.close();

      in = new BufferedReader(new FileReader(new File(dir, transFileName)));

      int numAccepted = 0;
      int numRejected = 0;
      String rejected = "";

      int numAttributes = 0;
      int numIgnored = 0;

      while ((line = in.readLine()) != null)
      {
         Matcher matcher = makeindexAcceptedPattern.matcher(line);

         if (matcher.matches())
         {
            try
            {
               numAccepted = Integer.parseInt(matcher.group(1));
               rejected = matcher.group(2);
               numRejected = Integer.parseInt(rejected);
            }
            catch (NumberFormatException e)
            {
            }
         }

         matcher = makeindexIstAttributePattern.matcher(line);

         if (matcher.matches())
         {
            try
            {
               numAttributes = Integer.parseInt(matcher.group(1));
               numIgnored = Integer.parseInt(matcher.group(2));
            }
            catch (NumberFormatException e)
            {
            }
         }
      }

      in.close();

      if (gloFile.exists())
      {
         in = new BufferedReader(new FileReader(gloFile));

         while ((line = in.readLine()) != null)
         {
            Matcher matcher = entryPattern.matcher(line);

            if (matcher.matches())
            {
               String key = matcher.group(1);

               Integer n = entryTable.get(key);

               if (n == null)
               {
                  n = new Integer(1);
               }
               else
               {
                  n = new Integer(n.intValue()+1);
               }

               entryTable.put(key, n);
            }
         }

         in.close();
      }

      if (exitCode > 0)
      {
         addErrorMessage(app.getLabelWithValues("error.app_failed",
            "Makeindex", ""+exitCode));

         if (processErrors != null)
         {
            addDiagnosticMessage(app.getLabelWithValues("diagnostics.app_err",
               "Makeindex", processErrors));
         }
         else
         {
            addDiagnosticMessage(app.getLabel("diagnostics.app_err_null"));
         }
      }
      else if (numRejected > 0)
      {
         if (numRejected == 1)
         {
            addErrorMessage(app.getLabel("error.entry_rejected"));
         }
         else
         {
            addErrorMessage(app.getLabelWithValue("error.entries_rejected", rejected));
         }

         if (numAccepted == 0)
         {
            addDiagnosticMessage(app.getLabelWithValue("diagnostics.makeindex_reject_all",
               label));
         }

         if (numIgnored > 0)
         {
            addDiagnosticMessage(app.getLabelWithValues
               ("diagnostics.bad_attributes", istName, "makeindex"));
         }
      }
      else if (numAccepted == 0)
      {
         if (label.equals("main"))
         {
            addDiagnosticMessage(app.getLabel("diagnostics.no_entries_main"));
         }
         else
         {
            addDiagnosticMessage(app.getLabelWithValue("diagnostics.no_entries",
               label));
         }

         addErrorMessage(app.getLabelWithValue("error.no_entries", label));
      }
   }

   public int getNumEntries()
   {
      return entryTable.size();
   }

   public Integer getEntryCount(int entryIdx)
   {
      int i = 0;

      for (Enumeration<Integer> en = entryTable.elements(); en.hasMoreElements();)
      {
         Integer val = en.nextElement();

         if (i == entryIdx) return val;

         i++;
      }

      return 0;
   }

   public String getEntryLabel(int entryIdx)
   {
      int i = 0;

      for (Enumeration<String> en = entryTable.keys(); en.hasMoreElements();)
      {
         String key = en.nextElement();

         if (i == entryIdx) return key;

         i++;
      }

      return null;
   }

   public int getEntryIdx(String label)
   {
      int i = 0;

      for (Enumeration<String> en = entryTable.keys(); en.hasMoreElements();)
      {
         String key = en.nextElement();

         if (key.equals(label)) return i;

         i++;
      }

      return -1;
   }

   public void setLanguage(String language)
   {
      this.language = app.getLanguage(language);
   }

   public void setCodePage(String codepage)
   {
      this.codepage = codepage;
   }

   public String displayCodePage()
   {
      return codepage == null ?
         "<font class=error>"+app.getLabel("error.unknown")+"</font>" :
         codepage;
   }

   public String displayLanguage()
   {
      return language == null ?
         "<font class=error>"+app.getLabel("error.unknown")+"</font>" :
         language;
   }

   public void addErrorMessage(String mess)
   {
      if (errorMessage == null)
      {
         errorMessage = mess;
      }
      else
      {
         errorMessage += "\n" + mess;
      }
   }

   public String getErrorMessages()
   {
      return errorMessage;
   }

   public String getLanguage()
   {
      return language;
   }

   public String getCodePage()
   {
      return codepage;
   }

   public String getDiagnosticMessages()
   {
      return diagnosticMessage;
   }

   public void addDiagnosticMessage(String mess)
   {
      if (diagnosticMessage == null)
      {
         diagnosticMessage = mess;
      }
      else
      {
         diagnosticMessage += "\n" + mess;
      }
   }

   public String label, transExt, glsExt, gloExt;

   private String language, codepage;

   private String errorMessage, diagnosticMessage;

   private Hashtable<String,Integer> entryTable;

   private MakeGlossariesGUI app;

   private static final Pattern makeindexAcceptedPattern 
      = Pattern.compile(".*(\\d+)\\s+entries\\s+accepted.*(\\d+)\\s+rejected.*");

   private static final Pattern makeindexIstAttributePattern
      = Pattern.compile(".*(\\d+)\\s+attributes\\s+redefined.*(\\d+).*ignored.*");

   private static final Pattern xindyIstPattern
      = Pattern.compile(".*variable (.*) has no value.*");

   private static final Pattern xindyModulePattern
      = Pattern.compile(".*Cannot\\s+locate\\s+xindy\\s+module\\s+for\\s+language\\s+([a-zA-Z0-9\\-]+)\\s+in\\s+codepage\\s+([a-zA-Z0-9\\-]+)..*");

   private static final Pattern entryPattern
      = Pattern.compile(".*\\\\glossaryentryfield\\{([^\\}]+)\\}.*");
}
