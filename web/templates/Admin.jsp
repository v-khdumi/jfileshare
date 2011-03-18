<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.sectra.jfileshare.objects.Conf"%>
<%@page import="com.sectra.jfileshare.objects.FileItem"%>
<html>
    <head>
        <title>Admin</title>
    </head>

    <body>
        <%@include file="/WEB-INF/jspf/MessageBoxes.jspf"%>

        <%
                    Conf conf = (Conf) getServletContext().getAttribute("conf");
        %>
        <form action="<%= request.getContextPath()%>/admin" method="post">
            <table>
                <tr>
                    <th>Company name: </th>
                    <td>
                        <input type="text" name="brandingCompany" value="<%= conf.getBrandingCompany()%>" />
                    </td>
                    <td>
                        <span class="note">
                            Note: The company name will be displayed in the user interface, and in
                            emails sent from the jfileshare app
                        </span>
                    </td>
                </tr>
                <tr>
                    <th>Domain name: </th>
                    <td>
                        <input type="text" name="brandingDomain" value="<%= conf.getBrandingDomain()%>" />
                    </td>
                    <td>
                        <span class="note">
                            Note: This domain name will be used during auto-creation of accounts. If user <strong>"xyz"</strong>
                            requests a password reset, and that user doesn't exist in the database, the reset instructions are
                            sent to xyz@&lt;domain-name&gt;.
                        </span>
                    </td>
                </tr>
                <tr>
                    <th>Company logo url: </th>
                    <td>
                        <input type="text" name="brandingLogo" value="<%= (conf.getBrandingLogo() == null ? "" : conf.getBrandingLogo())%>" />
                    </td>
                    <td>
                        <span class="note">
                            Note: Leave blank to use the default logo
                        </span>
                    </td>
                </tr>
                <tr>
                    <th>Path to filestore: </th>
                    <td><input type="text" name="pathStore" value="<%= conf.getPathStore()%>" /></td>
                </tr>
                <tr>
                    <th>Path to tempstore: </th>
                    <td><input type="text" name="pathTemp" value="<%= conf.getPathTemp()%>" /></td>
                </tr>
                <tr>
                    <th>Smtp server and port: </th>
                    <td><input type="text" name="smtpServer" value="<%= conf.getSmtpServer()%>" /></td>
                    <td><input type="text" name="smtpServerPort" value="<%= Integer.toString(conf.getSmtpServerPort())%>" size="4" /></td>
                </tr>
                <tr>
                    <th>File retention: </th>
                    <td><input type="text" name="daysFileRetention" value="<%= Integer.toString(conf.getDaysFileRetention())%>" size="4" /> days</td>
                </tr>
                <tr>
                    <th>Default user expiration: </th>
                    <td><input type="text" name="daysUserExpiration" value="<%= Integer.toString(conf.getDaysUserExpiration())%>" size="4" /> days</td>
                </tr>
                <tr>
                    <th>Maximum allowed file size: </th>
                    <td><input type="text" name="fileSizeMax" value="<%= conf.getFileSizeMax()%>" /></td>
                    <td>bytes</td>
                </tr>
                <tr><td>&nbsp;</td></tr>
                <tr>
                    <td></td>
                    <td><input type="submit" name="submit" value="Save & apply" /></td>
                </tr>
            </table>
        </form>
    </body>
</html>

