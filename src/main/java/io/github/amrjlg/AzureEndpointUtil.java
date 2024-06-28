package io.github.amrjlg;

import io.github.amrjlg.entity.AzureEnterpriseAppManagement;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author lingjiang
 */
public class AzureEndpointUtil {

    public static String authorization(String tenant) {
        return String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/authorize", tenant);
    }


    public static String token(String tenant) {
        return String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/token", tenant);
    }

    public static String scope(String client) {
        return String.format("%s/siemens+offline_access+openid+profile+email", client);
    }

    public static String logout(String tenant) {
        return String.format("https://login.microsoftonline.com/%s/oauth2/v2.0/logout", tenant);
    }

    public static String authorization(AzureEnterpriseAppManagement app, String state) throws UnsupportedEncodingException {
        return String.format("%s?" +
                        "client_id=%s" +
                        "&scope=%s" +
                        "&response_type=code" +
                        "&redirect_uri=%s" +
                        "&state=%s",
                authorization(app.getTenant()),
                app.getClient(),
                scope(app.getClient()),
                URLEncoder.encode(app.getRedirectUrl(), "utf8"),
                state
        );
    }
}
