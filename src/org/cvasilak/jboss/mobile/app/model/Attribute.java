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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class Attribute extends ManagementModelBase {

    protected List<String> path;
    protected boolean isReadOnly;

    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Attribute> CREATOR
            = new Parcelable.Creator<Attribute>() {
        public Attribute createFromParcel(Parcel in) {
            return new Attribute(in);
        }

        public Attribute[] newArray(int size) {
            return new Attribute[size];
        }
    };

    private Attribute(Parcel in) {
        readFromParcel(in);
    }

    public Attribute(Attribute attr) {
        super(attr);

        this.path = attr.path;
        this.isReadOnly = attr.isReadOnly;
    }

    public Attribute() {
        path = new ArrayList<String>();
    }

    public Attribute(String name) {
        this.name = name;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        isReadOnly = readOnly;
    }

    @Override
    public void readFromParcel(Parcel in) {
        super.readFromParcel(in);

        in.readStringList(path);
        isReadOnly = (in.readInt() != 0);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);

        out.writeStringList(path);
        out.writeInt(isReadOnly ? 1 : 0);
    }
}