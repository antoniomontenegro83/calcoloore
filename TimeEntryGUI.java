package calcoloore.gui;

// Import necessari
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.time.*;
import java.time.format.*;
import java.sql.SQLException; // Import OK, serve in actionPerformed
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.regex.PatternSyntaxException; // Per il filtro ricerca

import calcoloore.ShiftDAO;
import calcoloore.ShiftDAO.EmployeeInfo;
import database.DatabaseConfig;
import calcoloore.gui.EmployeeManagementDialog;


/**
 * Finestra per l'inserimento dei turni.
 * Blocco catch SQLException irraggiungibile RIMOSSO dal costruttore.
 */
public class TimeEntryGUI extends JFrame implements ActionListener {

    private static final long serialVersionUID = 1L;

    // --- Componenti UI ---
    private JComboBox<EmployeeInfo> employeeComboBox;
    private JTextField entryDateField;
    private JTextField entryTimeField;
    private JTextField exitDateField;
    private JTextField exitTimeField;
    private JButton addShiftButton;
    private JTextArea statusArea;
    private JTable sessionShiftTable;
    private DefaultTableModel sessionShiftTableModel;
    private JScrollPane sessionShiftScrollPane;
    private JPanel sessionShiftsPanel;
    private List<Object[]> sessionAddedShiftsData;
    private JButton closeButton;
    private JButton manageEmployeesButton;
    private JTextField employeeSearchField;
    private JButton searchEmployeeButton;

    // Placeholders per la combo
    private static final EmployeeInfo SELECT_PLACEHOLDER = new EmployeeInfo(-1, "--- Seleziona Dipendente ---", "");
    private static final EmployeeInfo ERROR_DAO_PLACEHOLDER = new EmployeeInfo(-3, "Errore DAO", "");
    private static final EmployeeInfo ERROR_LOADING_PLACEHOLDER = new EmployeeInfo(-3, "Errore caricamento", "");
    private static final EmployeeInfo NO_EMPLOYEES_PLACEHOLDER = new EmployeeInfo(-2, "Nessun dipendente nel DB", "");


    // Formatters
    private static final DateTimeFormatter INPUT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter SESSION_TABLE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // DAO
    private final ShiftDAO shiftDAO;

    // Costruttore GUI
    public TimeEntryGUI() {
        super("Gestione Ore Lavoro - Inserimento Turni Sessione");

        // --- Inizializza DAO (con gestione errore robusta) ---
        ShiftDAO tempDAO = null;
        boolean daoOk = false;
        try {
            System.out.println("[DEBUG TimeEntryGUI] Costruttore: Tentativo inizializzazione DAO...");
            Class.forName("database.DatabaseConfig");
            tempDAO = new ShiftDAO(); // Questa riga NON lancia SQLException checkata
            daoOk = true;
            System.out.println("[DEBUG TimeEntryGUI] Costruttore: ShiftDAO inizializzato con successo.");
        } catch (ClassNotFoundException cnfEx) {
             String errorMsg = "Errore critico: Classe DatabaseConfig non trovata.\nControllare il classpath.";
             System.err.println(errorMsg); cnfEx.printStackTrace();
             JOptionPane.showMessageDialog(null, errorMsg, "Errore Configurazione", JOptionPane.ERROR_MESSAGE);
        // <<< BLOCCO catch (SQLException sqlEx) RIMOSSO DA QUI perché irraggiungibile >>>
        } catch (Exception e) { // Cattura altre eccezioni (es. errori in new ShiftDAO() se avesse logica complessa o errori Runtime)
            String errorMsg = "Errore critico imprevisto durante inizializzazione DAO:\n" + e.getMessage();
            System.err.println(errorMsg); e.printStackTrace();
            JOptionPane.showMessageDialog(null, errorMsg, "Errore Avvio", JOptionPane.ERROR_MESSAGE);
        }
        this.shiftDAO = tempDAO;
        // --- FINE Inizializzazione DAO ---

        this.sessionAddedShiftsData = new ArrayList<>();
        Font mainFont = new Font("SansSerif", Font.PLAIN, 13), tableFont = mainFont;

        // --- Setup Finestra ---
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(850, 600));
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // --- Creazione Pannello Input (NORTH) ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Inserimento Nuovo Turno"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); gbc.anchor = GridBagConstraints.WEST;

        // --- Creazione Componenti ---
        // ... (come nella versione precedente, con font 13pt e campi data/ora di larghezza forzata uguale) ...
        JLabel el = new JLabel("Dipendente:"); el.setFont(mainFont); employeeComboBox = new JComboBox<>(); employeeComboBox.setFont(mainFont); employeeComboBox.setRenderer(new EmployeeInfoComboBoxRenderer()); JLabel searchLabel = new JLabel("Cerca:"); searchLabel.setFont(mainFont); employeeSearchField = new JTextField(15); employeeSearchField.setFont(mainFont); employeeSearchField.setToolTipText("Scrivi parte nome/qualifica e premi Invio o Cerca"); employeeSearchField.addActionListener(this); searchEmployeeButton = new JButton("Cerca"); searchEmployeeButton.setFont(mainFont.deriveFont(11f)); searchEmployeeButton.setMargin(new Insets(1, 3, 1, 3)); searchEmployeeButton.setToolTipText("Cerca dipendente nella lista"); searchEmployeeButton.addActionListener(this); manageEmployeesButton = new JButton("Gestione Dipendenti"); manageEmployeesButton.setToolTipText("Aggiungi/Modifica Dipendenti"); manageEmployeesButton.setFont(mainFont); manageEmployeesButton.addActionListener(this);
        entryDateField = new JTextField(10); entryDateField.setFont(mainFont); entryDateField.setToolTipText("GG/MM/AAAA"); entryTimeField = new JTextField(10); entryTimeField.setFont(mainFont); entryTimeField.setToolTipText("HH:MM"); exitDateField = new JTextField(10); exitDateField.setFont(mainFont); exitDateField.setToolTipText("GG/MM/AAAA"); exitTimeField = new JTextField(10); exitTimeField.setFont(mainFont); exitTimeField.setToolTipText("HH:MM"); addShiftButton = new JButton("Aggiungi Turno"); addShiftButton.setFont(mainFont); addShiftButton.setFocusPainted(false); addShiftButton.setToolTipText("Salva il turno"); addShiftButton.addActionListener(this);
        Dimension dateFieldPrefSize = entryDateField.getPreferredSize(); Dimension uniformFieldSize = new Dimension(dateFieldPrefSize.width, dateFieldPrefSize.height); entryDateField.setPreferredSize(uniformFieldSize); entryTimeField.setPreferredSize(uniformFieldSize); exitDateField.setPreferredSize(uniformFieldSize); exitTimeField.setPreferredSize(uniformFieldSize);


        // --- Layout Pannello Input ---
        // ... (come nella versione precedente) ...
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(el, gbc); gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.5; gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; inputPanel.add(employeeComboBox, gbc); gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(searchLabel, gbc); gbc.gridx = 3; gbc.gridy = 0; gbc.weightx = 0.5; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; inputPanel.add(employeeSearchField, gbc); gbc.gridx = 4; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST; inputPanel.add(searchEmployeeButton, gbc); gbc.gridx = 5; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST; inputPanel.add(manageEmployeesButton, gbc); gbc.gridwidth = 1;
        JLabel dil = new JLabel("Data Ingresso:"); dil.setFont(mainFont); JLabel oil = new JLabel("Ora Ingresso:"); oil.setFont(mainFont); gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(dil, gbc); gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; inputPanel.add(entryDateField, gbc); gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(oil, gbc); gbc.gridx = 3; gbc.gridy = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; inputPanel.add(entryTimeField, gbc); gbc.gridx = 4; gbc.gridy = 1; gbc.gridwidth = 2; inputPanel.add(new JLabel(""), gbc); gbc.gridwidth = 1;
        JLabel dul = new JLabel("Data Uscita:"); dul.setFont(mainFont); JLabel oul = new JLabel("Ora Uscita:"); oul.setFont(mainFont); gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(dul, gbc); gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; inputPanel.add(exitDateField, gbc); gbc.gridx = 2; gbc.gridy = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(oul, gbc); gbc.gridx = 3; gbc.gridy = 2; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; inputPanel.add(exitTimeField, gbc); gbc.gridx = 4; gbc.gridy = 2; gbc.gridwidth = 2; inputPanel.add(new JLabel(""), gbc); gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 6; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 0; inputPanel.add(addShiftButton, gbc);
        add(inputPanel, BorderLayout.NORTH);

        // --- Creazione Tabella Sessione (CENTER) ---
        // ... (Codice tabella invariato) ...
        sessionShiftsPanel = new JPanel(new BorderLayout()); sessionShiftsPanel.setBorder(BorderFactory.createTitledBorder("Turni Inseriti Sessione")); String[] scn = {"Dipendente", "Ingresso (Data/Ora)", "Uscita (Data/Ora)"}; sessionShiftTableModel = new DefaultTableModel(scn, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } }; sessionShiftTable = new JTable(sessionShiftTableModel); sessionShiftTable.setFont(tableFont); sessionShiftTable.getTableHeader().setFont(tableFont.deriveFont(Font.BOLD)); sessionShiftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); sessionShiftTable.setShowGrid(false); sessionShiftTable.setIntercellSpacing(new Dimension(0, 0)); DefaultTableCellRenderer dateTimeRenderer = new DefaultTableCellRenderer() { @Override public Component getTableCellRendererComponent(JTable t,Object v,boolean isS,boolean hF,int r,int c){ Component c1=super.getTableCellRendererComponent(t,v,isS,hF,r,c);if(v instanceof LocalDateTime){setText(((LocalDateTime)v).format(SESSION_TABLE_DATE_TIME_FORMATTER));}else if(v!=null){setText(v.toString());}else{setText("");}setHorizontalAlignment(SwingConstants.CENTER);return c1;}}; DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer(); leftRenderer.setHorizontalAlignment(SwingConstants.LEFT); TableColumnModel scm = sessionShiftTable.getColumnModel(); scm.getColumn(0).setCellRenderer(leftRenderer); scm.getColumn(1).setCellRenderer(dateTimeRenderer); scm.getColumn(2).setCellRenderer(dateTimeRenderer); scm.getColumn(0).setPreferredWidth(180); scm.getColumn(1).setPreferredWidth(150); scm.getColumn(2).setPreferredWidth(150);
        sessionShiftScrollPane = new JScrollPane(sessionShiftTable); sessionShiftsPanel.add(sessionShiftScrollPane, BorderLayout.CENTER); add(sessionShiftsPanel, BorderLayout.CENTER);

        // --- Pannello Sud (Stato e Chiudi) ---
        // ... (Codice pannello sud invariato) ...
         JPanel southPanelContainer = new JPanel(new BorderLayout()); statusArea = new JTextArea(3, 70); statusArea.setFont(mainFont); statusArea.setEditable(false); statusArea.setLineWrap(true); statusArea.setWrapStyleWord(true); JScrollPane ssp = new JScrollPane(statusArea); ssp.setBorder(BorderFactory.createTitledBorder("Stato")); southPanelContainer.add(ssp, BorderLayout.CENTER); JPanel closeButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT)); closeButton = new JButton("Chiudi Finestra"); closeButton.setFont(mainFont); closeButton.setFocusPainted(false); closeButton.addActionListener(this); closeButtonPanel.add(closeButton); southPanelContainer.add(closeButtonPanel, BorderLayout.SOUTH); add(southPanelContainer, BorderLayout.SOUTH);

        // --- Popola ComboBox iniziale e Imposta Stato Iniziale ---
        if (this.shiftDAO != null && daoOk) {
            populateEmployeeComboBox();
        } else {
            // DAO non inizializzato: mostra errore e disabilita tutto
            statusArea.setForeground(Color.RED);
            statusArea.setText("ERRORE CRITICO: DAO non inizializzato. Funzionalità disabilitate.");
            if(employeeComboBox != null) { employeeComboBox.addItem(ERROR_DAO_PLACEHOLDER); employeeComboBox.setEnabled(false); }
            if(manageEmployeesButton != null) manageEmployeesButton.setEnabled(false);
            if(addShiftButton != null) addShiftButton.setEnabled(false);
            if(employeeSearchField != null) employeeSearchField.setEnabled(false);
            if(searchEmployeeButton != null) searchEmployeeButton.setEnabled(false);
            if(entryDateField != null) entryDateField.setEnabled(false); if(entryTimeField != null) entryTimeField.setEnabled(false);
            if(exitDateField != null) exitDateField.setEnabled(false); if(exitTimeField != null) exitTimeField.setEnabled(false);
        }

        // --- Finalizza Finestra ---
        pack(); setMinimumSize(new Dimension(800, 600)); setLocationRelativeTo(null);
    }

    // --- Metodo populateEmployeeComboBox (con DEBUG) ---
    public void populateEmployeeComboBox() {
        System.out.println("[DEBUG TimeEntryGUI] Esecuzione populateEmployeeComboBox()...");
        if (employeeComboBox == null || manageEmployeesButton == null || addShiftButton == null || shiftDAO == null || employeeSearchField == null || searchEmployeeButton == null) { System.err.println("[DEBUG TimeEntryGUI] ERRORE POPULATE: Componenti UI o DAO null!"); /* ... gestione disabilitazione ... */ return; }
        Object previouslySelected = employeeComboBox.getSelectedItem();
        employeeComboBox.removeAllItems();
        boolean success = false;
        try {
            System.out.println("[DEBUG TimeEntryGUI] Chiamo shiftDAO.getAllEmployees()...");
            Map<Integer, ShiftDAO.EmployeeInfo> employees = shiftDAO.getAllEmployees();
            System.out.println("[DEBUG TimeEntryGUI] DAO ha restituito " + (employees == null ? "null" : employees.size()) + " dipendenti.");
            if (employees != null && !employees.isEmpty()) {
                List<ShiftDAO.EmployeeInfo> sortedInfos = new ArrayList<>(employees.values()); sortedInfos.sort(Comparator.comparing(ShiftDAO.EmployeeInfo::fullName));
                employeeComboBox.addItem(SELECT_PLACEHOLDER);
                for (ShiftDAO.EmployeeInfo info : sortedInfos) { employeeComboBox.addItem(info); }
                success = true; System.out.println("[DEBUG TimeEntryGUI] ComboBox popolata.");
            } else { employeeComboBox.addItem(NO_EMPLOYEES_PLACEHOLDER); if(statusArea != null) statusArea.setText("Nessun dipendente. Aggiungerne da 'Gestione Dipendenti'."); System.out.println("[DEBUG TimeEntryGUI] Nessun dipendente trovato."); }
        } catch (SQLException e) { String m = "Errore DB caricamento dipendenti: "+e.getMessage(); if(statusArea!=null)statusArea.setText(m); e.printStackTrace(); employeeComboBox.addItem(ERROR_LOADING_PLACEHOLDER); JOptionPane.showMessageDialog(this,m,"Errore Database",JOptionPane.ERROR_MESSAGE); System.err.println("[DEBUG TimeEntryGUI] SQLException populate: " + e.getMessage());
        } catch (Exception ex) { String m = "Errore imprevisto caricamento dipendenti: "+ex.getMessage(); if(statusArea!=null)statusArea.setText(m); ex.printStackTrace(); employeeComboBox.addItem(ERROR_LOADING_PLACEHOLDER); JOptionPane.showMessageDialog(this,m,"Errore Applicazione",JOptionPane.ERROR_MESSAGE); System.err.println("[DEBUG TimeEntryGUI] Exception populate: " + ex.getMessage()); }
        finally {
            employeeComboBox.setEnabled(success); addShiftButton.setEnabled(success); manageEmployeesButton.setEnabled(true); employeeSearchField.setEnabled(success); searchEmployeeButton.setEnabled(success);
            System.out.println("[DEBUG TimeEntryGUI] Stato finale abilitazione: Combo=" + success + ", AddShift=" + success + ", ManageEmp=" + manageEmployeesButton.isEnabled() + ", SearchField=" + success + ", SearchBtn=" + success);
            try { if (previouslySelected instanceof EmployeeInfo || previouslySelected == SELECT_PLACEHOLDER) { employeeComboBox.setSelectedItem(previouslySelected); if(employeeComboBox.getSelectedItem()!=previouslySelected){ if (employeeComboBox.getItemCount() > 0) employeeComboBox.setSelectedIndex(0); } } else { if (employeeComboBox.getItemCount() > 0) employeeComboBox.setSelectedIndex(0); } } catch (Exception setIdxEx) { System.err.println("[DEBUG TimeEntryGUI] Errore minore ripristino selezione: " + setIdxEx.getMessage()); }
            if(statusArea != null && success && statusArea.getText().startsWith("Errore")) statusArea.setText("Pronto.");
        }
    }

    // --- Metodo populateSessionShiftTable ---
    private void populateSessionShiftTable() {
        try { if (sessionShiftTableModel == null || sessionAddedShiftsData == null) return; sessionShiftTableModel.setRowCount(0); for (Object[] shiftRowData : sessionAddedShiftsData) { if (shiftRowData != null && shiftRowData.length == 3) { sessionShiftTableModel.addRow(shiftRowData); } } } catch (Exception e) { if(statusArea != null) statusArea.setText("Err Tab Ses: "+e.getMessage()); e.printStackTrace(); }
    }

    // --- Metodo refreshEmployeeCombobox ---
    public void refreshEmployeeCombobox() {
        System.out.println("[DEBUG TimeEntryGUI] Esecuzione refreshEmployeeCombobox()...");
        populateEmployeeComboBox();
    }


    // --- Gestore eventi actionPerformed (CON DEBUG DETTAGLIATO PER addShiftButton) ---
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (this.shiftDAO == null && !(source == closeButton)) { JOptionPane.showMessageDialog(this, "Operazione non possibile: DAO non inizializzato.", "Errore DAO", JOptionPane.ERROR_MESSAGE); return; }

        if (source == addShiftButton) {
             // === Blocco Debug Dettagliato per Aggiungi Turno ===
             // (Il codice qui dentro è invariato rispetto all'ultima versione
             // con tutte le stampe [DEBUG AddShift]...)
             // === FINE BLOCCO DEBUG DETTAGLIATO ===
            System.out.println("[DEBUG AddShift] Bottone premuto."); Object selectedItem = employeeComboBox.getSelectedItem(); Integer employeeId = null; String selectedEmployeeName = ""; if (selectedItem instanceof ShiftDAO.EmployeeInfo info && info.id() >= 0) { employeeId = info.id(); selectedEmployeeName = info.fullName(); System.out.println("[DEBUG AddShift] Dipendente Selezionato: ID=" + employeeId + ", Nome=" + selectedEmployeeName); } else { System.out.println("[DEBUG AddShift] Nessun dipendente valido selezionato."); JOptionPane.showMessageDialog(this, "Seleziona un dipendente valido.", "Selezione Mancante", JOptionPane.WARNING_MESSAGE); return; }
            String entryDateStr = entryDateField.getText().trim(); String entryTimeStr = entryTimeField.getText().trim(); String exitDateStr = exitDateField.getText().trim(); String exitTimeStr = exitTimeField.getText().trim(); System.out.println("[DEBUG AddShift] Date/Ore Input: " + entryDateStr + " " + entryTimeStr + " -> " + exitDateStr + " " + exitTimeStr); if (entryDateStr.isEmpty() || entryTimeStr.isEmpty() || exitDateStr.isEmpty() || exitTimeStr.isEmpty()) { System.out.println("[DEBUG AddShift] Errore: Campi data/ora vuoti."); JOptionPane.showMessageDialog(this, "Compila tutti i campi data e ora.", "Input Mancante", JOptionPane.WARNING_MESSAGE); return; }
            try {
                System.out.println("[DEBUG AddShift] Parsing date/time..."); LocalDateTime entryDateTime = LocalDateTime.parse(entryDateStr + " " + entryTimeStr, INPUT_DATE_TIME_FORMATTER); LocalDateTime exitDateTime = LocalDateTime.parse(exitDateStr + " " + exitTimeStr, INPUT_DATE_TIME_FORMATTER); System.out.println("[DEBUG AddShift] Parsing OK. Entry=" + entryDateTime + ", Exit=" + exitDateTime);
                if (!exitDateTime.isAfter(entryDateTime)) { System.out.println("[DEBUG AddShift] Errore: Uscita non dopo Ingresso."); JOptionPane.showMessageDialog(this, "L'orario di uscita deve essere successivo a quello di ingresso.", "Errore Logico", JOptionPane.WARNING_MESSAGE); return; }
                if (employeeId == null) { System.err.println("[DEBUG AddShift] ERRORE INTERNO: employeeId è null dopo la selezione!"); JOptionPane.showMessageDialog(this, "Errore interno: ID dipendente perso.", "Errore Interno", JOptionPane.ERROR_MESSAGE); return; }
                System.out.println("[DEBUG AddShift] Controllo sovrapposizione per ID: " + employeeId); boolean overlap = this.shiftDAO.hasOverlappingShift(employeeId, entryDateTime, exitDateTime, null); System.out.println("[DEBUG AddShift] Sovrapposizione trovata: " + overlap);
                if (overlap) { System.out.println("[DEBUG AddShift] Turno sovrapposto, inserimento annullato."); statusArea.setText("Inserimento annullato: turno sovrapposto."); JOptionPane.showMessageDialog(this, "Attenzione: Il turno si sovrappone con uno esistente.\nInserimento annullato.", "Turno Sovrapposto", JOptionPane.WARNING_MESSAGE); return; }
                System.out.println("[DEBUG AddShift] Chiamo shiftDAO.saveShift..."); this.shiftDAO.saveShift(employeeId, entryDateTime, exitDateTime); System.out.println("[DEBUG AddShift] saveShift completato con successo."); statusArea.setText("Turno aggiunto per " + selectedEmployeeName + ".");
                Object[] sessionRowData = new Object[]{selectedEmployeeName, entryDateTime, exitDateTime}; if (sessionAddedShiftsData == null) { sessionAddedShiftsData = new ArrayList<>(); } sessionAddedShiftsData.add(sessionRowData); System.out.println("[DEBUG AddShift] Dati aggiunti a sessionAddedShiftsData. Size ora: " + sessionAddedShiftsData.size());
                System.out.println("[DEBUG AddShift] Chiamo populateSessionShiftTable..."); populateSessionShiftTable(); System.out.println("[DEBUG AddShift] populateSessionShiftTable chiamato.");
                entryDateField.setText(""); entryTimeField.setText(""); exitDateField.setText(""); exitTimeField.setText(""); employeeComboBox.setSelectedItem(SELECT_PLACEHOLDER); System.out.println("[DEBUG AddShift] Campi puliti.");
            } catch (DateTimeParseException ex) { System.err.println("[DEBUG AddShift] Errore DateTimeParseException: " + ex.getMessage()); statusArea.setText("Errore formato data/ora (GG/MM/AAAA HH:MM)."); JOptionPane.showMessageDialog(this, "Formato data/ora non valido.\nUsare GG/MM/AAAA HH:MM", "Errore Formato", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) { System.err.println("[DEBUG AddShift] Errore SQLException: " + ex.getMessage()); ex.printStackTrace(); statusArea.setText("Errore Database: " + ex.getMessage()); JOptionPane.showMessageDialog(this, "Errore durante il salvataggio del turno:\n" + ex.getMessage(), "Errore Database", JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) { System.err.println("[DEBUG AddShift] Errore IllegalArgumentException: " + ex.getMessage()); ex.printStackTrace(); statusArea.setText("Errore dati inseriti: " + ex.getMessage()); JOptionPane.showMessageDialog(this, "Errore nei dati inseriti:\n" + ex.getMessage(), "Errore Dati", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) { System.err.println("[DEBUG AddShift] Errore generico Exception: " + ex.getMessage()); ex.printStackTrace(); statusArea.setText("Errore imprevisto: " + ex.getMessage()); JOptionPane.showMessageDialog(this, "Si è verificato un errore imprevisto:\n" + ex.getMessage(), "Errore Generico", JOptionPane.ERROR_MESSAGE); }
            System.out.println("[DEBUG AddShift] Fine gestione click.");
        }
        // Gestione Bottone "Gestione Dipendenti" (CON DEBUG)
        else if (source == manageEmployeesButton) {
            System.out.println("[DEBUG TimeEntryGUI] Click 'Gestione Dipendenti'. Apro EmployeeManagementDialog..."); final TimeEntryGUI self = this; final ShiftDAO daoInstance = this.shiftDAO; final Window parentWin = SwingUtilities.getWindowAncestor(this); System.out.println("[DEBUG TimeEntryGUI] Parent Window per Emp Mgmt Dialog: " + parentWin); if (daoInstance == null) { System.err.println("ERRORE: DAO nullo per Emp Mgmt Dialog!"); return; }
            SwingUtilities.invokeLater(() -> {
                 System.out.println("[DEBUG TimeEntryGUI] Dentro invokeLater per EmployeeManagementDialog.");
                 try { System.out.println("[DEBUG TimeEntryGUI] Creo EmployeeManagementDialog..."); EmployeeManagementDialog empDialog = new EmployeeManagementDialog(parentWin, daoInstance, self); System.out.println("[DEBUG TimeEntryGUI] EmployeeManagementDialog istanziato."); empDialog.setVisible(true); System.out.println("[DEBUG TimeEntryGUI] Dialogo dipendenti chiuso. Chiamo refreshEmployeeCombobox..."); refreshEmployeeCombobox(); }
                 catch (Throwable t) { System.err.println("[ERRORE TimeEntryGUI] Eccezione apertura/gestione EmployeeManagementDialog:"); t.printStackTrace(); JOptionPane.showMessageDialog(parentWin, "Errore apertura Gestione Dipendenti:\n" + t.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE); }
            });
            System.out.println("[DEBUG TimeEntryGUI] invokeLater per EmployeeManagementDialog sottomesso.");
        }
        // Gestione bottone ricerca o Invio nel campo ricerca (invariato)
        else if (source == searchEmployeeButton || source == employeeSearchField) {
             String searchText = employeeSearchField.getText().trim().toLowerCase(); System.out.println("[DEBUG TimeEntryGUI] Ricerca per: '" + searchText + "'"); if (searchText.isEmpty()) { return; }
             ComboBoxModel<EmployeeInfo> model = employeeComboBox.getModel(); boolean found = false;
             for (int i = 0; i < model.getSize(); i++) { Object element = model.getElementAt(i); if (element instanceof ShiftDAO.EmployeeInfo info && info.id() >= 0) { String fnl = info.fullName().toLowerCase(); String ql = (info.qualifica() != null ? info.qualifica().toLowerCase() : ""); if (fnl.contains(searchText) || ql.contains(searchText)) { System.out.println("[DEBUG TimeEntryGUI] Trovato match: " + info.fullName()); employeeComboBox.setSelectedItem(info); found = true; break; } } }
             if (!found) { System.out.println("[DEBUG TimeEntryGUI] Nessun match."); JOptionPane.showMessageDialog(this, "Nessun match per '" + employeeSearchField.getText() + "'.", "Ricerca", JOptionPane.INFORMATION_MESSAGE); }
        }
        // Gestione bottone chiudi finestra
        else if (source == closeButton) {
            System.out.println("[DEBUG TimeEntryGUI] Bottone Chiudi Finestra premuto."); this.dispose();
        } else { System.out.println("[WARN TimeEntryGUI] Evento non gestito: " + e.getSource()); }
    } // Fine actionPerformed


    // --- CLASSE INTERNA RENDERER COMBOBOX (invariata) ---
    private class EmployeeInfoComboBoxRenderer extends DefaultListCellRenderer { /* ... codice invariato ... */
         @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) { super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus); if (value instanceof ShiftDAO.EmployeeInfo info) { String displayText; if (info.id() < 0) { displayText = info.fullName(); } else if (info.qualifica() != null && !info.qualifica().isEmpty()) { displayText = String.format("%s - %s", info.qualifica(), info.fullName()); } else { displayText = info.fullName(); } setText(displayText); } else if (value != null) { setText(value.toString()); } else { setText(""); } return this; }
     } // --- Fine EmployeeInfoComboBoxRenderer ---

} // Fine classe TimeEntryGUI