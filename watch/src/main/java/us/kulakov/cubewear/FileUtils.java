package us.kulakov.cubewear;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by vasiliy on 2/22/16.
 */
public class FileUtils {

    /**
     * Reads an asset file into a string, returning the resulting string
     * @param context The asset's context
     * @param asset The name of the asset
     * @return The string if successful
     */
    public static String readStringAsset(Context context, String asset) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        InputStream inputStream = context.getAssets().open(asset);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        String line = bufferedReader.readLine();
        while (line != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
            line = bufferedReader.readLine();
        }
        return stringBuilder.toString();
    }
}
