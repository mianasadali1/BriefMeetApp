package com.subhadip.briefmeet.meeting_history;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

import com.subhadip.briefmeet.databinding.ItemviewMeetingHistoryBinding;
import com.subhadip.briefmeet.R;
import com.subhadip.briefmeet.bean.MeetingHistory;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.SharedObjects;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class MeetingHistoryAdapter extends RecyclerView.Adapter<MeetingHistoryAdapter.MeetingViewHolder> {

    ArrayList<MeetingHistory> list;
    Context context;
    OnItemClickListener onItemClickListener;
    HistorySelectedListener historySelectedListener;
    boolean isSelectionEnabled = false;
    int selectedItemCount = 0;

    public MeetingHistoryAdapter(ArrayList<MeetingHistory> list, Context context) {
        this.list = list;
        this.context = context;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public void setHistorySelectedListener(HistorySelectedListener historySelectedListener) {
        this.historySelectedListener = historySelectedListener;
    }

    @Override
    public MeetingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.itemview_meeting_history, parent, false);
        MeetingViewHolder holder = new MeetingViewHolder(v);
        return holder;
    }

    @Override
    public void onBindViewHolder(final MeetingViewHolder holder, final int position) {

        if(position % 2 == 0)
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.rowBgColor));

        final MeetingHistory bean = list.get(position);

        if (!TextUtils.isEmpty(bean.getMeeting_id())) {
            holder.binding.txtName.setText(bean.getMeeting_id());
        }else{
            holder.binding.txtName.setText("");
        }

        if (!TextUtils.isEmpty(bean.getStartTime())) {

            String date = SharedObjects.convertDateFormat(bean.getStartTime()
                    ,AppConstants.DateFormats.DATETIME_FORMAT_24,AppConstants.DateFormats.DATE_FORMAT_DD_MMM_YYYY);

            String time = SharedObjects.convertDateFormat(bean.getStartTime()
                    ,AppConstants.DateFormats.DATETIME_FORMAT_24, AppConstants.DateFormats.TIME_FORMAT_12);

            holder.binding.txtDate.setText(date + ", " + time);

            if (date.equalsIgnoreCase(SharedObjects.getTodaysDate(AppConstants.DateFormats.DATE_FORMAT_DD_MMM_YYYY))){
                holder.binding.btnJoin.setVisibility(View.VISIBLE);
            }else{
                holder.binding.btnJoin.setVisibility(View.GONE);
            }

        }else{
            holder.binding.txtDate.setText("");
        }

        if (!TextUtils.isEmpty(bean.getStartTime()) && !TextUtils.isEmpty(bean.getEndTime())) {

            //HH converts hour in 24 hours format (0-23), day calculation
            SimpleDateFormat format = new SimpleDateFormat(AppConstants.DateFormats.DATETIME_FORMAT_24);

            Date d1 = null;
            Date d2 = null;

            try {
                d1 = format.parse(bean.getStartTime());
                d2 = format.parse(bean.getEndTime());

                //in milliseconds
                long diff = d2.getTime() - d1.getTime();

                long diffSeconds = diff / 1000 % 60;
                long diffMinutes = diff / (60 * 1000) % 60;
                long diffHours = diff / (60 * 60 * 1000) % 24;
                long diffDays = diff / (24 * 60 * 60 * 1000);

                if (diffHours > 0){
                    holder.binding.txtDuration.setText(SharedObjects.pad(Integer.parseInt(String.valueOf(diffHours))) + ":"
                            + SharedObjects.pad(Integer.parseInt(String.valueOf(diffMinutes))) + ":"
                            + SharedObjects.pad(Integer.parseInt(String.valueOf(diffSeconds))));
                }else if (diffMinutes > 0){
                    holder.binding.txtDuration.setText(SharedObjects.pad(Integer.parseInt(String.valueOf(diffMinutes)))
                            + ":" + SharedObjects.pad(Integer.parseInt(String.valueOf(diffSeconds))));
                }else if (diffSeconds > 0){
                    holder.binding.txtDuration.setText(SharedObjects.pad(Integer.parseInt(String.valueOf(diffSeconds))) + " sec(s)");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }else{
            holder.binding.txtDuration.setText("-");
        }

//        holder.llDelete.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (onItemClickListener != null) {
//                    mItemManger.closeItem(position);
//                    onItemClickListener.onDeleteClickListener(position, list.get(position));
//                }
//            }
//        });

        holder.binding.info.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(!bean.isChecked()) {
                    bean.setChecked(true);
                    ++selectedItemCount;
                    holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.colorAccentRipple));
                    holder.binding.btnJoin.setText("Selected");

                    //Log.e("infoLongClick", String.valueOf(selectedItemCount));
                    isSelectionEnabled = true;
                } else {
                    bean.setChecked(false);
                    ++selectedItemCount;
                    if(position %2 == 0)
                        holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.rowBgColor));
                    else
                        holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.white));
                    holder.binding.btnJoin.setText("Join");

                    if(selectedItemCount == 0)
                        isSelectionEnabled = false;
                }

                //notifyItemChanged(position);
                historySelectedListener.updateSelectionCount();
                return true;
            }
        });

        holder.binding.info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("infoClick", String.valueOf(selectedItemCount));

                if(selectedItemCount == 0) {
                    isSelectionEnabled = false;
                    return;
                }

                if(isSelectionEnabled) {
                    if (!bean.isChecked()) {
                        selectedItemCount++;
                        bean.setChecked(true);
                        holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.colorAccentRipple));
                        holder.binding.btnJoin.setText("Selected");
                    } else {
                        selectedItemCount--;
                        bean.setChecked(false);
                        if (position % 2 == 0)
                            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.rowBgColor));
                        else
                            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.white));
                        holder.binding.btnJoin.setText("Join");
                    }
                    //notifyItemChanged(position);
                    historySelectedListener.updateSelectionCount();
                }
            }
        });

        holder.binding.btnJoin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onJoinClickListener(position, list.get(position));
                }
            }
        });

        holder.binding.llMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClickListener(position, list.get(position));
                }
            }
        });

        //mItemManger.bindView(holder.itemView, position);
    }

    public interface HistorySelectedListener {
        void updateSelectionCount();
    }


    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public interface OnItemClickListener {
        void onItemClickListener(int position, MeetingHistory bean);
        void onDeleteClickListener(int position, MeetingHistory bean);
        void onJoinClickListener(int position, MeetingHistory bean);
    }

    public class MeetingViewHolder extends RecyclerView.ViewHolder {

        ItemviewMeetingHistoryBinding binding;

        public MeetingViewHolder(View itemView) {
            super(itemView);
            binding = ItemviewMeetingHistoryBinding.bind(itemView);
        }
    }
}



