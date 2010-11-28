package com.sectra.jfileshare.filters;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sectra.jfileshare.objects.UserItem;
import com.sectra.jfileshare.objects.FileItem;

public class FileAuthenticationFilter implements Filter {

    private FilterConfig filterconfig;
    private DataSource ds;
    private static final Logger logger =
            Logger.getLogger(FileAuthenticationFilter.class.getName());

    @Override
    public void init(FilterConfig filterConfig)
            throws ServletException {
        this.filterconfig = filterConfig;
        try {
            Context env = (Context) new InitialContext().lookup("java:comp/env");
            ds = (DataSource) env.lookup("jdbc/jfileshare");
        } catch (NamingException e) {
            logger.severe(e.toString());
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {
        ds = null;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpSession session = req.getSession();

        Integer fid = Integer.parseInt(req.getPathInfo().substring(1));
        String md5sum = req.getParameter("md5");

        FileItem file = new FileItem(ds, fid);
        logger.info(file.getName());

        UserItem currentUser = null;
        if (session.getAttribute("user") != null) {
            currentUser = (UserItem) session.getAttribute("user");
        }
        if (file.getDownloads() != null) {
            logger.log(Level.INFO, "downloads left: {0}", Integer.toString(file.getDownloads()));
        }

        if (file.getFid() == null || !file.getMd5sum().equals(md5sum)) {
            logger.info("File not found in database");
            req.setAttribute("message_critical", "File not found");
            filterconfig.getServletContext().getRequestDispatcher("/templates/404.jsp").forward(servletRequest, servletResponse);
        } else if (currentUser != null && currentUser.hasEditAccessTo(file)) {
            filterChain.doFilter(servletRequest, servletResponse);
        } else if (!file.isEnabled()) {
            logger.info("File found, but it's disabled");
            req.setAttribute("message_critical", "The requested file has been disabled by it's owner");
            filterconfig.getServletContext().getRequestDispatcher("/templates/AccessDenied.jsp").forward(servletRequest, servletResponse);
        } else if (file.getDownloads() != null && file.getDownloads() == 0) {
            logger.info("File found, but has reached max number of downloads");
            req.setAttribute("message_critical", "The requested file exists, but it has reached its limit on number of downloads. If you require access to this file, please contact the file owner.");
            filterconfig.getServletContext().getRequestDispatcher("/templates/AccessDenied.jsp").forward(servletRequest, servletResponse);
        } else if (file.getPwHash() == null) {
            logger.info("File does not require authentication");
            filterChain.doFilter(servletRequest, servletResponse);
        } else if (!authenticated(file, session, req)) {
            // Send to password-screen
            logger.log(Level.INFO, "File {0} is password protected", file.getFid());
            servletRequest.setAttribute("tab", "File authentication");
            req.setAttribute("urlPattern", req.getServletPath() + (req.getPathInfo() == null ? "" : req.getPathInfo()) + "?md5=" + file.getMd5sum());
            filterconfig.getServletContext().getRequestDispatcher("/templates/FilePassword.jsp").forward(servletRequest, servletResponse);
        } else {
            // Everything appears to check out
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private boolean authenticated(FileItem file, HttpSession session, HttpServletRequest req) {
        //First, are we authenticated for this file
        if (session.getAttribute("authfiles") != null) {
            ArrayList authfiles = (ArrayList) session.getAttribute("authfiles");

            if (authfiles.contains(file.getFid())) {
                logger.info("User already authenticated for file");
                return true;
            }
        }

        // We need to authenticate; do we have a password to authenticate?
        if (req.getParameter("FilePassword") != null) {
            // We are logging in.. verify the password.
            if (file.authenticated(req.getParameter("FilePassword"))) {
                logger.log(Level.INFO, "Saving {0} to authfiles in session ", file.getFid());
                if (session.getAttribute("authfiles") != null) {
                    @SuppressWarnings("unchecked")
                    ArrayList<Integer> authfiles = (ArrayList<Integer>) session.getAttribute("authfiles");
                    authfiles.add(file.getFid());
                    session.setAttribute("authfiles", authfiles);
                } else {
                    ArrayList<Integer> authfiles = new ArrayList<Integer>();
                    authfiles.add(file.getFid());
                    session.setAttribute("authfiles", authfiles);
                }
                return true;
            } else {
                req.setAttribute("message_warning", "Incorrect password");
            }
        }
        return false;
    }
}
