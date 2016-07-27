import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.*;

/**
 * Created by alexanderchiou on 7/26/16.
 */
public class RequestService {
    public static final String PATH = "requests";
    private static final String USER_ID_KEY = "user_id";
    private static final String LINK_KEY = "link";
    private static final String BODY_KEY = "body";
    private static final String STATE_KEY = "state";
    private static final String TIME_UPDATED_KEY = "time_updated";

    private static final String OPEN = "Open";

    public static void getRequests(Connection connection, HttpServletResponse resp, long userId) throws IOException {
        try {
            String getRequestsQuery = "SELECT * FROM Request WHERE user_id = ? " +
                    "ORDER BY time_updated DESC;";
            PreparedStatement statement = connection.prepareStatement(getRequestsQuery);
            statement.setLong(1, userId);

            ResultSet resultSet =  statement.executeQuery();
            JSONArray requests = new JSONArray();
            while (resultSet.next()) {
                JSONObject request = new JSONObject();
                request.put(Constants.ID_KEY, resultSet.getLong(Constants.ID_KEY));
                request.put(Constants.TITLE_KEY, resultSet.getString(Constants.TITLE_KEY));
                request.put(LINK_KEY, resultSet.getString(LINK_KEY));
                request.put(BODY_KEY, resultSet.getString(BODY_KEY));
                request.put(STATE_KEY, resultSet.getString(STATE_KEY));
                request.put(TIME_UPDATED_KEY, resultSet.getLong(TIME_UPDATED_KEY));
                requests.put(request);
            }
            statement.close();
            resp.getWriter().print(requests.toString());
        }
        catch (JSONException e) {
            resp.setStatus(Constants.BAD_REQUEST);
        }
        catch (IOException|SQLException exception) {
            resp.setStatus(Constants.INTERNAL_SERVER_ERROR);
            resp.getWriter().print(Utils.getStackTrace(exception));
        }
    }

    public static void createRequest(Connection connection, HttpServletResponse resp, JSONObject request) throws IOException {
        try {
            // Parse request body
            long userId = request.getLong(USER_ID_KEY);
            String title = request.getString(Constants.TITLE_KEY);
            String link = request.getString(LINK_KEY);
            String body = request.getString(BODY_KEY);

            // Insert user
            String insertQuery = "INSERT INTO Request (user_id, title, link, body, state, time_updated)" +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, link);
            statement.setString(4, body);
            statement.setString(5, OPEN);
            statement.setLong(6, System.currentTimeMillis() / 1000L);

            long requestId;
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException();
            }
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    requestId = generatedKeys.getLong(1);
                } else {
                    throw new SQLException();
                }
            }

            JSONObject requestInfo = new JSONObject();
            requestInfo.put(Constants.ID_KEY, requestId);
            statement.close();
            resp.getWriter().print(requestInfo.toString());
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

    public static void updateRequest(Connection connection, HttpServletResponse resp, JSONObject requestBody) throws IOException {
        try {
            // Parse request body
            long requestId;
            try {
                requestId = requestBody.getLong(Constants.ID_KEY);
            } catch (JSONException e) {
                resp.setStatus(Constants.BAD_REQUEST);
                resp.getWriter().print(Utils.getStackTrace(e));
                return;
            }
            Request request = getRequest(connection, resp, requestId);
            try {
                request.setTitle(requestBody.getString(Constants.TITLE_KEY));
            } catch (JSONException ignored) {}
            try {
                request.setLink(requestBody.getString(LINK_KEY));
            } catch (JSONException ignored) {}
            try {
                request.setBody(requestBody.getString(BODY_KEY));
            } catch (JSONException ignored) {}
            try {
                request.setState(requestBody.getString(STATE_KEY));
            } catch (JSONException ignored) {}

            // Update request
            String updateQuery = "UPDATE Request " +
                    "SET title = ?, link = ?, body = ?, state = ?, time_updated = ? " +
                    "WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(updateQuery);
            statement.setString(1, request.getTitle());
            statement.setString(2, request.getLink());
            statement.setString(3, request.getBody());
            statement.setString(4, request.getState());
            statement.setLong(5, System.currentTimeMillis() / 1000L);
            statement.setLong(6, requestId);
            statement.executeUpdate();
            statement.close();
        }
        catch (SQLException exception) {
            resp.setStatus(Constants.INTERNAL_SERVER_ERROR);
            resp.getWriter().print(Utils.getStackTrace(exception));
        }
    }

    public static Request getRequest(Connection connection, HttpServletResponse resp, long requestId) throws IOException {
        Request request = new Request();
        try {
            String getUserInfoQuery = "SELECT * FROM Request WHERE id = ?;";
            PreparedStatement statement = connection.prepareStatement(getUserInfoQuery,
                    ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            statement.setLong(1, requestId);

            ResultSet resultSet = statement.executeQuery();
            if (resultSet.first()) {
                request.setTitle(resultSet.getString(Constants.TITLE_KEY));
                request.setLink(resultSet.getString(LINK_KEY));
                request.setBody(resultSet.getString(BODY_KEY));
                request.setState(resultSet.getString(STATE_KEY));
            } else {
                resp.setStatus(Constants.BAD_REQUEST);
            }
            statement.close();
        } catch (SQLException exception) {
            resp.setStatus(Constants.INTERNAL_SERVER_ERROR);
            resp.getWriter().print(Utils.getStackTrace(exception));
        }
        return request;
    }
}
