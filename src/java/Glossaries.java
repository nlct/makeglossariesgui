package com.dickimawbooks.makeglossariesgui;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.Element;

public class Glossaries
{
   public Glossaries(MakeGlossariesGUI application, String istName, String order)
   {
      app = application;
      this.istName = istName;
      this.order = order;
      glossaryList = new Vector<Glossary>();
   }

   public Glossaries(MakeGlossariesGUI application)
   {
      app = application;
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

   public static Glossaries loadGlossaries(MakeGlossariesGUI application, File file)
      throws IOException
   {
      Glossaries glossaries = new Glossaries(application);

      BufferedReader in = new BufferedReader(new FileReader(file));

      String line;

      while ((line = in.readLine()) != null)
      {
         Matcher matcher = newGlossaryPattern.matcher(line);

         if (matcher.matches())
         {
            glossaries.add(new Glossary(application, matcher.group(1), matcher.group(2),
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

         matcher = languagePattern.matcher(line);

         if (matcher.matches())
         {
            String label = matcher.group(1);

            Glossary g = glossaries.getGlossary(label);

            String language = matcher.group(2);

            if (g == null)
            {
               glossaries.addErrorMessage(MakeGlossariesGUI.getLabelWithValues(
                  "error.language_no_glossary", language, label));
               glossaries.addDiagnosticMessage(MakeGlossariesGUI.getLabelWithValues(
                  "diagnostics.language_no_glossary", language, label));
            }
            else
            {
               g.setLanguage(language);
            }
         }

         matcher = codepagePattern.matcher(line);

         if (matcher.matches())
         {
            String label = matcher.group(1);

            Glossary g = glossaries.getGlossary(label);

            String code = matcher.group(2);

            if (g == null)
            {
               glossaries.addErrorMessage(MakeGlossariesGUI.getLabelWithValues(
                  "error.codepage_no_glossary", code, label));
               glossaries.addDiagnosticMessage(MakeGlossariesGUI.getLabelWithValues(
                  "diagnostics.codepage_no_glossary", code, label));
            }
            else
            {
               g.setCodePage(code);
            }
         }
      }

      in.close();

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

      File file = app.getFile();

      String baseName = file.getName();

      int idx = baseName.lastIndexOf(".");

      if (idx != -1)
      {
         baseName = baseName.substring(0, idx);
      }

      File dir = file.getParentFile();

      for (int i = 0, n = getNumGlossaries(); i < n; i++)
      {
         Glossary g = getGlossary(i);

         try
         {
            if (useXindy())
            {
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
               throw new GlossaryException(app.getLabelWithValue("error.no_ist", istName),
                  app.getLabel("diagnostics.no_ist"));
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
         MakeGlossariesGUI.getLabel("error.no_glossaries"):
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
      if (order == null) return MakeGlossariesGUI.getLabel("error.missing_order");

      return isValidOrder() ? null : MakeGlossariesGUI.getLabel("error.invalid_order");
   }

   public String displayFormat()
   {
      if (istName == null) return MakeGlossariesGUI.getLabel("error.unknown");

      return useXindy() ? "xindy" : "makeindex";
   }

   public String getIstName()
   {
      return istName;
   }

   public String getIstNameError()
   {
      return istName == null ? MakeGlossariesGUI.getLabel("error.missing_ist") : null;
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
         return MakeGlossariesGUI.getLabel("error.cant_determine_indexer");
      }

      if (useXindy())
      {
         if (app.getXindyApp() == null)
         {
            return app.getLabel("error.no_xindy");
         }
      }
      else
      {
         if (app.getMakeIndexApp() == null)
         {
            return app.getLabel("error.no_makeindex");
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
            return app.getFileName().toLowerCase().endsWith(".aux") ?
                   app.getLabel("diagnostics.no_glossaries"):
                   app.getLabel("diagnostics.not_aux");
         }
         else
         {
            return app.getLabel("diagnostics.no_makeglossaries");
         }
      }

      String mess = getIndexerError();

      if (mess != null)
      {
         return mess + "\n" + app.getLabelWithValue("diagnostics.no_indexer", 
            displayFormat());
      }

      mess = diagnosticMessages;

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
               mess += "\n"+errmess;
            }
         }
      }

      return mess;
   }

   public void addDiagnosticMessage(String mess)
   {
      if (diagnosticMessages == null)
      {
         diagnosticMessages = mess;
      }
      else
      {
         diagnosticMessages += "\n" + mess;
      }
   }

   public void addErrorMessage(String mess)
   {
      if (errorMessages == null)
      {
         errorMessages = mess;
      }
      else
      {
         errorMessages += "\n" + mess;
      }
   }

   public String getErrorMessages()
   {
      return errorMessages;
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

   public static String getFieldLabel(int i)
   {
      return MakeGlossariesGUI.getLabel("main", fields[i]);
   }

   public String getField(int i)
   {
      switch (i)
      {
         case AUX: return app.getFileName();
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
         case AUX: return app.getFileName() == null ?
            app.getLabel("error.no_such_file") :
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

   private String errorMessages = null, diagnosticMessages;

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

   private MakeGlossariesGUI app;
}
