package com.dickimawbooks.makeglossariesgui;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.Element;

public class Glossaries
{
   public Glossaries(MakeGlossariesInvoker invoker, String istName, String order)
   {
      this.invoker = invoker;
      this.istName = istName;
      this.order = order;
      glossaryList = new Vector<Glossary>();
   }

   public Glossaries(MakeGlossariesInvoker invoker)
   {
      this.invoker = invoker;
      glossaryList = new Vector<Glossary>();
   }

   public void clear()
   {
      glossaryList.clear();
   }

   public void add(Glossary glossary)
   {
      glossaryList.add(glossary);
   }

   public Glossary getGlossary(int i)
   {
      return glossaryList.get(i);
   }

   public Glossary getGlossary(String label)
   {
      for (int i = 0, n = glossaryList.size(); i < n; i++)
      {
         Glossary g = glossaryList.get(i);

         if (g.label.equals(label))
         {
            return g;
         }
      }

      return null;
   }

   public static Glossaries loadGlossaries(MakeGlossariesInvoker invoker, File file)
      throws IOException
   {
      Glossaries glossaries = new Glossaries(invoker);

      Hashtable<String,String> languages = new Hashtable<String,String>();
      Hashtable<String,String> codePages = new Hashtable<String,String>();

      invoker.getMessageSystem().message(
       invoker.getLabelWithValue("message.loading", file.toString()));
      BufferedReader in = new BufferedReader(new FileReader(file));

      String line;

      boolean override = invoker.getProperties().isOverride();
      glossaries.noidx = false;

      while ((line = in.readLine()) != null)
      {
         Matcher matcher = newGlossaryPattern.matcher(line);

         if (matcher.matches())
         {
            glossaries.add(new Glossary(invoker, matcher.group(1), matcher.group(2),
              matcher.group(3), matcher.group(4)));

            continue;
         }

         matcher = istFilePattern.matcher(line);

         if (matcher.matches())
         {
            glossaries.istName = matcher.group(1);
            continue;
         }

         matcher = orderPattern.matcher(line);

         if (matcher.matches())
         {
            glossaries.order = matcher.group(1);
            continue;
         }

         if (!override)
         {
            matcher = languagePattern.matcher(line);

            if (matcher.matches())
            {
               String label = matcher.group(1);

               String language = matcher.group(2);

               if (language.isEmpty())
               {
                  language = invoker.getDefaultLanguage();

                  String variant = invoker.getDefaultXindyVariant();

                  if (variant != null && !variant.isEmpty())
                  {
                     language = String.format("%s-%s", language, variant);
                  }

                  glossaries.addErrorMessage(invoker.getLabelWithValue(
                     "error.no_language", label));
                  glossaries.addDiagnosticMessage(invoker.getLabelWithValues(
                     "diagnostics.no_language", label, language));
               }

               languages.put(label, language);

               continue;
            }

            matcher = codepagePattern.matcher(line);

            if (matcher.matches())
            {
               String label = matcher.group(1);

               String code = matcher.group(2);

               if (code.isEmpty())
               {
                  code = invoker.getDefaultCodePage();

                  glossaries.addErrorMessage(invoker.getLabelWithValue(
                     "error.no_codepage", label));
                  glossaries.addDiagnosticMessage(invoker.getLabelWithValues(
                     "diagnostics.no_codepage", label, code));
               }

               codePages.put(label, code);

               continue;
            }
         }

         matcher = glsreferencePattern.matcher(line);

         if (matcher.matches())
         {
            glossaries.noidx = true;
            continue;
         }

         matcher = extraMakeIndexOptsPattern.matcher(line);

         if (matcher.matches())
         {
            String opts = matcher.group(1);

            glossaries.extraMakeIndexOpts = splitArgs(opts);
         }
      }

      in.close();

      if (glossaries.noidx)
      {
         glossaries.addDiagnosticMessage(invoker.getLabel("diagnostics.noidx"));
      }
      else if (!override)
      {
         for (Enumeration<String> en = languages.keys(); en.hasMoreElements();)
         {
            String label = en.nextElement();
            String language = languages.get(label);

            Glossary g = glossaries.getGlossary(label);

            if (g == null)
            {
               glossaries.addErrorMessage(invoker.getLabelWithValues(
                  "error.language_no_glossary", language, label));
               glossaries.addDiagnosticMessage(invoker.getLabelWithValues(
                  "diagnostics.language_no_glossary", language, label));
            }
            else
            {
               g.setLanguage(language);
            }
         }

         for (Enumeration<String> en = codePages.keys(); en.hasMoreElements();)
         {
            String label = en.nextElement();
            String code = codePages.get(label);

            Glossary g = glossaries.getGlossary(label);

            if (g == null)
            {
               glossaries.addErrorMessage(invoker.getLabelWithValues(
                  "error.codepage_no_glossary", code, label));
               glossaries.addDiagnosticMessage(invoker.getLabelWithValues(
                  "diagnostics.codepage_no_glossary", code, label));
            }
            else
            {
               g.setCodePage(code);
            }
         }
      }

      return glossaries;
   }

   public static Vector<String> splitArgs(String str)
   {
      Vector<String> args = new Vector<String>();
      StringBuilder builder = null;
      int delim = -1;

      for (int i = 0, n = str.length(); i < n; i++)
      {
         char c = str.charAt(i);

         if (builder == null)
         {
            if (Character.isWhitespace(c))
            {
               continue;
            }

            builder = new StringBuilder();

            if (c == '\'' || c == '"')
            {
               delim = c;
            }
            else
            {
               delim = -1;
               builder.append(c);
            }
         }
         else if (c == '\\')
         {
            i++;

            if (i < n)
            {
               c = str.charAt(i);
            }

            builder.append(c);
         }
         else if (delim == c
              || (delim == -1 && Character.isWhitespace(c)))
         {
            args.add(builder.toString());
            builder = null;
         }
         else 
         {
            builder.append(c);
         }
      }

      if (builder != null)
      {
         args.add(builder.toString());
      }

      return args;
   }

   public void process()
      throws GlossaryException,IOException,InterruptedException
   {
      File file = invoker.getFile();

      String baseName = file.getName();

      int idx = baseName.lastIndexOf(".");

      if (idx != -1)
      {
         baseName = baseName.substring(0, idx);
      }

      File dir = file.getParentFile();

      if (!noidx)
      {
         String mess = getIndexerError();

         if (mess != null)
         {
            throw new GlossaryException(mess);
         }

         String lang = null;
         String codePage = null;

         if (invoker.getProperties().isOverride())
         {
            lang = invoker.getDefaultLanguage();
            codePage = invoker.getDefaultCodePage();

            String variant = invoker.getDefaultXindyVariant();

            if (variant != null && !variant.isEmpty())
            {
               lang = String.format("%s-%s", lang, variant);
            }
         }

         if (glossaryList.isEmpty())
         {
            addDiagnosticMessage(invoker.getLabel("diagnostics.no_glossaries"));
            addErrorMessage(invoker.getLabel("error.no_glossaries"));
         }

         for (int i = 0, n = getNumGlossaries(); i < n; i++)
         {
            Glossary g = getGlossary(i);

            try
            {
               if (useXindy())
               {
                  if (lang != null)
                  {
                     g.setLanguage(lang);
                  }

                  if (codePage != null)
                  {
                     g.setCodePage(codePage);
                  }

                  g.xindy(dir, baseName, isWordOrder(), istName);
               }
               else
               {
                  g.makeindex(dir, baseName, isWordOrder(), istName,
                    extraMakeIndexOpts);
               }

               String errMess = g.getErrorMessages();

               if (errMess != null)
               {
                  addErrorMessage(errMess);
               }
            }
            catch (IOException e)
            {
               File istFile = new File(dir, istName);

               if (!istFile.exists())
               {
                  throw new GlossaryException(invoker.getLabelWithValue("error.no_ist", istName),
                     invoker.getLabel("diagnostics.no_ist"), e);
               }
               else
               {
                  throw e;
               }
            }
         }
      }

      // Skip the log file check when in batch mode

      if (invoker.isBatchMode()) return;

      // Now check the log file for any problems

      File log = new File(dir, baseName+".log");

      if (!log.exists())
      {
         addDiagnosticMessage(invoker.getLabelWithValue(
           "diagnostics.no_log", log.getAbsolutePath()));
         return;
      }

      BufferedReader reader = null;

      try
      {
         reader = new BufferedReader(new FileReader(log));

         String line;

         while ((line = reader.readLine()) != null)
         {
            Matcher m = glossariesStyPattern.matcher(line);

            if (m.matches())
            {
               Calendar cal = Calendar.getInstance();

               String year = m.group(1);
               String mon  = m.group(2);
               String day  = m.group(3);
               String vers = m.group(4);

               try
               {
                  cal.set(Integer.parseInt(year),
                          Integer.parseInt(mon),
                          Integer.parseInt(day));
               }
               catch (NumberFormatException e)
               {// shouldn't happen
                  invoker.getMessageSystem().debug(e);
               }

               Calendar v416 = Calendar.getInstance();
               v416.set(2015, 7, 8);

               if (cal.before(v416))
               {
                  addDiagnosticMessage(invoker.getLabelWithValues(
                    "diagnostics.oldversion", vers, 
                    // use same format as LaTeX package info:
                    String.format("%s/%s/%s", year, mon, day)
                  ));
               }

               continue;
            }

            m = wrongGloTypePattern.matcher(line);

            if (m.matches())
            {
               String type = m.group(2);

               addDiagnosticMessage(invoker.getLabelWithValue(
                 "diagnostics.wrong_type", type));
               addErrorMessage(invoker.getLabelWithValue(
                 "error.wrong_type", type));

               continue;
            }

            if (invoker.isDocDefCheckOn())
            {
               m = docDefsPattern.matcher(line);

               if (m.matches())
               {
                  File f = new File(dir, baseName+".glsdefs");

                  addDiagnosticMessage(invoker.getLabelWithValue(
                    "diagnostics.doc_defs", f.getAbsolutePath()));

                  continue;
               }
            }

            if (invoker.isMissingLangCheckOn())
            {
               m = missingLangPattern.matcher(line);

               if (m.matches())
               {
                  addDiagnosticMessage(invoker.getLabelWithValue(
                    "diagnostics.missing_lang", m.group(1)));
                  continue;
               }
            }

            m = missingStyPattern.matcher(line);

            if (m.matches())
            {
               String sty = m.group(1);

               if (sty.equals("datatool-base"))
               {
                  addDiagnosticMessage(invoker.getLabel(
                     "diagnostics.missing_datatool_base"));
               }
               else
               {
                  addDiagnosticMessage(invoker.getLabelWithValue(
                     "diagnostics.missing_sty", sty));
               }

               continue;
            }

            m = warningPattern.matcher(line);

            if (m.matches())
            {
               StringBuilder builder = new StringBuilder(line);

               while ((line = reader.readLine()) != null)
               {
                  if (line.isEmpty())
                  {
                     break;
                  }

                  builder.append(line);
               }

               String msg = builder.toString();

               addDiagnosticMessage(msg);

               if (noidx)
               {
                  m = emptyNoIdxGlossaryPattern.matcher(msg);

                  if (m.matches())
                  {
                     String type = m.group(1);

                     if (!glossaryList.contains(type))
                     {
                        addDiagnosticMessage(invoker.getLabelWithValue(
                          "diagnostics.wrong_type_noidx", type));
                     }
                  }
               }

               continue;
            }

            m = systemPattern.matcher(line);

            if (m.matches())
            {
               StringBuilder builder = new StringBuilder(line);

               while ((line = reader.readLine()) != null)
               {
                  if (line.isEmpty())
                  {
                     break;
                  }

                  builder.append(line);
               }

               m = disabledSystemPattern.matcher(builder);

               if (m.matches())
               {
                  addDiagnosticMessage(invoker.getLabelWithValue(
                     "diagnostics.shell_disabled", m.group(1)));
               }

               continue;
            }

            m = undefControlSequencePattern.matcher(line);

            if (m.matches())
            {
               line = reader.readLine();

               addDiagnosticMessage(invoker.getLabelWithValue(
                 "diagnostics.undef_cs", line));

               continue;
            }

            m = unknownOptPattern.matcher(line);

            if (m.matches())
            {
               addDiagnosticMessage(invoker.getLabelWithValue(
                 "diagnostics.undef_opt", m.group(1)));

               continue;
            }
         }
      }
      finally
      {
         if (reader != null)
         {
            reader.close();
         }
      }     
   }

   public String displayGlossaryList()
   {
      String str = null;

      for (int i = 0, n = glossaryList.size(); i < n; i++)
      {
         Glossary glossary = glossaryList.get(i);

         if (str == null)
         {
            str = glossary.label;
         }
         else
         {
            str += ", " + glossary.label;
         }
      }

      return str;
   }

   public String getDisplayGlossaryListError()
   {
      return getNumGlossaries() == 0 ?
         invoker.getLabel("error.no_glossaries"):
         null;
   }

   public String getOrder()
   {
      return order;
   }

   public boolean isWordOrder()
   {
      if (order == null) return false;
      return order.equals("word");
   }

   public boolean isLetterOrder()
   {
      if (order == null) return false;
      return order.equals("letter");
   }

   public boolean isValidOrder()
   {
      if (order == null) return false;

      return order.equals("word") || order.equals("letter");
   }

   public String getOrderError()
   {
      if (order == null) return invoker.getLabel("error.missing_order");

      return isValidOrder() ? null : invoker.getLabel("error.invalid_order");
   }

   public String displayFormat()
   {
      if (istName == null) return invoker.getLabel("error.unknown");

      return useXindy() ? "xindy" : "makeindex";
   }

   public String getIstName()
   {
      return istName;
   }

   public String getIstNameError()
   {
      return istName == null ? invoker.getLabel("error.missing_ist") : null;
   }

   public boolean useXindy()
   {
      if (istName == null) return false;

      return istName.endsWith(".xdy");
   }

   public String getIndexerError()
   {
      if (noidx) return null;

      if (istName == null)
      {
         return invoker.getLabel("error.cant_determine_indexer");
      }

      if (useXindy())
      {
         if (invoker.getXindyApp() == null)
         {
            return invoker.getLabel("error.no_xindy");
         }
      }
      else
      {
         if (invoker.getMakeIndexApp() == null)
         {
            return invoker.getLabel("error.no_makeindex");
         }
      }

      return null;
   }

   public int getNumGlossaries()
   {
      return glossaryList.size();
   }

   public String getDiagnostics()
   {
      if (!noidx && istName == null && order == null)
      {
         if (glossaryList.size() == 0)
         {
            return invoker.getFileName().toLowerCase().endsWith(".aux") ?
                   invoker.getLabel("diagnostics.no_glossaries"):
                   invoker.getLabel("diagnostics.not_aux");
         }
         else
         {
            return invoker.getLabel("diagnostics.no_makeglossaries");
         }
      }

      String mess = getIndexerError();

      if (mess != null)
      {
         return mess + "\n" + invoker.getLabelWithValue("diagnostics.no_indexer", 
            displayFormat());
      }

      mess = getDiagnosticMessages();

      for (int i = 0, n = getNumGlossaries(); i < n; i++)
      {
         String errmess = getGlossary(i).getDiagnosticMessages();

         if (errmess != null)
         {
            if (mess == null)
            {
               mess = errmess;
            }
            else
            {
               mess = String.format("%s%n%s", mess, errmess);
            }
         }
      }

      return mess;
   }

   private String getDiagnosticMessages()
   {
      return diagnosticMessages == null ? null : diagnosticMessages.toString();
   }

   public void addDiagnosticMessage(String mess)
   {
      if (diagnosticMessages == null)
      {
         diagnosticMessages = new StringBuilder(mess);
      }
      else
      {
         diagnosticMessages.append(String.format("%n%s", mess));
      }
   }

   public void addErrorMessage(String mess)
   {
      if (errorMessages == null)
      {
         errorMessages = new StringBuilder(mess);
      }
      else
      {
         errorMessages.append(String.format("%n%s", mess));
      }
   }

   public String getErrorMessages()
   {
      return errorMessages == null ? null : errorMessages.toString();
   }

   public static int getNumFields()
   {
      return fields.length;
   }

   public static String getFieldTag(int i)
   {
      return fields[i];
   }

   public static Element getFieldElement(HTMLDocument doc, int i)
   {
      return doc.getElement(fields[i]);
   }

   public static Element getFieldLabelElement(HTMLDocument doc, int i)
   {
      return doc.getElement(fields[i]+"label");
   }

   public String getFieldLabel(int i)
   {
      return invoker.getLabel("main", fields[i]);
   }

   public String getField(int i)
   {
      switch (i)
      {
         case AUX: return invoker.getFileName();
         case ORDER: return getOrder();
         case IST: return getIstName();
         case INDEXER: return displayFormat();
         case GLOSSARIES: return displayGlossaryList();
      }

      return null;
   }

   public String getFieldError(int i)
   {
      switch (i)
      {
         case AUX: return invoker.getFileName() == null ?
            invoker.getLabel("error.no_such_file") :
            null;
         case ORDER: return getOrderError();
         case IST: return getIstNameError();
         case INDEXER: return getIndexerError();
         case GLOSSARIES: return getDisplayGlossaryListError();
      }

      return null;
   }

   private Vector<Glossary> glossaryList;
   private String istName;

   private Vector<String> extraMakeIndexOpts;

   private boolean noidx = false;

   private String order;

   private StringBuilder errorMessages, diagnosticMessages;

   private static final Pattern newGlossaryPattern
      = Pattern.compile("\\\\@newglossary\\{([^\\}]+)\\}\\{([^\\}]+)\\}\\{([^\\}]+)\\}\\{([^\\}]+)\\}");

   private static final Pattern istFilePattern
      = Pattern.compile("\\\\@istfilename\\{([^\\}]+)\\}");

   private static final Pattern orderPattern
      = Pattern.compile("\\\\@glsorder\\{([^\\}]+)\\}");

   private static final Pattern languagePattern
      = Pattern.compile("\\\\@xdylanguage\\{([^\\}]+)\\}\\{([^\\}]*)\\}");

   private static final Pattern codepagePattern
      = Pattern.compile("\\\\@gls@codepage\\{([^\\}]+)\\}\\{([^\\}]*)\\}");

   private static final Pattern glsreferencePattern
      = Pattern.compile("\\\\@gls@reference\\{.*?\\}\\{.*?\\}\\{.*\\}");

   private static final Pattern extraMakeIndexOptsPattern
      = Pattern.compile("\\\\@gls@extramakeindexopts\\{(.*)\\}");

   private static final Pattern glossariesStyPattern
      = Pattern.compile("Package: glossaries (\\d+)\\/(\\d+)\\/(\\d+) v([\\d\\.a-z]+) \\(NLCT\\).*");

   private static final Pattern glossariesXtrStyPattern
      = Pattern.compile("Package: glossaries-extra (\\d+)\\/(\\d+)\\/(\\d+) v([\\d\\.a-z]+) \\(NLCT\\).*");

   private static final Pattern wrongGloTypePattern
      = Pattern.compile("No file (.*?)\\.\\\\@glotype@(.*?)@in\\s*\\.");

   private static final Pattern docDefsPattern
      = Pattern.compile("\\\\openout\\d+\\s*=\\s*`.*\\.glsdefs'.");

   private static final Pattern missingLangPattern
      = Pattern.compile("Package glossaries Warning: No language module detected for `(.*)'.\\s*");

   private static final Pattern missingStyPattern
      = Pattern.compile(".*`(.*?)\\.sty' not found.*");

   private static final Pattern warningPattern
      = Pattern.compile("Package glossaries(-extra)? Warning: .*");

   private static final Pattern emptyNoIdxGlossaryPattern
      = Pattern.compile(".*Empty glossary for \\\\printnoidxglossary\\[type=\\{(.*?)\\}\\]\\..*");

   private static final Pattern systemPattern
      = Pattern.compile("runsystem\\(.*");

   private static final Pattern disabledSystemPattern
      = Pattern.compile("runsystem\\((.*)\\)\\.\\.\\.disabled\\..*");

   private static final Pattern undefControlSequencePattern
      = Pattern.compile(".* Undefined control sequence.");

   private static final Pattern unknownOptPattern
      = Pattern.compile(".*Unknown option `(.*)' for package `glossaries'.*");

   private static final String[] fields =
   {
      "aux",
      "order",
      "ist",
      "indexer",
      "list"
   };

   public static final int AUX=0, ORDER=1, IST=2, INDEXER=3, GLOSSARIES=4;

   private MakeGlossariesInvoker invoker;
}
