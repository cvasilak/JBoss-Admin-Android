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

package org.cvasilak.jboss.mobile.app.util.listview.rows;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import org.cvasilak.jboss.mobile.app.R;
import org.cvasilak.jboss.mobile.app.activities.JBossServerRootActivity;
import org.cvasilak.jboss.mobile.app.fragments.dialogs.InfoDialogFragment;
import org.cvasilak.jboss.mobile.app.fragments.util.ListEditorFragment;
import org.cvasilak.jboss.mobile.app.util.listview.adapters.ValueChangedListener;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class LabelButtonRow implements RowView, ListEditorFragment.ListEditorListener {

    private final Context context;
    private final LayoutInflater inflater;

    private final boolean isRequired;
    private final String label;
    private final String descr;

    private Object value;

    private final String hint;
    private final boolean isNumber;

    private ValueChangedListener listener;

    public LabelButtonRow(Context context, boolean isRequired, String label,
                          String descr, String hint, boolean isNumber, Object value, ValueChangedListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);

        this.isRequired = isRequired;
        this.label = label;
        this.descr = descr;
        this.hint = hint;
        this.isNumber = isNumber;
        this.listener = listener;

        this.value = value;
    }

    @Override
    public void onListEditorFinished(final List<String> list) {
        if (listener == null)
            return;

        // convert the string list to numbers
        if (isNumber) {
            List<Number> convList = new ArrayList<Number>(list.size());

            try {
                for (String elem : list) {
                    convList.add(NumberFormat.getInstance().parse(elem));
                }

                this.value = convList;

            } catch (ParseException e) {
            }
        } else {
            this.value = list;
        }

        listener.onValueChanged(label, value);
    }

    public View getView(View convertView) {
        View row = convertView;
        ViewHolder holder;

        if (row == null) {
            row = inflater.inflate(R.layout.label_btn, null);
            holder = new ViewHolder(row);

            row.setTag(holder);

        } else {
            holder = (ViewHolder) row.getTag();
        }

        holder.starIcon.setVisibility(isRequired ? View.VISIBLE : View.INVISIBLE);

        holder.label.setText(label);

        holder.infoIcon.setVisibility(View.VISIBLE);
        holder.infoIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoDialogFragment infoDialog = InfoDialogFragment.
                        newInstance(label, descr);

                infoDialog.show(((JBossServerRootActivity) context).getSupportFragmentManager(), InfoDialogFragment.TAG);
            }
        });

        holder.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void onClick(View v) {
                ArrayList<String> list = null;

                if (value != null) {
                    // convert to string list so the editor can work with
                    if (isNumber) {
                        list = new ArrayList<String>();

                        for (Number val : (List<Number>) value) {
                            list.add(val.toString());
                        }
                    } else {
                        list = (ArrayList<String>) value;
                    }
                }

                ListEditorFragment fragment = ListEditorFragment.newInstance(list, isNumber, hint);
                fragment.setListener(LabelButtonRow.this);

                ((JBossServerRootActivity) context).addFragment(fragment);
            }
        });

        return row;
    }

    public int getViewType() {
        return RowType.LABEL_LIST.ordinal();
    }

    class ViewHolder {
        ImageView starIcon = null;
        TextView label = null;
        Button btn = null;
        ImageView infoIcon = null;

        ViewHolder(View row) {
            this.starIcon = (ImageView) row.findViewById(R.id.row_star_icon);
            this.label = (TextView) row.findViewById(R.id.label);
            this.btn = (Button) row.findViewById(R.id.btn);
            this.infoIcon = (ImageView) row.findViewById(R.id.row_info_icon);
        }
    }
}
