package calcoloore;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// Rimosso import Comparator se non usato
// Rimosso import Connection (non più necessario qui)
// Rimosso import PreparedStatement (non più necessario qui)
// Rimosso import ResultSet (non più necessario qui)


import calcoloore.Shift;
import calcoloore.ShiftDAO;
import calcoloore.WorkHoursCalculator;

/**
 * Classe responsabile per recuperare i turni e generare i dati
 * aggregati e formattati per il report settimanale.
 */
public class WeeklyReportGenerator {

    private ShiftDAO shiftDAO;

    // Formatters usati per l'output nella tabella
    private static final DateTimeFormatter TABLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TABLE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public WeeklyReportGenerator(ShiftDAO shiftDAO) {
        if (shiftDAO == null) {
             throw new IllegalArgumentException("ShiftDAO non può essere nullo.");
        }
        this.shiftDAO = shiftDAO;
    }

    /**
     * Genera i dati formattati per il report settimanale di un dipendente.
     * Include le righe per ogni turno e una riga finale con i totali settimanali.
     *
     * @param employeeId L'ID del dipendente.
     * @param startOfWeek Data/ora di inizio della settimana (inclusiva).
     * @param endOfWeek Data/ora di fine della settimana (inclusiva).
     * @return Una lista di array Object, dove ogni array rappresenta una riga della tabella report.
     * Restituisce una lista vuota se non ci sono turni.
     * @throws IllegalArgumentException Se i parametri non sono validi.
     * @throws SQLException Se si verifica un errore durante il recupero dal database.
     */
    public List<Object[]> generateWeeklyReportData(Integer employeeId, LocalDateTime startOfWeek, LocalDateTime endOfWeek)
            throws IllegalArgumentException, SQLException {

        List<Object[]> reportData = new ArrayList<>();

        // Validazione input base
        if (employeeId == null) {
            throw new IllegalArgumentException("ID Dipendente non può essere nullo.");
        }
        if (startOfWeek == null || endOfWeek == null || endOfWeek.isBefore(startOfWeek)) {
            throw new IllegalArgumentException("Date di inizio/fine settimana non valide.");
        }

        // --- RECUPERO TURNI DAL DAO REALE ---
        // Chiama il nuovo metodo implementato in ShiftDAO
        List<Shift> shifts = shiftDAO.getShiftsForEmployeeBetween(employeeId, startOfWeek, endOfWeek);
        // --- FINE RECUPERO TURNI ---


        // Se non ci sono turni, restituisce lista vuota
        if (shifts == null || shifts.isEmpty()) { // Aggiunto controllo null per sicurezza
            return reportData;
        }

        // Variabili per i totali settimanali
        double totalFerialeSett = 0, totalNotturnoFestivoSett = 0, totalFestivoSett = 0, totalComplessivoSett = 0;

        // Elabora ogni turno trovato
        for (Shift shift : shifts) {
            // Calcola le ore dettagliate per il turno
            WorkHoursCalculator.ReportOreDettagliato oreTurno = WorkHoursCalculator.calculateHours(shift.getEntryTime(), shift.getExitTime());

            // Aggrega le ore come richiesto dalla tabella
            double ferialePerTabella = oreTurno.getFerialDayHours();
            double notturnoFestivoPerTabella = oreTurno.getFerialNightHours() + oreTurno.getFestiveNightHours();
            double festivoPerTabella = oreTurno.getFestiveDayHours();
            double totaleTurno = oreTurno.getTotalHours();

            // Formatta i dati per la riga della tabella
            Object[] rowData = new Object[8];
            rowData[0] = shift.getEntryTime().format(TABLE_DATE_FORMATTER);
            rowData[1] = shift.getEntryTime().format(TABLE_TIME_FORMATTER);
            rowData[2] = shift.getExitTime().format(TABLE_DATE_FORMATTER);
            rowData[3] = shift.getExitTime().format(TABLE_TIME_FORMATTER);
            rowData[4] = String.format(Locale.ITALY, "%.2f", ferialePerTabella);
            rowData[5] = String.format(Locale.ITALY, "%.2f", notturnoFestivoPerTabella);
            rowData[6] = String.format(Locale.ITALY, "%.2f", festivoPerTabella);
            rowData[7] = String.format(Locale.ITALY, "%.2f", totaleTurno);
            reportData.add(rowData);

            // Aggiorna i totali settimanali
            totalFerialeSett += ferialePerTabella;
            totalNotturnoFestivoSett += notturnoFestivoPerTabella;
            totalFestivoSett += festivoPerTabella;
            totalComplessivoSett += totaleTurno;
        }

        // Aggiunge la riga dei totali settimanali (solo se ci sono stati turni)
        if (!reportData.isEmpty()) {
             Object[] totalRowData = new Object[8];
             totalRowData[0] = "TOTALE SETT:"; totalRowData[1] = ""; totalRowData[2] = ""; totalRowData[3] = "";
             totalRowData[4] = String.format(Locale.ITALY, "%.2f", totalFerialeSett);
             totalRowData[5] = String.format(Locale.ITALY, "%.2f", totalNotturnoFestivoSett);
             totalRowData[6] = String.format(Locale.ITALY, "%.2f", totalFestivoSett);
             totalRowData[7] = String.format(Locale.ITALY, "%.2f", totalComplessivoSett);
             reportData.add(totalRowData);
        }

        return reportData;
    }

    // --- METODO IPOTETICO RIMOSSO ---
    // private List<Shift> getShiftsForEmployeeBetween(...) { ... }

    // --- METODO getConnection AUSILIARIO RIMOSSO ---
    // private Connection getConnection() throws SQLException { ... }

}