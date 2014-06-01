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

import java.util.List;

public class Operation extends ManagementModelBase {

    protected List<String> path;
    protected List<OperationParameter> parameters;

    protected boolean isReadOnly;

    public static final Parcelable.Creator<Operation> CREATOR
            = new Parcelable.Creator<Operation>() {
        public Operation createFromParcel(Parcel in) {
            return new Operation(in);
        }

        public Operation[] newArray(int size) {
            return new Operation[size];
        }
    };

    public Operation() {
    }

    public Operation(Parcel in) {
        readFromParcel(in);
    }

    public Operation(String name) {
        this.name = name;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }

    public List<OperationParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<OperationParameter> parameters) {
        this.parameters = parameters;
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
        in.readTypedList(parameters, OperationParameter.CREATOR);
        isReadOnly = (in.readInt() != 0);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);

        out.writeStringList(path);
        out.writeTypedList(parameters);
        out.writeInt(isReadOnly ? 1 : 0);
    }
}
