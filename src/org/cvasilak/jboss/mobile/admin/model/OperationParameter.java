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
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Comparator;

public class OperationParameter extends ManagementModelBase {

    private boolean isNillable;
    private boolean isRequired;
    private boolean isAddParameter;

    private Object defaultValue;

    public static final Comparator<OperationParameter> REQUIRED_COMPARATOR = new Comparator<OperationParameter>() {
        @Override
        public int compare(OperationParameter lhs, OperationParameter rhs) {
            if (lhs.isRequired() && !rhs.isRequired())
                return -1;
            else if (!lhs.isRequired() && rhs.isRequired())
                return 1;

            return 0;
        }
    };

    public static final Parcelable.Creator<OperationParameter> CREATOR
            = new Parcelable.Creator<OperationParameter>() {
        public OperationParameter createFromParcel(Parcel in) {
            return new OperationParameter(in);
        }

        public OperationParameter[] newArray(int size) {
            return new OperationParameter[size];
        }
    };

    public OperationParameter() {
    }

    public OperationParameter(Parcel in) {
        readFromParcel(in);
    }

    public OperationParameter(String name) {
        this.name = name;
    }

    public boolean isNillable() {
        return isNillable;
    }

    public void setNillable(boolean nillable) {
        isNillable = nillable;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public void setRequired(boolean required) {
        isRequired = required;
    }

    public boolean isAddParameter() {
        return isAddParameter;
    }

    public void setAddParameter(boolean addParameter) {
        isAddParameter = addParameter;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setDefaultValue(JsonElement value) {
        if (value instanceof JsonPrimitive) {
            JsonPrimitive primitive = (JsonPrimitive) value;

            if (primitive.isNumber()) {
                try {
                    this.defaultValue = NumberFormat.getInstance().parse(primitive.getAsString());
                } catch (ParseException e) {
                }
            } else if (primitive.isBoolean()) {
                this.defaultValue = primitive.getAsBoolean();
            } else if (primitive.isString()) {
                this.defaultValue = primitive.getAsString();
            }
        }
    }

    @Override
    public void readFromParcel(Parcel in) {
        super.readFromParcel(in);

        isNillable = (in.readInt() != 0);
        isRequired = (in.readInt() != 0);
        isAddParameter = (in.readInt() != 0);

        value = in.readValue(null);
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        super.writeToParcel(out, flags);

        out.writeInt(isNillable ? 1 : 0);
        out.writeInt(isRequired ? 1 : 0);
        out.writeInt(isAddParameter ? 1 : 0);

        out.writeValue(value);
    }
}
