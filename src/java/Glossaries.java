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

      while ((line = in.readLine()) != null)
      {
         Matcher matcher = newGlossaryPattern.matcher(line);

         if (matcher.matches())
         {
            glossaries.add(new Glossary(invoker, matcher.group(1), matcher.group(2),
              matcher.group(3), matcher.group(4)));
         }

         matcher = istFilePattern.matcher(line);

         if (matcher.matches())
         {
            glossaries.istName = matcher.group(1);
         }

         matcher = orderPattern.matcher(line);

         if (matcher.matches())
         {
            glossaries.order = matcher.group(1);
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
            }
         }
      }

      in.close();

      if (!override)
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

   public void process()
      throws GlossaryException,IOException,InterruptedException
   {
      String mess = getIndexerError();

      if (mess != null)
      {
         throw new GlossaryException(mess);
      }

      File file = invoker.getFile();

      String baseName = file.getName();

      int idx = baseName.lastIndexOf(".");

      if (idx != -1)
      {
         baseName = baseName.substring(0, idx);
      }

      File dir = file.getParentFile();

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
               g.makeindex(dir, baseName, isWordOrder(), istName);
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
                  invoker.getLabel("diagnostics.no_ist"));
            }
            else
            {
               throw e;
            }
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
      if (istName == null && order == null)
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
