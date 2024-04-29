package com.pitechitsolutions.kneumayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;

public class ScheduleAdapter extends ArrayAdapter<Schedule> {

    public ScheduleAdapter(Context context, ArrayList<Schedule> schedules) {
        super(context, 0, schedules);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_schedule, parent, false);
        }

        // Get the data item for this position
        Schedule schedule = getItem(position);

        // Lookup view for data population
        TextView tvUserName = convertView.findViewById(R.id.tvUserName);
        TextView tvDate = convertView.findViewById(R.id.tvDate);
        TextView tvClockInTime = convertView.findViewById(R.id.tvClockInTime);
        TextView tvClockOutTime = convertView.findViewById(R.id.tvClockOutTime);

        // Populate the data into the template view using the data object
        tvUserName.setText(schedule.getUserName());
        tvDate.setText(schedule.getDate());
        tvClockInTime.setText(schedule.getClockInTime());
        tvClockOutTime.setText(schedule.getClockOutTime());

        // Return the completed view to render on screen
        return convertView;
    }

    public void updateData(ArrayList<Schedule> schedules) {
        clear();
        addAll(schedules);
        notifyDataSetChanged();
    }
}
