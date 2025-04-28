package calcoloore;

import java.util.Locale; // <-- AGGIUNGI QUESTA RIGA
import java.time.DayOfWeek; // Per controllare il giorno della settimana
import java.time.LocalDate; // Aggiunto per rappresentare solo la data
import java.time.LocalDateTime; // Per gestire data e ora precise
import java.time.LocalTime; // Per gestire solo l'ora
import java.time.MonthDay; // Per gestire festività fisse (mese e giorno)
import java.time.temporal.ChronoUnit; // Per calcolare la differenza in minuti
import java.util.HashSet; // Per memorizzare le festività in un set
import java.util.Set; // Interfaccia per il set

public class WorkHoursCalculator {

    // Definizione delle fasce orarie DIURNE e NOTTURNE
    private static final LocalTime DAY_TIME_START = LocalTime.of(6, 0);
    private static final LocalTime DAY_TIME_END = LocalTime.of(22, 0);

    // Set di date festive FISSE (Giorno del mese e Mese) - INDIPENDENTI DALL'ANNO
    // Rinominato da FIXED_HOLIDAYS_2025 e rimossa Pasqua/Pasquetta
    private static final Set<MonthDay> FIXED_HOLIDAYS = new HashSet<>();
    static {
        FIXED_HOLIDAYS.add(MonthDay.of(1, 1));  // Capodanno
        FIXED_HOLIDAYS.add(MonthDay.of(1, 6));  // Epifania
        FIXED_HOLIDAYS.add(MonthDay.of(4, 25)); // Festa della Liberazione
        FIXED_HOLIDAYS.add(MonthDay.of(5, 1));  // Festa dei Lavoratori
        FIXED_HOLIDAYS.add(MonthDay.of(6, 2));  // Festa della Repubblica
        FIXED_HOLIDAYS.add(MonthDay.of(8, 15)); // Ferragosto
        FIXED_HOLIDAYS.add(MonthDay.of(11, 1)); // Ognissanti (Tutti i Santi)
        FIXED_HOLIDAYS.add(MonthDay.of(12, 8)); // Immacolata Concezione
        FIXED_HOLIDAYS.add(MonthDay.of(12, 25)); // Natale
        FIXED_HOLIDAYS.add(MonthDay.of(12, 26)); // Santo Stefano
    }

    /**
     * Calcola la data della Domenica di Pasqua per un dato anno usando un algoritmo comune.
     * Adattato da varie fonti online (es. algoritmo di Meeus/Jones/Butcher).
     * @param year L'anno per cui calcolare Pasqua (calendario Gregoriano, > 1582).
     * @return La LocalDate della Domenica di Pasqua.
     */
    public static LocalDate calculateEasterSunday(int year) {
        // Validazione semplice dell'anno
        if (year <= 1582) {
            throw new IllegalArgumentException("Algoritmo valido per il calendario Gregoriano (anno > 1582)");
        }

        // Algoritmo di Meeus/Jones/Butcher (comune per il calcolo di Pasqua Gregoriana)
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(year, month, day);
    }


    /**
     * Verifica se una data e ora specifica cade in un giorno festivo in Italia.
     * Considera Domeniche, festività fisse e Pasqua/Pasquetta calcolate dinamicamente.
     * @param dateTime La data e ora da controllare.
     * @return true se è festivo, false altrimenti.
     */
    public static boolean isHoliday(LocalDateTime dateTime) {
        LocalDate dateToCheck = dateTime.toLocalDate(); // Considera solo la data

        // Controlla se è Domenica
        if (dateToCheck.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return true;
        }

        // Controlla le festività fisse definite nel set
        MonthDay monthDay = MonthDay.from(dateToCheck);
        if (FIXED_HOLIDAYS.contains(monthDay)) {
            return true;
        }

        // --- Calcolo Dinamico Pasqua/Pasquetta ---
        int year = dateToCheck.getYear();
        LocalDate easterSunday = calculateEasterSunday(year);
        LocalDate easterMonday = easterSunday.plusDays(1); // Pasquetta è il giorno dopo Pasqua

        // Controlla se la data coincide con Pasqua o Pasquetta calcolate
        if (dateToCheck.equals(easterSunday) || dateToCheck.equals(easterMonday)) {
            return true;
        }
        // --- Fine Calcolo Dinamico ---

        // Se non è Domenica, né festività fissa, né Pasqua/Pasquetta, allora non è festivo
        return false;
    }

    /**
     * Calcola le ore lavorate in un intervallo di tempo suddividendole per categoria dettagliata.
     * Gestisce correttamente gli intervalli che scavallano la mezzanotte, i cambi di fascia oraria (diurna/notturna) e i cambi di giorno (feriale/festivo).
     *
     * @param entryTime Orario di ingresso (con data e ora).
     * @param exitTime  Orario di uscita (con data e ora).
     * @return Un oggetto ReportOreDettagliato con il totale delle ore per le 4 categorie per questo intervallo.
     */
    public static ReportOreDettagliato calculateHours(LocalDateTime entryTime, LocalDateTime exitTime) {
        ReportOreDettagliato report = new ReportOreDettagliato();

        if (exitTime.isBefore(entryTime) || exitTime.isEqual(entryTime)) {
            return report; // Intervallo non valido
        }

        LocalDateTime currentTime = entryTime;

        // Itera minuto per minuto
        while (currentTime.isBefore(exitTime)) {
            LocalDateTime oneMinuteLater = currentTime.plusMinutes(1);
            if (oneMinuteLater.isAfter(exitTime)) {
                oneMinuteLater = exitTime;
            }

            double minutes = ChronoUnit.MINUTES.between(currentTime, oneMinuteLater);
            if (minutes <= 0) {
                 currentTime = oneMinuteLater;
                 continue;
            }

            // Determina la categoria usando il metodo isHoliday aggiornato
            boolean isCurrentTimeHoliday = isHoliday(currentTime); // <-- Usa il metodo dinamico
            LocalTime currentLocalTime = currentTime.toLocalTime();
            boolean isNightTime = !currentLocalTime.isBefore(DAY_TIME_END) || currentLocalTime.isBefore(DAY_TIME_START);

            if (isCurrentTimeHoliday) {
                if (isNightTime) {
                    report.addFestiveNightHours(minutes / 60.0);
                } else {
                    report.addFestiveDayHours(minutes / 60.0);
                }
            } else {
                 if (isNightTime) {
                    report.addFerialNightHours(minutes / 60.0);
                } else {
                    report.addFerialDayHours(minutes / 60.0);
                }
            }
            currentTime = oneMinuteLater;
        }
        return report;
    }

    // Classe interna statica per tenere traccia delle ore calcolate nelle 4 categorie DETTAGLIATE
    public static class ReportOreDettagliato {
        private double ferialDayHours = 0;
        private double ferialNightHours = 0;
        private double festiveDayHours = 0;
        private double festiveNightHours = 0;

        public void addFerialDayHours(double hours) { this.ferialDayHours += hours; }
        public void addFerialNightHours(double hours) { this.ferialNightHours += hours; }
        public void addFestiveDayHours(double hours) { this.festiveDayHours += hours; }
        public void addFestiveNightHours(double hours) { this.festiveNightHours += hours; }

        public double getFerialDayHours() { return ferialDayHours; }
        public double getFerialNightHours() { return ferialNightHours; }
        public double getFestiveDayHours() { return festiveDayHours; }
        public double getFestiveNightHours() { return festiveNightHours; }

        public void add(ReportOreDettagliato other) {
            this.ferialDayHours += other.ferialDayHours;
            this.ferialNightHours += other.ferialNightHours;
            this.festiveDayHours += other.festiveDayHours;
            this.festiveNightHours += other.festiveNightHours;
        }

        public double getTotalHours() {
            return ferialDayHours + ferialNightHours + festiveDayHours + festiveNightHours;
        }

        @Override
        public String toString() {
             return String.format(Locale.ITALY,
                "Ore Dettagliate: [FerialiDiurne=%.2f, FerialiNotturne=%.2f, FestiveDiurne=%.2f, FestiveNotturne=%.2f, Totale=%.2f]",
                ferialDayHours, ferialNightHours, festiveDayHours, festiveNightHours, getTotalHours()
            );
        }
    }

    // Metodo main di esempio per testare il calcolo dinamico di Pasqua
    public static void main(String[] args) {
        System.out.println("--- Test Calcolo Pasqua e Festività Dinamiche ---");

        int testYear = 2024; // Anno da testare
        LocalDate easter2024 = calculateEasterSunday(testYear);
        LocalDate easterMonday2024 = easter2024.plusDays(1);
        System.out.println("Pasqua " + testYear + ": " + easter2024); // Atteso: 2024-03-31
        System.out.println("Pasquetta " + testYear + ": " + easterMonday2024); // Atteso: 2024-04-01

        testYear = 2025; // Anno da testare
        LocalDate easter2025 = calculateEasterSunday(testYear);
        LocalDate easterMonday2025 = easter2025.plusDays(1);
        System.out.println("Pasqua " + testYear + ": " + easter2025); // Atteso: 2025-04-20
        System.out.println("Pasquetta " + testYear + ": " + easterMonday2025); // Atteso: 2025-04-21

        testYear = 2026; // Anno da testare
        LocalDate easter2026 = calculateEasterSunday(testYear);
        LocalDate easterMonday2026 = easter2026.plusDays(1);
        System.out.println("Pasqua " + testYear + ": " + easter2026); // Atteso: 2026-04-05
        System.out.println("Pasquetta " + testYear + ": " + easterMonday2026); // Atteso: 2026-04-06

        System.out.println("\n--- Test isHoliday ---");
        System.out.println("È festivo il 2025-04-20 (Pasqua)? " + isHoliday(LocalDateTime.of(2025, 4, 20, 10, 0))); // true
        System.out.println("È festivo il 2025-04-21 (Pasquetta)? " + isHoliday(LocalDateTime.of(2025, 4, 21, 10, 0))); // true
        System.out.println("È festivo il 2025-04-22 (Martedì dopo)? " + isHoliday(LocalDateTime.of(2025, 4, 22, 10, 0))); // false
        System.out.println("È festivo il 2025-12-25 (Natale)? " + isHoliday(LocalDateTime.of(2025, 12, 25, 10, 0))); // true
        System.out.println("È festivo il 2025-07-14 (Feriale)? " + isHoliday(LocalDateTime.of(2025, 7, 14, 10, 0))); // false
        System.out.println("È festivo il 2025-04-27 (Domenica)? " + isHoliday(LocalDateTime.of(2025, 4, 27, 10, 0))); // true

        System.out.println("\n--- Test calculateHours con Pasqua/Pasquetta 2025 ---");
        // Turno che inizia Sabato Santo sera e finisce Pasqua mattina
        LocalDateTime entry1 = LocalDateTime.of(2025, 4, 19, 23, 0); // Sabato (Feriale Notturno)
        LocalDateTime exit1 = LocalDateTime.of(2025, 4, 20, 7, 0);   // Domenica di Pasqua (Festivo Notturno + Festivo Diurno)
        ReportOreDettagliato report1 = calculateHours(entry1, exit1);
        System.out.println("Turno Sabato Santo -> Pasqua ("+entry1+" -> "+exit1+"):\n" + report1);
        // Atteso: ~1 ora Feriale Notturno, ~6 ore Festive Notturne, ~1 ora Festiva Diurna

        // Turno che inizia Pasqua sera e finisce Pasquetta mattina
        LocalDateTime entry2 = LocalDateTime.of(2025, 4, 20, 21, 0); // Pasqua (Festivo Diurno + Festivo Notturno)
        LocalDateTime exit2 = LocalDateTime.of(2025, 4, 21, 8, 0);   // Pasquetta (Festivo Notturno + Festivo Diurno)
        ReportOreDettagliato report2 = calculateHours(entry2, exit2);
        System.out.println("Turno Pasqua -> Pasquetta ("+entry2+" -> "+exit2+"):\n" + report2);
        // Atteso: ~1 ora Festiva Diurna (Pasqua 21-22), ~8 ore Festive Notturne (Pasqua 22-00 + Pasquetta 00-06), ~2 ore Festive Diurne (Pasquetta 06-08)
    }
}