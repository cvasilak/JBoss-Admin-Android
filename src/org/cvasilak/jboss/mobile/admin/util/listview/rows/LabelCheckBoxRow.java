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

package org.cvasilak.jboss.mobile.admin.util.listview.rows;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import org.cvasilak.jboss.mobile.admin.R;
import org.cvasilak.jboss.mobile.admin.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.admin.fragments.dialogs.InfoDialogFragment;
import org.cvasilak.jboss.mobile.admin.util.listview.adapters.ValueChangedListener;

public class LabelCheckBoxRow implements RowView {

    private final Context context;
    private final LayoutInflater inflater;

    private final boolean isRequired;
    private final String label;
    private final String descr;

    private boolean value;

    private final ValueChangedListener listener;

    public LabelCheckBoxRow(Context context, boolean isRequired, String label,
                            String descr, Object value, ValueChangedListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        this.isRequired = isRequired;
        this.label = label;
        this.descr = descr;
        this.listener = listener;

        // for 'undefined' values default to false
        if (value instanceof String && value.equals("undefined"))
            this.value = false;
        else
            this.value = (Boolean) value;
    }

    public View getView(View convertView) {
        View row = convertView;
        ViewHolder holder;

        if (row == null) {
            row = inflater.inflate(R.layout.label_chkbox, null);
            holder = new ViewHolder(row);

            row.setTag(holder);

        } else {
            holder = (ViewHolder) row.getTag();
        }

        holder.starIcon.setVisibility(isRequired ? View.VISIBLE : View.INVISIBLE);

        holder.label.setText(label);
        holder.value.setChecked(value);

        holder.infoIcon.setVisibility(View.VISIBLE);
        holder.infoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoDialogFragment infoDialog = InfoDialogFragment.
                        newInstance(label, descr);

                infoDialog.show(((JBossServerRootActivity) context).getSupportFragmentManager(), InfoDialogFragment.TAG);
            }
        });

        holder.value.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (listener == null)
                    return;

                LabelCheckBoxRow.this.value = isChecked;

                listener.onValueChanged(label, isChecked);
            }
        });

        return row;
    }

    public int getViewType() {
        return RowType.LABEL_BOOLEAN.ordinal();
    }

    class ViewHolder {
        ImageView starIcon = null;
        TextView label = null;
        CheckBox value = null;
        ImageView infoIcon = null;

        ViewHolder(View row) {
            this.starIcon = (ImageView) row.findViewById(R.id.row_star_icon);
            this.label = (TextView) row.findViewById(R.id.label);
            this.value = (CheckBox) row.findViewById(R.id.value);
            this.infoIcon = (ImageView) row.findViewById(R.id.row_info_icon);
        }
    }
}
