package calcoloore.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.print.PrinterException;
import java.sql.SQLException; // Importato anche se il catch specifico potrebbe essere commentato
import java.text.MessageFormat;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
// Import per le classi di stampa usate nell'esempio
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

// Import classi necessarie
import calcoloore.MonthlyReportGenerator;
import calcoloore.ShiftDAO;

/**
 * Finestra di dialogo modale per visualizzare il report mensile generato.
 * Con funzionalità di stampa.
 * Versione pulita assumendo una sola definizione di classe.
 */
// La classe implementa ActionListener per i suoi pulsanti
public class MonthlyReportDisplayDialog extends JDialog implements ActionListener { // <<< ASSICURATI CI SIA SOLO UNA DEFINIZIONE DI CLASSE NEL FILE

    // --- Componenti GUI ---
    private JTable reportTable;
    private DefaultTableModel reportTableModel;
    private JScrollPane reportScrollPane;
    private JTextArea statusArea;
    private JButton printButton;
    private JButton closeButton;

    // --- Dati e logica ---
    private MonthlyReportGenerator reportGenerator;
    private Integer employeeId;
    private int year;
    private int month;
    private String employeeName;
    private String reportTitle;

    // --- Renderer Tabella (Interno) ---
    private class TotalRowRenderer extends DefaultTableCellRenderer {
         @Override
         public Component getTableCellRendererComponent(JTable table, Object value,
                                                        boolean isSelected, boolean hasFocus,
                                                        int row, int column) {
             Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
             boolean isTotalRow = (row == table.getModel().getRowCount() - 1 && table.getModel().getRowCount() > 0);

             // Stile per riga totali
             if (isTotalRow) {
                  c.setBackground(Color.YELLOW);
                  c.setFont(c.getFont().deriveFont(Font.BOLD));
             } else { // Stile per righe normali
                  if (isSelected) {
                       c.setBackground(table.getSelectionBackground());
                       c.setForeground(table.getSelectionForeground());
                  } else {
                       c.setBackground(table.getBackground());
                       c.setForeground(table.getForeground());
                  }
                  c.setFont(c.getFont().deriveFont(Font.PLAIN));
             }

             // Allineamento colonne ore (indici 4-7)
             if (column >= 4 && column <= 7) {
                  setHorizontalAlignment(SwingConstants.RIGHT);
                  // Gestione stringhe vs numeri per allineamento
                  if (value instanceof String) {
                       try {
                            Double.parseDouble(((String)value).replace(",","."));
                       } catch (NumberFormatException | NullPointerException e) {
                            if (!isTotalRow || column > 3) { // Non allineare a sx la label "TOTALE MESE" se è nella colonna giusta
                                 setHorizontalAlignment(SwingConstants.LEFT);
                            }
                       }
                  }
             } else { // Altre colonne a sinistra
                  setHorizontalAlignment(SwingConstants.LEFT);
             }
            return c;
        }
    } // Fine TotalRowRenderer

    // --- SwingWorker (Interno) ---
     private class ReportWorker extends SwingWorker<List<Object[]>, Void> {
        private final Integer workerEmployeeId;
        private final int workerYear;
        private final int workerMonth;

         public ReportWorker(Integer employeeId, int year, int month) {
              this.workerEmployeeId = employeeId;
              this.workerYear = year;
              this.workerMonth = month;
         }

        @Override
        protected List<Object[]> doInBackground() throws Exception {
            return MonthlyReportDisplayDialog.this.reportGenerator.generateMonthlyReportData(
                this.workerEmployeeId, this.workerYear, this.workerMonth
            );
        }

        @Override
        protected void done() {
            try {
                List<Object[]> reportData = get();
                reportTableModel.setRowCount(0);

                if (reportData == null || reportData.isEmpty()) {
                    statusArea.setText("Nessun turno trovato per " + MonthlyReportDisplayDialog.this.employeeName + " nel periodo selezionato.");
                    printButton.setEnabled(false);
                } else {
                    for (Object[] rowData : reportData) { reportTableModel.addRow(rowData); }
                    statusArea.setText("Report per " + MonthlyReportDisplayDialog.this.employeeName + " generato con successo.");
                    printButton.setEnabled(true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                statusArea.setText("Generazione report interrotta.");
                printButton.setEnabled(false);
                System.err.println("ReportWorker Interrotto: " + e.getMessage());
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                statusArea.setText("Errore durante la generazione del report: " + cause.getMessage());
                printButton.setEnabled(false);
                JOptionPane.showMessageDialog(MonthlyReportDisplayDialog.this, "Errore generazione report:\n" + cause.getMessage(), "Errore Report", JOptionPane.ERROR_MESSAGE);
                cause.printStackTrace();
            } catch (Exception e) {
                 statusArea.setText("Errore imprevisto: " + e.getMessage());
                 printButton.setEnabled(false);
                 JOptionPane.showMessageDialog(MonthlyReportDisplayDialog.this, "Errore imprevisto:\n" + e.getMessage(), "Errore GUI", JOptionPane.ERROR_MESSAGE);
                 e.printStackTrace();
            } finally {
                MonthlyReportDisplayDialog.this.setCursor(Cursor.getDefaultCursor());
                closeButton.setEnabled(true); // Abilita sempre chiudi alla fine
            }
        }
    } // Fine ReportWorker

    // --- Costruttore Principale ---
     public MonthlyReportDisplayDialog(Frame owner, ShiftDAO shiftDAO, Integer employeeId, int year, int month, String employeeName) {
        super(owner,
              "Report Mensile - " + employeeName + " (" + Month.of(month).getDisplayName(TextStyle.FULL, Locale.ITALIAN) + " " + year + ")",
              true);

        this.reportTitle = getTitle();
        if (shiftDAO == null) throw new IllegalArgumentException("ShiftDAO non può essere null.");
        this.reportGenerator = new MonthlyReportGenerator(shiftDAO);
        this.employeeId = employeeId;
        this.year = year;
        this.month = month;
        this.employeeName = employeeName;

        // Setup UI Base
        setSize(800, 550);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(5, 5));
        Font mainFont = new Font("SansSerif", Font.PLAIN, 12);
        Font buttonFont = mainFont.deriveFont(Font.BOLD, 14f);

        // Area Stato (NORTH)
        statusArea = new JTextArea(2, 60); statusArea.setFont(mainFont); statusArea.setEditable(false); statusArea.setLineWrap(true); statusArea.setWrapStyleWord(true); JScrollPane statusScrollPane = new JScrollPane(statusArea); add(statusScrollPane, BorderLayout.NORTH);

        // Tabella Report (CENTER)
        String[] columnNames = { "Data Ing.", "Ora Ing.", "Data Usc.", "Ora Usc.", "Feriale", "Notturno/Fest.", "Festivo Diu.", "Tot. Turno" };
        reportTableModel = new DefaultTableModel(columnNames, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        reportTable = new JTable(reportTableModel);
        reportTable.setFont(mainFont); reportTable.getTableHeader().setFont(mainFont.deriveFont(Font.BOLD)); reportTable.setAutoCreateRowSorter(true); reportTable.setDefaultRenderer(Object.class, new TotalRowRenderer()); reportTable.setFillsViewportHeight(true);
        reportTable.setShowGrid(false); reportTable.setIntercellSpacing(new Dimension(0, 0));
        TableColumnModel columnModel = reportTable.getColumnModel(); columnModel.getColumn(0).setPreferredWidth(90); columnModel.getColumn(1).setPreferredWidth(60); columnModel.getColumn(2).setPreferredWidth(90); columnModel.getColumn(3).setPreferredWidth(60); columnModel.getColumn(4).setPreferredWidth(70); columnModel.getColumn(5).setPreferredWidth(100); columnModel.getColumn(6).setPreferredWidth(90); columnModel.getColumn(7).setPreferredWidth(80);
        reportScrollPane = new JScrollPane(reportTable);
        add(reportScrollPane, BorderLayout.CENTER);

        // Pannello Bottoni (SOUTH)
        printButton = new JButton("Stampa Report");
        printButton.setFont(buttonFont);
        printButton.addActionListener(this); // Aggiunto listener
        printButton.setEnabled(false); // Inizia disabilitato

        closeButton = new JButton("Chiudi");
        closeButton.setFont(buttonFont);
        closeButton.addActionListener(this); // Aggiunto listener
        closeButton.setEnabled(false); // Inizia disabilitato

        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        southPanel.add(printButton);
        southPanel.add(closeButton);
        add(southPanel, BorderLayout.SOUTH);

        // Avvia generazione report
        startReportGeneration();
    }

    // Avvio SwingWorker
    private void startReportGeneration() {
        statusArea.setText("Generazione report in corso...");
        reportTableModel.setRowCount(0);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        printButton.setEnabled(false);
        closeButton.setEnabled(false);
        ReportWorker worker = new ReportWorker(this.employeeId, this.year, this.month);
        worker.execute();
    }

    // Gestione Azioni Bottoni (con debug prints rimossi per pulizia, ma logica ok)
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == printButton) {
            try {
                MessageFormat header = new MessageFormat(reportTitle);
                MessageFormat footer = new MessageFormat("Pagina {0,number,integer}");
                boolean complete = reportTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
                if (complete) { JOptionPane.showMessageDialog(this, "Stampa completata.", "Stampa", JOptionPane.INFORMATION_MESSAGE); }
                // else { JOptionPane.showMessageDialog(this, "Stampa annullata.", "Stampa", JOptionPane.WARNING_MESSAGE); } // Forse non necessario
            } catch (PrinterException pe) { JOptionPane.showMessageDialog(this, "Errore stampa:\n" + pe.getMessage(), "Errore Stampa", JOptionPane.ERROR_MESSAGE); pe.printStackTrace(); }
             catch (Exception ex) { JOptionPane.showMessageDialog(this, "Errore imprevisto stampa:\n" + ex.getMessage(), "Errore Stampa", JOptionPane.ERROR_MESSAGE); ex.printStackTrace(); }
        } else if (e.getSource() == closeButton) {
            dispose(); // Chiude il dialogo
        }
    } // Fine actionPerformed

} // <<< FINE CLASSE MonthlyReportDisplayDialog (assicurati sia l'unica nel file)