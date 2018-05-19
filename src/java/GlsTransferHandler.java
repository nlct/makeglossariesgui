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
