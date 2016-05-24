package com.dickimawbooks.makeglossariesgui;

import java.util.HashMap;
import java.util.Iterator;

public class XindyModule
{
   public XindyModule(String language, String defVariant,
     String[] variants, String[] codepages)
   {
      this.language   = language;
      this.defVariant = defVariant;
      this.variants   = variants;
      this.codepages  = codepages;
   }

   public XindyModule(String language, String[] codepages)
   {
      this(language, null, null, codepages);
   }

   public String getLanguage()
   {
      return language;
   }

   public String[] getVariants()
   {
      return variants;
   }

   public String getDefaultVariant()
   {
      return defVariant;
   }

   public boolean hasVariants()
   {
      return variants != null;
   }

   public String[] getCodePages()
   {
      return codepages;
   }

   public boolean hasCodePage(String code)
   {
      if (variants == null)
      {
         for (int i = 0; i < codepages.length; i++)
         {
            if (code.equals(codepages[i])) return true;
         }

         return false;
      }

      for (int i = 0; i < variants.length; i++)
      {
         for (int j = 0; j < codepages.length; j++)
         {
            if (code.equals(variants[i]+"-"+codepages[i]))
            {
               return true;
            }
         }
      }

      return false;
   }

   public static XindyModule getModule(String lang)
   {
      if (knownModules == null)
      {
         initKnownModules();
      }

      return knownModules.get(lang);
   }

   public static HashMap<String,XindyModule> getKnownModules()
   {
      if (knownModules == null)
      {
         initKnownModules();
      }

      return knownModules;
   }

   public static String[] getKnownLanguages()
   {
      if (knownModules == null)
      {
         initKnownModules();
      }

      String[] languages = new String[knownModules.size()];

      int idx = 0;
      
      for (Iterator<String> it = knownModules.keySet().iterator();
        it.hasNext(); idx++)
      {
         languages[idx] = it.next();
      }

      return languages;
   }

   private static void initKnownModules()
   {
      knownModules = new HashMap<String,XindyModule>();

      // need to find some way to do this programmatically

      knownModules.put("albanian", 
         new XindyModule("albanian", new String[]{"latin1", "utf8"}));

      knownModules.put("belarusian",
         new XindyModule("belarusian",
            new String[]{"cp1251", "iso88595", "isoir111", "utf8"}));

      knownModules.put("bulgarian", 
         new XindyModule("bulgarian", 
            new String[]{"cp1251", "iso88595", "koi8-r", "utf8"}));

      knownModules.put("croatian",
         new XindyModule("croatian",
         new String[]{"cp1250", "latin2", "utf8"}));

      knownModules.put("czech",
         new XindyModule("czech",
          new String[]{"cp1250", "latin2", "utf8"}));

      knownModules.put("danish",
         new XindyModule("danish", 
          new String[]{"cp1252", "latin9", "utf8"}));

      knownModules.put("dutch",
         new XindyModule("dutch", "ij-as-ij",
          new String[]{"ij-as-ij", "ij-as-y"},
          new String[]{"latin1", "utf8"}));

      knownModules.put("english",
         new XindyModule("english", new String[]{"cp1252", "latin9", "utf8"}));

      knownModules.put("esperanto",
         new XindyModule("esperanto", new String[]{"latin3", "utf8"}));

      knownModules.put("estonian",
         new XindyModule("estonian",
           new String[]{"cp1252", "latin9", "utf8"}));

      knownModules.put("finnish",
         new XindyModule("finnish", 
         new String[]{"cp1252", "latin9", "utf8"}));

      knownModules.put("french",
         new XindyModule("french", 
           new String[]{"cp1252", "latin9", "utf8"}));

      knownModules.put("general", 
         new XindyModule("general", 
           new String[]{"cp1252", "cp850", "latin9", "utf8"}));

      knownModules.put("georgian",
         new XindyModule("georgian", new String[]{"utf8"}));

      knownModules.put("german",
         new XindyModule("german", "din5007",
           new String[]{"braille", "din5007", "duden"},
           new String[]{"latin1", "utf8"}));

      knownModules.put("greek",
         new XindyModule("greek", 
          new String[]{"iso88597",
            "polytonic-utf8",
            "translit-latin4",
            "translit-utf8",
            "utf8"}));

      knownModules.put("gypsy", 
        new XindyModule("gypsy", new String[]{"northrussian-utf8"}));

      knownModules.put("hausa", new XindyModule("hausa", new String[]{"utf8"}));

      knownModules.put("hebrew",
        new XindyModule("hebrew", new String[]{"iso88598", "utf8"}));

      knownModules.put("hungarian", 
        new XindyModule("hungarian", new String[]{"cp1250", "latin2", "utf8"}));

      knownModules.put("icelandic",
        new XindyModule("icelandic", new String[]{"cp1252", "latin9", "utf8"}));

      knownModules.put("italian", 
        new XindyModule("italian", new String[]{"latin1", "utf8"}));

      knownModules.put("klingon",
        new XindyModule("klingon", new String[]{"utf8"}));

      knownModules.put("korean",
        new XindyModule("korean", new String[]{"utf8"}));

      knownModules.put("kurdish", 
        new XindyModule("kurdish", "bedirxan",
        new String[]{"bedirxan", "turkish-i"},
        new String[]{"latin5", "utf8"}));

      knownModules.put("latin", new XindyModule("latin", new String[]{"utf8"}));

      knownModules.put("latvian",
         new XindyModule("latvian", new String[]{"latin4", "utf8"}));

      knownModules.put("lithuanian", 
         new XindyModule("lithuanian", new String[]{"latin4", "utf8"}));

      knownModules.put("lower-sorbian",
         new XindyModule("lower-sorbian",
          new String[]{"cp1250", "latin2", "utf8"}));

      knownModules.put("macedonian",
         new XindyModule("macedonian",
           new String[]{"cp1251", "iso88595", "isoir111", "utf8"}));

      knownModules.put("mongolian",
         new XindyModule("mongolian", new String[]{"cyrillic-utf8"}));

      knownModules.put("norwegian",
         new XindyModule("norwegian", new String[]{"latin1", "utf8"}));

      knownModules.put("persian",
        new XindyModule("persian", "variant1",
        new String[]{"variant1", "variant2", "variant3"},
        new String[]{"utf8"}));

      knownModules.put("polish",
        new XindyModule("polish", new String[]{"cp1250", "latin2", "utf8"}));

      knownModules.put("portuguese",
        new XindyModule("portuguese", new String[]{"latin1", "utf8"}));

      knownModules.put("romanian",
        new XindyModule("romanian", new String[]{"latin2", "utf8"}));

      knownModules.put("russian",
        new XindyModule("russian",
          new String[]{"cp1251", "iso88595", "koi8-r", "translit-iso-utf8",
           "utf8"}));

      knownModules.put("serbian",
        new XindyModule("serbian",
          new String[]{"cp1251", "iso88595", "isoir111", "utf8"}));

      knownModules.put("slovak",
        new XindyModule("slovak", "large",
          new String[]{"large", "small"},
          new String[]{"cp1250", "latin2", "utf8"}));

      knownModules.put("slovenian",
        new XindyModule("slovenian", new String[]{"cp1250", "latin2", "utf8"}));

      knownModules.put("spanish",
        new XindyModule("spanish", "modern",
        new String[]{"modern", "traditional"},
        new String[]{"latin1", "utf8"}));

      knownModules.put("swedish",
        new XindyModule("swedish",
          new String[]{"cp1252", "latin9", "utf8"}));

      knownModules.put("turkish", 
        new XindyModule("turkish", new String[]{"latin3", "latin5", "utf8"}));

      knownModules.put("ukrainian", 
        new XindyModule("ukrainian", new String[]{"cp1251", "koi8-u", "utf8"}));

      knownModules.put("upper-sorbian", 
        new XindyModule("upper-sorbian", 
         new String[]{"cp1250", "latin2", "utf8"}));

      knownModules.put("vietnamese",
        new XindyModule("vietnamese", new String[]{"utf8"}));
   }

   private String language, defVariant;

   private String[] variants, codepages;

   private static HashMap<String,XindyModule> knownModules;

}
