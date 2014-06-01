/*
 * JBoss Admin
 * Copyright 2013, Christos Vasilakis, and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cvasilak.jboss.mobile.app.service;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.activities.UploadCompletedActivity;
import org.cvasilak.jboss.mobile.app.model.Server;
import org.cvasilak.jboss.mobile.app.net.ssl.CustomHTTPClient;
import org.cvasilak.jboss.mobile.app.util.CustomMultiPartEntity;

import java.io.File;

public class UploadToJBossServerService extends IntentService {

    private static final String TAG = UploadToJBossServerService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 1337;

    public UploadToJBossServerService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // initialize

        // extract details
        Server server = intent.getParcelableExtra("server");
        String directory = intent.getStringExtra("directory");
        String filename = intent.getStringExtra("filename");

        // get the json parser from the application
        JsonParser jsonParser = new JsonParser();

        // start the ball rolling...
        final NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder
                .setTicker(String.format(getString(R.string.notif_ticker), filename))
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_stat_notif_large))
                .setSmallIcon(R.drawable.ic_stat_notif_small)
                .setContentTitle(String.format(getString(R.string.notif_title), filename))
                .setContentText(getString(R.string.notif_content_init))
                        // to avoid NPE on android 2.x where content intent is required
                        // set an empty content intent
                .setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0))
                .setOngoing(true);

        // notify user that upload is starting
        mgr.notify(NOTIFICATION_ID, builder.build());

        // reset ticker for any subsequent notifications
        builder.setTicker(null);

        AbstractHttpClient client = CustomHTTPClient.getHttpClient();

        // enable digest authentication
        if (server.getUsername() != null && !server.getUsername().equals("")) {
            Credentials credentials = new UsernamePasswordCredentials(server.getUsername(), server.getPassword());

            client.getCredentialsProvider().setCredentials(new AuthScope(server.getHostname(), server.getPort(), AuthScope.ANY_REALM), credentials);
        }

        final File file = new File(directory, filename);
        final long length = file.length();

        try {
            CustomMultiPartEntity multipart = new CustomMultiPartEntity(HttpMultipartMode.BROWSER_COMPATIBLE,
                    new CustomMultiPartEntity.ProgressListener() {
                        @Override
                        public void transferred(long num) {
                            // calculate percentage

                            //System.out.println("transferred: " + num);

                            int progress = (int) ((num * 100) / length);

                            builder.setContentText(String.format(getString(R.string.notif_content), progress));

                            mgr.notify(NOTIFICATION_ID, builder.build());
                        }
                    });

            multipart.addPart(file.getName(), new FileBody(file));

            HttpPost httpRequest = new HttpPost(server.getHostPort() + "/management/add-content");
            httpRequest.setEntity(multipart);

            HttpResponse serverResponse = client.execute(httpRequest);

            BasicResponseHandler handler = new BasicResponseHandler();
            JsonObject reply = jsonParser.parse(handler.handleResponse(serverResponse)).getAsJsonObject();

            if (!reply.has("outcome")) {
                throw new RuntimeException(getString(R.string.invalid_response));
            } else {
                // remove the 'upload in-progress' notification
                mgr.cancel(NOTIFICATION_ID);

                String outcome = reply.get("outcome").getAsString();

                if (outcome.equals("success")) {

                    String BYTES_VALUE = reply.get("result").getAsJsonObject().get("BYTES_VALUE").getAsString();

                    Intent resultIntent = new Intent(this, UploadCompletedActivity.class);
                    // populate it
                    resultIntent.putExtra("server", server);
                    resultIntent.putExtra("BYTES_VALUE", BYTES_VALUE);
                    resultIntent.putExtra("filename", filename);
                    resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                    // the notification id for the 'completed upload' notification
                    // each completed upload will have a different id
                    int notifCompletedID = (int) System.currentTimeMillis();

                    builder
                            .setContentTitle(getString(R.string.notif_title_uploaded))
                            .setContentText(String.format(getString(R.string.notif_content_uploaded), filename))
                            .setContentIntent(
                                    // we set the (2nd param request code to completedID)
                                    // see http://tinyurl.com/kkcedju
                                    PendingIntent.getActivity(this, notifCompletedID, resultIntent,
                                            PendingIntent.FLAG_UPDATE_CURRENT))
                            .setAutoCancel(true)
                            .setOngoing(false);

                    mgr.notify(notifCompletedID, builder.build());

                } else {
                    JsonElement elem = reply.get("failure-description");

                    if (elem.isJsonPrimitive()) {
                        throw new RuntimeException(elem.getAsString());
                    } else if (elem.isJsonObject())
                        throw new RuntimeException(elem.getAsJsonObject().get("domain-failure-description").getAsString());
                }
            }

        } catch (Exception e) {
            builder.setContentText(e.getMessage())
                    .setAutoCancel(true)
                    .setOngoing(false);

            mgr.notify(NOTIFICATION_ID, builder.build());
        }
    }
}