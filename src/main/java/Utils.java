import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Created by alexanderchiou on 7/26/16.
 */
public class Utils {
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
