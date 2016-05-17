package com.dickimawbooks.makeglossariesgui;

public class GlossaryException extends Exception
{
   public GlossaryException(String message)
   {
      this(message, null);
   }

   public GlossaryException(String message, String diagnosticMessage)
   {
      super(message);
      diagnostic = diagnosticMessage;
   }

   public String getDiagnosticMessage()
   {
      return diagnostic;
   }

   private String diagnostic;
}
