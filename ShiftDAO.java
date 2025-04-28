package calcoloore;

// --- IMPORT NECESSARI ---
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;    // Per pstmt.setNull
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator; // Per ordinare EmployeeInfo
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors; // Per raccogliere in mappa

// Assicurati che la classe Shift sia accessibile
// import calcoloore.Shift;
// Assicurati che il package database sia corretto
import database.DatabaseConfig;
// --- FINE IMPORT ---


public class ShiftDAO {

    // Record EmployeeInfo per contenere info dipendente
    public record EmployeeInfo(int id, String fullName, String qualifica) {}

    // Metodo getConnection (invariato)
    private Connection getConnection() throws SQLException {
        String url = DatabaseConfig.getDbUrl(); String user = DatabaseConfig.getDbUser(); String password = DatabaseConfig.getDbPassword();
        if (url == null || user == null || password == null) throw new SQLException("Configurazione DB non caricata.");
        return DriverManager.getConnection(url, user, password);
    }

    // Metodo hasOverlappingShift (invariato)
    public boolean hasOverlappingShift(Integer employeeId, LocalDateTime newEntryTime, LocalDateTime newExitTime, Integer shiftIdToExclude) throws SQLException {
       if (employeeId == null || newEntryTime == null || newExitTime == null || !newExitTime.isAfter(newEntryTime)) { throw new IllegalArgumentException("Parametri non validi per il controllo sovrapposizione."); }
       String sql = "SELECT COUNT(*) FROM shifts WHERE dipendente_id = ? AND ingresso < ? AND uscita > ? " + (shiftIdToExclude != null ? "AND id <> ?" : "");
       try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
           pstmt.setInt(1, employeeId); pstmt.setObject(2, newExitTime); pstmt.setObject(3, newEntryTime);
           if (shiftIdToExclude != null) { pstmt.setInt(4, shiftIdToExclude); }
           try (ResultSet rs = pstmt.executeQuery()) { if (rs.next()) { return rs.getInt(1) > 0; } }
       } catch (SQLException e) { System.err.println("Errore DAO hasOverlappingShift: " + e.getMessage()); throw e; } return false;
    }

    // --- Metodo getAllEmployees (MODIFICATO per restituire EmployeeInfo) ---
    public Map<Integer, EmployeeInfo> getAllEmployees() throws SQLException {
         List<EmployeeInfo> employeeList = new ArrayList<>();
         String sql = "SELECT id, nome, cognome, qualifica FROM dipendenti ORDER BY cognome, nome"; // Seleziona qualifica

         try (Connection conn = getConnection();
              PreparedStatement pstmt = conn.prepareStatement(sql);
              ResultSet rs = pstmt.executeQuery()) {

             while (rs.next()) {
                 int id = rs.getInt("id");
                 String n = rs.getString("nome");
                 String c = rs.getString("cognome");
                 String q = rs.getString("qualifica"); // Recupera qualifica
                 if (q == null) { q = ""; }

                 String f = (c != null && !c.trim().isEmpty() ? c.trim() : "") +
                            (n != null && !n.trim().isEmpty() ? " " + n.trim() : "");
                 if (f.trim().isEmpty()) f = "[ID: " + id + "]";

                 employeeList.add(new EmployeeInfo(id, f.trim(), q.trim())); // Crea EmployeeInfo
             }
         } catch (SQLException e) {
              System.err.println("Errore DAO getAllEmployees: " + e.getMessage());
              throw e;
         }
         // Converte la lista in mappa
         return employeeList.stream()
                .collect(Collectors.toMap(EmployeeInfo::id, info -> info));
    }
    // --- Fine Metodo getAllEmployees Modificato ---

    // Metodo getShiftsForManagement (invariato)
    public List<Object[]> getShiftsForManagement(Integer employeeId, LocalDateTime startDate, LocalDateTime endDate) throws SQLException {
         List<Object[]> shifts = new ArrayList<>(); StringBuilder sql = new StringBuilder("SELECT s.id, s.dipendente_id, d.nome, d.cognome, s.ingresso, s.uscita FROM shifts s JOIN dipendenti d ON s.dipendente_id = d.id WHERE 1=1"); if (employeeId != null) sql.append(" AND s.dipendente_id = ?"); if (startDate != null) sql.append(" AND s.ingresso >= ?"); if (endDate != null) sql.append(" AND s.ingresso <= ?"); sql.append(" ORDER BY s.ingresso"); try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql.toString())) { int i=1; if(employeeId!=null)pstmt.setInt(i++, employeeId); if(startDate!=null)pstmt.setObject(i++, startDate); if(endDate!=null)pstmt.setObject(i++, endDate); try (ResultSet rs = pstmt.executeQuery()) { while (rs.next()) { String n=rs.getString("nome"); String c=rs.getString("cognome"); String f=(c != null && !c.trim().isEmpty() ? c.trim() : "") + (n != null && !n.trim().isEmpty() ? " " + n.trim() : ""); if(f.trim().isEmpty()) f="[ID: " + rs.getInt("dipendente_id") + "]"; shifts.add(new Object[]{rs.getInt("id"), rs.getInt("dipendente_id"), f, rs.getObject("ingresso", LocalDateTime.class), rs.getObject("uscita", LocalDateTime.class)}); } } } catch (SQLException e) { System.err.println("Errore DAO getShiftsForManagement: " + e.getMessage()); throw e; } return shifts;
    }

    // Metodo getShiftsForEmployeeAndMonth (invariato)
    public List<Shift> getShiftsForEmployeeAndMonth(Integer employeeId, int year, int month) throws IllegalArgumentException, SQLException {
        if(employeeId==null) throw new IllegalArgumentException("ID nullo"); if(year<1900||month<1||month>12) throw new IllegalArgumentException("Anno/mese non validi"); List<Shift> shifts = new ArrayList<>(); LocalDateTime start=LocalDateTime.of(year,month,1,0,0); LocalDateTime end=YearMonth.of(year,month).atEndOfMonth().atTime(LocalTime.MAX); String sql="SELECT id, dipendente_id, ingresso, uscita FROM shifts WHERE dipendente_id = ? AND ingresso >= ? AND ingresso <= ? ORDER BY ingresso"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setInt(1, employeeId); pstmt.setObject(2,start); pstmt.setObject(3,end); try(ResultSet rs=pstmt.executeQuery()){ while(rs.next()) shifts.add(new Shift(rs.getInt("id"), rs.getInt("dipendente_id"), rs.getObject("ingresso", LocalDateTime.class), rs.getObject("uscita", LocalDateTime.class))); }} catch(SQLException e){ System.err.println("Errore DAO getShiftsForEmployeeAndMonth: "+e.getMessage()); throw e;} return shifts;
    }

    // Metodo getShiftsForEmployeeBetween (invariato)
    public List<Shift> getShiftsForEmployeeBetween(Integer employeeId, LocalDateTime start, LocalDateTime end) throws IllegalArgumentException, SQLException {
        if(employeeId==null) throw new IllegalArgumentException("ID nullo"); if(start==null||end==null||end.isBefore(start)) throw new IllegalArgumentException("Date non valide"); List<Shift> shifts=new ArrayList<>(); String sql="SELECT id, dipendente_id, ingresso, uscita FROM shifts WHERE dipendente_id = ? AND ingresso <= ? AND uscita >= ? ORDER BY ingresso"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setInt(1, employeeId); pstmt.setObject(2,end); pstmt.setObject(3,start); try(ResultSet rs=pstmt.executeQuery()){ while(rs.next()) shifts.add(new Shift(rs.getInt("id"), rs.getInt("dipendente_id"), rs.getObject("ingresso", LocalDateTime.class), rs.getObject("uscita", LocalDateTime.class))); }} catch(SQLException e){ System.err.println("Errore DAO getShiftsForEmployeeBetween: "+e.getMessage()); throw e;} return shifts;
    }

    // Metodo getShiftsForDay (invariato)
    public List<Object[]> getShiftsForDay(LocalDate day) throws SQLException {
        if(day==null) throw new IllegalArgumentException("Data nulla"); List<Object[]> shifts=new ArrayList<>(); LocalDateTime start=day.atStartOfDay(); LocalDateTime end=day.atTime(LocalTime.MAX); String sql="SELECT s.id, s.dipendente_id, d.nome, d.cognome, s.ingresso, s.uscita FROM shifts s JOIN dipendenti d ON s.dipendente_id = d.id WHERE s.ingresso >= ? AND s.ingresso <= ? ORDER BY s.ingresso"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setObject(1,start); pstmt.setObject(2,end); try(ResultSet rs=pstmt.executeQuery()){ while(rs.next()){ String n=rs.getString("nome"); String c=rs.getString("cognome"); String f=(c != null && !c.trim().isEmpty() ? c.trim() : "") + (n != null && !n.trim().isEmpty() ? " " + n.trim() : ""); if(f.trim().isEmpty()) f="[ID: " + rs.getInt("dipendente_id") + "]"; shifts.add(new Object[]{rs.getInt("id"), rs.getInt("dipendente_id"), f, rs.getObject("ingresso", LocalDateTime.class), rs.getObject("uscita", LocalDateTime.class)}); }}} catch(SQLException e){ System.err.println("Errore DAO getShiftsForDay: "+e.getMessage()); throw e;} return shifts;
    }

    // Metodo getShiftsEnteredOn (invariato)
    public List<Object[]> getShiftsEnteredOn(LocalDate entryDate) throws SQLException {
         if(entryDate==null) throw new IllegalArgumentException("Data nulla"); List<Object[]> shifts=new ArrayList<>(); LocalDateTime start=entryDate.atStartOfDay(); LocalDateTime end=entryDate.atTime(LocalTime.MAX); String sql="SELECT s.id, s.dipendente_id, d.nome, d.cognome, s.ingresso, s.uscita, s.data_inserimento FROM shifts s JOIN dipendenti d ON s.dipendente_id = d.id WHERE s.data_inserimento >= ? AND s.data_inserimento <= ? ORDER BY s.data_inserimento"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setObject(1,start); pstmt.setObject(2,end); try(ResultSet rs=pstmt.executeQuery()){ while(rs.next()){ String n=rs.getString("nome"); String c=rs.getString("cognome"); String f=(c != null && !c.trim().isEmpty() ? c.trim() : "") + (n != null && !n.trim().isEmpty() ? " " + n.trim() : ""); if(f.trim().isEmpty()) f="[ID: " + rs.getInt("dipendente_id") + "]"; shifts.add(new Object[]{rs.getInt("id"), rs.getInt("dipendente_id"), f, rs.getObject("ingresso", LocalDateTime.class), rs.getObject("uscita", LocalDateTime.class)}); }}} catch(SQLException e){ System.err.println("Errore DAO getShiftsEnteredOn: "+e.getMessage()); throw e;} return shifts;
    }

    // Metodo deleteShift (invariato)
    public void deleteShift(Integer id) throws SQLException {
        if(id==null)return; String sql="DELETE FROM shifts WHERE id = ?"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setInt(1, id); pstmt.executeUpdate();} catch(SQLException e){throw e;}
    }

    // --- Metodo addEmployee (MODIFICATO per qualifica) ---
    public void addEmployee(String n, String c, String q) throws SQLException {
        if (n == null || n.trim().isEmpty() || c == null || c.trim().isEmpty()) { throw new SQLException("Nome e Cognome sono obbligatori."); }
        String sql = "INSERT INTO dipendenti (nome, cognome, qualifica) VALUES (?, ?, ?)";
        System.out.println("[DEBUG ShiftDAO] addEmployee: Nome=" + n + ", Cognome=" + c + ", Qualifica=" + q);
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, n.trim());
            pstmt.setString(2, c.trim());
            if (q != null && !q.trim().isEmpty()) { pstmt.setString(3, q.trim()); }
            else { pstmt.setNull(3, Types.VARCHAR); } // Usa java.sql.Types
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) { throw new SQLException("Inserimento dipendente fallito."); }
            System.out.println("[DEBUG ShiftDAO] Dipendente aggiunto con successo.");
        } catch (SQLException e) { System.err.println("[DEBUG ShiftDAO] Errore SQL in addEmployee: " + e.getMessage()); throw e; }
    }

    // Metodo deleteEmployee (invariato)
    public void deleteEmployee(Integer id) throws SQLException {
         if(id==null)return; String sqlShifts = "DELETE FROM shifts WHERE dipendente_id = ?"; String sqlEmployee = "DELETE FROM dipendenti WHERE id = ?"; Connection conn = null; PreparedStatement pstmtShifts = null; PreparedStatement pstmtEmployee = null; try { conn = getConnection(); conn.setAutoCommit(false); System.out.println("[DEBUG ShiftDAO] Elimino turni per ID: " + id); pstmtShifts = conn.prepareStatement(sqlShifts); pstmtShifts.setInt(1, id); int deletedShifts = pstmtShifts.executeUpdate(); System.out.println("[DEBUG ShiftDAO] Turni eliminati: " + deletedShifts); System.out.println("[DEBUG ShiftDAO] Elimino dipendente ID: " + id); pstmtEmployee = conn.prepareStatement(sqlEmployee); pstmtEmployee.setInt(1, id); int deletedEmployees = pstmtEmployee.executeUpdate(); System.out.println("[DEBUG ShiftDAO] Dipendenti eliminati: " + deletedEmployees); conn.commit(); System.out.println("[DEBUG ShiftDAO] Commit deleteEmployee ID: " + id); } catch (SQLException e) { System.err.println("[DEBUG ShiftDAO] Errore SQL deleteEmployee, rollback..."); if (conn != null) try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } throw e; } finally { if (pstmtShifts != null) try { pstmtShifts.close(); } catch (SQLException e) { e.printStackTrace(); } if (pstmtEmployee != null) try { pstmtEmployee.close(); } catch (SQLException e) { e.printStackTrace(); } if (conn != null) try { conn.setAutoCommit(true); conn.close(); } catch (SQLException e) { e.printStackTrace(); } }
    }
    // Metodo saveShift (invariato)
    public void saveShift(Integer employeeId, LocalDateTime start, LocalDateTime end) throws SQLException {
         if(employeeId==null)throw new SQLException("ID dipendente nullo"); if(start==null||end==null||!end.isAfter(start))throw new SQLException("Date/ore turno non valide"); String sql="INSERT INTO shifts (dipendente_id, ingresso, uscita) VALUES (?, ?, ?)"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setInt(1, employeeId); pstmt.setObject(2,start); pstmt.setObject(3,end); pstmt.executeUpdate();} catch(SQLException e){throw e;}
    }
    // Metodo updateShift (invariato)
    public void updateShift(Integer shiftId, int employeeId, LocalDateTime start, LocalDateTime end) throws SQLException {
        if(shiftId==null)throw new SQLException("ID turno nullo"); if(start==null||end==null||!end.isAfter(start))throw new SQLException("Date/ore turno non valide"); if (hasOverlappingShift(employeeId, start, end, shiftId)) { throw new SQLException("La modifica crea una sovrapposizione!"); } String sql="UPDATE shifts SET dipendente_id = ?, ingresso = ?, uscita = ? WHERE id = ?"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setInt(1, employeeId); pstmt.setObject(2,start); pstmt.setObject(3,end); pstmt.setInt(4,shiftId); pstmt.executeUpdate();} catch(SQLException e){throw e;}
    }

    // --- Metodo updateEmployee (MODIFICATO per includere qualifica) ---
    /**
     * Aggiorna nome, cognome e qualifica di un dipendente esistente.
     * @param id L'ID del dipendente da aggiornare.
     * @param n Il nuovo nome (obbligatorio).
     * @param c Il nuovo cognome (obbligatorio).
     * @param q La nuova qualifica (opzionale, null o vuota verrà salvata come NULL).
     * @throws SQLException Se nome/cognome sono vuoti o si verifica un errore DB.
     * @throws IllegalArgumentException Se nome/cognome sono vuoti.
     */
    public void updateEmployee(int id, String n, String c, String q) throws SQLException, IllegalArgumentException {
        if (n == null || n.trim().isEmpty() || c == null || c.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome e Cognome sono obbligatori.");
        }
        // Query SQL aggiornata
        String sql = "UPDATE dipendenti SET nome = ?, cognome = ?, qualifica = ? WHERE id = ?";
        System.out.println("[DEBUG ShiftDAO] updateEmployee: ID=" + id + ", Nome=" + n + ", Cognome=" + c + ", Qualifica=" + q);
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, n.trim());
            pstmt.setString(2, c.trim());
            if (q != null && !q.trim().isEmpty()) {
                pstmt.setString(3, q.trim()); // Imposta qualifica
            } else {
                pstmt.setNull(3, Types.VARCHAR); // Imposta NULL
            }
            pstmt.setInt(4, id); // Clausola WHERE
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) { System.err.println("[WARN ShiftDAO] updateEmployee: Nessuna riga aggiornata per ID " + id); }
            else { System.out.println("[DEBUG ShiftDAO] Dipendente aggiornato con successo."); }
        } catch (SQLException e) { System.err.println("[DEBUG ShiftDAO] Errore SQL in updateEmployee: " + e.getMessage()); throw e; }
    }
    // --- Fine updateEmployee Modificato ---

    // Metodo getEmployeeIdByName (privato, invariato)
    private Integer getEmployeeIdByName(String n, String c) throws SQLException {
         Integer id=null; if(n==null||c==null)return null; String sql="SELECT id FROM dipendenti WHERE nome = ? AND cognome = ?"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setString(1,n.trim()); pstmt.setString(2,c.trim()); try(ResultSet rs=pstmt.executeQuery()){ if(rs.next())id=rs.getInt("id"); }} catch(SQLException e){throw e;} return id;
    }
    // Metodo getShiftById (invariato)
    public Shift getShiftById(Integer id) throws SQLException {
         Shift s=null; if(id==null)return null; String sql="SELECT id, dipendente_id, ingresso, uscita FROM shifts WHERE id = ?"; try(Connection conn=getConnection(); PreparedStatement pstmt=conn.prepareStatement(sql)){ pstmt.setInt(1,id); try(ResultSet rs=pstmt.executeQuery()){ if(rs.next()) { s=new Shift(rs.getInt("id"), rs.getInt("dipendente_id"), rs.getObject("ingresso", LocalDateTime.class), rs.getObject("uscita", LocalDateTime.class)); } }} catch(SQLException e){throw e;} return s;
    }

} // Fine classe ShiftDAO