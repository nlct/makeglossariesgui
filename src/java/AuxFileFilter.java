// File          : AuxFileFilter.java
// Author        : Nicola L.C. Talbot
//                 http://www.dickimaw-books.com/

/*
    Copyright (C) 2006 Nicola L.C. Talbot

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
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

/**
 * Filter for AUX files. Recognised extension: aux.
 */
public class AuxFileFilter extends javax.swing.filechooser.FileFilter
{
   /**
    * Creates a file filter with default description.
    * The default description is "AUX Files".
    */
   public AuxFileFilter()
   {
      this("AUX Files");
   }

   /**
    * Creates a file filter with given description.
    */
   public AuxFileFilter(String description)
   {
      super();

      this.description = description;
   }

   /**
    * Determines whether given file is accepted by this filter.
    */
   public boolean accept(File f)
   {
      if (f.isDirectory()) return true;

      String name = f.getName().toLowerCase();

      if (name.endsWith(".aux"))
      {
         return true;
      }
      
      return false;
   }

   /**
    * Gets the description of this filter.
    */
   public String getDescription()
   {
      return description;
   }

   public String getDefaultExtension()
   {
      return "tex";
   }

   private String description;
}
