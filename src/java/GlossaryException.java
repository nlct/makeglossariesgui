package com.dickimawbooks.makeglossariesgui;

public class GlossaryException extends Exception
{
   public GlossaryException(String message)
   {
      this(message, (String)null);
   }

   public GlossaryException(String message, String diagnosticMessage)
   {
      super(message);
      diagnostic = diagnosticMessage;
   }

   public GlossaryException(String message, Throwable cause)
   {
      this(message, null, cause);
   }

   public GlossaryException(String message, String diagnosticMessage, Throwable cause)
   {
      super(message, cause);
      diagnostic = diagnosticMessage;
   }

   public String getDiagnosticMessage()
   {
      return diagnostic;
   }

   private String diagnostic;
}
