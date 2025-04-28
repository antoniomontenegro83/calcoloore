package calcoloore;

import java.time.LocalDateTime;

/**
 * Rappresenta un singolo turno lavorativo.
 */
public class Shift {
    private Integer id;
    private int employeeId;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    // Aggiungi altri campi se necessari

    /**
     * Costruttore per un turno esistente (con ID).
     * @param id ID del turno dal database.
     * @param employeeId ID del dipendente associato.
     * @param entryTime Data e ora di ingresso.
     * @param exitTime Data e ora di uscita.
     */
    public Shift(Integer id, int employeeId, LocalDateTime entryTime, LocalDateTime exitTime) {
        this.id = id;
        this.employeeId = employeeId;
        this.entryTime = entryTime;
        this.exitTime = exitTime;
    }

    /**
     * Costruttore per un nuovo turno (senza ID, verrà generato dal DB).
     * @param employeeId ID del dipendente associato.
     * @param entryTime Data e ora di ingresso.
     * @param exitTime Data e ora di uscita.
     */
    public Shift(int employeeId, LocalDateTime entryTime, LocalDateTime exitTime) {
         this(null, employeeId, entryTime, exitTime); // Chiama il costruttore principale con ID nullo
    }


    // Getter methods
    public Integer getId() { return id; }
    public int getEmployeeId() { return employeeId; }
    public LocalDateTime getEntryTime() { return entryTime; }
    public LocalDateTime getExitTime() { return exitTime; }

    // Setter methods (potrebbero servire)
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }
    public void setEntryTime(LocalDateTime entryTime) { this.entryTime = entryTime; }
    public void setExitTime(LocalDateTime exitTime) { this.exitTime = exitTime; }
    // L'ID di solito non viene modificato dopo la creazione

    @Override
    public String toString() {
        return "Shift{" +
               "id=" + id +
               ", employeeId=" + employeeId +
               ", entryTime=" + entryTime +
               ", exitTime=" + exitTime +
               '}';
    }
}