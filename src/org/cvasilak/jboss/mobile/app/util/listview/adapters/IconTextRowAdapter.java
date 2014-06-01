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

package org.cvasilak.jboss.mobile.app.util.listview.adapters;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.cvasilak.jboss.mobile.app.R;

import java.util.List;

public class IconTextRowAdapter extends ArrayAdapter<String> {

    private final int iconId;

    public IconTextRowAdapter(Context context, List<String> list, int iconId) {
        super(context, R.layout.icon_text_row, R.id.row_name, list);

        this.iconId = iconId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = super.getView(position, convertView, parent);
        ViewHolder holder = (ViewHolder) row.getTag();

        if (holder == null) {
            holder = new ViewHolder(row);
            row.setTag(holder);
        }

        holder.populate(getItem(position));

        return (row);
    }

    class ViewHolder {
        ImageView icon = null;
        TextView name = null;

        ViewHolder(View row) {
            this.icon = (ImageView) row.findViewById(R.id.row_icon);
            this.name = (TextView) row.findViewById(R.id.row_name);
        }

        void populate(String value) {
            name.setText(value);
            icon.setImageResource(iconId);
        }
    }
}
