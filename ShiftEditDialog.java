package calcoloore.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.sql.SQLException;

import calcoloore.ShiftDAO;

/**
 * Finestra di dialogo per modificare un turno esistente.
 * Modificata: Aggiunto debug prints in actionPerformed. Rimosso focus painting. Testo Chiudi.
 */
public class ShiftEditDialog extends JDialog implements ActionListener {

    private JTextField entryTimeField;
    private JTextField exitTimeField;
    private JButton saveButton;
    private JButton cancelButton; // Tasto chiudi

    private int shiftId;
    private int employeeId;
    private ShiftDAO shiftDAO;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ShiftEditDialog(JDialog owner, int shiftId, int employeeId, LocalDateTime currentEntryTime, LocalDateTime currentExitTime, ShiftDAO shiftDAO) {
        super(owner, "Modifica Turno", true);

        // Controllo DAO
        if (shiftDAO == null) {
            System.err.println("[DEBUG ShiftEditDialog] ERRORE CRITICO: shiftDAO ricevuto è NULL nel costruttore!");
            JOptionPane.showMessageDialog(owner, "Errore interno: DAO non disponibile.", "Errore", JOptionPane.ERROR_MESSAGE);
            // Potrebbe essere meglio impedire l'apertura
        } else {
             System.out.println("[DEBUG ShiftEditDialog] shiftDAO ricevuto correttamente nel costruttore.");
        }
        this.shiftId = shiftId;
        this.employeeId = employeeId;
        this.shiftDAO = shiftDAO;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Ingresso
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("Nuovo Ingresso (GG/MM/AAAA HH:MM):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        entryTimeField = new JTextField(formatter.format(currentEntryTime), 15);
        add(entryTimeField, gbc);

        // Uscita
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("Nuova Uscita (GG/MM/AAAA HH:MM):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        exitTimeField = new JTextField(formatter.format(currentExitTime), 15);
        add(exitTimeField, gbc);

        // Pulsanti
        JPanel buttonPanel = new JPanel(new FlowLayout());
        saveButton = new JButton("Salva Modifiche");
        saveButton.setFocusPainted(false);
        saveButton.addActionListener(this);

        cancelButton = new JButton("Chiudi");
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(this);

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
        add(buttonPanel, gbc);

        pack();
        setLocationRelativeTo(owner);
    }

    // actionPerformed con DEBUG Prints
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        System.out.println("[DEBUG ShiftEditDialog] actionPerformed: source=" + source.getClass().getName()); // DEBUG

        if (source == saveButton) {
            System.out.println("[DEBUG ShiftEditDialog] 'Salva Modifiche' premuto."); // DEBUG
            String entryTimeStr = entryTimeField.getText().trim();
            String exitTimeStr = exitTimeField.getText().trim();
            System.out.println("[DEBUG ShiftEditDialog] Input: Ingresso='" + entryTimeStr + "', Uscita='" + exitTimeStr + "'"); // DEBUG

            if (entryTimeStr.isEmpty() || exitTimeStr.isEmpty()) {
                 System.out.println("[DEBUG ShiftEditDialog] Errore: Campi vuoti."); // DEBUG
                 JOptionPane.showMessageDialog(this, "Compila entrambi i campi data/ora.", "Input Mancante", JOptionPane.WARNING_MESSAGE);
                 return;
            }

             if (shiftDAO == null) { // Controllo aggiunto per sicurezza
                 System.err.println("[DEBUG ShiftEditDialog] ERRORE: shiftDAO è null quando si tenta di salvare!");
                 JOptionPane.showMessageDialog(this, "Errore interno: DAO non disponibile.", "Errore", JOptionPane.ERROR_MESSAGE);
                 return;
             }

            try {
                 System.out.println("[DEBUG ShiftEditDialog] Inizio blocco try..."); // DEBUG
                LocalDateTime newEntryTime = LocalDateTime.parse(entryTimeStr, formatter);
                LocalDateTime newExitTime = LocalDateTime.parse(exitTimeStr, formatter);
                 System.out.println("[DEBUG ShiftEditDialog] Parsing date OK."); // DEBUG

                if (newExitTime.isBefore(newEntryTime) || newExitTime.isEqual(newEntryTime)) {
                     System.out.println("[DEBUG ShiftEditDialog] Errore: Uscita non successiva a Ingresso."); // DEBUG
                     JOptionPane.showMessageDialog(this, "L'orario di uscita deve essere successivo a quello di ingresso.", "Errore Validazione", JOptionPane.WARNING_MESSAGE);
                     return;
                }
                 System.out.println("[DEBUG ShiftEditDialog] Validazione date OK."); // DEBUG
                 System.out.println("[DEBUG ShiftEditDialog] Chiamo shiftDAO.updateShift con ID Turno: " + this.shiftId + ", ID Dip: " + this.employeeId); // DEBUG

                // --- Chiamata al DAO ---
                shiftDAO.updateShift(this.shiftId, this.employeeId, newEntryTime, newExitTime);
                // ----------------------

                 System.out.println("[DEBUG ShiftEditDialog] Chiamata a updateShift completata."); // DEBUG
                JOptionPane.showMessageDialog(this, "Turno aggiornato con successo.", "Successo", JOptionPane.INFORMATION_MESSAGE);
                 System.out.println("[DEBUG ShiftEditDialog] Chiamo dispose() dopo salvataggio..."); // DEBUG
                dispose(); // Chiudi la finestra dopo il successo

            } catch (DateTimeParseException ex) {
                 System.err.println("[DEBUG ShiftEditDialog] Errore DateTimeParseException: " + ex.getMessage()); // DEBUG ERRORE
                JOptionPane.showMessageDialog(this, "Formato data/ora non valido.\nUsa GG/MM/AAAA HH:MM", "Errore Formato Data", JOptionPane.WARNING_MESSAGE);
            } catch (IllegalArgumentException ex) {
                 System.err.println("[DEBUG ShiftEditDialog] Errore IllegalArgumentException: " + ex.getMessage()); // DEBUG ERRORE
                 ex.printStackTrace(); // Stampa lo stack trace per più dettagli
                 JOptionPane.showMessageDialog(this, "Errore di validazione: " + ex.getMessage(), "Input Non Valido", JOptionPane.WARNING_MESSAGE);
            } catch (SQLException ex) {
                 System.err.println("[DEBUG ShiftEditDialog] Errore SQLException: " + ex.getMessage()); // DEBUG ERRORE
                 ex.printStackTrace(); // Stampa lo stack trace per più dettagli
                 JOptionPane.showMessageDialog(this, "Errore durante l'aggiornamento del turno: " + ex.getMessage(), "Errore Database", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) { // Catch generico per sicurezza
                 System.err.println("[DEBUG ShiftEditDialog] Errore generico Exception: " + ex.getMessage()); // DEBUG ERRORE
                 ex.printStackTrace(); // Stampa lo stack trace per più dettagli
                 JOptionPane.showMessageDialog(this, "Errore generico durante l'aggiornamento: " + ex.getMessage(), "Errore", JOptionPane.ERROR_MESSAGE);
            }
             System.out.println("[DEBUG ShiftEditDialog] Fine gestione pulsante Salva."); // DEBUG
        }
        else if (source == cancelButton) { // Pulsante "Chiudi"
             System.out.println("[DEBUG ShiftEditDialog] 'Chiudi' premuto. Chiamo dispose()..."); // DEBUG
            dispose(); // Chiudi senza salvare
        }
    }
}