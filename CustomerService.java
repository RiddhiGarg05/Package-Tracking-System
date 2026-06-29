import java.sql.*;
public class CustomerService{
 public static void addCustomer(Connection con,String n,String p)throws Exception{
  CallableStatement cs=con.prepareCall("{CALL add_customer_proc(?,?)}");
  cs.setString(1,n); cs.setString(2,p); cs.execute(); cs.close();
 }}