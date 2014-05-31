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

package org.cvasilak.jboss.mobile.admin.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Server implements Parcelable {

    private String name;
    private String hostname;
    private int port;
    private boolean isSSLSecured;
    private String username;
    private String password;

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Server> CREATOR
            = new Parcelable.Creator<Server>() {
        public Server createFromParcel(Parcel in) {
            return new Server(in);
        }

        public Server[] newArray(int size) {
            return new Server[size];
        }
    };

    private Server(Parcel in) {
        readFromParcel(in);
    }

    public Server() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSSLSecured() {
        return isSSLSecured;
    }

    public void setSSLSecured(boolean isSSLSecured) {
        this.isSSLSecured = isSSLSecured;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getHostPort() {
        StringBuilder builder = new StringBuilder();

        builder.append(isSSLSecured ? "https://" : "http://");
        builder.append(hostname).append(":").append(port);

        return builder.toString();
    }

    public void readFromParcel(Parcel in) {
        name = in.readString();
        hostname = in.readString();
        port = in.readInt();
        isSSLSecured = (in.readInt() != 0);
        username = in.readString();
        password = in.readString();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(hostname);
        out.writeInt(port);
        out.writeInt(isSSLSecured ? 1 : 0);
        out.writeString(username);
        out.writeString(password);
    }
}
