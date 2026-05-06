
import java.sql.*;

public class CheckComponents {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/asset_mgmt", "postgres", "postgres");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT id, component_type, status, old_component_disposition, asset_id FROM asset_components ORDER BY id DESC LIMIT 10");
        System.out.println("ID | Type | Status | Disposition | Asset ID");
        while (rs.next()) {
            System.out.println(rs.getLong("id") + " | " + rs.getString("component_type") + " | " + 
                               rs.getString("status") + " | " + rs.getString("old_component_disposition") + " | " + 
                               rs.getObject("asset_id"));
        }
        conn.close();
    }
}
