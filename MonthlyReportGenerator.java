package calcoloore;

import java.sql.SQLException;
import java.time.LocalDateTime; // Import necessario
import java.time.format.DateTimeFormatter; // Import necessario
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Comparator; // Mantenuto se si vuole riattivare l'ordinamento qui

import calcoloore.Shift;
import calcoloore.ShiftDAO;
import calcoloore.WorkHoursCalculator; // Necessario per il calcolo ore

/**
 * Classe responsabile per recuperare i turni e generare i dati
 * aggregati e formattati per il report mensile.
 */
public class MonthlyReportGenerator {

    private ShiftDAO shiftDAO;

    // Formatters usati per l'output nella tabella (gli stessi di TimeEntryGUI)
    private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TABLE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public MonthlyReportGenerator(ShiftDAO shiftDAO) {
        if (shiftDAO == null) {
             throw new IllegalArgumentException("ShiftDAO non può essere nullo.");
        }
        this.shiftDAO = shiftDAO;
    }

    /**
     * Genera i dati formattati per il report mensile di un dipendente.
     * Include le righe per ogni turno e una riga finale con i totali.
     *
     * @param employeeId L'ID del dipendente.
     * @param year L'anno del report.
     * @param month Il mese del report (1-12).
     * @return Una lista di array Object, dove ogni array rappresenta una riga della tabella report.
     * Restituisce una lista vuota se non ci sono turni o in caso di errore lieve.
     * Puo' lanciare SQLException o IllegalArgumentException per errori gravi.
     * @throws IllegalArgumentException Se i parametri non sono validi.
     * @throws SQLException Se si verifica un errore grave durante il recupero dal database.
     */
    public List<Object[]> generateMonthlyReportData(Integer employeeId, int year, int month) throws IllegalArgumentException, SQLException {

        List<Object[]> reportData = new ArrayList<>();

        // Recupera i turni grezzi dal DAO (validazione parametri già fatta nel DAO)
        List<Shift> shifts = shiftDAO.getShiftsForEmployeeAndMonth(employeeId, year, month);

        // Se non ci sono turni, restituisce una lista vuota (la GUI gestirà il messaggio)
        if (shifts == null || shifts.isEmpty()) {
            return reportData; // Lista vuota
        }

        // Ordinamento opzionale (il DAO dovrebbe già ordinarli)
        // shifts.sort(Comparator.comparing(Shift::getEntryTime));

        // Variabili per i totali mensili
        double totalFerialeMensile = 0, totalNotturnoFestivoMensile = 0, totalFestivoMensile = 0, totalComplessivoMensile = 0;

        // Elabora ogni turno
        for (Shift shift : shifts) {
            // Calcola le ore dettagliate per il turno corrente
            WorkHoursCalculator.ReportOreDettagliato oreTurno = WorkHoursCalculator.calculateHours(shift.getEntryTime(), shift.getExitTime());

            // Aggrega le ore come richiesto dalla tabella report
            double ferialePerTabella = oreTurno.getFerialDayHours();
            double notturnoFestivoPerTabella = oreTurno.getFerialNightHours() + oreTurno.getFestiveNightHours(); // Notturno (feriale+festivo)
            double festivoPerTabella = oreTurno.getFestiveDayHours(); // Solo festivo diurno
            double totaleTurno = oreTurno.getTotalHours();

            // Formatta i dati per la riga della tabella
            Object[] rowData = new Object[8];
            rowData[0] = shift.getEntryTime().format(TABLE_DATE_FORMATTER); // Data Ingresso
            rowData[1] = shift.getEntryTime().format(TABLE_TIME_FORMATTER); // Ora Ingresso
            rowData[2] = shift.getExitTime().format(TABLE_DATE_FORMATTER);  // Data Uscita
            rowData[3] = shift.getExitTime().format(TABLE_TIME_FORMATTER);  // Ora Uscita
            rowData[4] = String.format(Locale.ITALY, "%.2f", ferialePerTabella);           // Feriale
            rowData[5] = String.format(Locale.ITALY, "%.2f", notturnoFestivoPerTabella);  // Notturno/Festivo
            rowData[6] = String.format(Locale.ITALY, "%.2f", festivoPerTabella);           // Festivo (diurno)
            rowData[7] = String.format(Locale.ITALY, "%.2f", totaleTurno);                 // Totale Turno
            reportData.add(rowData); // Aggiunge la riga alla lista

            // Aggiorna i totali mensili
            totalFerialeMensile += ferialePerTabella;
            totalNotturnoFestivoMensile += notturnoFestivoPerTabella;
            totalFestivoMensile += festivoPerTabella;
            totalComplessivoMensile += totaleTurno;
        } // Fine ciclo for sui turni

        // Aggiunge la riga dei totali alla fine della lista
        Object[] totalRowData = new Object[8];
        totalRowData[0] = "TOTALE MESE:"; totalRowData[1] = ""; totalRowData[2] = ""; totalRowData[3] = "";
        totalRowData[4] = String.format(Locale.ITALY, "%.2f", totalFerialeMensile);
        totalRowData[5] = String.format(Locale.ITALY, "%.2f", totalNotturnoFestivoMensile);
        totalRowData[6] = String.format(Locale.ITALY, "%.2f", totalFestivoMensile);
        totalRowData[7] = String.format(Locale.ITALY, "%.2f", totalComplessivoMensile);
        reportData.add(totalRowData); // Aggiunge la riga totali

        return reportData; // Restituisce la lista di righe pronte per la tabella
    }

    // Il vecchio metodo getShiftsForMonth può essere rimosso se non serve più
    /*
    public List<Shift> getShiftsForMonth(Integer employeeId, int year, int month) throws IllegalArgumentException, SQLException {
        // ... vecchia implementazione ...
        List<Shift> shifts = shiftDAO.getShiftsForEmployeeAndMonth(employeeId, year, month);
        if (shifts != null && !shifts.isEmpty()) {
           shifts.sort(Comparator.comparing(Shift::getEntryTime));
        }
        return shifts;
    }
    */
}