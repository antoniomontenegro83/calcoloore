package calcoloore.gui;

// --- Import Necessari ---
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // Necessario per ordinare EmployeeInfo
// import java.util.Arrays; // Necessario solo se decommenti la stampa della riga in populateShiftTable

import calcoloore.ShiftDAO;
import calcoloore.ShiftDAO.EmployeeInfo; // Import per EmployeeInfo
import calcoloore.gui.ShiftEditDialog; // Assumendo che esista
// --- Fine Import ---

/**
 * Finestra di dialogo per la gestione dei turni.
 * Aggiornata per usare EmployeeInfo nella ComboBox filtro.
 * Include stampe di debug dettagliate.
 */
public class ShiftManagementDialog extends JDialog implements ActionListener {

    // --- Componenti UI ---
    private JComboBox<EmployeeInfo> employeeFilterComboBox;
    private JTextField startDateField;
    private JTextField endDateField;
    private JButton searchButton;
    private JTable shiftTable;
    private DefaultTableModel shiftTableModel;
    private JButton editButton;
    private JButton deleteButton;
    private JButton closeButton;

    // --- Dati e Logica ---
    private final ShiftDAO shiftDAO;
    // Mappa Nome -> ID non più necessaria qui
    private Map<String, Object> lastSearchParams = new HashMap<>(); // Memorizza ultimi parametri ricerca

    // Placeholder per la combo "Tutti"
    private static final EmployeeInfo ALL_EMPLOYEES_PLACEHOLDER = new EmployeeInfo(-1, "Tutti i dipendenti", "");
    // Placeholder per errori nella combo
    private static final EmployeeInfo ERROR_DAO_PLACEHOLDER = new EmployeeInfo(-3, "Errore DAO", "");
    private static final EmployeeInfo ERROR_LOADING_PLACEHOLDER = new EmployeeInfo(-3, "Errore Caricamento", "");

    // Formatters
    private static final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // --- Costruttore ---
    public ShiftManagementDialog(Frame owner, ShiftDAO dao) {
        super(owner, "Gestione e Ricerca Turni", true); // Modale

        if (dao == null) { throw new IllegalArgumentException("ShiftDAO non può essere nullo."); }
        this.shiftDAO = dao;
        // this.employeeNameToIdMap = new HashMap<>(); // Rimosso

        // --- Setup UI ---
        setLayout(new BorderLayout(5, 5));
        setPreferredSize(new Dimension(750, 500));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        Font dialogFont = new Font("SansSerif", Font.PLAIN, 13), tableFont = dialogFont, tableHeaderFont = tableFont.deriveFont(Font.BOLD);

        // Pannello Filtri (NORTH)
        JPanel filterPanel = new JPanel(new GridBagLayout()); filterPanel.setBorder(BorderFactory.createTitledBorder("Filtri Ricerca"));
        GridBagConstraints gbc = new GridBagConstraints(); gbc.insets = new Insets(5, 5, 5, 5); gbc.anchor = GridBagConstraints.WEST;
        // Dipendente
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST; filterPanel.add(new JLabel("Dipendente:"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.anchor = GridBagConstraints.WEST;
        employeeFilterComboBox = new JComboBox<>(); // Tipo EmployeeInfo
        employeeFilterComboBox.setFont(dialogFont);
        employeeFilterComboBox.setRenderer(new EmployeeInfoComboBoxRenderer()); // Usa Renderer
        filterPanel.add(employeeFilterComboBox, gbc);
        // Periodo
        gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; filterPanel.add(new JLabel("Periodo Lavoro Da:"), gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5; gbc.anchor = GridBagConstraints.WEST; startDateField = new JTextField(10); startDateField.setFont(dialogFont); startDateField.setToolTipText("GG/MM/AAAA"); filterPanel.add(startDateField, gbc);
        gbc.gridx = 2; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0; gbc.anchor = GridBagConstraints.EAST; filterPanel.add(new JLabel("A:"), gbc);
        gbc.gridx = 3; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5; gbc.anchor = GridBagConstraints.WEST; endDateField = new JTextField(10); endDateField.setFont(dialogFont); endDateField.setToolTipText("GG/MM/AAAA"); filterPanel.add(endDateField, gbc);
        // Bottone Cerca
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 0;
        searchButton = new JButton("Cerca per Dipendente/Periodo"); searchButton.setFont(dialogFont); searchButton.setFocusPainted(false); searchButton.addActionListener(this); searchButton.setEnabled(false); // Inizia disabilitato
        filterPanel.add(searchButton, gbc);
        add(filterPanel, BorderLayout.NORTH);

        // Popola ComboBox Filtro Dipendenti (che abiliterà il bottone Cerca)
        populateEmployeeFilterComboBox();

        // Pannello Tabella (CENTER)
        JPanel tablePanel = new JPanel(new BorderLayout(0, 5)); tablePanel.setBorder(BorderFactory.createTitledBorder("Elenco Turni Trovati")); String[] columnNames = {"ID Turno", "ID Dip", "Dipendente", "Ingresso", "Uscita"};
        shiftTableModel = new DefaultTableModel(columnNames, 0) {
             @Override public boolean isCellEditable(int r,int c){return false;}
             @Override public Class<?> getColumnClass(int c){ switch(c){ case 0: return Integer.class; case 1: return Integer.class; case 2: return String.class; case 3: return LocalDateTime.class; case 4: return LocalDateTime.class; default: return Object.class; }}
        };
        shiftTable = new JTable(shiftTableModel); shiftTable.setAutoCreateRowSorter(true); shiftTable.setFont(tableFont); shiftTable.getTableHeader().setFont(tableHeaderFont); shiftTable.setRowHeight(tableFont.getSize()+6); shiftTable.setShowGrid(false); shiftTable.setIntercellSpacing(new Dimension(0, 0)); shiftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumnModel tcm = shiftTable.getColumnModel(); tcm.getColumn(0).setMaxWidth(0); tcm.getColumn(0).setMinWidth(0); tcm.getColumn(0).setPreferredWidth(0); tcm.getColumn(1).setMaxWidth(0); tcm.getColumn(1).setMinWidth(0); tcm.getColumn(1).setPreferredWidth(0); // Nascondi anche ID Dip
        tcm.getColumn(2).setPreferredWidth(200); // Più spazio per nome
        tcm.getColumn(3).setPreferredWidth(150); // Ingresso
        tcm.getColumn(4).setPreferredWidth(150); // Uscita
        // Renderer per LocalDateTime
        DefaultTableCellRenderer dtr = new DefaultTableCellRenderer(){ @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean isS,boolean hF,int r,int c){ JLabel l=(JLabel)super.getTableCellRendererComponent(t,v,isS,hF,r,c); if(v instanceof LocalDateTime)l.setText(((LocalDateTime)v).format(displayFormatter)); else l.setText(v==null?"":v.toString()); l.setHorizontalAlignment(v instanceof LocalDateTime?SwingConstants.CENTER:SwingConstants.LEFT); return l;} };
        shiftTable.setDefaultRenderer(LocalDateTime.class, dtr);
        // Doppio click per modificare
        shiftTable.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) { int vr = shiftTable.getSelectedRow(); if (vr != -1) startEditForSelectedRow(vr); } } });
        tablePanel.add(new JScrollPane(shiftTable), BorderLayout.CENTER);

        // Pannello Azioni Tabella
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        editButton = new JButton("Modifica Selezionato"); editButton.setFont(dialogFont); editButton.setToolTipText("Modifica il turno selezionato"); editButton.setFocusPainted(false); editButton.addActionListener(this); actionPanel.add(editButton);
        deleteButton = new JButton("Elimina Selezionato"); deleteButton.setFont(dialogFont); deleteButton.setFocusPainted(false); deleteButton.addActionListener(this); actionPanel.add(deleteButton);
        tablePanel.add(actionPanel, BorderLayout.SOUTH);
        add(tablePanel, BorderLayout.CENTER);

        // Pannello Chiusura (SOUTH)
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeButton = new JButton("Chiudi"); closeButton.setFont(dialogFont); closeButton.setFocusPainted(false); closeButton.addActionListener(this); closePanel.add(closeButton);
        add(closePanel, BorderLayout.SOUTH);

        // Finalizza
        pack(); setMinimumSize(new Dimension(700, 450)); setLocationRelativeTo(owner); setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }


    // --- Metodo populateEmployeeFilterComboBox (Aggiornato per EmployeeInfo) ---
    private void populateEmployeeFilterComboBox() {
        System.out.println("[DEBUG ShiftManagementDialog] Entrato in populateEmployeeFilterComboBox()");
        if (employeeFilterComboBox == null || searchButton == null || shiftDAO == null) { System.err.println("ERRORE: Componenti o DAO null in populateEmployeeFilterComboBox!"); return; }

        Object previouslySelected = employeeFilterComboBox.getSelectedItem();
        employeeFilterComboBox.removeAllItems();
        // employeeNameToIdMap.clear(); // Rimosso
        boolean employeesFound = false;
        try {
            employeeFilterComboBox.addItem(ALL_EMPLOYEES_PLACEHOLDER); // Aggiungi placeholder "Tutti"

            Map<Integer, ShiftDAO.EmployeeInfo> employees = shiftDAO.getAllEmployees();
            System.out.println("[DEBUG ShiftManagementDialog] DAO getAllEmployees ha restituito " + (employees == null ? "null" : employees.size() + " elementi."));

            if (employees != null && !employees.isEmpty()) {
                 System.out.println("[DEBUG ShiftManagementDialog] Popolo filtro ComboBox...");
                 List<ShiftDAO.EmployeeInfo> sortedInfos = new ArrayList<>(employees.values());
                 sortedInfos.sort(Comparator.comparing(ShiftDAO.EmployeeInfo::fullName));
                 for (ShiftDAO.EmployeeInfo info : sortedInfos) { employeeFilterComboBox.addItem(info); } // Aggiungi oggetti
                 employeesFound = true;
            } else { System.out.println("[DEBUG ShiftManagementDialog] Nessun dipendente trovato per popolare il filtro."); }
        } catch (SQLException e) {
             System.err.println("[DEBUG ShiftManagementDialog] SQLException in populateEmployeeFilterComboBox:"); e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Errore caricamento dipendenti per filtro: " + e.getMessage(), "Errore DB", JOptionPane.ERROR_MESSAGE);
            employeeFilterComboBox.addItem(ERROR_LOADING_PLACEHOLDER); // Placeholder Errore
        } finally {
             employeeFilterComboBox.setEnabled(true); // Abilita sempre la combo
             searchButton.setEnabled(true); // Abilita sempre il cerca
             // Ripristina selezione
             if (previouslySelected != null) { employeeFilterComboBox.setSelectedItem(previouslySelected); if(employeeFilterComboBox.getSelectedItem() != previouslySelected) employeeFilterComboBox.setSelectedIndex(0); }
             else { employeeFilterComboBox.setSelectedIndex(0); } // Seleziona "Tutti"
             System.out.println("[DEBUG ShiftManagementDialog] Filtro ComboBox abilitato e selezione ripristinata (se possibile).");
        }
         System.out.println("[DEBUG ShiftManagementDialog] Uscito da populateEmployeeFilterComboBox(). Elementi: " + employeeFilterComboBox.getItemCount());
    }
    // --- Fine populateEmployeeFilterComboBox ---


    // --- Metodo populateShiftTable (CON DEBUG PRINTS DETTAGLIATI) ---
    // Assicurati che questo sia il codice presente nel tuo ShiftManagementDialog.java
    private void populateShiftTable(List<Object[]> shiftsData) {
        // DEBUG: Inizio metodo e dati ricevuti
        System.out.println("[DEBUG ShiftManagementDialog] === Entrato in populateShiftTable() ==="); // DEBUG 1
        System.out.println("[DEBUG ShiftManagementDialog] Dati ricevuti: " + (shiftsData == null ? "null" : shiftsData.size() + " righe.")); // DEBUG 2

        if (shiftTableModel == null) {
            System.err.println("[DEBUG ShiftManagementDialog] ERRORE FATALE: shiftTableModel è null!");
            return;
        }

        // Svuota tabella
        shiftTableModel.setRowCount(0);
        System.out.println("[DEBUG ShiftManagementDialog] Tabella (modello) svuotata."); // DEBUG 3

        int rowsAdded = 0; // Contatore righe aggiunte
        if (shiftsData != null && !shiftsData.isEmpty()) {
            System.out.println("[DEBUG ShiftManagementDialog] Inizio ciclo for per aggiungere righe..."); // DEBUG 4
            for (Object[] row : shiftsData) {
                // DEBUG: Stampa la riga che sta per essere aggiunta (opzionale)
                // System.out.println("[DEBUG ShiftManagementDialog] Processing row: " + java.util.Arrays.toString(row));
                if (row != null && row.length >= 5) {
                    // Aggiunge i primi 5 elementi come definito dalle colonne visibili/DAO
                    // ID Turno, ID Dip, Dipendente, Ingresso, Uscita
                    shiftTableModel.addRow(new Object[]{ row[0], row[1], row[2], row[3], row[4] });
                    rowsAdded++; // Incrementa contatore
                } else {
                    System.err.println("[WARN ShiftManagementDialog] Trovato row nullo o di lunghezza < 5 in shiftsData! Riga saltata."); // DEBUG 5 (se errore)
                }
            }
            System.out.println("[DEBUG ShiftManagementDialog] Fine ciclo for."); // DEBUG 6
        } else {
             System.out.println("[DEBUG ShiftManagementDialog] Lista shiftsData è nulla o vuota, nessuna riga da aggiungere."); // DEBUG 7 (se vuota)
        }

        // DEBUG: Righe aggiunte e righe totali nel modello
        System.out.println("[DEBUG ShiftManagementDialog] populateShiftTable: Aggiunte effettivamente " + rowsAdded + " righe al modello."); // DEBUG 8
        System.out.println("[DEBUG ShiftManagementDialog] === Uscito da populateShiftTable(). Righe totali nel modello: " + shiftTableModel.getRowCount() + " ==="); // DEBUG 9
     }
     // --- Fine populateShiftTable ---


    // --- Gestore eventi actionPerformed (CON DEBUG PRINT DOPO CHIAMATA DAO) ---
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        System.out.println("[DEBUG ShiftManagementDialog] actionPerformed source: " + source.getClass().getName());
         if (shiftDAO == null) { JOptionPane.showMessageDialog(this, "DAO non disponibile.", "Errore", JOptionPane.ERROR_MESSAGE); return; }

        if (source == searchButton) {
             System.out.println("[DEBUG ShiftManagementDialog] Click su Search Button.");
             lastSearchParams.clear(); lastSearchParams.put("type", "work_period");

             // Ottieni ID da EmployeeInfo selezionato
             Object selectedItem = employeeFilterComboBox.getSelectedItem();
             Integer eid = null; // null per "Tutti"
             if (selectedItem instanceof ShiftDAO.EmployeeInfo info && info.id() >= 0) { // ID >= 0 sono dipendenti reali
                 eid = info.id();
                 lastSearchParams.put("employeeId", eid);
                 System.out.println("[DEBUG ShiftManagementDialog] Filtro Dipendente ID: " + eid + " ("+info.fullName()+")");
             } else {
                 System.out.println("[DEBUG ShiftManagementDialog] Filtro Dipendente: Tutti");
             }

             // Parsing date
             LocalDateTime sf=null, ef=null; String startDateStr = startDateField.getText().trim(); String endDateStr = endDateField.getText().trim();
             try{ if(!startDateStr.isEmpty()){ sf=LocalDate.parse(startDateStr,DATE_ONLY_FORMATTER).atStartOfDay(); lastSearchParams.put("startDate",sf); } if(!endDateStr.isEmpty()){ ef=LocalDate.parse(endDateStr,DATE_ONLY_FORMATTER).atTime(LocalTime.MAX); lastSearchParams.put("endDate",ef); } }
             catch(DateTimeParseException ex){ JOptionPane.showMessageDialog(this,"Formato Data errato (GG/MM/AAAA)","Errore",JOptionPane.WARNING_MESSAGE); return; }
             if(sf!=null&&ef!=null&&ef.isBefore(sf)){ JOptionPane.showMessageDialog(this,"Data 'A' non può precedere 'Da'.","Errore",JOptionPane.WARNING_MESSAGE); return; }

             // Esecuzione query
             try{
                 System.out.println("[DEBUG ShiftManagementDialog] Chiamo shiftDAO.getShiftsForManagement con ID: " + eid + ", Start: " + sf + ", End: " + ef); // Debug prima chiamata DAO
                 List<Object[]> results = shiftDAO.getShiftsForManagement(eid,sf,ef); // Usa parametri corretti
                 // *** STAMPA DEBUG AGGIUNTA/VERIFICATA ***
                 System.out.println("[DEBUG ShiftManagementDialog] DAO getShiftsForManagement ha restituito: " + (results == null ? "null" : results.size() + " risultati."));
                 populateShiftTable(results); // Chiama il popolamento (che ora ha i suoi debug)
             } catch(SQLException ex){ System.err.println("[DEBUG ShiftManagementDialog] SQLException in searchButton:"); ex.printStackTrace(); JOptionPane.showMessageDialog(this,"Errore recupero turni: "+ex.getMessage(),"Errore Database",JOptionPane.ERROR_MESSAGE);
             } catch (Exception ex) { System.err.println("[DEBUG ShiftManagementDialog] Exception generica in searchButton:"); ex.printStackTrace(); JOptionPane.showMessageDialog(this,"Errore imprevisto ricerca: "+ex.getMessage(),"Errore Applicazione",JOptionPane.ERROR_MESSAGE); }
        }
        else if (source == deleteButton) {
            int vr=shiftTable.getSelectedRow(); if(vr==-1){ JOptionPane.showMessageDialog(this,"Seleziona un turno da eliminare.","Attenzione",JOptionPane.WARNING_MESSAGE); return; }
            int mr=shiftTable.convertRowIndexToModel(vr); Integer sid=(Integer)shiftTableModel.getValueAt(mr,0);
            if(sid == null) { JOptionPane.showMessageDialog(this,"ID turno non valido.","Errore Dati",JOptionPane.ERROR_MESSAGE); return; }
            int conf=JOptionPane.showConfirmDialog(this,"Eliminare il turno selezionato (ID: "+sid+")?","Conferma",JOptionPane.YES_NO_OPTION);
            if(conf==JOptionPane.YES_OPTION){ try{ shiftDAO.deleteShift(sid); JOptionPane.showMessageDialog(this,"Turno eliminato.","Successo",JOptionPane.INFORMATION_MESSAGE); refreshLastSearch(); } catch(SQLException ex){ /*...*/ JOptionPane.showMessageDialog(this,"Errore eliminazione: "+ex.getMessage(),"Errore DB",JOptionPane.ERROR_MESSAGE); ex.printStackTrace(); } }
        }
        else if (source == editButton) {
            int vr=shiftTable.getSelectedRow(); if(vr!=-1) { startEditForSelectedRow(vr); } else { JOptionPane.showMessageDialog(this,"Seleziona un turno da modificare.","Attenzione",JOptionPane.WARNING_MESSAGE); }
        }
        else if (source == closeButton) {
             System.out.println("[DEBUG ShiftManagementDialog] Chiudo dialogo.");
             dispose();
        }
     } // Fine actionPerformed

    // Metodo helper startEditForSelectedRow (invariato)
    private void startEditForSelectedRow(int viewRow) {
        if(viewRow<0||viewRow>=shiftTable.getRowCount()) return;
        int modelRowIndex = shiftTable.convertRowIndexToModel(viewRow);
        try {
            Integer sid = (Integer)shiftTableModel.getValueAt(modelRowIndex,0); Integer eid = (Integer)shiftTableModel.getValueAt(modelRowIndex,1); LocalDateTime start = (LocalDateTime)shiftTableModel.getValueAt(modelRowIndex,3); LocalDateTime end = (LocalDateTime)shiftTableModel.getValueAt(modelRowIndex,4);
            if (sid==null||eid==null||start==null||end==null){ JOptionPane.showMessageDialog(this,"Dati turno non validi.","Errore",JOptionPane.ERROR_MESSAGE); return; }
            ShiftEditDialog ed = new ShiftEditDialog(this, sid, eid, start, end, this.shiftDAO);
            ed.setVisible(true);
            refreshLastSearch();
        } catch (ClassCastException cce) { JOptionPane.showMessageDialog(this,"Errore formato dati tabella.","Errore Interno",JOptionPane.ERROR_MESSAGE); cce.printStackTrace();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this,"Errore apertura modifica: "+ex.getMessage(),"Errore",JOptionPane.ERROR_MESSAGE); ex.printStackTrace(); }
    }

    // Metodo refreshLastSearch (invariato)
    private void refreshLastSearch() {
        System.out.println("[DEBUG ShiftManagementDialog] Entrato in refreshLastSearch(). Parametri: " + lastSearchParams);
        String st=(String)lastSearchParams.getOrDefault("type","none");
        try{
            List<Object[]> s=null;
            if (shiftDAO == null) { System.err.println("ERRORE: DAO null in refresh!"); JOptionPane.showMessageDialog(this, "Errore DAO.", "Errore", JOptionPane.ERROR_MESSAGE); populateShiftTable(new ArrayList<>()); return; }
            if("work_period".equals(st)){ Integer eid=(Integer)lastSearchParams.get("employeeId"); LocalDateTime sd=(LocalDateTime)lastSearchParams.get("startDate"); LocalDateTime ed=(LocalDateTime)lastSearchParams.get("endDate"); System.out.println("[DEBUG] Rieseguo getShiftsForManagement ID:" + eid + ", S:" + sd + ", E:" + ed); s=shiftDAO.getShiftsForManagement(eid,sd,ed); }
            else { System.out.println("[DEBUG] Ricerca generale..."); s=shiftDAO.getShiftsForManagement(null,null,null); }
            populateShiftTable(s);
        }catch(SQLException ex){ System.err.println("SQLException in refreshLastSearch:"); ex.printStackTrace(); JOptionPane.showMessageDialog(this,"Errore aggiornamento: "+ex.getMessage(),"Errore DB",JOptionPane.ERROR_MESSAGE); populateShiftTable(new ArrayList<>());
        } catch (Exception ex) { System.err.println("Exception in refreshLastSearch:"); ex.printStackTrace(); JOptionPane.showMessageDialog(this,"Errore imprevisto aggiornamento: "+ex.getMessage(),"Errore App",JOptionPane.ERROR_MESSAGE); populateShiftTable(new ArrayList<>()); }
         System.out.println("[DEBUG ShiftManagementDialog] Uscito da refreshLastSearch()");
    }


    // --- CLASSE INTERNA RENDERER (Aggiornata per gestire placeholder "Tutti") ---
    private class EmployeeInfoComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ShiftDAO.EmployeeInfo info) {
                String displayText;
                // Gestisce i placeholder speciali (ID < 0)
                if (info.id() < 0) {
                     displayText = info.fullName(); // Mostra "Tutti i dipendenti", "Errore", ecc.
                } else if (info.qualifica() != null && !info.qualifica().isEmpty()) {
                    // Formato: "Qualifica - Cognome Nome"
                    displayText = String.format("%s - %s", info.qualifica(), info.fullName());
                } else {
                    // Formato: "Cognome Nome" (se qualifica manca)
                    displayText = info.fullName();
                }
                setText(displayText);
            } else if (value != null) { setText(value.toString()); } // Fallback
              else { setText(""); }
            return this;
        }
    } // --- Fine Renderer ---

} // Fine classe ShiftManagementDialog