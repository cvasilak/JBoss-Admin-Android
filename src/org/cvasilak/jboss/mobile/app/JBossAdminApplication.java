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

package org.cvasilak.jboss.mobile.app;

import android.app.Application;
import android.os.Environment;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import org.cvasilak.jboss.mobile.app.model.Server;
import org.cvasilak.jboss.mobile.app.model.ServersManager;
import org.cvasilak.jboss.mobile.app.net.JBossOperationsManager;

import java.io.File;

public class JBossAdminApplication extends Application {

    private static final String TAG = JBossAdminApplication.class.getSimpleName();

    private ServersManager serversManager;

    private JBossOperationsManager operationsManager;

    private Gson jsonBuilder;

    private JsonParser jsonParser;

    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "@onCreate()");

        jsonBuilder = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

        jsonParser = new JsonParser();

        serversManager = new ServersManager(getApplicationContext());
        try {
            serversManager.load();
        } catch (Exception e) {
            Log.d(TAG, "exception on load", e);
        }
    }

    public ServersManager getServersManager() {
        return serversManager;
    }

    public void setCurrentActiveServer(Server currentActiveServer) {
        this.operationsManager = new JBossOperationsManager(getApplicationContext(), currentActiveServer);
    }

    public JBossOperationsManager getOperationsManager() {
        return operationsManager;
    }

    public Gson getJSONBuilder() {
        return jsonBuilder;
    }

    public JsonParser getJSONParser() {
        return jsonParser;
    }

    public File getLocalDeploymentsDirectory() {
        File root;

        // external storage has priority
        if (isExternalStorageAvailable()) {
            root = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "JBoss-Admin");

            // create the directory if it doesn't exist
            if (!root.exists()) {
                root.mkdir();
            }

        } else {
            root = getFilesDir();  // use the app internal dir
        }

        return root;
    }

    public boolean isExternalStorageAvailable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }
}
