package calcoloore.gui;

// --- Import Necessari ---
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // Necessario per ordinare EmployeeInfo
import java.util.List;

import calcoloore.ShiftDAO;
import calcoloore.ShiftDAO.EmployeeInfo; // <<< IMPORT AGGIUNTO >>>
import calcoloore.gui.WeeklyReportDisplayDialog;
// --- Fine Import ---

/**
 * Finestra di dialogo modale per selezionare i parametri (dipendente, settimana)
 * per la generazione del report settimanale.
 * Aggiornata per usare EmployeeInfo nella ComboBox.
 */
public class WeeklyReportDialog extends JDialog implements ActionListener {

    // --- Componenti UI ---
    // <<< MODIFICA TIPO >>>
    private JComboBox<EmployeeInfo> employeeComboBox;
    private JTextField weekDateField;
    private JButton generateButton;
    private JButton cancelButton;

    // --- Dati e Logica ---
    private final ShiftDAO shiftDAO;
    // private Map<String, Integer> employeeNameToIdMap; // <<< NON PIU' NECESSARIO >>>

    // Placeholder per la combo
    private static final EmployeeInfo SELECT_PLACEHOLDER = new EmployeeInfo(-1, "--- Seleziona Dipendente ---", "");

    // Formatter per l'input della data
    private static final DateTimeFormatter DATE_INPUT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // --- Costruttore ---
    public WeeklyReportDialog(JFrame owner, ShiftDAO dao) {
        super(owner, "Seleziona Parametri Report Settimanale", true);
        if (dao == null) { throw new IllegalArgumentException("ShiftDAO non può essere nullo."); }
        this.shiftDAO = dao;
        // this.employeeNameToIdMap = new HashMap<>(); // Rimosso

        // --- Setup UI ---
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); gbc.fill = GridBagConstraints.HORIZONTAL;

        Font dialogFont = new Font("SansSerif", Font.PLAIN, 13);

        // 1. Dipendente
        JLabel employeeLabel = new JLabel("Dipendente:"); employeeLabel.setFont(dialogFont);
        gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.0; add(employeeLabel, gbc);
        // <<< MODIFICA TIPO COMBOBOX >>>
        employeeComboBox = new JComboBox<>();
        employeeComboBox.setFont(dialogFont);
        // <<< AGGIUNTA RENDERER >>>
        employeeComboBox.setRenderer(new EmployeeInfoComboBoxRenderer());
        gbc.gridx = 1; gbc.gridy = 0; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0; add(employeeComboBox, gbc);

        // 2. Data Settimana
        JLabel weekDateLabel = new JLabel("Data nella settimana:"); weekDateLabel.setFont(dialogFont);
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.0; add(weekDateLabel, gbc);
        weekDateField = new JTextField(10); weekDateField.setFont(dialogFont); weekDateField.setToolTipText("GG/MM/AAAA"); weekDateField.setText(LocalDate.now().format(DATE_INPUT_FORMATTER));
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0; add(weekDateField, gbc);

        // 3. Pulsanti
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        generateButton = new JButton("Genera Report Settimanale"); generateButton.setFont(dialogFont); generateButton.addActionListener(this); generateButton.setEnabled(false);
        cancelButton = new JButton("Annulla"); cancelButton.setFont(dialogFont); cancelButton.addActionListener(this);
        buttonPanel.add(generateButton); buttonPanel.add(cancelButton);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 0.0;
        add(buttonPanel, gbc);

        // Popola ComboBox dipendenti
        populateEmployeeComboBoxInternal();

        // Finalizza
        pack(); setResizable(false); setLocationRelativeTo(owner); setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    // --- Metodo populateEmployeeComboBoxInternal (MODIFICATO per EmployeeInfo) ---
    private void populateEmployeeComboBoxInternal() {
        System.out.println("[DEBUG WeeklyReportDialog] Esecuzione populateEmployeeComboBoxInternal...");
        if (employeeComboBox == null || generateButton == null || /*employeeNameToIdMap == null ||*/ shiftDAO == null) { System.err.println("ERRORE INTERNO: Componenti o DAO null!"); return; }

        Object previouslySelected = employeeComboBox.getSelectedItem(); // Salva selezione
        employeeComboBox.removeAllItems();
        // employeeNameToIdMap.clear(); // Rimosso
        boolean success = false;
        try {
            Map<Integer, ShiftDAO.EmployeeInfo> employees = shiftDAO.getAllEmployees();
            if (employees != null && !employees.isEmpty()) {
                List<ShiftDAO.EmployeeInfo> sortedInfos = new ArrayList<>(employees.values());
                sortedInfos.sort(Comparator.comparing(ShiftDAO.EmployeeInfo::fullName));

                employeeComboBox.addItem(SELECT_PLACEHOLDER); // Aggiungi placeholder
                for (ShiftDAO.EmployeeInfo info : sortedInfos) {
                    employeeComboBox.addItem(info); // Aggiungi oggetto
                    // employeeNameToIdMap.put(info.fullName(), info.id()); // Rimosso
                }
                success = true;
            } else {
                employeeComboBox.addItem(new EmployeeInfo(-2, "Nessun dipendente trovato", ""));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Errore caricamento dipendenti: " + e.getMessage(), "Errore Database", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(); employeeComboBox.addItem(new EmployeeInfo(-3, "Errore caricamento", ""));
        } finally {
            employeeComboBox.setEnabled(success);
            generateButton.setEnabled(success);
            // Ripristina selezione
            if (previouslySelected != null) {
                employeeComboBox.setSelectedItem(previouslySelected);
                if (employeeComboBox.getSelectedItem() != previouslySelected) employeeComboBox.setSelectedIndex(0);
            } else { employeeComboBox.setSelectedIndex(0); }
            System.out.println("[DEBUG WeeklyReportDialog] Bottone 'Genera' abilitato: " + success);
        }
    }
    // --- Fine populateEmployeeComboBoxInternal ---


    // --- Metodo actionPerformed (MODIFICATO per ottenere ID da EmployeeInfo) ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == generateButton) {
            System.out.println("[DEBUG WeeklyReportDialog] Bottone 'Genera Report Settimanale' premuto.");

            // --- Recupero e Validazione Parametri ---
            // <<< MODIFICA: Ottieni oggetto selezionato >>>
            Object selectedItem = employeeComboBox.getSelectedItem();
            Integer selectedEmployeeId = null;
            String selectedEmployeeName = "";

            // Verifica che sia un EmployeeInfo valido e non il placeholder
            if (selectedItem instanceof ShiftDAO.EmployeeInfo info && info.id() >= 0) {
                 selectedEmployeeId = info.id();
                 selectedEmployeeName = info.fullName();
            } else {
                JOptionPane.showMessageDialog(this, "Seleziona un dipendente valido.", "Selezione Mancante", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // <<< FINE MODIFICA >>>

            String weekDateStr = weekDateField.getText().trim();
            if (weekDateStr.isEmpty()) { JOptionPane.showMessageDialog(this, "Inserisci una data valida (GG/MM/AAAA).", "Input Mancante", JOptionPane.WARNING_MESSAGE); return; }

            LocalDate dateInWeek;
            try { dateInWeek = LocalDate.parse(weekDateStr, DATE_INPUT_FORMATTER); }
            catch (DateTimeParseException ex) { JOptionPane.showMessageDialog(this, "Formato data non valido. Usa GG/MM/AAAA.", "Input Non Valido", JOptionPane.WARNING_MESSAGE); return; }

            // Integer selectedEmployeeId = employeeNameToIdMap.get(selectedEmployeeName); // <<< RIMOSSO USO MAPPA >>>
            if (selectedEmployeeId == null) { JOptionPane.showMessageDialog(this, "ID dipendente non trovato.", "Errore Interno", JOptionPane.ERROR_MESSAGE); return; } // Controllo ridondante ma sicuro

            // Calcola inizio e fine settimana ISO
            LocalDateTime startOfWeek = dateInWeek.with(DayOfWeek.MONDAY).atStartOfDay();
            LocalDateTime endOfWeek = dateInWeek.with(DayOfWeek.SUNDAY).atTime(LocalTime.MAX);
            System.out.println("[DEBUG WeeklyReportDialog] Calcolato periodo: " + startOfWeek + " -> " + endOfWeek);

            dispose(); // Chiudi questo dialogo

            Window ownerWindow = SwingUtilities.getWindowAncestor(this.getOwner());
            Frame ownerFrame = (ownerWindow instanceof Frame) ? (Frame) ownerWindow : null;

            // Crea e mostra il dialogo di VISUALIZZAZIONE
            try {
                WeeklyReportDisplayDialog displayDialog = new WeeklyReportDisplayDialog(
                        ownerFrame, this.shiftDAO, selectedEmployeeId, startOfWeek, endOfWeek, selectedEmployeeName
                );
                displayDialog.setVisible(true);
            } catch (Exception displayEx) { /* ... gestione errore ... */ }

        } else if (e.getSource() == cancelButton) {
            System.out.println("[DEBUG WeeklyReportDialog] Bottone 'Annulla' premuto.");
            dispose();
        }
    } // --- Fine actionPerformed ---

    // --- CLASSE INTERNA RENDERER (Aggiunta) ---
    private class EmployeeInfoComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ShiftDAO.EmployeeInfo info) {
                String displayText;
                if (info.id() < 0) { // Placeholder o Errore
                     displayText = info.fullName();
                } else if (info.qualifica() != null && !info.qualifica().isEmpty()) {
                    displayText = String.format("%s - %s", info.qualifica(), info.fullName());
                } else {
                    displayText = info.fullName();
                }
                setText(displayText);
            } else if (value != null) { setText(value.toString()); }
              else { setText(""); } // Gestisce valore nullo iniziale
            return this;
        }
    } // --- Fine Renderer ---

} // --- Fine classe WeeklyReportDialog ---