package pageservlets;

import generic.ServletPageRequestHandler;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import utils.CustomLogger;
import utils.MD5OutputStream;

import java.sql.SQLException;
import java.sql.Connection;
import java.util.regex.Pattern;
import java.util.Date;
import java.util.Enumeration;
import java.io.File;
import java.io.IOException;

import http.MultipartRequest;
import http.UploadedFile;
import http.Exceptions.MultipartRequestException;
import objects.FileItem;
import objects.UserItem;
import config.Config;

/**
 * Created by Zoran Pucar zoran@sectra.se.
 * User: zoran
 * Date: 2007-jan-06
 * Time: 01:16:57
 * This is copyrighted software. If you got hold
 * of this software by unauthorized means, please
 * contact us at email above.
 */
public class UploadPageHandler implements ServletPageRequestHandler {


        public UploadPageHandler(){

        }

        public boolean liveConnection(){
            return true;
        }

        public boolean handleRequest(String urlPattern){

            if ( Pattern.compile("(upload)").matcher(urlPattern).find()){
                return true;
            } else {
                return false;
            }

        }

        public String handlePageRequest(Connection conn, HttpServletRequest request, HttpServletResponse response, ServletContext context)
            throws SQLException, ServletException {
            String urlPattern = request.getServletPath();

            HttpSession session = request.getSession();
            Enumeration attrs = session.getAttributeNames();
            while (attrs.hasMoreElements()) {
                String attr =  (String) attrs.nextElement();
                CustomLogger.logme(this.getClass().getName(),"SESSION VAR " + attr + " = " + session.getAttribute(attr));

            }
            CustomLogger.logme(this.getClass().getName(),"UploadPageHandler");
            String[] pathparts = request.getServletPath().split("/");
            String lastpart = pathparts[pathparts.length - 1 ];
            CustomLogger.logme(this.getClass().getName(),"Lastpart is " + lastpart);
            MultipartRequest req = null;
            MultipartRequest req2 = null;

            if (request.getParameter("action") != null ) CustomLogger.logme(this.getClass().getName(),request.getParameter("action"));
            CustomLogger.logme(this.getClass().getName(),"EXPECTING: " + request.getContentLength());
            File tmp_file = null;
            String upid = null;
            if ( request.getParameter("action") == null || ! request.getParameter("action").equals("login")){
            if ( request.getParameter("upid") != null ){
            CustomLogger.logme(this.getClass().getName(),"Request has unique id " + request.getParameter("upid"));
            } else {
                CustomLogger.logme(this.getClass().getName(),"No request unique id");
            }
            try {
                req2 = new MultipartRequest(request, MultipartRequest.DELAY_FILEREAD);
                if ( req2 != null && req2.isMultipart()){
                    String tmp_filename = (String) session.getAttribute(req2.getParameter("upid"));
                    CustomLogger.logme(this.getClass().getName(),tmp_filename!=null?tmp_filename:"tmp_filename appears to be null");
                    CustomLogger.logme(this.getClass().getName(),"UNIQUE id = " + req2.getParameter("upid"));


                    try {
                        if ( ! tmp_file.exists() ) tmp_file.createNewFile();
                    } catch (IOException e) {
                        CustomLogger.logme(this.getClass().getName(),"Failed to create tmp_file" + e.toString());
                    }
                    CustomLogger.logme(this.getClass().getName(),"Reading complete request");
                    req = new MultipartRequest(request,tmp_file);



                }
            } catch (MultipartRequestException e) {
                CustomLogger.logme(this.getClass().getName(), e.toString(),true);
            }

            if ( req != null && req.isMultipart()){
                CustomLogger.logme(this.getClass().getName(),"Prior to reading file Expecting " + req.getContentLength() + " bytes");

                req.readFilePart();
                CustomLogger.logme(this.getClass().getName(),"Expecting " + req.getContentLength() + " bytes");

                if (req.getFile("file") != null ){
                    UploadedFile file = req.getFile("file");
                    FileItem savedfile = new FileItem();
                    CustomLogger.logme(this.getClass().getName(),"Name: " + file.getName());
                    CustomLogger.logme(this.getClass().getName(),"Type: " + file.getType());
                    CustomLogger.logme(this.getClass().getName(),"MD5: " + MD5OutputStream.getMD5(file.getFile().getPath()) );
                    savedfile.setDdate(new Date());
                    savedfile.setName(file.getName());
                    savedfile.setType(file.getType());
                    savedfile.setSize(new Double(file.getFile().length()));
                    savedfile.setMd5sum(MD5OutputStream.getMD5(file.getFile().getPath()));
                    UserItem owner = (UserItem) request.getSession().getAttribute("user");
                    savedfile.setOwner(owner);
                    savedfile.save(conn);
                    File destfile = new File(Config.getFilestore() + "/" +  savedfile.getFid());
                    file.getFile().renameTo(destfile);
                    CustomLogger.logme(this.getClass().getName(),"File " + destfile.getAbsolutePath() + " saved");
                    CustomLogger.logme(this.getClass().getName(), file.getName());
                    CustomLogger.logme(this.getClass().getName(), file.getFile().getPath());
                    CustomLogger.logme(this.getClass().getName(), file.getFile().getAbsolutePath());
                }

            }
            }

            return "/templates/UploaderPage.jsp";

        }


    }
