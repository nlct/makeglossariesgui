package com.dickimawbooks.makeglossariesgui;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.nio.charset.Charset;
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

   public static Glossaries loadGlossaries(MakeGlossariesInvoker invoker, 
    File file)
      throws IOException
   {
      Glossaries glossaries = new Glossaries(invoker);

      Hashtable<String,String> languages = new Hashtable<String,String>();
      Hashtable<String,String> codePages = new Hashtable<String,String>();

      invoker.getMessageSystem().message(
       invoker.getLabelWithValues("message.loading", file));
      BufferedReader in = new BufferedReader(new InputStreamReader(
              new FileInputStream(file), invoker.getEncoding()));

      String line;

      boolean override = invoker.getProperties().isOverride();

      while ((line = in.readLine()) != null)
      {
         glossaries.parseAux(file.getParentFile(),
            line, override, languages, codePages);
      }

      in.close();

      if (glossaries.noidx)
      {
         glossaries.addDiagnosticMessage(invoker.getLabel("diagnostics.noidx"));
      }
      else if (glossaries.requiresBib2Gls && glossaries.istName == null)
      {
         String base = file.getName();

         if (base.endsWith(".aux"))
         {
            base = base.substring(0, base.length()-4);
         }

         glossaries.addDiagnosticMessage(String.format("%s %s", 
             invoker.getLabelWithValues(
             "diagnostics.bib2gls", base), 
             invoker.getLabel("diagnostics.build")));

         if (!glossaries.hasRecords && !glossaries.selectionAllFound)
         {
            glossaries.addDiagnosticMessage(invoker.getLabel(
             "diagnostics.bib2gls_norecords"));
         }

         // Is bib2gls on the system PATH?

         File bib2gls = invoker.findApp("bib2gls", "bib2gls.exe", "bib2gls.sh");

         if (bib2gls == null)
         {
            glossaries.addDiagnosticMessage(invoker.getLabelWithValues(
              "error.missing_application", "bib2gls", System.getenv("PATH")));
         }
         else
         {
            glossaries.addDiagnosticMessage(invoker.getLabelWithValues(
              "message.found", bib2gls));
         }
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

      if (glossaries.requiresBib2Gls)
      {
      }

      return glossaries;
   }

   private void parseAux(File dir, String line, boolean override,
     Hashtable<String,String> languages, Hashtable<String,String> codePages)
   throws IOException
   {
      Matcher matcher = newGlossaryPattern.matcher(line);

      if (matcher.matches())
      {
         add(new Glossary(invoker, matcher.group(1), matcher.group(2),
           matcher.group(3), matcher.group(4)));

         return;
      }

      matcher = istFilePattern.matcher(line);

      if (matcher.matches())
      {
         istName = matcher.group(1);
         return;
      }

      matcher = bib2glsPattern.matcher(line);

      if (matcher.matches())
      {
         requiresBib2Gls = true;

         matcher = selectAllPattern.matcher(matcher.group(1));

         if (matcher.matches())
         {
            selectionAllFound = true;
         }

         return;
      }

      matcher = orderPattern.matcher(line);

      if (matcher.matches())
      {
         order = matcher.group(1);
         return;
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

               addErrorMessage(invoker.getLabelWithValues(
                  "error.no_language", label));
               addDiagnosticMessage(invoker.getLabelWithValues(
                  "diagnostics.no_language", label, language));
            }

            languages.put(label, language);

            return;
         }

         matcher = codepagePattern.matcher(line);

         if (matcher.matches())
         {
            String label = matcher.group(1);

            String code = matcher.group(2);

            if (code.isEmpty())
            {
               code = invoker.getDefaultCodePage();

               addErrorMessage(invoker.getLabelWithValues(
                  "error.no_codepage", label));
               addDiagnosticMessage(invoker.getLabelWithValues(
                  "diagnostics.no_codepage", label, code));
            }

            codePages.put(label, code);

            return;
         }
      }

      matcher = glsreferencePattern.matcher(line);

      if (matcher.matches())
      {
         noidx = true;
         return;
      }

      matcher = glsrecordPattern.matcher(line);

      if (matcher.matches())
      {
         hasRecords = true;
         return;
      }

      matcher = extraMakeIndexOptsPattern.matcher(line);

      if (matcher.matches())
      {
         String opts = matcher.group(1);

         extraMakeIndexOpts = splitArgs(opts);

         return;
      }

      matcher = inputPattern.matcher(line);

      if (matcher.matches())
      {
         BufferedReader in = null;

         String aux = matcher.group(1)+".aux";

         String[] split = aux.split("/");

         File f = dir;

         for (int i = 0; i < split.length; i++)
         {
            f = new File(f, split[i]);
         }

         try
         {
            invoker.getMessageSystem().message(
              invoker.getLabelWithValues("message.loading", f));

            in = new BufferedReader(new InputStreamReader(
              new FileInputStream(f), invoker.getEncoding()));

            while ((line = in.readLine()) != null)
            {
               parseAux(f.getParentFile(), line, override, languages, codePages);
            }
         }
         finally
         {
            if (in != null)
            {
               in.close();
            }
         }

         return;
      }
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

      Vector<Charset> indexerEncodings = new Vector<Charset>();

      if (!noidx)
      {
         String mess = getIndexerError();

         if (mess != null)
         {
            if (invoker.isBatchMode())
            {
               throw new GlossaryException(mess);
            }

            addErrorMessage(mess);
            parseLog(dir, baseName);

            return;
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
                  throw new GlossaryException(invoker.getLabelWithValues(
                     "error.no_ist", istName),
                     invoker.getLabel("diagnostics.no_ist"), e);
               }
               else
               {
                  throw e;
               }
            }

            Charset encoding = invoker.getEncoding();

            if (!indexerEncodings.contains(encoding))
            {
               indexerEncodings.add(encoding);
            }
         }
      }

      // Skip the log file check when in batch mode

      if (invoker.isBatchMode()) return;

      parseLog(dir, baseName);

      // Now the log has been parsed, the document encodings should
      // be known (if support has been provided).

      if (supportedEncodings == null)
      {
         // No document encoding detected, assume ASCII (which is
         // fine for makeindex). Provide advisory note for xindy.

         if (useXindy())
         {
            addAdvisoryMessage(invoker.getLabel(
              "diagnostics.xindy_no_doc_encoding"));
         }
      }
      else
      {
         for (Charset encoding: indexerEncodings)
         {
            if (!supportedEncodings.contains(encoding))
            {
               addAdvisoryMessage(invoker.getLabelWithValues(
                "diagnostics.doc_indexer_encoding_mismatch", encoding,
                 supportedEncodings.size(), inputEnc));
            }
         }
      }
   }

   public void parseLog(File dir, String baseName)
     throws IOException
   {

      // Now check the log file for any problems

      File log = new File(dir, baseName+".log");

      if (!log.exists())
      {
         addDiagnosticMessage(invoker.getLabelWithValues(
           "diagnostics.no_log", log.getAbsolutePath()));

         // Are there other .aux files in the same directory?

         File[] list = dir.listFiles(new FileFilter()
          {
             public boolean accept(File f)
             {
                return f.getName().toLowerCase().endsWith(".aux");
             }
          });

         if (list != null && list.length > 1)
         {
            addDiagnosticMessage(invoker.getLabelWithValues(
              "diagnostics.multi_aux", dir));
         }

         return;
      }

      BufferedReader reader = null;

      boolean checkNonAscii = false;

      String vers = null;

      String build = null;

      int makeglossDisabledLine = -1;
      int makeglossRestoredLine = -1;

      try
      {
         reader = new BufferedReader(new InputStreamReader(
              new FileInputStream(log), invoker.getEncoding()));

         String line;

         while ((line = reader.readLine()) != null)
         {
            Matcher m;

            if (line.startsWith("Package glossaries Info:") && !line.endsWith("."))
            {
               String nextLine;

               while ((nextLine = reader.readLine()) != null)
               {
                  line += nextLine;

                  if (nextLine.endsWith("."))
                  {
                     break;
                  }
               }

               m = makeglossDisabledPattern.matcher(line);

               if (m.matches())
               {
                  String lineRef = m.group(1);

                  addDiagnosticMessage(invoker.getLabelWithValues(
                     "diagnostics.makeglossdisabled", lineRef));

                  try
                  {
                     makeglossDisabledLine = Integer.parseInt(lineRef);
                  }
                  catch (NumberFormatException e)
                  {// shouldn't happen, pattern enforces correct format
                  }
               }

               m = makeglossRestoredPattern.matcher(line);

               if (m.matches())
               {
                  String lineRef = m.group(1);

                  addDiagnosticMessage(invoker.getLabelWithValues(
                     "diagnostics.makeglossrestored", lineRef));

                  try
                  {
                     makeglossRestoredLine = Integer.parseInt(lineRef);
                  }
                  catch (NumberFormatException e)
                  {// shouldn't happen, pattern enforces correct format
                  }
               }
            }

            if (latexFormat == null)
            {
               m = formatPattern.matcher(line);

               if (m.matches())
               {
                  latexFormat = m.group(1);

                  if (latexFormat.startsWith("xe") 
                      || latexFormat.startsWith("lua"))
                  {
                     addSupportedEncoding("utf8");
                  }

                  continue;
               }
            }

            m = inputEncPattern.matcher(line);

            if (m.matches())
            {
               addSupportedEncoding(m.group(1));
               continue;
            }

            m = glossariesStyPattern.matcher(line);

            if (m.matches())
            {
               Calendar cal = Calendar.getInstance();

               String year = m.group(1);
               String mon  = m.group(2);
               String day  = m.group(3);
               vers = m.group(4);

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

               addDiagnosticMessage(invoker.getLabelWithValues(
                 "diagnostics.wrong_type", type));
               addErrorMessage(invoker.getLabelWithValues(
                 "error.wrong_type", type));

               continue;
            }

            if (invoker.isDocDefCheckOn())
            {
               m = docDefsPattern.matcher(line);

               if (m.matches())
               {
                  File f = new File(dir, baseName+".glsdefs");

                  addDiagnosticMessage(invoker.getLabelWithValues(
                    "diagnostics.doc_defs", f.getAbsolutePath()));

                  continue;
               }
            }

            if (invoker.isMissingLangCheckOn())
            {
               m = missingLangPattern.matcher(line);

               if (m.matches())
               {
                  addDiagnosticMessage(invoker.getLabelWithValues(
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
                  addDiagnosticMessage(invoker.getLabelWithValues(
                     "diagnostics.missing_sty", sty));
               }

               continue;
            }

            m = glsGroupHeadingPattern.matcher(line);

            if (m.matches())
            {
               String val = m.group(1);

               if (!problemGroupLabels.contains(val))
               {
                  addDiagnosticMessage(String.format("%s<pre>%s</pre>%s", 
                     invoker.getLabelWithValues(
                     "diagnostics.problem_group_label", val),
                     invoker.escapeHTML(line),
                     invoker.getLabel(isUnicodeEngine() ? 
                       "diagnostics.suggest_bib2gls" : 
                       "diagnostics.suggest_unicode_or_bib2gls")));

                  problemGroupLabels.add(val);
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

               if (msg.contains("Package inputenc Error: Unicode char"))
               {
                  // If a warning message contains this, it's likely
                  // that an entry label contains non-ASCII
                  // characters.

                  checkNonAscii = true;
                  continue;
               }

               if (requiresBib2Gls)
               {
                  m = missingGlsTeXPattern.matcher(msg);

                  if (m.matches())
                  {
                     addDiagnosticMessage(invoker.getLabelWithValues(
                      istName == null ? "diagnostics.missing_glstex" :
                      "diagnostics.missing_glstex_hybrid",
                      m.group(1)));

                     if (build == null)
                     {
                        if (istName == null)
                        {
                           build = invoker.getLabelWithValues(
                             "diagnostics.bib2gls_build", getLaTeXFormat(), 
                              baseName
                           );
                        }
                        else
                        {
                           build = invoker.getLabelWithValues(
                             "diagnostics.hybrid_build", getLaTeXFormat(),
                              baseName,
                             "makeglossaries"
                           );
                        }

                        addDiagnosticMessage(build);
                     }
                     continue;
                  }
               }

               addDiagnosticMessage(msg);

               if (noidx)
               {
                  m = emptyNoIdxGlossaryPattern.matcher(msg);

                  if (m.matches())
                  {
                     String type = m.group(1);

                     if (!glossaryList.contains(type))
                     {
                        addDiagnosticMessage(invoker.getLabelWithValues(
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

               while (!line.endsWith(".") && (line = reader.readLine()) != null)
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
                  String cmd = m.group(1);
                  String rest = m.group(2);

                  if (m.groupCount() == 2)
                  {
                     addAdvisoryMessage(invoker.getLabelWithValues(
                        "diagnostics.shell_disabled", cmd+rest));
                  }
                  else
                  {
                     addAdvisoryMessage(invoker.getLabelWithValues(
                        "diagnostics.shell_restricted", cmd+rest, cmd));
                  }


               }

               continue;
            }

            m = undefControlSequencePattern.matcher(line);

            if (m.matches())
            {
               line = reader.readLine();

               m = fragileBrokenPattern.matcher(line);

               if (m.matches())
               {
                  addDiagnosticMessage(invoker.getLabel(
                    "diagnostics.fragile"));
                  continue;
               }

               if (line.contains("\\GenericError"))
               {
                  // Too complicated, so don't show to avoid
                  // overcluttering the diagnostic panel.
                  continue;
               }

               if (line.endsWith("\\indexspace "))
               {
                  addDiagnosticMessage(invoker.getLabel(
                    "diagnostics.undef_indexspace"));
                  continue;
               }

               addDiagnosticMessage(invoker.getLabelWithValues(
                 "diagnostics.undef_cs", line));

               continue;
            }

            m = unknownOptPattern.matcher(line);

            if (m.matches())
            {
               addDiagnosticMessage(invoker.getLabelWithValues(
                 "diagnostics.undef_opt", m.group(1)));

               continue;
            }

            m = infoPattern.matcher(line);

            if (m.matches())
            {
               StringBuilder builder = new StringBuilder(m.group(1));

               while (!line.endsWith(".")
                      && (line = reader.readLine()) != null
                      && !line.isEmpty())
               {
                  builder.append(line);
               }

               m = wrglossaryPattern.matcher(builder);

               if (m.matches())
               {
                  String type = m.group(1);
                  String info = m.group(2);
                  String lineNum = m.group(3);

                  addDiagnosticMessage(String.format("%s%n<pre>%s</pre>%n",
                     invoker.getLabelWithValues(
                      "diagnostics.wrglossary", type, lineNum),
                      info));
               }

               continue;
            }

            m = inputPattern.matcher(line);

            if (m.matches())
            {
               addDiagnosticMessage(invoker.getLabelWithValues(
                  "diagnostics.include", m.group(1)));
               continue;
            }

            m = istFilePattern.matcher(line);

            if (m.matches())
            {
               addDiagnosticMessage(invoker.getLabel(
                  "diagnostics.ist_in_log"));
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

      if (makeglossDisabledLine > -1 && istName == null)
      {
         if (makeglossRestoredLine == -1)
         {
            addDiagnosticMessage(invoker.getLabel("diagnostics.restoremakegloss"));
         }
         else if (makeglossRestoredLine < makeglossDisabledLine)
         {
            addDiagnosticMessage(
               invoker.getLabel("diagnostics.restoremakegloss.cancelled"));
         }
         else
         {
            addDiagnosticMessage(
               invoker.getLabel("diagnostics.restoremakegloss.late"));
         }
      }

      if (vers == null)
      {
         addDiagnosticMessage(invoker.getLabel(
          "diagnostics.no_version"));
      }

      if (checkNonAscii)
      {
         addDiagnosticMessage(invoker.getLabel(
            "diagnostics.inputenc"));

         for (int i = 0, n = glossaryList.size(); i < n; i++)
         {
            Glossary glossary = glossaryList.get(i);

            glossary.checkNonAsciiLabels();
         }
      }
   }

   public String getLaTeXFormat()
   {
      return latexFormat == null ? "latex" : latexFormat;
   }

   public boolean isUnicodeEngine()
   {
      if (latexFormat == null)
      {
         return false;
      }

      return latexFormat.startsWith("xe") || latexFormat.startsWith("lua");
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

   public String displayEncoding()
   {
      return invoker.getEncoding().name();
   }

   private void addSupportedEncoding(String encLabel)
   {
      Charset charset = invoker.getCharset(encLabel);

      if (charset == null)
      {
         addDiagnosticMessage(invoker.getLabelWithValues(
          "diagnostics.unknown.encoding", encLabel));
      }
      else
      {
         if (supportedEncodings == null)
         {
            supportedEncodings = new Vector<Charset>();
         }

         supportedEncodings.add(charset);
      }

      if (inputEnc == null)
      {
         inputEnc = encLabel;
      }
      else
      {
         inputEnc = String.format("%s, %s", inputEnc, encLabel);
      }
   }

   public String displayDocumentEncoding()
   {
      return inputEnc == null ? invoker.getLabel("error.unknown") : inputEnc;
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

      return isValidOrder() ? null 
        : invoker.getLabelWithValues("error.invalid_order", order);
   }

   public String displayFormat()
   {
      if (istName == null)
      {
         if (requiresBib2Gls)
         {
            return "bib2gls";
         }
         else
         {
            return invoker.getLabel("error.unknown");
         }
      }

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
         return  
           invoker.getLabel(requiresBib2Gls ? "error.bib2gls_indexer" :
             "error.cant_determine_indexer");
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
      String mess;

      if (!noidx && istName == null && order == null)
      {
         mess = getDiagnosticMessages();

         if (mess != null)
         {
            return mess;
         }
         else if (glossaryList.size() == 0)
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

      mess = getIndexerError();

      if (mess != null)
      {
         return String.format("%s%n%s", mess, 
            invoker.getLabelWithValues("diagnostics.no_indexer", 
            displayFormat()));
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
               mess = String.format("%s%n<p>%s", mess, errmess);
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
         diagnosticMessages.append(String.format("<p>%s", mess));
      }
   }

   public String getAdvisoryMessages()
   {
      return advisoryMessages == null ? null : advisoryMessages.toString();
   }

   public void addAdvisoryMessage(String mess)
   {
      if (advisoryMessages == null)
      {
         advisoryMessages = new StringBuilder(mess);
      }
      else
      {
         advisoryMessages.append(String.format("<p>%s", mess));
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
         case ENCODING: return displayEncoding();
         case DOC_ENCODING: return displayDocumentEncoding();
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

   private boolean requiresBib2Gls = false;

   private StringBuilder errorMessages, diagnosticMessages, advisoryMessages;

   private static final Pattern newGlossaryPattern
      = Pattern.compile("\\\\@newglossary\\{([^\\}]+)\\}\\{([^\\}]+)\\}\\{([^\\}]+)\\}\\{([^\\}]+)\\}");

   private static final Pattern istFilePattern
      = Pattern.compile("\\\\@istfilename\\{([^\\}]+)\\}");

   private static final Pattern bib2glsPattern
      = Pattern.compile("\\\\glsxtr@resource\\{(.*)\\}\\{(.*?)\\}");

   private static final Pattern selectAllPattern
      = Pattern.compile(".*selection\\s*=\\s*(all|\\{all\\}).*");

   private static final Pattern orderPattern
      = Pattern.compile("\\\\@glsorder\\{([^\\}]+)\\}");

   private static final Pattern languagePattern
      = Pattern.compile("\\\\@xdylanguage\\{([^\\}]+)\\}\\{([^\\}]*)\\}");

   private static final Pattern codepagePattern
      = Pattern.compile("\\\\@gls@codepage\\{([^\\}]+)\\}\\{([^\\}]*)\\}");

   private static final Pattern glsreferencePattern
      = Pattern.compile("\\\\@gls@reference\\{.*?\\}\\{.*?\\}\\{.*\\}");

   private static final Pattern glsrecordPattern
      = Pattern.compile("\\\\glsxtr@record\\{.*?\\}\\{.*?\\}\\{.*\\}");

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

   private static final Pattern missingGlsTeXPattern
      = Pattern.compile(".*No file `(.*?\\.glstex)'.*");

   private static final Pattern warningPattern
      = Pattern.compile("Package glossaries(-extra)? Warning: .*");

   private static final Pattern emptyNoIdxGlossaryPattern
      = Pattern.compile(".*Empty glossary for \\\\printnoidxglossary\\[type=\\{(.*?)\\}\\]\\..*");

   private static final Pattern systemPattern
      = Pattern.compile("runsystem\\(.*");

   private static final Pattern disabledSystemPattern
      = Pattern.compile("runsystem\\(([^ ]+)(.*)\\)\\.\\.\\.disabled(\\s+\\(restricted\\))?\\.");

   private static final Pattern undefControlSequencePattern
      = Pattern.compile(".* Undefined control sequence.");

   private static final Pattern fragileBrokenPattern
      = Pattern.compile(".*\\\\in@ #1#2->\\\\begingroup \\\\def \\\\in@@\\s*");

   private static final Pattern unknownOptPattern
      = Pattern.compile(".*Unknown option `(.*)' for package `glossaries'.*");

   private static final Pattern infoPattern
      = Pattern.compile("Package glossaries Info:\\s*(.*)");

   private static final Pattern wrglossaryPattern
      = Pattern.compile("wrglossary\\((.*?)\\)\\((.*)\\) on input line (\\d+).*");

   private static final Pattern inputPattern
      = Pattern.compile("\\\\@input\\{(.*)\\.aux\\}");

   private static final Pattern formatPattern
      = Pattern.compile(".* format=(xelatex|lualatex|pdflatex|latex) .*");

   private static final Pattern inputEncPattern
      = Pattern.compile("File: (utf8|latin[0-9]+|cp[0-9]+(?:de)?|decmulti|applemac|macce|next|ansinew|ascii)\\.def.*");

   private static final Pattern glsGroupHeadingPattern
      = Pattern.compile(".*\\\\glsgroupheading\\{(.+?)\\}.*");

   private static final Pattern makeglossDisabledPattern = Pattern.compile(
      "Package glossaries Info: \\\\makeglossaries and \\\\makenoidxglossaries have been disabled on input line ([0-9]+)\\.");

   private static final Pattern makeglossRestoredPattern = Pattern.compile(
      "Package glossaries Info: \\\\makeglossaries and \\\\makenoidxglossaries have been restored on input line ([0-9]+)\\.");

   private Vector<String> problemGroupLabels = new Vector<String>();

   private static final String[] fields =
   {
      "aux",
      "order",
      "ist",
      "indexer",
      "list",
      "encoding",
      "docencoding"
   };

   public static final int AUX=0, ORDER=1, IST=2, INDEXER=3, GLOSSARIES=4,
     ENCODING=5, DOC_ENCODING=6;

   private String latexFormat = null;

   private String inputEnc = null;

   private Vector<Charset> supportedEncodings;

   private MakeGlossariesInvoker invoker;

   private boolean hasRecords = false;
   private boolean selectionAllFound = false;
}
