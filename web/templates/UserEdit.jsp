<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@page import="com.sectra.jfileshare.objects.UserItem"%>
<%@page import="com.sectra.jfileshare.utils.Helpers"%>
<html>

    <head>
        <title>Edit User</title>
        <link rel="stylesheet" type="text/css" href="<%= request.getContextPath()%>/styles/user.css" />
    </head>

    <body>
        <%@include file="/WEB-INF/jspf/MessageBoxes.jspf"%>
        <%
                    UserItem currentUser = (UserItem) session.getAttribute("user");
                    UserItem user = (UserItem) request.getAttribute("user");
        %>

        <form action="<%= request.getContextPath()%>/user/edit/<%=user.getUid()%>" method="post">
            <table id="singleentry">
                <tr>
                    <th>Userid: </th><td><%= user.getUid()%></td>
                </tr>
                <tr>
                    <th>Username: </th>
                    <td>
                        <%
                                    if (currentUser.isAdmin()) {

                        %>
                        <input type="text" name="username" value="<%= Helpers.htmlSafe((String) request.getAttribute("validatedUsername"))%>" />
                        <%
                                    } else {
                                        out.print(user.getUsername());
                                    }
                        %>
                    </td>
                </tr>
                <tr>
                    <th>Email: </th><td><input type="text" name="email" value="<%= Helpers.htmlSafe((String) request.getAttribute("validatedEmail"))%>" /></td>
                </tr>
                <tr>
                    <th>Password: </th>
                    <td><input type="password" name="password1" value="<%= Helpers.htmlSafe((String) request.getAttribute("validatedPassword1"))%>"/><span class="note">Note: leave blank in order to keep existing password unchanged</span></td>
                </tr>
                <tr>
                    <th>Verify password: </th>
                    <td><input type="password" name="password2" value="<%= Helpers.htmlSafe((String) request.getAttribute("validatedPassword2"))%>"/></td>
                </tr>

                <%
                            boolean bExpiration = (Boolean) request.getAttribute("validatedBExpiration");
                            int daysUntilExpiration = (Integer) request.getAttribute("validatedDaysUntilExpiration");

                            // Only allow setting expiration on users that
                            // you have edit access to
                            if (currentUser.isAdmin() || currentUser.isParentTo(user)) {
                %>
                <tr>
                    <th>Expiration:</th>
                    <td>
                        <input id="bExpiration" type="checkbox" name="bExpiration" value="true"<%=bExpiration ? " checked=\"checked\"" : ""%> onchange="ToggleVisibility('ExpirationBlock', 'table-row-group');"/>
                    </td>
                </tr>
                <tr>
                    <td></td>
                    <td>
                        <div id="ExpirationBlock" style="display: <%= bExpiration ? "block" : "none"%>;">
                            Account will expire in
                            <input style="width: 4em; text-align: right;" type="text" name="daysUntilExpiration" value="<%= daysUntilExpiration%>"> days
                            <br />
                            <span class="note">Note: When the account expires, it will be deleted along with any and all uploaded files</span>
                        </div>
                    </td>
                </tr>
                <%
                                            } else {
                %>
                <tr>
                    <th>Expiration:</th>
                    <td>Account will expire in <%=daysUntilExpiration%> days</td>
                </tr>
                <%
                            }
                            if (currentUser.isAdmin()) {
                                Integer usertype = (Integer) request.getAttribute("validatedUsertype");
                %>
                <tr>
                    <th>User Type:</th>
                    <td>
                        <select name="usertype">
                            <option value="<%=UserItem.TYPE_ADMIN%>"<%=usertype.equals(UserItem.TYPE_ADMIN) ? " selected=\"selected\"" : ""%>>Administrator</option>
                            <option value="<%=UserItem.TYPE_INTERNAL%>"<%=usertype.equals(UserItem.TYPE_INTERNAL) ? " selected=\"selected\"" : ""%>>Sectra internal</option>
                            <option value="<%=UserItem.TYPE_EXTERNAL%>"<%=usertype.equals(UserItem.TYPE_EXTERNAL) ? " selected=\"selected\"" : ""%>>External</option>
                        </select>
                    </td>
                </tr>
                <%
                            }
                %>

            </table>
            <p>
                <input type="hidden" name="action" value="updateuser" />
                <input type="submit" name="update" value="Update user" />
            </p>
        </form>
    </body>
</html>

