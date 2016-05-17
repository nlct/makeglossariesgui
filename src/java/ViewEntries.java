package com.dickimawbooks.makeglossariesgui;

import java.net.URL;
import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.table.*;

public class ViewEntries extends JFrame
   implements ActionListener
{
   public ViewEntries(MakeGlossariesGUI application, Glossary g, Font font)
   {
      super(MakeGlossariesGUI.getLabelWithValue("entry.title", g.label));

      app = application;
      glossary = g;

      setIconImage(app.getIconImage());

      TableModel model = new AbstractTableModel()
      {
         public int getRowCount() {return glossary.getNumEntries();}
         public int getColumnCount() {return 2;}
         public boolean isCellEditable(int row, int column) {return false;}

         public Object getValueAt(int row, int column)
         {
            if (column == 0)
            {
               return glossary.getEntryLabel(row);
            }
            else
            {
               return glossary.getEntryCount(row);
            }
         }

         public String getColumnName(int columnIndex)
         {
            return columnIndex == 0 ?
               MakeGlossariesGUI.getLabel("entry.label"):
               MakeGlossariesGUI.getLabel("entry.count");
         }
      };

      table = new JTable(model);

      getContentPane().add(new JScrollPane(table), "Center");

      toolbar = new JToolBar();
      getContentPane().add(toolbar, "North");

      JLabel label = new JLabel(app.getLabel("entry", "find_label"));
      label.setDisplayedMnemonic(app.getMnemonic("entry", "find_label"));
      toolbar.add(label);

      searchField = new JTextField();
      searchField.registerKeyboardAction(this, "find", 
         KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
         JComponent.WHEN_FOCUSED);
      toolbar.add(searchField);
      label.setLabelFor(searchField);

      addButton("find", "Find24", 
         KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));

      pack();
   }

   private JButton addButton(String label, String imageName, KeyStroke keyStroke)
   {
      JButton button = new JButton();

      String tooltip = app.getLabel("entry."+label, "tooltip");
      String alttext = app.getLabel("entry", label);

      String imgLocation = "/toolbarButtonGraphics/general/"+imageName+".gif";

      URL imageURL = app.getClass().getResource(imgLocation);

      button.setActionCommand(label);
      button.addActionListener(this);

      if (keyStroke != null)
      {
         button.registerKeyboardAction(this, label, keyStroke,
            JComponent.WHEN_IN_FOCUSED_WINDOW);
      }

      button.setToolTipText(tooltip);

      if (imageURL != null)
      {
         button.setIcon(new ImageIcon(imageURL, alttext));
      }
      else
      {
         button.setText(alttext);
         System.err.println("Unable to find resource: "+imageURL);
      }

      toolbar.add(button);

      return button;
   }

   public void actionPerformed(ActionEvent evt)
   {
      String action = evt.getActionCommand();

      if (action == null) return;

      if (action.equals("find"))
      {
         String label = searchField.getText();

         int idx = glossary.getEntryIdx(label);

         if (idx == -1)
         {
            app.error(this, app.getLabelWithValue("error.no_such_entry", label));
         }
         else
         {
            table.clearSelection();
            table.addRowSelectionInterval(idx, idx);

            Rectangle rect = table.getCellRect(idx, 1, true);
            table.scrollRectToVisible(rect);
         }
      }
   }

   private Glossary glossary;

   private JTable table;

   private JTextField searchField;

   private MakeGlossariesGUI app;

   private JToolBar toolbar;
}
