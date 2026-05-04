import java.sql.*;

public class CheckDB {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/assetmgmt", "postgres", "postgres");
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) FROM assets");
            if (rs.next()) {
                System.out.println("Total assets: " + rs.getInt(1));
            }
            rs = stmt.executeQuery("SELECT asset_tag, name, status FROM assets LIMIT 5");
            while (rs.next()) {
                System.out.println("Asset: " + rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3));
            }
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
