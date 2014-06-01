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
import com.google.gson.*;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class ManagementModelBase implements Parcelable, Comparable<ManagementModelBase> {

    public enum Type implements Parcelable {
        STRING,
        INT,
        LONG,
        BIG_DECIMAL,
        BIG_INTEGER,
        DOUBLE,
        BOOLEAN,
        PROPERTY,
        OBJECT,
        BYTES,
        LIST,
        UNDEFINED;

        public static final Parcelable.Creator<Type> CREATOR = new Parcelable.Creator<Type>() {

            public Type createFromParcel(Parcel in) {
                return Type.values()[in.readInt()];
            }

            public Type[] newArray(int size) {
                return new Type[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(ordinal());
        }
    }

    protected String name;
    protected String descr;
    protected Object value;
    protected Type type;
    protected Type valueType;

    public ManagementModelBase() {
    }

    public ManagementModelBase(ManagementModelBase modelBase) {
        this.name = modelBase.name;
        this.descr = modelBase.descr;
        this.value = modelBase.value;
        this.type = modelBase.type;
        this.valueType = modelBase.valueType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescr() {
        return descr;
    }

    public void setDescr(String descr) {
        this.descr = descr;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setTypeFromString(String str) {
        this.type = Type.valueOf(str);
    }

    public Type getValueType() {
        return valueType;
    }

    public void setValueType(Type valueType) {
        this.valueType = valueType;
    }

    public void setValueTypeFromString(String str) {
        this.valueType = Type.valueOf(str);
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        if (isNumber() && value instanceof String) {
            try {
                value = NumberFormat.getInstance().parse((String) value);
            } catch (ParseException e) {
            }
        }

        this.value = value;
    }

    public void setValue(JsonElement value) {
        if (value instanceof JsonPrimitive) {
            JsonPrimitive primitive = (JsonPrimitive) value;

            if (primitive.isNumber()) {
                try {
                    this.value = NumberFormat.getInstance().parse(primitive.getAsString());
                } catch (ParseException e) {
                }
            } else if (primitive.isBoolean()) {
                this.value = primitive.getAsBoolean();
            } else if (primitive.isString()) {
                this.value = primitive.getAsString();
            }

        } else if (value instanceof JsonNull) {
            this.value = "undefined";

        } else if (value instanceof JsonArray) {
            List<String> list = new ArrayList<String>();

            Iterator<JsonElement> iterator = value.getAsJsonArray().iterator();

            while (iterator.hasNext()) {
                JsonElement elem = iterator.next();

                if (elem instanceof JsonObject)
                    list.add(elem.toString());
                else
                    list.add(elem.getAsString());
            }

            this.value = list;

        } else if (value instanceof JsonObject) {
            this.value = value.toString();
        }
    }

    public boolean isNumber() {
        switch (type) {
            case BIG_INTEGER:
            case BIG_DECIMAL:
            case INT:
            case DOUBLE:
            case LONG:
                return true;
            default:
                return false;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        name = in.readString();
        descr = in.readString();
        value = in.readValue(null);
        type = in.readParcelable(Type.class.getClassLoader());
        valueType = in.readParcelable(Type.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(name);
        out.writeString(descr);
        out.writeValue(value);
        out.writeParcelable(type, flags);
        out.writeParcelable(valueType, flags);
    }

    // Allow sort by name
    @Override
    public int compareTo(ManagementModelBase another) {
        return name.compareTo(another.name);
    }
}
