package calcoloore; // Package principale

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;

// Import necessari per le classi GUI e configurazione
import calcoloore.gui.WelcomeDialog;
import calcoloore.gui.LoginDialog;
import calcoloore.gui.DashboardFrame; // Importa la nuova finestra principale
// Rimuovi l'import di TimeEntryGUI se non viene più creata qui
// import calcoloore.gui.TimeEntryGUI;
import database.DatabaseConfig;


/**
 * Classe principale per l'avvio dell'applicazione Gestione Ore Lavoro.
 * Gestisce la sequenza iniziale: Config DB -> Welcome -> Login -> Dashboard.
 */
public class MainApplication {

    public static void main(String[] args) {
        // Imposta Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Impossibile impostare il Look and Feel di sistema.");
            // Non fatale, si può continuare con il L&F di default
        }

        // Esegui il resto sulla Event Dispatch Thread (EDT) di Swing
        SwingUtilities.invokeLater(() -> {
            // Carica la configurazione del database all'inizio
            try {
                // Questo forza l'esecuzione del blocco statico in DatabaseConfig
                Class.forName("database.DatabaseConfig");
                System.out.println("[DEBUG MainApp] DatabaseConfig caricata con successo.");
            } catch (ClassNotFoundException | RuntimeException e) {
                // Errore critico se la configurazione DB fallisce
                System.err.println("[DEBUG MainApp] Errore CRITICO durante il caricamento di DatabaseConfig:");
                e.printStackTrace(System.err);
                // Mostra un messaggio all'utente e termina
                JOptionPane.showMessageDialog(null, // Nessun parent frame disponibile qui
                    "Errore critico durante l'inizializzazione del database.\n" +
                    "Controllare il file db.properties, il driver JDBC e la console.\n\n" +
                    "Dettagli: " + e.getMessage(),
                    "Errore Avvio Applicazione",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Uscita forzata
                return; // Non necessario dopo System.exit(1), ma per chiarezza
            }

            // 1. Mostra il WelcomeDialog (modale)
            System.out.println("[DEBUG MainApp] Mostro WelcomeDialog...");
            WelcomeDialog welcomeDialog = new WelcomeDialog(null); // null perché non c'è ancora un Frame principale
            welcomeDialog.setVisible(true); // Blocca finché non viene chiuso
            System.out.println("[DEBUG MainApp] WelcomeDialog chiuso.");

            // 2. Mostra il LoginDialog (modale)
            System.out.println("[DEBUG MainApp] Creazione LoginDialog...");
            LoginDialog loginDialog = new LoginDialog(null); // null perché non c'è ancora un Frame principale
            loginDialog.setVisible(true); // Blocca finché non viene chiuso
            System.out.println("[DEBUG MainApp] LoginDialog chiuso. Successo? " + loginDialog.isLoginSuccessful());

            // 3. Controlla l'esito del login
            if (loginDialog.isLoginSuccessful()) {
                // 4. Se login OK, la Dashboard è già stata aperta da LoginDialog.
                // Non dobbiamo fare nulla qui per mostrarla.
                System.out.println("[DEBUG MainApp] Login OK. L'applicazione continua con la Dashboard.");
            } else {
                // 5. Se login fallito o annullato, termina l'applicazione
                System.out.println("[DEBUG MainApp] Login fallito o annullato. Uscita.");
                System.exit(0);
            }
        });
    }
}