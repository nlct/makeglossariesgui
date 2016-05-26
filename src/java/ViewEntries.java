package com.dickimawbooks.makeglossariesgui;

import java.net.URL;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;

import javax.swing.*;
import javax.swing.table.*;

public class ViewEntries extends JDialog
   implements ActionListener
{
   public ViewEntries(MakeGlossariesGUI application, Glossary g, Font font)
   {
      super(application, application.getLabelWithValue("entry.title", g.label),
         true);

      app = application;
      glossary = g;
      entryLabels = glossary.getEntryLabels();

      setIconImage(app.getIconImage());

      TableModel model = new AbstractTableModel()
      {
         public int getRowCount() {return entryLabels.length;}
         public int getColumnCount() {return 3;}
         public boolean isCellEditable(int row, int column) {return false;}

         public Class<?> getColumnClass(int column)
         {
            return column < 2 ? String.class : Integer.class;
         }

         public Object getValueAt(int row, int column)
         {
            String label = entryLabels[row];

            if (column == 0)
            {
               return label;
            }
            else if (column == 1)
            {
               return glossary.getEntrySort(label);
            }
            else if (column == 2)
            {
               return glossary.getEntryCount(label);
            }

            return null;
         }

         public String getColumnName(int columnIndex)
         {
            switch (columnIndex)
            {
               case 0: return app.getLabel("entry.label");
               case 1: return app.getLabel("entry.sort");
               case 2: return app.getLabel("entry.count");
            }

            return null;
         }
      };

      table = new JTable(model);

      table.setAutoCreateRowSorter(true);

      table.setDefaultRenderer(String.class, 
        new EntryTableCellRenderer(this));
      table.setFont(app.getFont());

      getContentPane().add(new JScrollPane(table), "Center");

      toolbar = new JToolBar();
      getContentPane().add(toolbar, "North");

      JLabel label = new JLabel(app.getLabel("entry", "find_label"));
      label.setDisplayedMnemonic(app.getMnemonic("entry", "find_label"));
      toolbar.add(label);

      searchField = new JTextField(32);
      searchField.registerKeyboardAction(this, "find", 
         KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
         JComponent.WHEN_FOCUSED);
      toolbar.add(searchField);
      label.setLabelFor(searchField);

      addButton("find", "Find24", 
         KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));

      pack();
      setLocationRelativeTo(application);
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

         int start = table.getSelectedRow();

         int idx = -1;

         Pattern p = Pattern.compile(label);

         for (int i = start+1; i < entryLabels.length; i++)
         {
            Matcher m = p.matcher(entryLabels[i]);

            if (m.find())
            {
               idx = i;
               break;
            }
         }

         for (int i = 0; idx == -1 && i <= start; i++)
         {
            Matcher m = p.matcher(entryLabels[i]);

            if (m.find())
            {
               idx = i;
               break;
            }
         }

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

   public boolean hasProblem(int row)
   {
      String label = entryLabels[row];

      return glossary.hasProblem(label);
   }

   private Glossary glossary;

   private String[] entryLabels;

   private JTable table;

   private JTextField searchField;

   private MakeGlossariesGUI app;

   private JToolBar toolbar;
}

class EntryTableCellRenderer extends DefaultTableCellRenderer
{
   public EntryTableCellRenderer(ViewEntries view)
   {
      super();
      this.view = view;
   }

   public Component getTableCellRendererComponent(JTable table, Object value, 
     boolean isSelected, boolean hasFocus, int row, int column)
   {
      Component comp = super.getTableCellRendererComponent(table,
        value, isSelected, hasFocus, row, column);

      if (column == 1 && view.hasProblem(row))
      {
         setForeground(Color.red);
      }
      else
      {
         setForeground(Color.black);
      }

      return comp;
   }

   private ViewEntries view;
}
