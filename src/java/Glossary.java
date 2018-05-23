package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

public class Glossary
{
   public Glossary(MakeGlossariesInvoker invoker, String label, String transExt,
      String glsExt, String gloExt)
   {
      this.invoker = invoker;
      this.label = label;
      this.transExt = transExt;
      this.glsExt = glsExt;
      this.gloExt = gloExt;

      entryTable = new Hashtable<String,GlossaryEntry>();
   }

   public void xindy(File dir, String baseName, 
      boolean isWordOrder, String istName)
      throws IOException,InterruptedException,GlossaryException
   {
      File xindyApp = new File(invoker.getXindyApp());

      if (!xindyApp.exists())
      {
         throw new GlossaryException(invoker.getLabelWithValues(
            "error.no_indexer_app", "xindy", xindyApp.getAbsolutePath()),
            invoker.getLabelWithValues("diagnostics.no_indexer", "xindy"));
      }

      String transFileName = baseName+"."+transExt;

      if (language == null || language.equals(""))
      {
         language = invoker.getDefaultLanguage();
      }

      if (codepage == null || codepage.equals(""))
      {
         codepage = invoker.getDefaultCodePage();
      }

      if (!invoker.getProperties().isOverride())
      {
         XindyModule mod = XindyModule.getModule(language);

         if (mod != null)
         {
            String variant = mod.getDefaultVariant();

            if (variant != null && !mod.hasCodePage(codepage))
            {
               addDiagnosticMessage(invoker.getLabelWithValues(
                 "diagnostics.variant", language, codepage, variant));
               codepage = variant+"-"+codepage;
            }
         }
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

      int exitCode = 0;
      BufferedReader in=null;

      Charset charset = null;

      try
      {
         charset = invoker.getCharset(codepage);
         invoker.setEncoding(charset);
      }
      catch (Exception e)
      {
         invoker.getMessageSystem().error(e);
      }

      if (charset == null)
      {
         addDiagnosticMessage(invoker.getLabelWithValues(
          "diagnostics.unknown.encoding", codepage));
         charset = invoker.getEncoding();
      }

      File transFile = new File(dir, transFileName);

      invoker.getMessageSystem().aboutToExec(cmdArray, dir);

      if (invoker.isDryRunMode())
      {
         if (transFile.exists())
         {
            in = new BufferedReader(new InputStreamReader(
              new FileInputStream(transFile), charset));
         }
      }
      else
      {
         Process p = Runtime.getRuntime().exec(cmdArray, null, dir);

         exitCode = p.waitFor();

         in = new BufferedReader(new InputStreamReader(p.getErrorStream(),
                charset));
      }

      String line;

      StringBuilder processErrors = null;

      String unknownMod = null;
      boolean emptySortFound = false;

      while (in != null && (line = in.readLine()) != null)
      {
         if (processErrors == null)
         {
            processErrors = new StringBuilder();
         }

         processErrors.append(String.format("%n%s", line));

         Matcher matcher = xindyModulePattern.matcher(line);

         if (matcher.matches())
         {
            unknownMod = invoker.getLabelWithValues(
              "diagnostics.unknown_language_or_codepage",
               matcher.group(1), matcher.group(2));
         }
         else
         {
            matcher = emptySortPattern.matcher(line);

            if (matcher.matches())
            {
               emptySortFound = true;
            }
            else
            {
               matcher = collapsedSortPattern.matcher(line);

               if (matcher.matches())
               {
                  emptySortFound = true;
               }
            }
         }
      }

      if (in != null)
      {
         in.close();
      }

      String unknownVar = null;

      if (transFile.exists())
      {
         in = new BufferedReader(new InputStreamReader(
                new FileInputStream(transFile), charset));

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

      if (emptySortFound)
      {
         addErrorMessage(invoker.getLabel("error.empty_sort"));
      }

      boolean deprecated = false;
      boolean depCheck = false;

      if (gloFile.exists())
      {
         in = new BufferedReader(new InputStreamReader(
                new FileInputStream(gloFile), charset));

         while ((line = in.readLine()) != null)
         {
            Matcher matcher;

            boolean retry = false;

            if (depCheck)
            {
               matcher = (deprecated ? makeindexOldEntryPattern.matcher(line):
                          xindyEntryPattern.matcher(line));
            }
            else
            {
               matcher = xindyEntryPattern.matcher(line);

               depCheck = true;

               if (!matcher.matches())
               {
                  matcher = xindyOldEntryPattern.matcher(line);
                  retry = true;
               }
            }

            if (matcher.matches())
            {
               if (retry)
               {
                  deprecated = true;
               }

               String sort = matcher.group(1);
               String key = matcher.group(2);

               GlossaryEntry entry = entryTable.get(key);

               if (entry == null)
               {
                  sort = sort.replaceAll("\\\\\\\\", "\\\\");
                  entry = new GlossaryEntry(key, sort);
                  entryTable.put(key, entry);

                  if (emptySortFound)
                  {
                     matcher = xindyEmptySortPattern.matcher(sort);

                     if (matcher.matches())
                     {
                        addDiagnosticMessage(invoker.getLabelWithValues(
                          "diagnostics.empty_sort", 
                           sort, key));
                        entry.setHasProblem(true);
                     }
                  }
               }

               entry.increment();
            }
         }

         in.close();
      }

      if (deprecated)
      {
         addDiagnosticMessage(invoker.getLabel("diagnostics.deprecated"));
      }

      if (exitCode > 0)
      {
         addErrorMessage(invoker.getLabelWithValues("error.app_failed",
            "Xindy", ""+exitCode));

         if (unknownVar != null)
         {
            addDiagnosticMessage(invoker.getLabelWithValues
               ("diagnostics.bad_attributes", istName, "xindy"));
         }
         else if (unknownMod != null)
         {
            addDiagnosticMessage(unknownMod);
         }
         else if (processErrors != null)
         {
            addDiagnosticMessage(invoker.getLabelWithValues(
               "diagnostics.app_err",
               "Xindy", processErrors.toString()));
         }
         else
         {
            addDiagnosticMessage(invoker.getLabel("diagnostics.app_err_null"));
         }
      }
      else if (entryTable.size() == 0)
      {
         addDiagnosticMessage(invoker.getLabelWithValues(
            "diagnostics.no_entries", label));
      }
   }

   public void makeindex(File dir, String baseName,
       boolean isWordOrder, String istName, Vector<String> extra)
      throws IOException,InterruptedException,GlossaryException
   {
      File makeindexApp = new File(invoker.getMakeIndexApp());

      if (!makeindexApp.exists())
      {
         throw new GlossaryException(invoker.getLabelWithValues(
            "error.no_indexer_app", 
            "makeindex", makeindexApp.getAbsolutePath()),
            invoker.getLabelWithValues("diagnostics.no_indexer", "makeindex"));
      }

      String transFileName = baseName+"."+transExt;

      File gloFile = new File(dir, baseName+"."+gloExt);

      String[] cmdArray;

      int n = (extra == null ? 0 : extra.size());

      if (isWordOrder)
      {
         cmdArray = new String[8+n];
         int idx = 0;
         cmdArray[idx++] = makeindexApp.getAbsolutePath();
         cmdArray[idx++] = "-s";
         cmdArray[idx++] = istName;
         cmdArray[idx++] = "-t";
         cmdArray[idx++] = transFileName;
         cmdArray[idx++] = "-o";
         cmdArray[idx++] = baseName+"."+glsExt;

         for (int i = 0; i < n; i++)
         {
            cmdArray[idx++] = extra.get(i);
         }

         cmdArray[idx++] = gloFile.getName();
      }
      else
      {
         cmdArray = new String[9+n];
         int idx = 0;
         cmdArray[idx++] = makeindexApp.getAbsolutePath();
         cmdArray[idx++] = "-l";
         cmdArray[idx++] = "-s";
         cmdArray[idx++] = istName;
         cmdArray[idx++] = "-t";
         cmdArray[idx++] = transFileName;
         cmdArray[idx++] = "-o";
         cmdArray[idx++] = baseName+"."+glsExt;

         for (int i = 0; i < n; i++)
         {
            cmdArray[idx++] = extra.get(i);
         }

         cmdArray[idx++] = gloFile.getName();
      }

      int exitCode = 0;
      BufferedReader in = null;

      invoker.getMessageSystem().aboutToExec(cmdArray, dir);

      // makeindex is limited to the range 1 ... 255
      Charset charset = StandardCharsets.ISO_8859_1;
      invoker.setEncoding(charset);

      if (invoker.isDryRunMode())
      {
         File transFile = new File(dir, transFileName);

         if (transFile.exists())
         {
            in = new BufferedReader(new InputStreamReader(
                   new FileInputStream(transFile), charset));
         }
      }
      else
      {
         Process p = Runtime.getRuntime().exec(cmdArray, null, dir);

         exitCode = p.waitFor();

         in = new BufferedReader(new InputStreamReader(p.getErrorStream(),
               charset));
      }

      String line;

      StringBuilder processErrors = null;

      while (in != null && (line = in.readLine()) != null)
      {
         if (processErrors == null)
         {
            processErrors = new StringBuilder();
         }

         processErrors.append(String.format("%n%s", line));
      }

      in.close();

      in = new BufferedReader(new InputStreamReader(
         new FileInputStream(new File(dir, transFileName)), charset));

      int numAccepted = 0;
      int numRejected = 0;
      String rejected = "";

      int numAttributes = 0;
      int numIgnored = 0;
      int numTooLong = 0;

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

            continue;
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

            continue;
         }

         matcher = makeindexTooLongPattern.matcher(line);

         if (matcher.matches())
         {
            numTooLong++;
         }
      }

      in.close();

      boolean deprecated = false;
      boolean depCheck = false;

      if (gloFile.exists())
      {
         in = new BufferedReader(new InputStreamReader(
            new FileInputStream(gloFile), charset));

         while ((line = in.readLine()) != null)
         {
            Matcher matcher;
            boolean depRetry = false;

            if (depCheck)
            {
               matcher = (deprecated ?
                          makeindexOldEntryPattern.matcher(line) :
                          makeindexEntryPattern.matcher(line));
            }
            else
            {
               matcher = makeindexEntryPattern.matcher(line);

               depCheck = true;

               if (!matcher.matches())
               {
                  matcher = makeindexOldEntryPattern.matcher(line);

                  depRetry = true;
               }
            }

            if (matcher.matches())
            {
               if (depRetry)
               {
                  deprecated = true;
               }

               String sort = matcher.group(1);
               String key = matcher.group(2);

               GlossaryEntry entry = entryTable.get(key);

               if (entry == null)
               {
                  entry = new GlossaryEntry(key, sort);
                  entryTable.put(key, entry);
               }

               entry.increment();
            }
         }

         in.close();
      }

      if (exitCode > 0)
      {
         addErrorMessage(invoker.getLabelWithValues("error.app_failed",
            "Makeindex", exitCode));

         if (processErrors != null)
         {
            addDiagnosticMessage(invoker.getLabelWithValues(
               "diagnostics.app_err",
               "Makeindex", processErrors.toString()));
         }
         else
         {
            addDiagnosticMessage(invoker.getLabel("diagnostics.app_err_null"));
         }
      }
      else if (numRejected > 0)
      {
         addErrorMessage(invoker.getLabelWithValues("error.entries_rejected", 
           numRejected));

         if (numAccepted == 0)
         {
            addDiagnosticMessage(invoker.getLabelWithValues(
              "diagnostics.makeindex_reject_all", label));
         }

         if (numIgnored > 0)
         {
            addDiagnosticMessage(invoker.getLabelWithValues
               ("diagnostics.bad_attributes", istName, "makeindex"));
         }

         if (numTooLong > 0)
         {
            if (deprecated)
            {
               addDiagnosticMessage(invoker.getLabelWithValues(
                "diagnostics.old_too_long", numTooLong));
            }
            else
            {
               addDiagnosticMessage(invoker.getLabelWithValues(
                "diagnostics.too_long", numTooLong));
            }
         }
      }
      else if (numAccepted == 0)
      {
         if (label.equals("main"))
         {
            addDiagnosticMessage(invoker.getLabel(
               "diagnostics.no_entries_main"));
         }
         else
         {
            addDiagnosticMessage(invoker.getLabelWithValues(
               "diagnostics.no_entries", label));
         }

         addErrorMessage(invoker.getLabelWithValues("error.no_entries", label));
      }
   }

   public int getNumEntries()
   {
      return entryTable.size();
   }

   public String[] getEntryLabels()
   {
      int n = entryTable.size();

      String[] array = new String[n];

      int i = 0;

      for (Enumeration<String> en = entryTable.keys(); en.hasMoreElements();)
      {
         array[i] = en.nextElement();
         i++;
      }

      return array;
   }

   public Integer getEntryCount(String key)
   {
      GlossaryEntry entry = entryTable.get(key);

      return entry == null ? 0 : entry.getCount();
   }

   public String getEntrySort(String key)
   {
      GlossaryEntry entry = entryTable.get(key);

      return entry == null ? null : entry.getSort();
   }

   public boolean hasProblem(String key)
   {
      GlossaryEntry entry = entryTable.get(key);

      return entry == null ? false : entry.hasProblem();
   }

   public Integer getEntryCount(int entryIdx)
   {
      int i = 0;

      for (Enumeration<GlossaryEntry> en = entryTable.elements();
         en.hasMoreElements();)
      {
         GlossaryEntry val = en.nextElement();

         if (i == entryIdx) return val.getCount();

         i++;
      }

      return 0;
   }

   public String getEntrySort(int entryIdx)
   {
      int i = 0;

      for (Enumeration<GlossaryEntry> en = entryTable.elements();
         en.hasMoreElements();)
      {
         GlossaryEntry val = en.nextElement();

         if (i == entryIdx) return val.getSort();

         i++;
      }

      return null;
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
      String mappedLang = invoker.getLanguage(language);

      if (!mappedLang.equals(language))
      {
         addDiagnosticMessage(invoker.getLabelWithValues(
           "diagnostics.mapped_lang", language, mappedLang));
         this.language = mappedLang;
      }
      else
      {
         this.language = language;
      }
   }

   public void setCodePage(String codepage)
   {
      this.codepage = codepage;
   }

   public String displayCodePage()
   {
      return codepage == null ?
         "<font class=error>"+invoker.getLabel("error.unknown")+"</font>" :
         codepage;
   }

   public String displayLanguage()
   {
      return language == null ?
         "<font class=error>"+invoker.getLabel("error.unknown")+"</font>" :
         language;
   }

   public void addErrorMessage(String mess)
   {
      if (errorMessage == null)
      {
         errorMessage = new StringBuilder(mess);
      }
      else
      {
         errorMessage.append(String.format("%n%s", mess));
      }
   }

   public String getErrorMessages()
   {
      return errorMessage == null ? null : errorMessage.toString();
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
      return diagnosticMessage == null ? null : diagnosticMessage.toString();
   }

   public void addDiagnosticMessage(String mess)
   {
      if (diagnosticMessage == null)
      {
         diagnosticMessage = new StringBuilder(mess);
      }
      else
      {
         diagnosticMessage.append(String.format("<p>%s", mess));
      }
   }


   public void checkNonAsciiLabels()
   {
      int numFound = 0;
      StringBuilder builder = null;

      for (Enumeration<String> en=entryTable.keys(); en.hasMoreElements();)
      {
         String key = en.nextElement();

         for (int i = 0, n = key.length(); i < n; i++)
         {
            int c = key.charAt(i);

            if (c > '|' || c < '!' || c == '#' || c == '$' || c == '&'
             || c == '\\' || c == '^' || c == '_')
            {
               numFound++;

               if (builder == null)
               {
                  builder = new StringBuilder(key);
               }
               else
               {
                  builder.append(", ");
                  builder.append(key);
               }

               break;
            }
         }
      }

      if (numFound > 0)
      {
         addDiagnosticMessage(invoker.getLabelWithValues(
           "diagnostics.labels_with_problem_char",
              numFound, label, builder.toString()));
      }
   }

   public String label, transExt, glsExt, gloExt;

   private String language, codepage;

   private StringBuilder errorMessage, diagnosticMessage;

   private Hashtable<String,GlossaryEntry> entryTable;

   private MakeGlossariesInvoker invoker;

   private static final Pattern makeindexAcceptedPattern 
      = Pattern.compile(".*?(\\d+)\\s+entries\\s+accepted.*(\\d+)\\s+rejected.*");

   private static final Pattern makeindexIstAttributePattern
      = Pattern.compile(".*?(\\d+)\\s+attributes\\s+redefined.*(\\d+).*ignored.*");

   private static final Pattern makeindexTooLongPattern
      = Pattern.compile("\\s+-- First argument too long \\(max \\d+\\)\\.");

   private static final Pattern xindyIstPattern
      = Pattern.compile(".*variable (.*) has no value.*");

   private static final Pattern emptySortPattern
      = Pattern.compile(".*Would replace complete index key by empty string.*");

   private static final Pattern collapsedSortPattern
      = Pattern.compile(".*index 0 should be less than the length of the string.*");

   private static final Pattern xindyModulePattern
      = Pattern.compile(".*Cannot\\s+locate\\s+xindy\\s+module\\s+for\\s+language\\s+([a-zA-Z0-9\\-]+)\\s+in\\s+codepage\\s+([a-zA-Z0-9\\-]+)..*");

   private static final Pattern makeindexOldEntryPattern
      = Pattern.compile("\\\\glossaryentry\\{(.*?)\\?\\\\glossaryentryfield\\{(.*?)\\}.*");

   private static final Pattern makeindexEntryPattern
      = Pattern.compile("\\\\glossaryentry\\{(.*?)\\?(?:\\\\gls(?:no)?nextpages\\s)?\\\\glossentry\\{(.*?)\\}.*");

   private static final Pattern xindyOldEntryPattern = Pattern.compile(
   "\\(indexentry\\s+:tkey\\s*\\(\\s*\\(\\s*\"(.*?)\"\\s+\"\\\\\\\\glossaryentryfield\\{(.*?)\\}.*\".*");

   private static final Pattern xindyEntryPattern = Pattern.compile(
   "\\(indexentry\\s+:tkey\\s*\\(\\s*\\(\\s*\"(.*?)\"\\s+\"(?:\\\\\\\\gls(?:no)?nextpages\\s)?\\\\\\\\glossentry\\{(.*?)\\}.*\".*");

   private static final Pattern xindyEmptySortPattern = Pattern.compile(
   "(?:\\$|\\{\\\\[a-zA-Z@]+ *\\}|\\\\[a-zA-Z@]+ *)+");
}

class GlossaryEntry
{
   public GlossaryEntry(String label, String sort)
   {
      this.label = label;
      this.sort = sort;
      this.count = 0;
      this.hasProblem = false;
   }

   public void increment()
   {
      count++;
   }

   public int getCount()
   {
      return count;
   }

   public String getSort()
   {
      return sort;
   }

   public String getLabel()
   {
      return label;
   }

   public boolean hasProblem()
   {
      return hasProblem;
   }

   public void setHasProblem(boolean hasProblem)
   {
      this.hasProblem = hasProblem;
   }

   private int count = 0;
   private String label, sort;
   private boolean hasProblem;
}

