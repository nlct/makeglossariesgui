package com.dickimawbooks.makeglossariesgui;

import java.io.*;
import java.awt.Cursor;

public class ProcessThread extends Thread
{
   public ProcessThread(MakeGlossariesGUI application, File file)
   {
      super();
      app = application;
      this.file = file;
   }

   public void run()
   {
      Cursor orgCursor = app.getCursor();

      app.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

      try
      {
         processGlossaries();
         app.setCursor(orgCursor);
      }
      catch (InterruptedException e)
      {
         app.setCursor(orgCursor);

         if (app.glossaries != null)
         {
            app.glossaries.addDiagnosticMessage(app.getLabel("diagnostics.interrupt"));
         }

         app.error(app, app.getLabel("error.interrupt")+"\n"+e.getMessage());
      }
   }

   public void processGlossaries()
      throws InterruptedException
   {
      try
      {
         app.glossaries = Glossaries.loadGlossaries(app, file);
      }
      catch (IOException e)
      {
         app.error(app, "Unable to load file:\n" + e.getMessage());
         return;
      }

      try
      {
         app.glossaries.process();
      }
      catch (GlossaryException e)
      {
         String mess = e.getDiagnosticMessage();

         if (mess != null)
         {
            app.glossaries.addDiagnosticMessage(mess);
         }

         app.error(app, e.getMessage());
         app.selectDiagnosticComponent();
      }
      catch (IOException e)
      {
         app.glossaries.addDiagnosticMessage(app.getLabel("diagnostics.io_error"));
         app.error(app, e.getMessage());
      }

      String errMess = app.glossaries.getErrorMessages();

      if (errMess != null)
      {
         app.error(app, errMess);
      }

      try
      {
         app.updateInfoPanel();
      }
      catch (Exception e)
      {
         app.error(app, e.getMessage());
      }

      app.updateDiagnostics();
   }

   private MakeGlossariesGUI app;

   private File file;
}
