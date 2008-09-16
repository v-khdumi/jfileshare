package objects;

import utils.CustomLogger;
import utils.Jcrypt;


import config.Config;

import java.util.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.*;
import java.io.File;

/**
 * Created by Zoran Pucar zoran@medorian.com.
 * User: zoran
 * Date: 2007-jan-06
 * Time: 10:13:04
 * This is copyrighted software. If you got hold
 * of this software by unauthorized means, please
 * contact us at email above.
 */
public class UserItem {
    private String username;
    private String password;
    private String cleartextpassword;
    private String email;
    private int uid = -1;
    private int usertype = TYPE_EXTERNAL;
    private Date created;
    private Date lastlogin;
    private boolean expires = false;
    private Date expiry;
    private int daystoexpire = -1;
    private UserItem creator;
    private Map<Integer,UserItem> children = new TreeMap<Integer,UserItem>();
    private Map<Integer, FileItem> files = new TreeMap<Integer,FileItem>();
    public static final int TYPE_ADMIN = 1;
    public static final int TYPE_SECTRA = 2;
    public static final int TYPE_EXTERNAL = 3;



    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setClearTextPassword(String password){
        this.password = Jcrypt.crypt(password);
        this.cleartextpassword = password;
    }

    public String getClearTextPassword(){
        return this.cleartextpassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getUserType(){
        return this.usertype;
    }

    public void setUserType(int usertype){
        this.usertype = usertype;
    }

    public Date getCreated(){
        return this.created;
    }

    public void setCreated(Date created){
        this.created = created;
    }

    public Date getLastlogin(){
        return this.lastlogin;
    }

    public void setLastlogin(Date lastlogin){
        this.lastlogin = lastlogin;
    }

    public boolean expires(){
        return this.expires;
    }

    public void setExpires(boolean expires){
        this.expires = expires;
    }

    public void setExpiry(int days){
        Date thedate = new Date();
        if ( this.expiry == null ) this.expiry = thedate;
        GregorianCalendar expiry = new GregorianCalendar(
                            Integer.parseInt(new SimpleDateFormat("yyyy").format(this.expiry)),
                            Integer.parseInt(new SimpleDateFormat("m").format(this.expiry)),
                            Integer.parseInt(new SimpleDateFormat("d").format(this.expiry)) + days
                    );


        this.expiry = expiry.getTime();
        this.daystoexpire = days;
    }

    public UserItem getCreator(){
        return this.creator;
    }

    public void setCreator(UserItem creator){
        this.creator = creator;
    }

    public Map<Integer,UserItem> getChildren(){
        return this.children;
    }

    public void setChildren(Map<Integer,UserItem> children){
        this.children = children;
    }
    
    public void addChild(UserItem user){
        this.children.put(user.getUid(),user);
    }

    public void deleteChild(UserItem user){
        this.children.remove(user.getUid());
    }

    public Map<Integer,FileItem> getFiles(){
        return this.files;
    }

    public void setFiles(Map<Integer,FileItem> files ){
        this.files = files;
    }

    public void addFile(FileItem file){
        this.files.put(file.getFid(),file);
    }

    public FileItem getFile(int key){
        return this.files.containsKey(key)?this.files.get(key):null;
    }

    public int getDaysToExpire(){
        return this.daystoexpire;
    }


    public boolean save(Connection conn){
        PreparedStatement st = null;
        try {
            if ( this.uid==-1 ) {
                st=conn.prepareStatement("insert into UserItems values(NULL,?,?,?,?,now(),NULL,?,?,?)", Statement.RETURN_GENERATED_KEYS);
                st.setInt(1,this.usertype);
                st.setString(2,this.username);
                st.setString(3,this.password);
                st.setString(4,this.email);
                st.setBoolean(5,this.expires);
                if ( this.daystoexpire == -1 ){
                    st.setInt(6,Config.getUserExpires());
                } else {
                    st.setInt(6,this.daystoexpire);
                }
                if ( this.creator != null ){
                    st.setInt(7,this.creator.getUid());
                } else {
                    st.setNull(7,java.sql.Types.INTEGER);
                }
            } else {
                st=conn.prepareStatement("update UserItems set usertype=?,username=?,password=?,email=?,lastlogin=?,expires=?,daystoexpire=? where uid=?");
                st.setInt(1,this.usertype);
                st.setString(2,this.username);
                st.setString(3,this.password);
                st.setString(4,this.email);
                st.setTimestamp(5,new java.sql.Timestamp((new java.util.Date()).getTime()));
                st.setBoolean(6,this.expires);
                if ( this.expires ){
                    if ( daystoexpire == -1 ){
                        GregorianCalendar cal = new GregorianCalendar(
                                Integer.parseInt(new SimpleDateFormat("yyyy").format(this.created)),
                                Integer.parseInt(new SimpleDateFormat("m").format(this.created)),
                                Integer.parseInt(new SimpleDateFormat("d").format(this.created)) + Config.getUserExpires()
                        );
                        this.expiry = cal.getTime();
                    } else {
                        GregorianCalendar cal = new GregorianCalendar(
                            Integer.parseInt(new SimpleDateFormat("yyyy").format(this.created)),
                            Integer.parseInt(new SimpleDateFormat("m").format(this.created)),
                            Integer.parseInt(new SimpleDateFormat("d").format(this.created)) + this.daystoexpire
                        );
                        this.expiry = cal.getTime();
                    }
                    st.setTimestamp(7,new Timestamp(this.expiry.getTime()));

                } else {
                    st.setNull(7,java.sql.Types.TIMESTAMP);
                }
                st.setInt(8,this.uid);
            }

            st.executeUpdate();
            if (this.uid == -1){
                ResultSet rs = st.getGeneratedKeys();
                while ( rs.next()){
                    this.uid = rs.getInt(1);
                }
            }
            st.close();
            return true;
        } catch (SQLException e) {
            CustomLogger.logme(this.getClass().getName(), e.toString(),true);
            return false;
        }
    }

    public void delete(Connection conn){
        if ( this.uid != -1 ){
            //Delete all files
            if ( this.files.size() > 0 ){
                for ( Integer key: this.files.keySet()){
                    FileItem file = this.files.get(key);
                    CustomLogger.logme(this.getClass().getName(),"Deleting " + file.getName());
                    this.files.remove(key);
                    file.delete(conn);
                }
                CustomLogger.logme(this.getClass().getName(),"Files removed");
            } else {
                CustomLogger.logme(this.getClass().getName(),"No files for this user"); 
            }

            //Delete all children
            if ( this.children.size() > 0 ){
                for ( Integer key: this.children.keySet()){
                    UserItem child = this.children.get(key);
                    CustomLogger.logme(this.getClass().getName(),"Deleting child " + child.getUsername());
                    child.delete(conn);
                    this.children.remove(key);
                    CustomLogger.logme(this.getClass().getName(),"Child successfully deleted");
                }
            }
            try {
                PreparedStatement st = conn.prepareStatement("delete from UserItems where uid=?");
                st.setInt(1,this.uid);
                st.executeUpdate();
                st.close();
            } catch (SQLException e) {
                CustomLogger.logme(this.getClass().getName(), e.toString(),true);
            }
        }
    }

    public boolean isExpired(){
        if ( ! expires ) return expires;
        GregorianCalendar now = new GregorianCalendar(
                            Integer.parseInt(new SimpleDateFormat("yyyy").format(new Date().getTime())),
                            Integer.parseInt(new SimpleDateFormat("M").format(new Date().getTime()))-1 ,
                            Integer.parseInt(new SimpleDateFormat("d").format(new Date().getTime()))
                    );
        GregorianCalendar expiry = new GregorianCalendar(
                Integer.parseInt(new SimpleDateFormat("yyyy").format(this.created.getTime())),
                Integer.parseInt(new SimpleDateFormat("M").format(this.created.getTime()))-1 ,
                  Integer.parseInt(new SimpleDateFormat("d").format(this.created.getTime()))
                    );


        expiry.add(GregorianCalendar.DATE,this.daystoexpire);

        if ( expiry.before(now)) return true;
        CustomLogger.logme(this.getClass().getName(),(expiry.getTime().getTime() - now.getTime().getTime())/1000/60/60/24  + " -days to expire");

        return false;

    }

    /**
     *      CustomLogger.logme(this.getClass().getName(),expiry.compareTo(now) + " DAYS untill expiration");
     * @return value of days before expiration
     */

    public int getDaysUntillExpiration(){
        GregorianCalendar creation = new GregorianCalendar(
                                    Integer.parseInt(new SimpleDateFormat("yyyy").format(this.created)),
                                    Integer.parseInt(new SimpleDateFormat("M").format(this.created))-1,
                                    Integer.parseInt(new SimpleDateFormat("d").format(this.created))
                            );
        GregorianCalendar now = new GregorianCalendar(
                                    Integer.parseInt(new SimpleDateFormat("yyyy").format(new Date())),
                                    Integer.parseInt(new SimpleDateFormat("M").format(new Date()))-1,
                                    Integer.parseInt(new SimpleDateFormat("d").format(new Date()))
                            );


        GregorianCalendar expiry = (GregorianCalendar) creation.clone();
        expiry.add(GregorianCalendar.DATE,this.daystoexpire);

        return Integer.parseInt(Long.toString((expiry.getTime().getTime()-now.getTime().getTime())/1000/60/60/24));

    }

    public boolean isChildTo(UserItem user){
        return this.creator.getUid()==user.getUid();

    }

    public boolean isParentTo(UserItem user){
        return user.getCreator().getUid() == this.uid;
    }

    public static boolean checkUserExists(Connection conn,String username){
        boolean retval = false;
        try {
            PreparedStatement st = conn.prepareStatement("SELECT username FROM UserItems where username=?");
            st.setString(1,username);
            ResultSet rs = st.executeQuery();
            while ( rs.next() ){
                retval = true;
            }
        } catch (SQLException e) {
            CustomLogger.logme("objects.UserItem",e.toString(),true);
        }


        return retval;

    }

    public static boolean checkEmailExists(Connection conn,String email){
        boolean retval = false;
        try {
            PreparedStatement st = conn.prepareStatement("SELECT username FROM UserItems where email=?");
            st.setString(1,email);
            ResultSet rs = st.executeQuery();
            while ( rs.next() ){
                retval = true;
            }
        } catch (SQLException e) {
            CustomLogger.logme("objects.UserItem",e.toString(),true);
        }


        return retval;

    }

    public void generateRandomPw(){
        String randompw = Jcrypt.random(10,0,0,true,true,null,new Random());
        this.setClearTextPassword(randompw);
    }


}
