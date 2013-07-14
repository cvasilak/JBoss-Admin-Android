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

package org.cvasilak.jboss.mobile.admin.net;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.cvasilak.jboss.mobile.admin.JBossAdminApplication;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.model.Server;
import org.cvasilak.jboss.mobile.admin.net.ssl.CustomHTTPClient;
import org.cvasilak.jboss.mobile.admin.util.ParametersMap;

public class TalkToJBossServerTask extends AsyncTask<ParametersMap, Void, JsonElement> {

    private static final String TAG = TalkToJBossServerTask.class.getSimpleName();

    private final AbstractHttpClient client;
    private final Server server;
    private final Callback callback;

    private Context context;

    private Exception exception;

    // indicate whether to send back to the client
    // that initiated the AsyncTask the full JSON
    // response without any processing
    private boolean shouldProcessRequest;

    private Gson jsonBuilder;
    private JsonParser jsonParser;

    public TalkToJBossServerTask(Context context, Server server, Callback callback) {
        this(context, server, callback, true);
    }

    public TalkToJBossServerTask(Context context, Server server, Callback callback, boolean shouldProcessRequest) {
        this.context = context;
        this.server = server;
        this.callback = callback;
        this.shouldProcessRequest = shouldProcessRequest;

        this.client = CustomHTTPClient.getHttpClient();
        this.jsonBuilder = ((JBossAdminApplication) context.getApplicationContext()).getJSONBuilder();
        this.jsonParser = ((JBossAdminApplication) context.getApplicationContext()).getJSONParser();

        // enable digest authentication
        if (server.getUsername() != null && !server.getUsername().equals("")) {
            Credentials credentials = new UsernamePasswordCredentials(server.getUsername(), server.getPassword());

            client.getCredentialsProvider().setCredentials(new AuthScope(server.getHostname(), server.getPort(), AuthScope.ANY_REALM), credentials);
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected JsonElement doInBackground(ParametersMap... objects) {
        if (client == null) {
            return null;
        }

        ParametersMap params = objects[0];

        // ask the server to pretty print
        params.add("json.pretty", Boolean.TRUE);

        try {
            String json = jsonBuilder.toJson(params);
            StringEntity entity = new StringEntity(json, "UTF-8");
            entity.setContentType("application/json");

            HttpPost httpRequest = new HttpPost(server.getHostPort() + "/management");
            httpRequest.setEntity(entity);

            Log.d(TAG, "--------> " + json);

            HttpResponse serverResponse = client.execute(httpRequest);

            JBossResponseHandler handler = new JBossResponseHandler();
            String response = handler.handleResponse(serverResponse);

            Log.d(TAG, "<-------- " + response);

            return jsonParser.parse(response).getAsJsonObject();

        } catch (Exception e) {
            this.exception = e;
            cancel(true);
        }

        return null;
    }

    @Override
    protected void onPostExecute(JsonElement reply) {
        super.onPostExecute(reply);

        if (callback == null)
            return;

        if (reply == null)
            callback.onFailure(new RuntimeException(context.getString(R.string.empty_response)));

        if (!reply.isJsonObject())
            callback.onFailure(new RuntimeException(context.getString(R.string.invalid_response)));

        // return the full response if the shouldProcessRequest flag is not set
        if (!shouldProcessRequest) {
            callback.onSuccess(reply);
            return;
        }

        JsonObject jsonObj = (JsonObject) reply;

        if (!jsonObj.has("outcome"))
            callback.onFailure(new RuntimeException(context.getString(R.string.invalid_response)));
        else {
            String outcome = jsonObj.get("outcome").getAsString();

            if (outcome.equals("success")) {
                callback.onSuccess(jsonObj.get("result"));
            } else {
                JsonElement elem = jsonObj.get("failure-description");

                if (elem.isJsonPrimitive()) {
                    callback.onFailure(new RuntimeException(elem.getAsString()));
                } else if (elem.isJsonObject())
                    callback.onFailure(new RuntimeException(elem.getAsJsonObject().get("domain-failure-description").toString()));
            }
        }
    }

    @Override
    protected void onCancelled() {
        if (callback != null)
            callback.onFailure(this.exception);
    }
}
