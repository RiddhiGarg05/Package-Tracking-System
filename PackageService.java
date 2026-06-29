import java.sql.*;
public class PackageService{
 public static void addPackage(Connection con,int id,double w,String s,String r)throws Exception{
  CallableStatement cs=con.prepareCall("{CALL add_package_proc(?,?,?,?)}");
  cs.setInt(1,id); cs.setDouble(2,w); cs.setString(3,s); cs.setString(4,r); cs.execute(); cs.close();
 }}