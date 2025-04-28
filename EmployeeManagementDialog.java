package calcoloore.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;
import java.util.regex.PatternSyntaxException;

// Importa il DAO e il record EmployeeInfo interno al DAO
import calcoloore.ShiftDAO;
import calcoloore.ShiftDAO.EmployeeInfo;
// Importa TimeEntryGUI se necessario per il tipo parentGUI (anche se può essere null)
import calcoloore.gui.TimeEntryGUI;


/**
 * Finestra di dialogo per gestire i dipendenti.
 * Versione COMPLETA e CORRETTA con JTable, Ricerca, Add, Edit, Delete.
 * Con bottone separato "Modifica Qualifica" e "Modifica Dati" solo per nome e cognome.
 */
public class EmployeeManagementDialog extends JDialog implements ActionListener {

    // --- Campi UI ---
    private JTextField firstNameField;
    private JTextField lastNameField;
    private JTextField qualificaField;
    private JButton addButton;
    private JButton deleteButton;
    private JButton editButton; // Bottone "Modifica Dati" (modifica solo C, N)
    private JButton editQualificaButton; // Bottone "Modifica Qualifica"
    private JTable employeeTable;
    private DefaultTableModel employeeTableModel;
    private JButton closeButton;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;

    // --- Riferimenti ---
    private final ShiftDAO shiftDAO;
    private final TimeEntryGUI parentGUI;

    // --- Costruttore ---
    public EmployeeManagementDialog(Window owner, ShiftDAO shiftDAO, TimeEntryGUI parentGUI) {
        super(owner, "Gestione Dipendenti", Dialog.ModalityType.APPLICATION_MODAL);
        this.shiftDAO = shiftDAO;
        this.parentGUI = parentGUI;
        if (this.shiftDAO == null) {
            System.err.println("ERRORE CRITICO: DAO nullo passato a EmployeeManagementDialog!");
            JOptionPane.showMessageDialog(owner, "Errore interno: DAO non disponibile.", "Errore", JOptionPane.ERROR_MESSAGE);
            // Considera di lanciare eccezione o chiudere subito
            // throw new IllegalArgumentException("ShiftDAO non può essere nullo.");
            // Per ora, continuiamo ma la GUI sarà probabilmente inutilizzabile
        }

        // --- Inizializzazione UI ---
        Font contentFont = new Font("SansSerif", Font.PLAIN, 13);
        setPreferredSize(new Dimension(650, 550));
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // --- Pannello Input (NORTH) ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Aggiungi Nuovo Dipendente"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); gbc.anchor = GridBagConstraints.WEST;
        // Qualifica
        JLabel qualificaLabel = new JLabel("Qualifica:"); qualificaLabel.setFont(contentFont);
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(qualificaLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; qualificaField = new JTextField(20); qualificaField.setFont(contentFont); inputPanel.add(qualificaField, gbc);
        // Cognome
        JLabel lastNameLabel = new JLabel("Cognome:"); lastNameLabel.setFont(contentFont);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(lastNameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; lastNameField = new JTextField(20); lastNameField.setFont(contentFont); inputPanel.add(lastNameField, gbc);
        // Nome
        JLabel firstNameLabel = new JLabel("Nome:"); firstNameLabel.setFont(contentFont);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST; inputPanel.add(firstNameLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST; firstNameField = new JTextField(20); firstNameField.setFont(contentFont); inputPanel.add(firstNameField, gbc);
        // Pulsante Aggiungi
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        addButton = new JButton("Aggiungi Dipendente"); addButton.setFont(contentFont); addButton.addActionListener(this); inputPanel.add(addButton, gbc);
        gbc.gridwidth = 1;
        add(inputPanel, BorderLayout.NORTH);

        // --- Pannello Elenco Dipendenti (CENTER) ---
        JPanel listPanel = new JPanel(new BorderLayout(0, 5));
        listPanel.setBorder(BorderFactory.createTitledBorder("Elenco Dipendenti"));

        // Pannello Ricerca
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel searchLabel = new JLabel("Cerca:"); searchLabel.setFont(contentFont);
        searchPanel.add(searchLabel);
        searchField = new JTextField(30); // Campo ricerca
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterTable(); }
        });
        searchPanel.add(searchField);
        listPanel.add(searchPanel, BorderLayout.NORTH);

        // Creazione Modello Tabella
        String[] columnNames = {"ID", "Qualifica", "Cognome", "Nome"};
        employeeTableModel = new DefaultTableModel(columnNames, 0) {
             @Override public boolean isCellEditable(int row, int column) { return false; }
             @Override public Class<?> getColumnClass(int columnIndex) {
                 switch (columnIndex) {
                     case 0: return Integer.class; case 1: return String.class;
                     case 2: return String.class; case 3: return String.class;
                     default: return Object.class;
                 }
             }
        };

        // Creazione JTable
        employeeTable = new JTable(employeeTableModel);
        employeeTable.setFont(contentFont);
        employeeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        employeeTable.setFillsViewportHeight(true);
        employeeTable.getTableHeader().setFont(contentFont.deriveFont(Font.BOLD));

        // Creazione e impostazione Sorter
        sorter = new TableRowSorter<>(employeeTableModel);
        employeeTable.setRowSorter(sorter);

        // Nascondi colonna ID e imposta larghezze
        TableColumnModel tcm = employeeTable.getColumnModel();
        TableColumn idColumn = tcm.getColumn(0);
        idColumn.setMinWidth(0); idColumn.setMaxWidth(0); idColumn.setPreferredWidth(0); idColumn.setResizable(false);
        tcm.getColumn(1).setPreferredWidth(150); // Qualifica
        tcm.getColumn(2).setPreferredWidth(150); // Cognome
        tcm.getColumn(3).setPreferredWidth(150); // Nome

        JScrollPane listScrollPane = new JScrollPane(employeeTable);
        listPanel.add(listScrollPane, BorderLayout.CENTER); // Tabella al centro

        // Pulsanti Azione Lista (Con pulsante "Modifica Qualifica" e "Modifica Dati" per nome e cognome)
        JPanel actionListPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        
        // Pulsante Modifica Qualifica
        editQualificaButton = new JButton("Modifica Qualifica");
        editQualificaButton.setFont(contentFont);
        editQualificaButton.setToolTipText("Modifica solo la Qualifica del dipendente selezionato");
        editQualificaButton.addActionListener(this);
        actionListPanel.add(editQualificaButton);
        
        // Pulsante Modifica Dati (solo nome e cognome)
        editButton = new JButton("Modifica Dati");
        editButton.setFont(contentFont);
        editButton.setToolTipText("Modifica Cognome e Nome del dipendente selezionato");
        editButton.addActionListener(this);
        actionListPanel.add(editButton);
        
        // Pulsante Elimina
        deleteButton = new JButton("Elimina Selezionato");
        deleteButton.setFont(contentFont);
        deleteButton.addActionListener(this);
        actionListPanel.add(deleteButton);
        
        listPanel.add(actionListPanel, BorderLayout.SOUTH); // Bottoni sotto la tabella
        add(listPanel, BorderLayout.CENTER); // Pannello lista al centro del dialogo

        // Pannello Pulsante Chiudi (SOUTH)
        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        closeButton = new JButton("Chiudi"); closeButton.setFont(contentFont);
        closeButton.addActionListener(this); closePanel.add(closeButton);
        add(closePanel, BorderLayout.SOUTH);

        // Carica dati iniziali nella tabella
        populateEmployeeTable();

        // Finalizza
        pack();
        setMinimumSize(new Dimension(600, 500));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        System.out.println("[DEBUG EmpMgmt Constr] Fine Costruttore.");
    }

    // --- Metodo per Filtrare la Tabella ---
    private void filterTable() {
        String text = searchField.getText();
        System.out.println("[DEBUG EmpMgmt Filter] filterTable() con testo: '" + text + "'"); // Debug
        if (sorter == null) { return; } // Sicurezza
        if (text.trim().length() == 0) {
            sorter.setRowFilter(null); // Rimuovi filtro
        } else {
            try {
                // Filtra su Qualifica (1), Cognome (2), Nome (3) - case-insensitive
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 1, 2, 3));
            } catch (PatternSyntaxException pse) {
                System.err.println("Errore regex filtro: " + pse);
                sorter.setRowFilter(null); // In caso di errore, non filtrare
            }
        }
    }
    // --- Fine Metodo Filtro ---

    // --- Metodo populateEmployeeTable (Senza debug eccessivo) ---
    private void populateEmployeeTable() {
        System.out.println("[DEBUG EmpMgmt] Esecuzione populateEmployeeTable()..."); // Log inizio
        if (employeeTableModel == null) { System.err.println("ERRORE: Modello tabella nullo!"); return; }

        RowFilter<? super DefaultTableModel, ? super Integer> currentFilter = null;
        if (sorter != null) { currentFilter = sorter.getRowFilter(); sorter.setRowFilter(null); } // Salva/Rimuovi filtro

        employeeTableModel.setRowCount(0); // Svuota tabella
        boolean dataLoaded = false;

        try {
            if (this.shiftDAO != null) {
                Map<Integer, EmployeeInfo> employeesMap = this.shiftDAO.getAllEmployees();
                if (employeesMap != null && !employeesMap.isEmpty()) {
                    List<EmployeeInfo> sortedList = new ArrayList<>(employeesMap.values());
                    sortedList.sort(Comparator.comparing(EmployeeInfo::fullName));
                    for (EmployeeInfo info : sortedList) {
                        String cognome = ""; String nome = ""; String fullName = info.fullName();
                        if (fullName == null) fullName = "";
                        int firstSpace = fullName.indexOf(' ');
                        if (firstSpace != -1) { cognome = fullName.substring(0, firstSpace); nome = fullName.substring(firstSpace + 1); }
                        else { cognome = fullName; }
                        employeeTableModel.addRow(new Object[]{info.id(), info.qualifica(), cognome, nome});
                    }
                    if (employeeTableModel.getRowCount() > 0) dataLoaded = true;
                } else { System.out.println("[DEBUG EmpMgmt] Nessun dipendente restituito dal DAO."); }
            } else { System.err.println("[DEBUG EmpMgmt] DAO è null in populateEmployeeTable!"); }
        } catch (SQLException e) {
             JOptionPane.showMessageDialog(this,"Errore caricamento dipendenti: "+e.getMessage(),"Errore DB",JOptionPane.ERROR_MESSAGE); e.printStackTrace();
        } catch (Exception ex) {
             JOptionPane.showMessageDialog(this,"Errore imprevisto caricamento: "+ex.getMessage(),"Errore App",JOptionPane.ERROR_MESSAGE); ex.printStackTrace();
        } finally {
            enableActionButtons(dataLoaded); // Abilita/disabilita bottoni azione
            if (sorter != null) { try { sorter.setRowFilter(currentFilter); } catch (Exception filterEx) { System.err.println("Errore ripristino filtro: "+ filterEx.getMessage());} } // Ripristina filtro
        }
        System.out.println("[DEBUG EmpMgmt] Fine populateEmployeeTable(). Righe: " + employeeTableModel.getRowCount()); // Log fine
    }
    // --- Fine populateEmployeeTable ---


    // Helper abilitazione bottoni azione
    private void enableActionButtons(boolean enable) {
         if (deleteButton != null) deleteButton.setEnabled(enable);
         if (editButton != null) editButton.setEnabled(enable);
         if (editQualificaButton != null) editQualificaButton.setEnabled(enable);
    }

    // Notifica GUI parente
    private void notifyParentGUI() { if (this.parentGUI != null) { this.parentGUI.refreshEmployeeCombobox(); } }

    // --- Gestore Eventi actionPerformed COMPLETO ---
    @Override
    public void actionPerformed(ActionEvent e) {
         Object source = e.getSource();
         
        if (this.shiftDAO == null && (source == addButton || source == deleteButton || source == editButton || source == editQualificaButton)) {
            JOptionPane.showMessageDialog(this, "DAO non inizializzato.", "Errore", JOptionPane.ERROR_MESSAGE); return;
        }

        if (source == addButton) {
            // --- Logica Aggiungi Dipendente ---
             String firstName = firstNameField.getText().trim(); String lastName = lastNameField.getText().trim(); String qualifica = qualificaField.getText().trim();
             if (firstName.isEmpty() || lastName.isEmpty()) { JOptionPane.showMessageDialog(this, "Cognome e Nome obbligatori.", "Input Mancante", JOptionPane.WARNING_MESSAGE); return; }
             try {
                 this.shiftDAO.addEmployee(firstName, lastName, qualifica.isEmpty() ? null : qualifica);
                 JOptionPane.showMessageDialog(this, "Dipendente aggiunto.", "Successo", JOptionPane.INFORMATION_MESSAGE);
                 firstNameField.setText(""); lastNameField.setText(""); qualificaField.setText("");
                 populateEmployeeTable(); notifyParentGUI();
             } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Errore DB aggiunta: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE); ex.printStackTrace();
             } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Errore generico aggiunta: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE); ex.printStackTrace(); }

        } else if (source == deleteButton) {
            // --- Logica Elimina Dipendente ---
             int selectedRow = employeeTable.getSelectedRow(); if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Seleziona dalla tabella.", "Selezione Mancante", JOptionPane.WARNING_MESSAGE); return; }
             int modelRow = employeeTable.convertRowIndexToModel(selectedRow); Integer employeeId = (Integer) employeeTableModel.getValueAt(modelRow, 0); String qualifica = (String) employeeTableModel.getValueAt(modelRow, 1); String cognome = (String) employeeTableModel.getValueAt(modelRow, 2); String nome = (String) employeeTableModel.getValueAt(modelRow, 3); String nomeCompleto = (qualifica != null && !qualifica.isEmpty() ? qualifica + " - " : "") + cognome + " " + nome;
             if (employeeId == null) { JOptionPane.showMessageDialog(this, "ID non valido.", "Errore", JOptionPane.ERROR_MESSAGE); return; }
             int confirm = JOptionPane.showConfirmDialog(this, "Eliminare '" + nomeCompleto + "' e i suoi turni?", "Conferma", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
             if (confirm == JOptionPane.YES_OPTION) { try { this.shiftDAO.deleteEmployee(employeeId); JOptionPane.showMessageDialog(this, "Dipendente eliminato.", "Successo", JOptionPane.INFORMATION_MESSAGE); populateEmployeeTable(); notifyParentGUI(); } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Errore DB eliminazione: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE); ex.printStackTrace(); } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Errore generico eliminazione: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE); ex.printStackTrace(); } }

        } else if (source == editButton) { // Modifica solo nome e cognome
            // --- Logica Modifica Dati (solo nome e cognome) ---
             int selectedRow = employeeTable.getSelectedRow(); 
             if (selectedRow == -1) { 
                 JOptionPane.showMessageDialog(this, "Seleziona dalla tabella.", "Selezione Mancante", JOptionPane.WARNING_MESSAGE); 
                 return; 
             }
             
             int modelRow = employeeTable.convertRowIndexToModel(selectedRow); 
             Integer currentEmployeeId = (Integer) employeeTableModel.getValueAt(modelRow, 0); 
             String currentQualifica = (String) employeeTableModel.getValueAt(modelRow, 1); 
             String currentLastName = (String) employeeTableModel.getValueAt(modelRow, 2); 
             String currentFirstName = (String) employeeTableModel.getValueAt(modelRow, 3); 
             String currentFullName = currentLastName + " " + currentFirstName;
             
             if (currentEmployeeId == null) { 
                 JOptionPane.showMessageDialog(this, "ID non valido.", "Errore", JOptionPane.ERROR_MESSAGE); 
                 return; 
             }
             
             try {
                 // Richiedi solo cognome e nome (non la qualifica)
                 String newLastName = JOptionPane.showInputDialog(this, "Nuovo cognome:", currentLastName); 
                 if (newLastName == null) return; 
                 newLastName = newLastName.trim(); 
                 if (newLastName.isEmpty()) { 
                     JOptionPane.showMessageDialog(this, "Cognome vuoto.", "Input Mancante", JOptionPane.WARNING_MESSAGE); 
                     return; 
                 }
                 
                 String newFirstName = JOptionPane.showInputDialog(this, "Nuovo nome:", currentFirstName); 
                 if (newFirstName == null) return; 
                 newFirstName = newFirstName.trim(); 
                 if (newFirstName.isEmpty()) { 
                     JOptionPane.showMessageDialog(this, "Nome vuoto.", "Input Mancante", JOptionPane.WARNING_MESSAGE); 
                     return; 
                 }
                 
                 // Chiama DAO per aggiornare mantenendo la stessa qualifica
                 this.shiftDAO.updateEmployee(currentEmployeeId, newFirstName, newLastName, currentQualifica);
                 
                 JOptionPane.showMessageDialog(this, "Nome e cognome aggiornati.", "Successo", JOptionPane.INFORMATION_MESSAGE);
                 populateEmployeeTable(); 
                 notifyParentGUI(); // Aggiorna UI
                 
             } catch (SQLException ex) { 
                 JOptionPane.showMessageDialog(this, "Errore DB aggiornamento: " + ex.getMessage(), "Errore DB", JOptionPane.ERROR_MESSAGE); 
                 ex.printStackTrace();
             } catch (IllegalArgumentException ex) { 
                 JOptionPane.showMessageDialog(this, "Errore input: " + ex.getMessage(), "Input Non Valido", JOptionPane.WARNING_MESSAGE); 
                 ex.printStackTrace();
             } catch (Exception ex) { 
                 JOptionPane.showMessageDialog(this, "Errore generico aggiornamento: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE); 
                 ex.printStackTrace(); 
             }

        } else if (source == editQualificaButton) { // Modifica Solo Qualifica
            // --- Logica Modifica Solo Qualifica ---
            int selectedRow = employeeTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Seleziona un dipendente dalla tabella.", 
                    "Selezione Mancante", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int modelRow = employeeTable.convertRowIndexToModel(selectedRow);
            Integer employeeId = (Integer) employeeTableModel.getValueAt(modelRow, 0);
            String currentQualifica = (String) employeeTableModel.getValueAt(modelRow, 1);
            String cognome = (String) employeeTableModel.getValueAt(modelRow, 2);
            String nome = (String) employeeTableModel.getValueAt(modelRow, 3);
            String nomeCompleto = cognome + " " + nome;
            
            if (employeeId == null) {
                JOptionPane.showMessageDialog(this, "ID dipendente non valido.", 
                    "Errore", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            try {
                // Chiedi la nuova qualifica
                String newQualifica = JOptionPane.showInputDialog(this, 
                    "Nuova qualifica per '" + nomeCompleto + "':", 
                    currentQualifica != null ? currentQualifica : "");
                
                // Se l'utente annulla, esci
                if (newQualifica == null) return;
                
                // Rimuovi spazi iniziali e finali
                newQualifica = newQualifica.trim();
                
                // Aggiorna solo la qualifica nel database
                // Riusa nome e cognome esistenti
                this.shiftDAO.updateEmployee(employeeId, nome, cognome, 
                    newQualifica.isEmpty() ? null : newQualifica);
                
                JOptionPane.showMessageDialog(this, 
                    "Qualifica aggiornata con successo.", 
                    "Aggiornamento Completato", JOptionPane.INFORMATION_MESSAGE);
                
                // Aggiorna la tabella e notifica la GUI principale
                populateEmployeeTable();
                notifyParentGUI();
                
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, 
                    "Errore database durante l'aggiornamento: " + ex.getMessage(), 
                    "Errore Database", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Errore durante l'aggiornamento: " + ex.getMessage(), 
                    "Errore", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }

        } else if (source == closeButton) {
            dispose();
        }
    } // Fine actionPerformed

} // Fine classe EmployeeManagementDialog