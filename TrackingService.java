import java.sql.*;
public class TrackingService{
 public static String track(Connection con,int id)throws Exception{
  CallableStatement cs=con.prepareCall("{?=CALL track_package_func(?)}");
  cs.registerOutParameter(1,Types.REF_CURSOR); cs.setInt(2,id); cs.execute();
  ResultSet rs=(ResultSet)cs.getObject(1); StringBuilder sb=new StringBuilder();
  while(rs.next()) sb.append(rs.getString(1)).append(" | ").append(rs.getString(2)).append(" | ").append(rs.getTimestamp(3)).append("\n");
  rs.close(); cs.close(); return sb.length()==0?"No Data Found":sb.toString();
 }}