package com.subhadip.briefmeet.firebase_db;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.subhadip.briefmeet.bean.MeetingHistory;
import com.subhadip.briefmeet.bean.Schedule;
import com.subhadip.briefmeet.bean.UserBean;
import com.subhadip.briefmeet.utils.AppConstants;
import com.subhadip.briefmeet.utils.SharedObjects;

import java.util.ArrayList;


/**
 * The common Database manager is used for storing signin info and other meeting parser classed with list of firebase database
 * references
 */
public class DatabaseManager {

    private static final String TAG = DatabaseManager.class.getSimpleName();

    private FirebaseDatabase mDatabase;

    DatabaseReference databaseUsers;
    DatabaseReference databaseMeetingHistory;
    DatabaseReference databaseSchedule;

    private OnDatabaseDataChanged mDatabaseListener;
    private OnUserAddedListener onUserAddedListener;
    private OnUserListener onUserListener;
    private OnUserPasswordListener onUserPasswordListener;
    private OnScheduleListener onScheduleListener;
    private OnMeetingHistoryListener onMeetingHistoryListener;
    private OnUserDeleteListener onUserDeleteListener;

    Context context;

    ArrayList<MeetingHistory> arrMeetingHistory = new ArrayList<>();
    ArrayList<Schedule> arrSchedule = new ArrayList<>();
    ArrayList<UserBean> arrUsers = new ArrayList<>();

    SharedObjects sharedObjects;

    UserBean userBean = null;

    public DatabaseManager(Context context) {
        this.context = context;

        sharedObjects = new SharedObjects(context);

        mDatabase = FirebaseDatabase.getInstance();

        databaseUsers = mDatabase.getReference(AppConstants.Table.USERS);
        databaseUsers.keepSynced(true);

        databaseMeetingHistory = mDatabase.getReference(AppConstants.Table.MEETING_HISTORY);
        databaseMeetingHistory.keepSynced(true);

        databaseSchedule = mDatabase.getReference(AppConstants.Table.SCHEDULE);
        databaseSchedule.keepSynced(true);
    }

    public void initUsers() {
        databaseUsers.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    arrUsers = new ArrayList<>();
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        UserBean customer = postSnapshot.getValue(UserBean.class);
                        if (customer != null) {
                            arrUsers.add(customer);
                        }
                    }
                    if (mDatabaseListener != null) {
                        mDatabaseListener.onDataChanged(AppConstants.Table.USERS, dataSnapshot);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
                if (mDatabaseListener != null) {
                    mDatabaseListener.onCancelled(databaseError);
                }
            }
        });
    }

    public ArrayList<UserBean> getUsers() {
        return arrUsers;
    }

    public UserBean getCurrentUser() {
        return userBean;
    }

    public void addUser(UserBean bean) {
//        String id = databaseUsers.push().getKey();
//        bean.setId(id);
        databaseUsers.child(bean.getId()).setValue(bean).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (onUserAddedListener != null) {
                    onUserAddedListener.onSuccess();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (onUserAddedListener != null) {
                    onUserAddedListener.onFail();
                }
            }
        });
    }

    public void updateUser(UserBean bean) {
        DatabaseReference db = databaseUsers.child(bean.getId());
        db.setValue(bean).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (onUserAddedListener != null) {
                    onUserAddedListener.onSuccess();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (onUserAddedListener != null) {
                    onUserAddedListener.onFail();
                }
            }
        });
    }

    public void getUser(final String id) {
        Query query = databaseUsers.orderByChild("id").equalTo(id);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(UserBean.class).getId().equals(id)) {
                            userBean = postSnapshot.getValue(UserBean.class);
                        }

                    }
                }
                if (onUserListener != null) {
                    onUserListener.onUserFound();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (onUserListener != null) {
                    onUserListener.onUserNotFound();
                }
            }
        });
    }

    public void updateUserPassword(UserBean bean) {
        DatabaseReference db = databaseUsers.child(bean.getId());
        db.setValue(bean).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (onUserPasswordListener!= null) {
                    onUserPasswordListener.onPasswordUpdateSuccess();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (onUserPasswordListener != null) {
                    onUserPasswordListener.onPasswordUpdateFail();
                }
            }
        });
    }

    public void deleteUser(UserBean bean) {
        databaseUsers.child(bean.getId()).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                    if (onUserDeleteListener != null) {
                        onUserDeleteListener.onUserDeleteFail();
                    }
                } else {
                    if (onUserDeleteListener != null) {
                        onUserDeleteListener.onUserDeleteSuccess();
                    }
                }
            }
        });
    }

    public void getScheduleByUser(final String id) {
        Query query = databaseSchedule.orderByChild("userId").equalTo(id);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                arrSchedule = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(Schedule.class).getUserId().equals(id)) {
                            Schedule products = postSnapshot.getValue(Schedule.class);
                            if (products != null) {
                                arrSchedule.add(products);
                            }
                        }

                    }
                }
                if (mDatabaseListener != null) {
                    mDatabaseListener.onDataChanged(AppConstants.Table.SCHEDULE, dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                if (mDatabaseListener != null) {
                    mDatabaseListener.onCancelled(databaseError);
                }
            }
        });
    }

    public ArrayList<Schedule> getUserSchedule() {
        return arrSchedule;
    }

    public void addSchedule(Schedule bean) {
        String id = databaseSchedule.push().getKey();
        bean.setId(id);
        databaseSchedule.child(bean.getId()).setValue(bean).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (onScheduleListener != null) {
                    onScheduleListener.onAddSuccess();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (onScheduleListener != null) {
                    onScheduleListener.onAddFail();
                }
            }
        });
    }

    public void updateSchedule(Schedule bean) {
        DatabaseReference db = databaseSchedule.child(bean.getId());
        db.setValue(bean).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (onScheduleListener != null) {
                    onScheduleListener.onUpdateSuccess();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (onScheduleListener != null) {
                    onScheduleListener.onUpdateFail();
                }
            }
        });
    }

    public void deleteSchedule(Schedule bean) {
        databaseSchedule.child(bean.getId()).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                    if (onScheduleListener != null) {
                        onScheduleListener.onDeleteFail();
                    }
                } else {
                    if (onScheduleListener != null) {
                        onScheduleListener.onDeleteSuccess();
                    }
                }
            }
        });
    }

    public void addMeetingHistory(MeetingHistory bean) {
        databaseMeetingHistory.child(bean.getId()).setValue(bean).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (onMeetingHistoryListener != null) {
                    onMeetingHistoryListener.onAddSuccess();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (onMeetingHistoryListener != null) {
                    onMeetingHistoryListener.onAddFail();
                }
            }
        });
    }

    public String getKeyForMeetingHistory() {
        return  databaseMeetingHistory.push().getKey();
    }

    public void updateMeetingHistory(MeetingHistory bean) {
        DatabaseReference db = databaseMeetingHistory.child(bean.getId());
        db.setValue(bean).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                if (onMeetingHistoryListener != null) {
                    onMeetingHistoryListener.onUpdateSuccess();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (onMeetingHistoryListener != null) {
                    onMeetingHistoryListener.onUpdateFail();
                }
            }
        });
    }

    public void getMeetingHistoryByUser(final String userId) {
        Query query = databaseMeetingHistory.orderByChild("userId").equalTo(userId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                arrMeetingHistory = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                        if (postSnapshot.getValue(MeetingHistory.class).getUserId().equals(userId)) {
                            MeetingHistory meetingHistory = postSnapshot.getValue(MeetingHistory.class);
                            if (meetingHistory != null) {
                                arrMeetingHistory.add(meetingHistory);
                            }
                        }

                    }
                }
                if (mDatabaseListener != null) {
                    mDatabaseListener.onDataChanged(AppConstants.Table.MEETING_HISTORY, dataSnapshot);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                if (mDatabaseListener != null) {
                    mDatabaseListener.onCancelled(databaseError);
                }
            }
        });
    }

    public ArrayList<MeetingHistory> getUserMeetingHistory() {
        return arrMeetingHistory;
    }

    public void deleteMeetingHistory(MeetingHistory bean) {
        databaseMeetingHistory.child(bean.getId()).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                if (databaseError != null) {
                    if (onMeetingHistoryListener != null) {
                        onMeetingHistoryListener.onDeleteFail();
                    }
                } else {
                    if (onMeetingHistoryListener != null) {
                        onMeetingHistoryListener.onDeleteSuccess();
                    }
                }
            }
        });
    }

    public interface OnUserListener {
        void onUserFound();
        void onUserNotFound();
    }

    public interface OnDatabaseDataChanged {
        void onDataChanged(String url, DataSnapshot dataSnapshot);
        void onCancelled(DatabaseError error);
    }

    public interface OnUserAddedListener {
        void onSuccess();
        void onFail();
    }

    public interface OnUserDeleteListener {
        void onUserDeleteSuccess();
        void onUserDeleteFail();
    }

    public void setOnUserAddedListener(OnUserAddedListener listener) {
        onUserAddedListener = listener;
    }

    public void setDatabaseManagerListener(OnDatabaseDataChanged listener) {
        mDatabaseListener = listener;
    }

    public interface OnUserPasswordListener {
        void onPasswordUpdateSuccess();
        void onPasswordUpdateFail();
    }

    public interface OnScheduleListener {
        void onAddSuccess();
        void onUpdateSuccess();
        void onDeleteSuccess();
        void onAddFail();
        void onUpdateFail();
        void onDeleteFail();
    }

    public interface OnMeetingHistoryListener {
        void onAddSuccess();
        void onUpdateSuccess();
        void onDeleteSuccess();
        void onAddFail();
        void onUpdateFail();
        void onDeleteFail();
    }

    public void setOnUserPasswordListener(OnUserPasswordListener onUserPasswordListener) {
        this.onUserPasswordListener = onUserPasswordListener;
    }

    public OnUserListener getOnUserListener() {
        return onUserListener;
    }

    public void setOnUserListener(OnUserListener onUserListener) {
        this.onUserListener = onUserListener;
    }

    public OnScheduleListener getOnScheduleListener() {
        return onScheduleListener;
    }

    public void setOnScheduleListener(OnScheduleListener onScheduleListener) {
        this.onScheduleListener = onScheduleListener;
    }
}
