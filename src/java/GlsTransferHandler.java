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

import java.util.ArrayList;
import java.io.File;
import java.io.IOException;
import java.awt.datatransfer.*;
import javax.swing.TransferHandler;

public class GlsTransferHandler extends TransferHandler
{
   public GlsTransferHandler(MakeGlossariesGUI application)
   {
      super();
      this.app = application;
   }

   public boolean canImport(TransferHandler.TransferSupport support)
   {
      int action = support.getDropAction();

      if (action != COPY && action != COPY_OR_MOVE)
      {
         return false;
      }

      return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
   }

   public boolean importData(TransferHandler.TransferSupport support)
   {
      int action = support.getDropAction();

      if (action != COPY && action != COPY_OR_MOVE)
      {
         return false;
      }

      if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
      {
         return false;
      }

      Transferable t = support.getTransferable();

      try
      {
         Object data = t.getTransferData(DataFlavor.javaFileListFlavor);

         @SuppressWarnings("unchecked")
         ArrayList<File> list = (ArrayList<File>)data;

         if (list.isEmpty())
         {
            return false;
         }

         app.load(list.get(0));
      }
      catch (Exception e)
      {
         app.debug(e);
         return false;
      }

      return true;
   }

   private MakeGlossariesGUI app;
}
