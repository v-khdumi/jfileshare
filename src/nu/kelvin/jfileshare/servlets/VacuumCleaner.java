/**
 *  Copyright 2011 SECTRA Imtec AB
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * @author      Markus Berg <markus.berg @ sectra.se>
 * @version     1.7
 * @since       2012-03-14
 */
package nu.kelvin.jfileshare.servlets;

import nu.kelvin.jfileshare.objects.Conf;
import nu.kelvin.jfileshare.objects.FileItem;
import nu.kelvin.jfileshare.objects.UserItem;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.http.HttpServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import javax.sql.DataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class VacuumCleaner extends HttpServlet {

    static final long serialVersionUID = 1L;
    private DataSource ds;
    private static final Logger logger =
            Logger.getLogger(VacuumCleaner.class.getName());
    private long VACUUM_INTERVAL = 1000 * 60 * 10;
    private Timer timer = null;

    @Override
    public void init(ServletConfig config)
            throws ServletException {
        super.init(config);

        try {
            Context env = (Context) new InitialContext().lookup("java:comp/env");
            ds = (DataSource) env.lookup("jdbc/jfileshare");
        } catch (NamingException e) {
            throw new ServletException(e);
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(new PerformVacuum(), 0, VACUUM_INTERVAL);
    }

    @Override
    public void destroy() {
        timer.cancel();
        timer = null;
    }

    class PerformVacuum extends TimerTask {

        @Override
        public void run() {
            vacuum();
        }
    }

    /*
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException {
    vacuum();
    }
     */
    private void vacuum() {
        // logger.info("Running scheduled vacuum of database");
        Conf conf = (Conf) getServletContext().getAttribute("conf");
        if (conf == null) {
            conf = new Conf(ds);
        }

        // Delete expired users
        ArrayList<UserItem> users = UserItem.fetchExpired(ds);
        if (!users.isEmpty()) {
            logger.log(Level.INFO, "Vacuuming {0} expired user(s) from the database", users.size());
            for (UserItem user : users) {
                user.delete(ds, conf.getPathStore(), "vacuum");
            }
        }

        // Delete expired files
        ArrayList<FileItem> files = FileItem.fetchExpired(ds);
        if (!files.isEmpty()) {
            logger.log(Level.INFO, "Vacuuming {0} expired file(s) from the database", files.size());
            for (FileItem file : files) {
                file.delete(ds, conf.getPathStore(), "vacuum");
            }
        }

        // Delete password requests older than 2 days
        try {
            Connection dbConn = ds.getConnection();
            Statement st = dbConn.createStatement();
            int i = st.executeUpdate("DELETE FROM PasswordReset where dateRequest < ( now() - INTERVAL 2 DAY )");

            if (i > 0) {
                logger.log(Level.INFO, "Vacuuming {0} entries from password reset table", Integer.toString(i));
            }
            st.close();
            dbConn.close();
        } catch (SQLException e) {
        }

        // Clean out old log entries
        // except file download logs where the files still exist on the server
        try {
            Connection dbConn = ds.getConnection();
            PreparedStatement st = dbConn.prepareStatement("DELETE FROM Logs where date < ( now() - INTERVAL ? DAY ) and (`action`!=? or id not in (select fid from FileItems))");
            st.setInt(1, conf.getDaysLogRetention());
            st.setString(2, "download");
            int i = st.executeUpdate();

            if (i > 0) {
                logger.log(Level.INFO, "Vacuuming {0} log entries", Integer.toString(i));
            }
            st.close();
            dbConn.close();
        } catch (SQLException e) {
        }
    }
}