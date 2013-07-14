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

package org.cvasilak.jboss.mobile.admin.util.listview.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import org.cvasilak.jboss.mobile.admin.model.ManagementModelBase;
import org.cvasilak.jboss.mobile.admin.model.OperationParameter;
import org.cvasilak.jboss.mobile.admin.util.listview.rows.*;

import java.util.ArrayList;
import java.util.List;

public class ManagementModelRowAdapter extends BaseAdapter {

    private final List<RowView> rows;

    public ManagementModelRowAdapter(Context context, List<? extends ManagementModelBase> types, ValueChangedListener listener) {
        this.rows = new ArrayList<RowView>();

        for (ManagementModelBase type : types) {
            boolean isRequired = (type instanceof OperationParameter && ((OperationParameter) type).isRequired());

            switch (type.getType()) {
                case BOOLEAN:
                    rows.add(new LabelCheckBoxRow(context, isRequired, type.getName(), type.getDescr(), type.getValue(), listener));
                    break;
                case LIST:
                    rows.add(new LabelButtonRow(context, isRequired, type.getName(), type.getDescr(), (type.getValueType() != null ? type.getValueType().name() : null), type.isNumber(), type.getValue(), listener));
                    break;
                default:
                    rows.add(new LabelEditTextRow(context, isRequired, type.getName(), type.getDescr(), type.getType().name(), type.isNumber(), type.getValue(), listener));
            }
        }
    }

    @Override
    public int getViewTypeCount() {
        return RowType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).getViewType();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return rows.get(position).getView(convertView);
    }
}