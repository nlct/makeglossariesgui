/*
    Copyright (C) 2018-2020 Nicola L.C. Talbot
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

import java.util.Hashtable;
import java.util.Properties;
import java.util.Iterator;
import java.text.MessageFormat;
import java.text.ChoiceFormat;

public class MakeGlossariesDictionary extends Hashtable<String,MessageFormat>
{
    public MakeGlossariesDictionary(Properties props) throws InvalidSyntaxException
    {
       super(props.isEmpty() ? 10 : props.size());

       init(props);
    }

    private void init(Properties props) throws InvalidSyntaxException
    {
       Iterator<Object> it = props.keySet().iterator();

       while (it.hasNext())
       {
          Object key = it.next();

          try
          {
             String message = (String)props.get(key);
             int n = message.length();

             StringBuilder builder = new StringBuilder(n);
             StringBuilder csBuilder = null;

             for (int i = 0; i < n; )
             {
                int cp = message.codePointAt(i);
                i += Character.charCount(cp);

                if (csBuilder != null)
                {
                   if (cp == '|')
                   {
                      builder.append('\\');
                      builder.append(csBuilder);
                      csBuilder = null;
                   }
                   else if (Character.isAlphabetic(cp))
                   {
                      csBuilder.appendCodePoint(cp);
                   }
                   else
                   {
                      builder.append('|');
                      builder.append(csBuilder);
                      builder.appendCodePoint(cp);
                      csBuilder = null;
                   }
                }
                else if (cp == '\\' && i < n)
                {
                   int nextCp = message.codePointAt(i);
                   i += Character.charCount(nextCp);

                   if (nextCp == 'n')
                   {
                      builder.append(String.format("%n"));
                   }
                   else if (nextCp == 't')
                   {
                      builder.append('\t');
                   }
                   else
                   {
                      builder.appendCodePoint(nextCp);
                   }
                }
                else if (cp == '|')
                {
                   csBuilder = new StringBuilder();
                }
                else
                {
                   builder.appendCodePoint(cp);
                }
             }

             if (csBuilder != null)
             {
                builder.append('|');
                builder.append(csBuilder);
             }

             put((String)key, new MessageFormat(builder.toString()));
          }
          catch (IllegalArgumentException e)
          {
             throw new InvalidSyntaxException(
              String.format(
               "Property '%s': Invalid message format: %s", 
               key, e.getMessage()),
              e);
          }
       }
    }

    public String getMessageIfExists(String label, Object... args)
    {
       MessageFormat msg = get(label);

       if (msg == null)
       {
          return null;
       }

       return msg.format(args);
    }

    public String getMessage(String label, Object... args)
    {
       MessageFormat msg = get(label);

       if (msg == null)
       {
          throw new IllegalArgumentException(
           "Invalid message label: "+label);
       }

       return msg.format(args);
    }


    public String getChoiceMessage(String label, int argIdx,
      String choiceLabel, int numChoices, Object... args)
    {
       String[] part = new String[numChoices];

       double[] limits = new double[numChoices];

       for (int i = 0; i < numChoices; i++)
       {
          String tag = String.format("message.%d.%s", i, choiceLabel);

          MessageFormat fmt = get(tag);

          if (fmt == null)
          {
             throw new IllegalArgumentException(
              "Invalid message label: "+tag);
          }

          part[i] = fmt.toPattern();
          limits[i] = i;
       }

       MessageFormat fmt = get(label);

       if (fmt == null)
       {
          throw new IllegalArgumentException(
           "Invalid message label: "+label);
       }

       ChoiceFormat choiceFmt = new ChoiceFormat(limits, part);

       fmt.setFormatByArgumentIndex(argIdx, choiceFmt);

       return fmt.format(args);
    }
}
