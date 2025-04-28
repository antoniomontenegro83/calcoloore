package calcoloore.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import calcoloore.ShiftDAO;

/**
 * Finestra di dialogo per la ricerca turni per data inserimento.
 * Modificata: Rimosso focus painting dai pulsanti.
 */
public class SearchByEntryDateDialog extends JDialog implements ActionListener {

    // Componenti
    private JTextField entryDateSearchField;
    private JButton searchButton;
    private JTable shiftTable;
    private DefaultTableModel shiftTableModel;
    private JButton closeButton;

    // DAO
    private ShiftDAO shiftDAO;

    // Formatters
    private static final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Costruttore
    public SearchByEntryDateDialog(Frame owner, ShiftDAO shiftDAO) { // Modificato per Frame
        super(owner, "Ricerca Turni per Data Inserimento", true);
        this.shiftDAO = shiftDAO;
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(700, 450));

        Font dialogFont = new Font("SansSerif", Font.PLAIN, 13);
        Font tableFont = dialogFont;
        Font tableHeaderFont = tableFont.deriveFont(Font.BOLD);

        // --- Pannello Filtri ---
        JPanel filterPanel = new JPanel(new GridBagLayout());
        filterPanel.setBorder(BorderFactory.createTitledBorder("Filtro Ricerca"));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(5, 5, 5, 5);
        // Data
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST; filterPanel.add(new JLabel("Data Inserimento:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        entryDateSearchField = new JTextField(10); entryDateSearchField.setFont(dialogFont); entryDateSearchField.setToolTipText("GG/MM/AAAA - Cerca turni inseriti in questa data"); filterPanel.add(entryDateSearchField, gbc);
        // Bottone Cerca
        gbc.gridx = 2; gbc.gridy = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        searchButton = new JButton("Cerca"); searchButton.setFont(dialogFont);
        searchButton.setFocusPainted(false); // *** AGGIUNTO ***
        searchButton.addActionListener(this); filterPanel.add(searchButton, gbc);
        add(filterPanel, BorderLayout.NORTH);

        // --- Pannello Tabella ---
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Elenco Turni Trovati"));
        String[] columnNames = {"ID Turno", "ID Dip", "Dipendente", "Ingresso", "Uscita"};
        shiftTableModel = new DefaultTableModel(columnNames, 0) { /* ... renderer e classi ... */ };
        shiftTable = new JTable(shiftTableModel);
        shiftTable.setAutoCreateRowSorter(true); shiftTable.setFont(tableFont); shiftTable.getTableHeader().setFont(tableHeaderFont); shiftTable.setRowHeight(tableFont.getSize()+6);
        shiftTable.setShowGrid(false); shiftTable.setIntercellSpacing(new Dimension(0, 0));
        // Nascondi ID
        shiftTable.getColumnModel().getColumn(0).setMaxWidth(0); shiftTable.getColumnModel().getColumn(0).setMinWidth(0); shiftTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        shiftTable.getColumnModel().getColumn(1).setMaxWidth(0); shiftTable.getColumnModel().getColumn(1).setMinWidth(0); shiftTable.getColumnModel().getColumn(1).setPreferredWidth(0);
        // Renderer Date
        DefaultTableCellRenderer dtr = new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object v,boolean isS,boolean hF,int r,int c){JLabel l=(JLabel)super.getTableCellRendererComponent(t,v,isS,hF,r,c); if(v instanceof LocalDateTime)l.setText(((LocalDateTime)v).format(displayFormatter)); else l.setText(v==null?"":v.toString()); l.setHorizontalAlignment(v instanceof LocalDateTime?SwingConstants.CENTER:SwingConstants.LEFT); return l;}};
        shiftTable.setDefaultRenderer(LocalDateTime.class, dtr);
        tablePanel.add(new JScrollPane(shiftTable), BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);

        // --- Pannello Chiudi ---
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeButton = new JButton("Chiudi"); closeButton.setFont(dialogFont);
        closeButton.setFocusPainted(false); // *** AGGIUNTO ***
        closeButton.addActionListener(this); closePanel.add(closeButton);
        add(closePanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
    }

    // Metodo populateShiftTable (invariato)
    private void populateShiftTable(List<Object[]> shiftsData) { /* ... codice invariato ... */ }

    // Metodo actionPerformed (invariato)
    @Override
    public void actionPerformed(ActionEvent e) { /* ... codice invariato ... */ }
}