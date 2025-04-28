package calcoloore.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import calcoloore.ShiftDAO; // Importa il DAO
// Importa le altre classi Dialog necessarie
import calcoloore.gui.TimeEntryGUI;
import calcoloore.gui.ShiftManagementDialog; // Se esiste
import calcoloore.gui.EmployeeManagementDialog;
import calcoloore.gui.WeeklyReportDialog; // Dialogo selezione parametri sett.
import calcoloore.gui.MonthlyReportDialog; // Dialogo selezione parametri mens.
// Importa eventuali altre classi (es. per i report display, ma non servono qui)

/**
 * Nuova finestra principale stile Dashboard.
 * Versione corretta con gestione istanza TimeEntryGUI e chiamata corretta
 * per WeeklyReportDialog (selezione parametri).
 */
public class DashboardFrame extends JFrame implements ActionListener {

    // --- Campi UI ---
    private JButton manageShiftsButton;
    private JButton manageEmployeesButton;
    private JButton weeklyReportButton;
    private JButton monthlyReportButton;
    private JButton exitButton;
    private JButton openTimeEntryButton;

    // --- Campi Logica/Dati ---
    private final ShiftDAO shiftDAO; // DAO reso final dopo inizializzazione
    private TimeEntryGUI mainTimeEntryGUI; // Istanza unica della finestra inserimento turni

    // --- Costruttore ---
    public DashboardFrame() {
        super("Gestione Ore Lavoro - Dashboard");

        // --- Inizializza DAO ---
        ShiftDAO tempDAO = null;
        try {
            // Assicurati che DatabaseConfig sia nel classpath
            // Class.forName("database.DatabaseConfig"); // Potrebbe non essere necessario se il driver si registra da solo
            tempDAO = new ShiftDAO();
            System.out.println("[DEBUG Dashboard] ShiftDAO inizializzato.");
        } catch (Exception e) { // Cattura generica per sicurezza (es. SQLException, ClassNotFoundException)
            String errorMessage = "Errore critico durante inizializzazione DAO:\n" + e.getMessage() + "\nMolte funzionalità non saranno disponibili.";
            System.err.println(errorMessage);
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, errorMessage, "Errore Avvio DAO", JOptionPane.ERROR_MESSAGE);
        }
        this.shiftDAO = tempDAO; // Assegna al campo final (potrebbe essere null se fallisce)

        // --- Inizializza TimeEntryGUI (ma non mostrarla) ---
        try {
            // Crea l'istanza UNICA che verrà usata per tutta la sessione
            this.mainTimeEntryGUI = new TimeEntryGUI();
            this.mainTimeEntryGUI.setVisible(false); // Nascondi all'inizio
            System.out.println("[DEBUG Dashboard] TimeEntryGUI inizializzata.");
        } catch (Exception e) {
            String errorMessage = "Errore critico durante inizializzazione TimeEntryGUI:\n" + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, errorMessage, "Errore Avvio GUI", JOptionPane.ERROR_MESSAGE);
            // L'applicazione potrebbe continuare ma l'inserimento turni fallirà
            this.mainTimeEntryGUI = null;
        }

        // --- Setup Finestra ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(700, 500));
        setLayout(new BorderLayout(10, 10));

        // --- Pannello Titolo ---
        JLabel titleLabel = new JLabel("Dashboard Principale", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        // --- Pannello Pulsanti Centrali ---
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridBagLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Font buttonFont = new Font("SansSerif", Font.BOLD, 14);

        // Creazione e aggiunta bottoni
        openTimeEntryButton = createDashboardButton("Inserisci Turni Sessione", buttonFont, gbc, 0, 0, buttonPanel);
        manageShiftsButton = createDashboardButton("Gestisci Turni (Modifica/Ricerca)", buttonFont, gbc, 0, 1, buttonPanel);
        manageEmployeesButton = createDashboardButton("Gestisci Dipendenti", buttonFont, gbc, 0, 2, buttonPanel);
        weeklyReportButton = createDashboardButton("Report Settimanale", buttonFont, gbc, 0, 3, buttonPanel);
        monthlyReportButton = createDashboardButton("Report Mensile", buttonFont, gbc, 0, 4, buttonPanel);

        // Spacer per spingere i bottoni in alto (opzionale)
        gbc.gridy = 5; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        buttonPanel.add(new JPanel(), gbc);

        add(new JScrollPane(buttonPanel), BorderLayout.CENTER);

        // --- Pulsante Esci in basso ---
        exitButton = createExitButton("Esci dall'Applicazione", buttonFont);
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        southPanel.add(exitButton);
        add(southPanel, BorderLayout.SOUTH);

        // --- Finalizzazione ---
        pack();
        setLocationRelativeTo(null); // Centra
    }

    // Metodo helper per creare bottoni del dashboard
    private JButton createDashboardButton(String text, Font font, GridBagConstraints gbc, int gridx, int gridy, JPanel panel) {
        JButton button = new JButton(text);
        button.setFont(font);
        button.setFocusPainted(false); // Rimuovi focus visuale
        button.addActionListener(this); // Associa l'azione
        gbc.gridx = gridx;
        gbc.gridy = gridy;
        panel.add(button, gbc);
        return button;
    }
     // Metodo helper per bottone Esci (leggermente diverso, non usa GBC)
    private JButton createExitButton(String text, Font font) {
         JButton button = new JButton(text);
         button.setFont(font);
         button.setFocusPainted(false);
         button.addActionListener(this);
         return button;
    }


    // --- Gestione Azioni ---
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        // Controllo preliminare DAO per la maggior parte delle azioni
         if (this.shiftDAO == null && source != exitButton) {
             JOptionPane.showMessageDialog(this, "Operazione non possibile: DAO non inizializzato.", "Errore DAO", JOptionPane.ERROR_MESSAGE);
             return;
         }

        if (source == openTimeEntryButton) {
             System.out.println("[DEBUG Dashboard] Mostro TimeEntryGUI esistente...");
             if (this.mainTimeEntryGUI != null) {
                 this.mainTimeEntryGUI.setVisible(true); // Mostra l'istanza unica
                 this.mainTimeEntryGUI.setState(Frame.NORMAL); // Porta in primo piano se minimizzata
                 this.mainTimeEntryGUI.toFront();
             } else {
                 JOptionPane.showMessageDialog(this, "Errore: Istanza TimeEntryGUI non è stata creata correttamente all'avvio.", "Errore", JOptionPane.ERROR_MESSAGE);
             }

        } else if (source == manageShiftsButton) {
             System.out.println("[DEBUG Dashboard] Apro ShiftManagementDialog...");
             // Assumendo che ShiftManagementDialog esista e prenda questi parametri
             ShiftManagementDialog shiftDialog = new ShiftManagementDialog(this, this.shiftDAO);
             shiftDialog.setVisible(true);

        } else if (source == manageEmployeesButton) {
             System.out.println("[DEBUG Dashboard] Apro EmployeeManagementDialog...");
             // Verifica che TimeEntryGUI sia disponibile (necessario per il costruttore di EmployeeManagementDialog)
             if (this.mainTimeEntryGUI == null) {
                 JOptionPane.showMessageDialog(this, "Errore: Istanza TimeEntryGUI non disponibile per Gestione Dipendenti.", "Errore", JOptionPane.ERROR_MESSAGE);
                 return;
             }
             // Prepara variabili final per la lambda
             final TimeEntryGUI guiInstance = this.mainTimeEntryGUI;
             final ShiftDAO daoInstance = this.shiftDAO;
             final Window parentWin = this; // this è il DashboardFrame (JFrame -> Frame -> Window)

             SwingUtilities.invokeLater(() -> {
                 try {
                      EmployeeManagementDialog empDialog = new EmployeeManagementDialog(parentWin, daoInstance, guiInstance);
                      empDialog.setVisible(true);
                 } catch (Throwable t) {
                     System.err.println("[ERRORE Dashboard] Eccezione apertura EmployeeManagementDialog:");
                     t.printStackTrace();
                     JOptionPane.showMessageDialog(parentWin, "Errore imprevisto aprendo dialogo gestione dipendenti:\n" + t.getMessage(), "Errore Grave", JOptionPane.ERROR_MESSAGE);
                 }
             });

        // ===========================================================
        // *** BLOCCO CORRETTO PER REPORT SETTIMANALE ***
        // ===========================================================
        } else if (source == weeklyReportButton) {
            System.out.println("[DEBUG Dashboard] Click su Report Settimanale RILEVATO."); // DEBUG 1

            // DAO è già stato controllato all'inizio del metodo

            try {
                System.out.println("[DEBUG Dashboard] Sto per creare istanza di WeeklyReportDialog (SELEZIONE PARAMETRI)..."); // DEBUG 2
                // *** QUESTA è la riga chiave: DEVE creare WeeklyReportDialog ***
                WeeklyReportDialog paramDialog = new WeeklyReportDialog(this, this.shiftDAO);
                // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

                System.out.println("[DEBUG Dashboard] Istanza WeeklyReportDialog creata: " + (paramDialog != null)); // DEBUG 3

                System.out.println("[DEBUG Dashboard] Chiamo paramDialog.setVisible(true)..."); // DEBUG 4
                paramDialog.setVisible(true); // Mostra il dialogo di SELEZIONE PARAMETRI
                System.out.println("[DEBUG Dashboard] Chiamata a paramDialog.setVisible(true) completata."); // DEBUG 5

            } catch (Exception ex) {
                // Cattura QUALSIASI eccezione durante creazione o visualizzazione
                System.err.println("[ERRORE Dashboard] Eccezione durante creazione/visualizzazione di WeeklyReportDialog:"); // DEBUG 6
                ex.printStackTrace(); // Stampa lo stack trace completo dell'errore
                JOptionPane.showMessageDialog(this, "Errore imprevisto nell'apertura del dialogo selezione parametri sett.:\n" + ex.getMessage(), "Errore Applicazione", JOptionPane.ERROR_MESSAGE);
            }
            System.out.println("[DEBUG Dashboard] Gestione click Report Settimanale terminata."); // DEBUG 7
        // ===========================================================
        // *** FINE BLOCCO CORRETTO PER REPORT SETTIMANALE ***
        // ===========================================================

        } else if (source == monthlyReportButton) {
            System.out.println("[DEBUG Dashboard] Apro MonthlyReportDialog...");
            // Assumendo che MonthlyReportDialog esista e prenda questi parametri
            MonthlyReportDialog monthlyDialog = new MonthlyReportDialog(this, this.shiftDAO);
            monthlyDialog.setVisible(true);

        } else if (source == exitButton) {
            int confirm = JOptionPane.showConfirmDialog(this, "Sei sicuro di voler uscire?", "Conferma Uscita", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                System.out.println("[DEBUG Dashboard] Uscita dall'applicazione.");
                // Qui potresti voler chiudere risorse, es. connessione DB se fosse mantenuta aperta
                // if (this.shiftDAO != null) { this.shiftDAO.closeConnection(); } // Esempio
                System.exit(0); // Termina l'applicazione
            }
        } else {
             // Evento non gestito
             System.out.println("[WARN Dashboard] Evento non gestito per sorgente: " + e.getSource());
        }
    } // Fine actionPerformed


    // Metodo main per avviare la dashboard (come nel tuo codice)
    public static void main(String[] args) {
         try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
         catch (Exception e) { e.printStackTrace(); }

         SwingUtilities.invokeLater(() -> {
             DashboardFrame frame = new DashboardFrame();
             // Potremmo aggiungere un controllo sul DAO anche qui prima di mostrare
             // if (frame.shiftDAO == null) { /* Non mostrare? Uscire? */ } else
             frame.setVisible(true);
         });
    }

} // Fine classe DashboardFrame