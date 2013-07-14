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
import android.widget.ArrayAdapter;
import android.widget.TextView;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.model.Metric;

import java.util.List;

public class MetricsAdapter extends ArrayAdapter<Metric> {

    public MetricsAdapter(Context context, List<Metric> metrics) {
        super(context, R.layout.twoline_list_item, R.id.text1, metrics);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = super.getView(position, convertView, parent);

        ViewHolder holder = (ViewHolder) row.getTag();

        if (holder == null) {
            holder = new ViewHolder(row);
            row.setTag(holder);
        }

        holder.populateFrom(getItem(position));

        return (row);
    }

    @Override
    public boolean isEnabled(int position) {
        // metrics by default are not selectable
        return false;
    }

    static class ViewHolder {
        TextView text1;
        TextView text2;

        ViewHolder(View row) {
            this.text1 = (TextView) row.findViewById(R.id.text1);
            this.text2 = (TextView) row.findViewById(R.id.text2);
        }

        void populateFrom(Metric metric) {
            text1.setText(metric.getName());

            if (metric.getValue() != null)
                text2.setText(metric.getValue());
            else
                text2.setText("");
        }
    }
}