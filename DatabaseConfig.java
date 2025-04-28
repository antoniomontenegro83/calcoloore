package database;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DatabaseConfig {

    private static final String PROPERTIES_FILE = "db.properties";
    private static final Properties properties = new Properties();

    static {
        // <<< DEBUG: Inizio blocco statico >>>
        System.out.println("[DEBUG] DatabaseConfig: Inizio blocco statico...");
        // <<< FINE DEBUG >>>

        try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {

            // <<< DEBUG: Tentativo caricamento properties >>>
            System.out.println("[DEBUG] DatabaseConfig: Tentativo caricamento file: " + PROPERTIES_FILE);
            // <<< FINE DEBUG >>>

            if (input == null) {
                // <<< DEBUG: File properties non trovato >>>
                System.err.println("[DEBUG] DatabaseConfig: ERRORE CRITICO - File properties '" + PROPERTIES_FILE + "' non trovato nel classpath.");
                // <<< FINE DEBUG >>>
                throw new FileNotFoundException("Impossibile trovare il file di configurazione '" + PROPERTIES_FILE + "' nel classpath.");
            }

            // Carica le proprietà dal file
            properties.load(input);
            // <<< DEBUG: Properties caricate >>>
            System.out.println("[DEBUG] DatabaseConfig: File properties caricato.");
            // <<< FINE DEBUG >>>


            // Carica il driver JDBC qui, una sola volta
            String driver = getDbDriver(); // Leggi prima il nome del driver
            // <<< DEBUG: Tentativo caricamento driver >>>
            System.out.println("[DEBUG] DatabaseConfig: Tentativo caricamento driver JDBC: " + driver);
            // <<< FINE DEBUG >>>
            try {
                Class.forName(driver);
                System.out.println("[DEBUG] DatabaseConfig: Driver JDBC caricato con successo: " + driver);
            } catch (ClassNotFoundException e) {
                System.err.println("[DEBUG] DatabaseConfig: ERRORE CRITICO - Driver JDBC non trovato: " + driver);
                // Stampa lo stack trace dell'eccezione originale
                e.printStackTrace(System.err);
                throw new RuntimeException("Driver JDBC non trovato: " + driver, e); // Rilancia come RuntimeException
            } catch (ExceptionInInitializerError e) {
                System.err.println("[DEBUG] DatabaseConfig: ERRORE CRITICO - Errore nell'inizializzazione del driver JDBC: " + driver);
                e.printStackTrace(System.err);
                throw e; // Rilancia l'errore originale
             } catch (Throwable t) { // Cattura qualsiasi altro errore durante il caricamento del driver
                 System.err.println("[DEBUG] DatabaseConfig: ERRORE CRITICO IMPREVISTO durante caricamento driver JDBC: " + driver);
                 t.printStackTrace(System.err);
                 throw new RuntimeException("Errore imprevisto caricamento driver: " + driver, t);
             }

        } catch (FileNotFoundException e) {
             System.err.println("[DEBUG] DatabaseConfig: Eccezione FileNotFoundException gestita.");
             System.err.println("Errore critico: " + e.getMessage());
             throw new RuntimeException(e); // Rilancia
        } catch (IOException ex) {
            System.err.println("[DEBUG] DatabaseConfig: Eccezione IOException gestita.");
            System.err.println("Errore durante la lettura del file di configurazione '" + PROPERTIES_FILE + "': " + ex.getMessage());
            ex.printStackTrace(System.err);
             throw new RuntimeException("Errore IO caricamento configurazione DB.", ex); // Rilancia
        } catch (Throwable t) { // Cattura qualsiasi altro errore nel blocco try-with-resources
            System.err.println("[DEBUG] DatabaseConfig: ERRORE CRITICO IMPREVISTO nel blocco statico.");
            t.printStackTrace(System.err);
            throw new RuntimeException("Errore imprevisto nel blocco statico DatabaseConfig.", t);
        }
        // <<< DEBUG: Fine blocco statico >>>
        System.out.println("[DEBUG] DatabaseConfig: Fine blocco statico.");
        // <<< FINE DEBUG >>>
    }

    // Metodi getter (invariati)
    public static String getDbUrl() { return properties.getProperty("db.url"); }
    public static String getDbUser() { return properties.getProperty("db.user"); }
    public static String getDbPassword() { return properties.getProperty("db.password"); }
    public static String getDbDriver() { return properties.getProperty("db.driver", ""); }

    // Metodo privato per prevenire l'istanziazione
    private DatabaseConfig() {}

    // Metodo main di test (invariato)
    public static void main(String[] args) { /* ... */ }
}