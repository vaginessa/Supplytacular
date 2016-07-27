import com.heroku.sdk.jdbc.DatabaseUrl;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class Main extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Connection connection = DatabaseUrl.extract().getConnection();
            if (connection != null) {
                String path = request.getRequestURI();
                String[] pathPieces = path.split("/");

                switch (pathPieces[1]) {
                    // PATH = /requests/{user_id}
                    case RequestService.PATH:
                        long userId = Long.valueOf(pathPieces[2]);
                        RequestService.getRequests(connection, response, userId);
                        break;
                    // PATH = /users/{email}
                    case UserService.PATH:
                        String email = pathPieces[2];
                        UserService.login(connection, response, email);
                        break;
                    default:
                        response.setStatus(Constants.NOT_FOUND);
                }
                connection.close();
            }
        } catch (Exception e) {
            response.setStatus(Constants.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Get request body into string
        StringBuilder requestBody = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        } catch (IOException e) {
            response.setStatus(Constants.INTERNAL_SERVER_ERROR);
            response.getWriter().print(Constants.READ_BODY_FAIL);
            return;
        }

        // Get connection to DB
        try {
            Connection connection = DatabaseUrl.extract().getConnection();
            if (connection != null) {
                try {
                    JSONObject jsonObject = new JSONObject(requestBody.toString());
                    String path = request.getRequestURI();
                    String[] pathPieces = path.split("/");
                    switch (pathPieces[1]) {
                        // PATH = /users/
                        case UserService.PATH:
                            UserService.signUp(connection, response, jsonObject);
                            break;
                        // PATH = /requests/
                        case RequestService.PATH:
                            RequestService.createRequest(connection, response, jsonObject);
                            break;
                        default:
                            response.setStatus(Constants.NOT_FOUND);
                    }
                    connection.close();
                } catch (JSONException e) {
                    response.setStatus(Constants.BAD_REQUEST);
                    response.getWriter().print(Constants.BAD_BODY_MESSAGE);
                } catch (SQLException e) {
                    response.setStatus(Constants.INTERNAL_SERVER_ERROR);
                    response.getWriter().print(Utils.getStackTrace(e));
                }
            }
        } catch (Exception e) {
            response.setStatus(Constants.INTERNAL_SERVER_ERROR);
            response.getWriter().print(Utils.getStackTrace(e));
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Get request body into string
        StringBuilder requestBody = new StringBuilder();
        String line;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        } catch (IOException e) {
            response.setStatus(Constants.INTERNAL_SERVER_ERROR);
            response.getWriter().print(Constants.READ_BODY_FAIL);
            return;
        }

        // Get connection to DB
        try {
            Connection connection = DatabaseUrl.extract().getConnection();
            if (connection != null) {
                try {
                    JSONObject jsonObject = new JSONObject(requestBody.toString());
                    String path = request.getRequestURI();
                    String[] pathPieces = path.split("/");
                    switch (pathPieces[1]) {
                        // PATH = /requests/
                        case RequestService.PATH:
                            RequestService.updateRequest(connection, response, jsonObject);
                            break;
                        default:
                            response.setStatus(Constants.NOT_FOUND);
                    }
                    connection.close();
                } catch (JSONException e) {
                    response.setStatus(Constants.BAD_REQUEST);
                    response.getWriter().print(Constants.BAD_BODY_MESSAGE);
                } catch (SQLException e) {
                    response.setStatus(Constants.INTERNAL_SERVER_ERROR);
                    response.getWriter().print(Utils.getStackTrace(e));
                }
            }
        } catch (Exception e) {
            response.setStatus(Constants.INTERNAL_SERVER_ERROR);
            response.getWriter().print(Utils.getStackTrace(e));
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new Server(Integer.valueOf(System.getenv("PORT")));
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        context.addServlet(new ServletHolder(new Main()), "/*");
        server.start();
        server.join();
    }
}