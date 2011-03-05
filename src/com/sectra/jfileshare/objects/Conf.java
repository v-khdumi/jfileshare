package com.sectra.jfileshare.objects;

import java.io.File;
import java.io.Serializable;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Simple config object for the webapp
 * @author markus
 */
public class Conf implements Serializable {

    private int daysFileRetention;
    private int daysUserExpiration;
    private Long fileSizeMax;
    private String pathStore;
    private String pathTemp;
    private String smtpServer = "localhost";
    private int smtpServerPort = 25;
    private InternetAddress smtpSender;
    private static final Logger logger =
            Logger.getLogger(Conf.class.getName());

    public int getDaysFileRetention() {
        return daysFileRetention;
    }

    public void setDaysFileRetention(int days) {
        daysFileRetention = days;
    }

    public int getDaysUserExpiration() {
        return daysUserExpiration;
    }

    public void setDaysUserExpiration(int days) {
        daysUserExpiration = days;
    }

    public Long getFileSizeMax() {
        return fileSizeMax;
    }

    public void setFileSizeMax(Long size) {
        fileSizeMax = size;
    }

    public String getPathStore() {
        return pathStore;
    }

    public void setPathStore(String path) {
        pathStore = path;
    }

    public String getPathTemp() {
        return pathTemp;
    }

    public void setPathTemp(String path) {
        pathTemp = path;
    }

    public InternetAddress getSmtpSender() {
        return smtpSender;
    }

    public void setSmtpSender(String sender) {
        try {
            smtpSender = new InternetAddress(sender);
            smtpSender.validate();
        } catch (AddressException e) {
            logger.info("Smtp sender address doesn't validate");
        }
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String server) {
        smtpServer = server;
    }

    public int getSmtpServerPort() {
        return smtpServerPort;
    }

    public void setSmtpServerPort(int port) {
        smtpServerPort = port;
    }
}
