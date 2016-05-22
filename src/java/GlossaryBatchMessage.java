package com.dickimawbooks.makeglossariesgui;

import java.io.File;

public class GlossaryBatchMessage implements GlossaryMessage
{
   public void setQuiet(boolean isQuiet)
   {
      this.quiet = isQuiet;
   }

   public boolean isQuiet()
   {
      return quiet;
   }

   public void message(String msg)
   {
      if (!isQuiet())
      {
         System.out.println(msg);
      }
   }

   public void message(GlossaryException e)
   {
      if (!quiet)
      {
         System.out.println(e.getMessage());
         System.out.println(e.getDiagnosticMessage());
      }
   }

   public void error(Exception e)
   {
      System.err.println(e.getMessage());
   }

   public void error(String message)
   {
      System.err.println(message);
   }

   public void showMessages()
   {
   }

   public void aboutToExec(String[] cmdArray, File dir)
   {
      if (!quiet)
      {
         for (int i = 0, n = cmdArray.length-1; i < cmdArray.length; i++)
         {
            if (i == n)
            {
               System.out.println(cmdArray[i]);
            }
            else
            {
               System.out.print(cmdArray[i]);
               System.out.print(" ");
            }
         }
      }
   }

   private boolean quiet=false;
}
