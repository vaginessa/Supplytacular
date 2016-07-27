import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

/**
 * Created by alexanderchiou on 7/26/16.
 */
public class User {
    public static final String PATH = "users";
    private static final String FIRST_NAME_KEY = "first_name";
    private static final String LAST_NAME_KEY = "last_name";
    private static final String EMAIL_KEY = "email";

    public static void login(Connection connection, HttpServletResponse resp, String email) throws IOException {
        try {
            String getUserInfoQuery = "SELECT * FROM Person WHERE email = ?;";
            PreparedStatement statement = connection.prepareStatement(getUserInfoQuery,
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            statement.setString(1, email);

            ResultSet resultSet =  statement.executeQuery();
            JSONObject userInfo = new JSONObject();
            if (resultSet.first()) {
                userInfo.put(Constants.ID_KEY, resultSet.getLong(Constants.ID_KEY));
                userInfo.put(FIRST_NAME_KEY, resultSet.getString(FIRST_NAME_KEY));
                userInfo.put(LAST_NAME_KEY, resultSet.getString(LAST_NAME_KEY));
            } else {
                resp.setStatus(Constants.UNAUTHORIZED);
            }
            statement.close();
            resp.getWriter().print(userInfo.toString());
        }
        catch (Exception exception) {
            resp.setStatus(Constants.INTERNAL_SERVER_ERROR);
            resp.getWriter().print(Utils.getStackTrace(exception));
        }
    }

    public static void signUp(Connection connection, HttpServletResponse resp, JSONObject user) throws IOException {
        try {
            // Parse request body
            String email = user.getString(EMAIL_KEY);
            String firstName = user.getString(FIRST_NAME_KEY);
            String lastName = user.getString(LAST_NAME_KEY);

            // Insert user
            String insertQuery = "INSERT INTO Person (first_name, last_name, email)" +
                    "VALUES (?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, email);

            long userId;
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException();
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getLong(1);
                } else {
                    throw new SQLException();
                }
            }

            JSONObject userInfo = new JSONObject();
            userInfo.put(Constants.ID_KEY, userId);
            statement.close();
            resp.getWriter().print(userInfo.toString());
        }
        catch (JSONException e) {
            resp.setStatus(Constants.BAD_REQUEST);
            resp.getWriter().print(e.getMessage());
        }
        catch (IOException|SQLException exception) {
            resp.setStatus(Constants.INTERNAL_SERVER_ERROR);
            resp.getWriter().print(Utils.getStackTrace(exception));
        }
    }
}
