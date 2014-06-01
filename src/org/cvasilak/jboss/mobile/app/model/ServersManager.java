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

package org.cvasilak.jboss.mobile.app.model;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.cvasilak.jboss.mobile.app.JBossAdminApplication;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ServersManager {

    private static final String TAG = ServersManager.class.getSimpleName();

    private final Context context;
    private final Gson gjson;

    private List<Server> servers;

    public ServersManager(Context context) {
        this.context = context;
        this.gjson = ((JBossAdminApplication) context.getApplicationContext()).getJSONBuilder();
    }

    public void load() throws Exception {
        servers = new ArrayList<Server>();

        // if file does not exist start from scratch
        if (!context.getFileStreamPath("servers.archive").exists())
            return;

        Log.d(TAG, "initializing servers list from servers.archive file");

        FileInputStream fin = context.openFileInput("servers.archive");
        BufferedReader reader = new BufferedReader(new InputStreamReader(fin));

        StringBuilder json = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            json.append(line);
        }

        Type collectionType = new TypeToken<List<Server>>() {
        }.getType();

        servers = gjson.fromJson(json.toString(), collectionType);
    }

    public void save() throws Exception {
        // output json to file
        FileOutputStream fout = context.openFileOutput("servers.archive",
                Context.MODE_PRIVATE);

        fout.write(gjson.toJson(servers).getBytes());
        fout.close();
    }

    public void addServer(Server server) {
        servers.add(server);
    }

    public void removeServerAtIndex(int index) {
        servers.remove(index);
    }

    public Server serverAtIndex(int index) {
        return servers.get(index);
    }

    public List<Server> getServers() {
        return servers;
    }
}
