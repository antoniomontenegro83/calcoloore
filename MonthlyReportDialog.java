package calcoloore.gui;

// --- Import Necessari ---
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.time.Year;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // Necessario per ordinare EmployeeInfo
import java.util.List;

import calcoloore.ShiftDAO;
import calcoloore.ShiftDAO.EmployeeInfo; // <<< IMPORT AGGIUNTO >>>
import calcoloore.gui.MonthlyReportDisplayDialog;
// --- Fine Import ---


/**
 * Finestra di dialogo modale per selezionare i parametri (dipendente, mese, anno)
 * per la generazione del report mensile.
 * Aggiornata per usare EmployeeInfo nella ComboBox.
 */
public class MonthlyReportDialog extends JDialog implements ActionListener {

    // --- Componenti UI ---
    // <<< MODIFICA TIPO >>>
    private JComboBox<EmployeeInfo> employeeComboBox;
    private JComboBox<String> monthComboBox;
    private JTextField yearField;
    private JButton generateButton;
    private JButton cancelButton;

    // --- Dati e Logica ---
    private final ShiftDAO shiftDAO;
    // private Map<String, Integer> employeeNameToIdMap; // <<< NON PIU' NECESSARIO >>>

    // Placeholder per la combo
    private static final EmployeeInfo SELECT_PLACEHOLDER = new EmployeeInfo(-1, "--- Seleziona Dipendente ---", "");

    // --- Costruttore ---
    public MonthlyReportDialog(JFrame owner, ShiftDAO dao) {
        super(owner, "Seleziona Parametri Report Mensile", true);
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

        // 2. Mese (invariato)
        JLabel monthLabel = new JLabel("Mese:"); monthLabel.setFont(dialogFont);
        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.0; add(monthLabel, gbc);
        String[] monthNames = new String[12];
        for (int i = 0; i < 12; i++) { monthNames[i] = java.time.Month.of(i + 1).getDisplayName(TextStyle.FULL, Locale.ITALIAN); }
        monthComboBox = new JComboBox<>(monthNames); monthComboBox.setFont(dialogFont);
        monthComboBox.setSelectedItem(java.time.LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, Locale.ITALIAN));
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0; add(monthComboBox, gbc);

        // 3. Anno (invariato)
        JLabel yearLabel = new JLabel("Anno:"); yearLabel.setFont(dialogFont);
        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.0; add(yearLabel, gbc);
        yearField = new JTextField(5); yearField.setFont(dialogFont);
        yearField.setText(String.valueOf(Year.now().getValue()));
        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST; gbc.weightx = 1.0; add(yearField, gbc);

        // 4. Pulsanti (invariato)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        generateButton = new JButton("Genera Report"); generateButton.setFont(dialogFont); generateButton.addActionListener(this); generateButton.setEnabled(false);
        cancelButton = new JButton("Annulla"); cancelButton.setFont(dialogFont); cancelButton.addActionListener(this);
        buttonPanel.add(generateButton); buttonPanel.add(cancelButton);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER; gbc.weightx = 0.0;
        add(buttonPanel, gbc);

        // Popola ComboBox dipendenti
        populateEmployeeComboBoxInternal();

        // Finalizza
        pack(); setResizable(false); setLocationRelativeTo(owner); setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    // --- Metodo populateEmployeeComboBoxInternal (MODIFICATO per EmployeeInfo) ---
    private void populateEmployeeComboBoxInternal() {
        System.out.println("[DEBUG MonthlyReportDialog] Esecuzione populateEmployeeComboBoxInternal...");
        if (employeeComboBox == null || generateButton == null || /*...*/ shiftDAO == null) { System.err.println("ERRORE INTERNO: Componenti o DAO null!"); return; }

        Object previouslySelected = employeeComboBox.getSelectedItem();
        employeeComboBox.removeAllItems();
        // employeeNameToIdMap.clear(); // Rimosso
        boolean success = false;
        try {
            Map<Integer, ShiftDAO.EmployeeInfo> employees = shiftDAO.getAllEmployees();
            if (employees != null && !employees.isEmpty()) {
                List<ShiftDAO.EmployeeInfo> sortedInfos = new ArrayList<>(employees.values());
                sortedInfos.sort(Comparator.comparing(ShiftDAO.EmployeeInfo::fullName));

                employeeComboBox.addItem(SELECT_PLACEHOLDER); // Placeholder
                for (ShiftDAO.EmployeeInfo info : sortedInfos) {
                    employeeComboBox.addItem(info); // Aggiungi oggetto
                }
                success = true;
            } else {
                employeeComboBox.addItem(new EmployeeInfo(-2, "Nessun dipendente trovato", ""));
            }
        } catch (SQLException e) { /* ... gestione errore ... */ }
        finally {
            employeeComboBox.setEnabled(success);
            generateButton.setEnabled(success);
            // Ripristina selezione
            if (previouslySelected != null) { employeeComboBox.setSelectedItem(previouslySelected); if (employeeComboBox.getSelectedItem() != previouslySelected) employeeComboBox.setSelectedIndex(0); }
            else { employeeComboBox.setSelectedIndex(0); }
            System.out.println("[DEBUG MonthlyReportDialog] Bottone 'Genera' abilitato: " + success);
        }
    }
    // --- Fine populateEmployeeComboBoxInternal ---


    // --- Metodo actionPerformed (MODIFICATO per ottenere ID da EmployeeInfo) ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == generateButton) {
            System.out.println("[DEBUG MonthlyReportDialog] Bottone 'Genera Report' premuto.");

            // --- Recupero e Validazione Parametri ---
            // <<< MODIFICA: Ottieni oggetto selezionato >>>
            Object selectedItem = employeeComboBox.getSelectedItem();
            Integer selectedEmployeeId = null;
            String selectedEmployeeName = "";
            if (selectedItem instanceof ShiftDAO.EmployeeInfo info && info.id() >= 0) {
                 selectedEmployeeId = info.id();
                 selectedEmployeeName = info.fullName();
            } else { JOptionPane.showMessageDialog(this, "Seleziona un dipendente valido.", "Selezione Mancante", JOptionPane.WARNING_MESSAGE); return; }
            // <<< FINE MODIFICA >>>

            String selectedMonthName = (String) monthComboBox.getSelectedItem();
            String yearText = yearField.getText().trim();

            // Validazione anno (invariata)
            if (yearText.isEmpty()) { /*...*/ return; }
            int selectedYear;
            try { selectedYear = Integer.parseInt(yearText); if (selectedYear < 1900 || selectedYear > Year.now().getValue() + 10) throw new NumberFormatException("Anno fuori range."); }
            catch (NumberFormatException ex) { /*...*/ return; }
            // Trova numero mese (invariato)
            int selectedMonth = -1;
            for (java.time.Month m : java.time.Month.values()) { if (m.getDisplayName(TextStyle.FULL, Locale.ITALIAN).equals(selectedMonthName)) { selectedMonth = m.getValue(); break; } }
            if (selectedMonth == -1) { /*...*/ return; }
            // Integer selectedEmployeeId = employeeNameToIdMap.get(selectedEmployeeName); // <<< RIMOSSO USO MAPPA >>>
            if (selectedEmployeeId == null) { JOptionPane.showMessageDialog(this, "ID dipendente non trovato.", "Errore Interno", JOptionPane.ERROR_MESSAGE); return; } // Controllo sicurezza

            // --- Avvia il dialogo di visualizzazione ---
            dispose();
            Window ownerWindow = SwingUtilities.getWindowAncestor(this.getOwner());
            Frame ownerFrame = (ownerWindow instanceof Frame) ? (Frame) ownerWindow : null;
             try {
                 MonthlyReportDisplayDialog displayDialog = new MonthlyReportDisplayDialog(
                         ownerFrame, this.shiftDAO, selectedEmployeeId, selectedYear, selectedMonth, selectedEmployeeName
                 );
                 displayDialog.setVisible(true);
             } catch (Exception displayEx) { /* ... gestione errore ... */ }

        } else if (e.getSource() == cancelButton) {
            System.out.println("[DEBUG MonthlyReportDialog] Bottone 'Annulla' premuto.");
            dispose();
        }
    } // --- Fine actionPerformed ---

    // --- CLASSE INTERNA RENDERER (Aggiunta) ---
    // Identica a quella usata per WeeklyReportDialog
    private class EmployeeInfoComboBoxRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof ShiftDAO.EmployeeInfo info) {
                String displayText;
                if (info.id() < 0) { displayText = info.fullName(); } // Placeholder
                else if (info.qualifica() != null && !info.qualifica().isEmpty()) { displayText = String.format("%s - %s", info.qualifica(), info.fullName()); }
                else { displayText = info.fullName(); }
                setText(displayText);
            } else if (value != null) { setText(value.toString()); }
              else { setText(""); }
            return this;
        }
    } // --- Fine Renderer ---

} // --- Fine classe MonthlyReportDialog ---