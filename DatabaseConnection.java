import java.sql.*;
public class DatabaseConnection {
  private static final String URL="jdbc:oracle:thin:@localhost:1521:xe";
  private static final String USER="system";
  private static final String PASS="student";
  public static Connection getConnection() throws Exception{
    Class.forName("oracle.jdbc.driver.OracleDriver");
    return DriverManager.getConnection(URL,USER,PASS);
  }
}