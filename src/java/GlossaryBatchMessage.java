package com.dickimawbooks.makeglossariesgui;

import java.io.File;

public class GlossaryBatchMessage implements GlossaryMessage
{
   public GlossaryBatchMessage(MakeGlossariesInvoker invoker)
   {
      this.invoker = invoker;
   }

   public void message(String msg)
   {
      if (msg == null)
      {
         throw new NullPointerException();
      }

      if (!invoker.isQuiet())
      {
         System.out.println(msg);
      }
   }

   public void debug(String msg)
   {
      if (msg == null)
      {
         throw new NullPointerException();
      }

      if (invoker.isDebugMode())
      {
         System.out.println(msg);
      }
   }

   public void debug(Throwable e)
   {
      if (invoker.isDebugMode())
      {
         e.printStackTrace();
      }
   }

   public void message(GlossaryException e)
   {
      if (!invoker.isQuiet())
      {
         System.out.println(e.getMessage());

         String msg = e.getDiagnosticMessage();

         if (msg != null)
         {
            System.out.println(msg);
         }

         if (invoker.isDebugMode())
         {
            e.printStackTrace();
         }
      }
   }

   public void error(Exception e)
   {
      System.err.println(e.getMessage());
   }

   public void error(String message)
   {
      if (message == null)
      {
         throw new NullPointerException();
      }

      System.err.println(message);
   }

   public void showMessages()
   {
   }

   public void aboutToExec(String[] cmdArray, File dir)
   {
      if (invoker.isDryRunMode() || !invoker.isQuiet())
      {
         if (invoker.isDryRunMode())
         {
            System.out.println(invoker.getLabel("diagnostics.dry_run"));

         }

         for (int i = 0, n = cmdArray.length-1; i < cmdArray.length; i++)
         {
            if (i == n)
            {
               System.out.println(String.format("\"%s\"", cmdArray[i]));
            }
            else
            {
               System.out.print(String.format("\"%s\" ", cmdArray[i]));
            }
         }
      }
   }

   private MakeGlossariesInvoker invoker;
}
