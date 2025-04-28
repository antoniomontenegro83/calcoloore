package calcoloore.gui;

// --- Import Necessari ---
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.sql.SQLException; // Per potenziali eccezioni dal Generator/DAO
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.time.LocalDate; // Per stampa header/footer
import java.time.format.FormatStyle; // Per stampa header/footer

// Import classi del progetto
import calcoloore.WeeklyReportGenerator; // Il generator che fa il lavoro
import calcoloore.ShiftDAO; // Passato al generator

/**
 * Finestra di dialogo modale per visualizzare il report settimanale generato.
 * Versione finale con worker corretto che delega al WeeklyReportGenerator.
 */
public class WeeklyReportDisplayDialog extends JDialog implements ActionListener {

    // --- Componenti UI ---
    private JTable reportTable;
    private DefaultTableModel reportTableModel;
    private JScrollPane reportScrollPane;
    private JTextArea statusArea;
    private JButton printButton;
    private JButton closeButton;

    // --- Dati e Logica ---
    private final WeeklyReportGenerator reportGenerator; // Generator per la logica del report
    private final Integer employeeId;
    private final LocalDateTime startOfWeek;
    private final LocalDateTime endOfWeek;
    private final String employeeName;
    private final String reportTitle; // Titolo per stampa e finestra

    // Formatter per il titolo (static)
    private static final DateTimeFormatter TITLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // --- Renderer Tabella (Interno) ---
    private class TotalRowRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            boolean isTotalRow = (row == table.getModel().getRowCount() - 1 && table.getModel().getRowCount() > 0);

            // Stile riga totali
            if (isTotalRow) {
                 c.setBackground(Color.YELLOW);
                 c.setFont(c.getFont().deriveFont(Font.BOLD));
            } else { // Stile righe normali
                 if (isSelected) {
                      c.setBackground(table.getSelectionBackground());
                      c.setForeground(table.getSelectionForeground());
                 } else {
                      c.setBackground(table.getBackground());
                      c.setForeground(table.getForeground());
                 }
                 c.setFont(c.getFont().deriveFont(Font.PLAIN));
            }
            // Allineamento colonne ore (presunte 4-7) - DA VERIFICARE SE LE COLONNE SONO CORRETTE
            if (column >= 4 && column < table.getColumnCount()) { // Usa getColumnCount per sicurezza
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (value instanceof String) {
                    try { Double.parseDouble(((String)value).replace(",",".")); }
                    catch (NumberFormatException | NullPointerException e) {
                         if (column == 4 && isTotalRow) { // Assumendo etichetta "TOTALE" in colonna 4
                            setHorizontalAlignment(SwingConstants.LEFT);
                         } else if (!isTotalRow) {
                            setHorizontalAlignment(SwingConstants.LEFT);
                         }
                         // Lascia a destra i totali numerici
                    }
                } else { // Allinea a destra numeri o null
                     setHorizontalAlignment(SwingConstants.RIGHT);
                }
            } else { // Altre colonne a sinistra
                setHorizontalAlignment(SwingConstants.LEFT);
            }
           return c;
       }
    } // --- Fine TotalRowRenderer ---


    // --- SwingWorker CORRETTO con Costruttore - Delega a WeeklyReportGenerator ---
    private class ReportWorker extends SwingWorker<List<Object[]>, Void> {
        private final Integer workerEmployeeId;
        private final LocalDateTime workerStartOfWeek;
        private final LocalDateTime workerEndOfWeek;

        // Costruttore che riceve e memorizza i parametri
         public ReportWorker(Integer employeeId, LocalDateTime startOfWeek, LocalDateTime endOfWeek) {
              this.workerEmployeeId = employeeId;
              this.workerStartOfWeek = startOfWeek;
              this.workerEndOfWeek = endOfWeek;
              System.out.println("[DEBUG ReportWorker] Worker Sett. creato per ID: " + employeeId + ", Start: " + startOfWeek + ", End: " + endOfWeek);
         }

        @Override
        protected List<Object[]> doInBackground() throws Exception {
            System.out.println("[DEBUG ReportWorker] doInBackground - Chiamo WeeklyReportGenerator.generateWeeklyReportData...");
            if (WeeklyReportDisplayDialog.this.reportGenerator == null) {
                 throw new IllegalStateException("WeeklyReportGenerator non inizializzato nel dialogo!");
            }
            // Chiamata diretta al metodo del Generator che fa tutto
            List<Object[]> reportData = WeeklyReportDisplayDialog.this.reportGenerator.generateWeeklyReportData(
                this.workerEmployeeId,
                this.workerStartOfWeek,
                this.workerEndOfWeek
            );
            System.out.println("[DEBUG ReportWorker] Generator ha restituito " + (reportData == null ? "null" : reportData.size() + " righe."));
            return reportData; // Ritorna la lista pronta per la tabella
        }

        // done() gestisce il risultato o le eccezioni e aggiorna la GUI
        @Override
        protected void done() {
             System.out.println("[DEBUG ReportWorker] done() avviato...");
            try {
                List<Object[]> reportData = get(); // Ottiene List<Object[]> da doInBackground
                reportTableModel.setRowCount(0); // Pulisce tabella

                if (reportData == null || reportData.isEmpty()) {
                    statusArea.setText("Nessun turno trovato per " + WeeklyReportDisplayDialog.this.employeeName + " nella settimana.");
                    printButton.setEnabled(false); // Non abilitare stampa senza dati
                    System.out.println("[DEBUG ReportWorker] Nessun dato trovato restituito dal generator.");
                } else {
                    System.out.println("[DEBUG ReportWorker] Dati trovati: " + reportData.size() + " righe. Popolo tabella...");
                    for (Object[] rowData : reportData) {
                        reportTableModel.addRow(rowData); // Aggiunge riga alla tabella
                    }
                    statusArea.setText("Report settimanale per " + WeeklyReportDisplayDialog.this.employeeName + " generato.");
                    printButton.setEnabled(true); // Abilita stampa
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                statusArea.setText("Generazione report interrotta."); reportTableModel.setRowCount(0); printButton.setEnabled(false);
                System.err.println("ReportWorker Sett. (Dialogo) interrotto: " + e.getMessage());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                statusArea.setText("Errore durante la generazione del report: " + cause.getMessage()); reportTableModel.setRowCount(0); printButton.setEnabled(false);
                JOptionPane.showMessageDialog(WeeklyReportDisplayDialog.this, "Errore generazione report sett.:\n" + cause.getMessage(), "Errore Report", JOptionPane.ERROR_MESSAGE);
                System.err.println("Errore Esecuzione Worker:"); cause.printStackTrace();
            } catch (Exception e) {
                 statusArea.setText("Errore imprevisto durante finalizzazione worker: " + e.getMessage()); reportTableModel.setRowCount(0); printButton.setEnabled(false);
                 JOptionPane.showMessageDialog(WeeklyReportDisplayDialog.this, "Errore imprevisto:\n" + e.getMessage(), "Errore GUI", JOptionPane.ERROR_MESSAGE);
                 System.err.println("Errore generico in done():"); e.printStackTrace();
            } finally {
                setCursor(Cursor.getDefaultCursor()); // Ripristina cursore
                closeButton.setEnabled(true); // Abilita SEMPRE il bottone Chiudi
                System.out.println("[DEBUG ReportWorker] Done() completato, closeButton abilitato.");
            }
        } // Fine done()
    } // --- Fine Classe ReportWorker ---


    /**
     * Costruttore del dialogo di visualizzazione report settimanale.
     */
    public WeeklyReportDisplayDialog(Frame owner, ShiftDAO shiftDAO, Integer employeeId,
                                     LocalDateTime startOfWeek, LocalDateTime endOfWeek, String employeeName) {
        // Chiamata a super() con titolo dinamico
        super(owner,
              "Report Sett. - " + employeeName + " ("
                  + startOfWeek.format(TITLE_DATE_FORMATTER) + " - "
                  + endOfWeek.format(TITLE_DATE_FORMATTER) + ")",
              true); // Modale

        // Inizializza campi final DOPO super()
        this.reportTitle = getTitle();
        if (shiftDAO == null) throw new IllegalArgumentException("ShiftDAO non può essere null.");
        // !!! ASSICURATI CHE WeeklyReportGenerator ESISTA E SIA IMPORTABILE !!!
        this.reportGenerator = new WeeklyReportGenerator(shiftDAO); // Crea generator
        this.employeeId = employeeId;
        this.startOfWeek = startOfWeek;
        this.endOfWeek = endOfWeek;
        this.employeeName = employeeName;

        // --- Setup Finestra e Layout ---
        setSize(800, 550);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(5, 5));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        // --- Creazione Componenti GUI ---
        Font mainFont = new Font("SansSerif", Font.PLAIN, 12);
        Font buttonFont = mainFont.deriveFont(Font.BOLD, 14f);

        // Area Stato (NORTH)
        statusArea = new JTextArea(2, 60); statusArea.setFont(mainFont); statusArea.setEditable(false); statusArea.setLineWrap(true); statusArea.setWrapStyleWord(true); JScrollPane statusScrollPane = new JScrollPane(statusArea); add(statusScrollPane, BorderLayout.NORTH);

        // Tabella Report (CENTER)
         // !!! ADATTA NOMI COLONNE ALL'OUTPUT REALE DEL TUO GENERATOR !!!
        String[] columnNames = { "Data Ing.", "Ora Ing.", "Data Usc.", "Ora Usc.", "Feriale", "Notturno/Fest.", "Festivo Diu.", "Tot. Turno" };
        reportTableModel = new DefaultTableModel(columnNames, 0) { @Override public boolean isCellEditable(int r, int c){return false;} };
        reportTable = new JTable(reportTableModel);
        reportTable.setFont(mainFont); reportTable.getTableHeader().setFont(mainFont.deriveFont(Font.BOLD)); reportTable.setAutoCreateRowSorter(true); reportTable.setDefaultRenderer(Object.class, new TotalRowRenderer()); reportTable.setFillsViewportHeight(true);
        reportTable.setShowGrid(false); reportTable.setIntercellSpacing(new Dimension(0, 0));
        // Imposta larghezze colonne (ESEMPIO - ADATTA)
        TableColumnModel columnModel = reportTable.getColumnModel(); columnModel.getColumn(0).setPreferredWidth(90); columnModel.getColumn(1).setPreferredWidth(60); columnModel.getColumn(2).setPreferredWidth(90); columnModel.getColumn(3).setPreferredWidth(60); columnModel.getColumn(4).setPreferredWidth(70); columnModel.getColumn(5).setPreferredWidth(100); columnModel.getColumn(6).setPreferredWidth(90); columnModel.getColumn(7).setPreferredWidth(80);
        reportScrollPane = new JScrollPane(reportTable);
        add(reportScrollPane, BorderLayout.CENTER);

        // Pannello Sud con Pulsanti (SOUTH)
        printButton = new JButton("Stampa Report");
        printButton.setFont(buttonFont);
        printButton.addActionListener(this); // Registra listener
        printButton.setEnabled(false);       // Inizia disabilitato

        closeButton = new JButton("Chiudi");
        closeButton.setFont(buttonFont);
        closeButton.addActionListener(this); // Registra listener
        closeButton.setEnabled(false);       // Inizia disabilitato

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(printButton);
        southPanel.add(closeButton);
        add(southPanel, BorderLayout.SOUTH);

        // Avvia generazione dati in background
        startReportGeneration();
    }

    // --- Metodo startReportGeneration CORRETTO ---
    // Avvia lo SwingWorker passando i parametri necessari al suo costruttore
    private void startReportGeneration() {
        statusArea.setText("Generazione report settimanale in corso...");
        reportTableModel.setRowCount(0);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        printButton.setEnabled(false); // Disabilita Stampa
        closeButton.setEnabled(false); // Disabilita Chiudi durante generazione

        // *** Passa i parametri al costruttore del worker corretto ***
        ReportWorker worker = new ReportWorker(this.employeeId, this.startOfWeek, this.endOfWeek);
        worker.execute();
        System.out.println("[DEBUG WeeklyReportDisplayDialog] ReportWorker eseguito."); // Debug
    }

    // --- Metodo actionPerformed (Logica bottoni OK) ---
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == printButton) {
            System.out.println("[DEBUG] Stampa premuto"); // Debug base
            MessageFormat header = new MessageFormat(reportTitle); // Titolo finestra come header
            MessageFormat footer = new MessageFormat("Pagina {0,number,integer}"); // Numero pagina footer
            try {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                // Stampa la tabella
                boolean complete = reportTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
                setCursor(Cursor.getDefaultCursor());
                // Mostra messaggio solo se la stampa NON è stata annullata
                if (complete) {
                    JOptionPane.showMessageDialog(this, "Stampa inviata alla stampante.", "Stampa", JOptionPane.INFORMATION_MESSAGE);
                } else {
                     System.out.println("Stampa annullata dall'utente.");
                }
            } catch (PrinterException pe) {
                setCursor(Cursor.getDefaultCursor());
                JOptionPane.showMessageDialog(this, "Errore durante la stampa:\n" + pe.getMessage() + "\nVerificare la configurazione della stampante.", "Errore Stampa", JOptionPane.ERROR_MESSAGE);
                pe.printStackTrace();
            } catch(Exception ex) {
                 setCursor(Cursor.getDefaultCursor());
                 JOptionPane.showMessageDialog(this, "Errore imprevisto durante la stampa:\n" + ex.getMessage(), "Errore Stampa", JOptionPane.ERROR_MESSAGE);
                 ex.printStackTrace();
            }
        } else if (e.getSource() == closeButton) {
            System.out.println("[DEBUG] Chiudi premuto"); // Debug base
            dispose(); // Chiude questo JDialog
        }
    } // --- Fine actionPerformed ---

} // --- Fine classe WeeklyReportDisplayDialog ---