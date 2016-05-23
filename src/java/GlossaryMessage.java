package com.dickimawbooks.makeglossariesgui;

import java.io.File;

public interface GlossaryMessage
{
   public void message(String msg);
   public void message(GlossaryException e);
   public void debug(String msg);
   public void debug(Throwable e);
   public void error(Exception e);
   public void error(String message);
   public void showMessages();
   public void aboutToExec(String[] cmdArray, File dir);
}
