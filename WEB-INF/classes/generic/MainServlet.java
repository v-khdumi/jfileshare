package generic;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.sql.DataSource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.*;
import java.util.logging.Logger;
import java.sql.Connection;
import java.sql.SQLException;

import config.Config;
import utils.CustomLogger;
import pageservlets.*;
import objects.UserItem;
import objects.FileItem;
import views.UserItemView;
import views.FileItemView;

/**
 * User: zoran@sectra.se
 * Date: 2005-sep-10
 * Time: 16:50:55
 */
public class MainServlet extends HttpServlet {

    Hashtable handlers = new Hashtable(50);
    DataSource datasource;

    private void initHandlers(){

	    handlers.put("0",new UploadPageHandler());
        handlers.put("1",new DownloadPageHandler());
        handlers.put("2",new RegistrationPageHandler());
        handlers.put("3",new AdminPageHandler());
        handlers.put("4",new AjaxPageHandler());
        handlers.put("5",new MainAdminPageHandler());

    }


    public void init(ServletConfig config) throws ServletException {
	super.init(config);
	try {
	    Context env = (Context) new InitialContext().lookup("java:comp/env");
	    datasource = (DataSource) env.lookup("jdbc/" + Config.getDb());

	} catch (NamingException e){
	    throw new ServletException(e);
	}
	initHandlers();

    }


    private java.sql.Connection getConnection()
     throws SQLException {


		return datasource.getConnection();



    }


    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws
		ServletException, java.io.IOException
    {
	String servername = request.getServerName();
	ServletContext app = getServletContext();
	String includefile = "/templates/noinclude.jsp";
	RequestDispatcher disp = null;
	Connection conn = null;
	String urlPattern = "";
        Connection conn1 = null;
        if ( ! request.getServletPath().contains("ajax")){
        try {
            conn1 = getConnection();
            Set<UserItem> expiredusers = new UserItemView().getExpiredUsers(conn1);
            CustomLogger.logme(this.getClass().getName(),"REMOVING EXPIRED USERS");
            if ( expiredusers != null && expiredusers.size() > 0 ){
                for ( UserItem user: expiredusers){
                    CustomLogger.logme(this.getClass().getName(),"User " + user.getUsername() + " is expired");
                    user.delete(conn1);
                    CustomLogger.logme(this.getClass().getName(),"Deleted user " + user.getUsername());
                }

            }
            CustomLogger.logme(this.getClass().getName(),"REMOVING EXPIRED FILES");
            Set<FileItem> expiredfiles = new FileItemView().getExpiredFiles(conn1);
            if ( expiredfiles != null && expiredfiles.size() > 0 ){
                for ( FileItem file: expiredfiles ){
                    CustomLogger.logme(this.getClass().getName(),"File " + file.getName() + " " + file.getMd5sum() + " is expired");
                    file.delete(conn1);
                    CustomLogger.logme(this.getClass().getName(),"Removed file " + file.getName());
                }
            }
            conn1.close();
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            try {
                conn1.close();
            } catch (SQLException e1) {
                e1.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }

        } finally {
            if ( conn1 != null ){
                try {
                    conn1.close();
                } catch (SQLException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        }

        try {
		boolean handler_done = false;
		urlPattern = request.getServletPath();
		CustomLogger.logme(this.getClass().getName(),"urlPattern: " + urlPattern );

		for ( Enumeration e = this.handlers.elements(); e.hasMoreElements();){
		    ServletPageRequestHandler handler = (ServletPageRequestHandler) e.nextElement();

		    if ( ! handler_done ){
			if ( handler.handleRequest(urlPattern)){
			    CustomLogger.logme(this.getClass().getName(),"MainServlet.processRequest." + handler.getClass().getName() + "=true" );
			    conn = getConnection();
			    includefile = handler.handlePageRequest(conn, request, response, app);
			    CustomLogger.logme(this.getClass().getName(),"includefile = " + includefile);
			    handler_done = true;
			} else {
			    CustomLogger.logme(this.getClass().getName(),"MainServlet.processRequest." + handler.getClass().getName() + "=false");
			}
		    }
		  request.setAttribute("urlPattern",urlPattern);


        }
	} catch (SQLException e){
	    CustomLogger.logme(this.getClass().getName(),"Exception happened " + e.toString(),true);

	} finally {
	    try {
		if ( conn != null) {
		    conn.close();
		}
	    } catch (SQLException e){
		CustomLogger.logme(this.getClass().getName(),"SQLException",true);
	    }
	}

	request.setAttribute("urlPattern", urlPattern);
	disp = app.getRequestDispatcher(includefile);
	disp.forward(request,response);
    }


    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, java.io.IOException{
	processRequest(request,response);
    }


   public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, java.io.IOException{
	processRequest(request,response);
    }


}
