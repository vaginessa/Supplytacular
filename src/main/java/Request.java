import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by alexanderchiou on 7/26/16.
 */
public class Request {
    public static final String PATH = "requests";
    private static final String LINK_KEY = "link";
    private static final String BODY_KEY = "body";
    private static final String STATE_KEY = "state";
    private static final String TIME_UPDATED_KEY = "time_updated";

    private static final String OPEN = "open";

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
}
