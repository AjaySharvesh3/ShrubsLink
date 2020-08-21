package com.shrubsink.everylifeismatter;

import android.app.ProgressDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.shrubsink.everylifeismatter.adapter.NotificationAdapter;
import com.shrubsink.everylifeismatter.adapter.QueryPostRecyclerAdapter;
import com.shrubsink.everylifeismatter.model.Notification;
import com.shrubsink.everylifeismatter.model.QueryPost;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.supercharge.shimmerlayout.ShimmerLayout;


public class NotificationFragment extends Fragment {

    FirebaseAuth mFirebaseAuth;
    RecyclerView notificationRecyclerView;
    List<Notification> notificationList;
    static ProgressDialog mProgressDialog;
    String currentUserId;
    private FirebaseFirestore firebaseFirestore;
    private NotificationAdapter notificationAdapter;
    private DocumentSnapshot lastVisible;
    private Boolean isFirstPageFirstLoad = true;

    ShimmerLayout shimmerLayout;

    public NotificationFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        mFirebaseAuth = FirebaseAuth.getInstance();
        currentUserId = mFirebaseAuth.getCurrentUser().getUid();

        notificationList = new ArrayList<>();
        notificationRecyclerView = view.findViewById(R.id.notification_list);

        notificationAdapter = new NotificationAdapter(notificationList);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(container.getContext()));
        notificationRecyclerView.setAdapter(notificationAdapter);
        notificationRecyclerView.setHasFixedSize(true);

        listNotifications();

        return view;
    }

    public void listNotifications() {
        if (mFirebaseAuth.getCurrentUser() != null) {
            firebaseFirestore = FirebaseFirestore.getInstance();
            notificationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    Boolean reachedBottom = !recyclerView.canScrollVertically(1);
                    if (reachedBottom) {
                        loadMoreQueries();
                    }
                }
            });

            try {
                new Thread(new Runnable() {
                    public void run() {
                        /*shimmerLayout.startShimmerAnimation();*/
                        Query firstQuery = firebaseFirestore.collection("user_bio" + currentUserId + "notifications")
                                .orderBy("timestamp", Query.Direction.DESCENDING);
                        firstQuery.addSnapshotListener(requireActivity(), new EventListener<QuerySnapshot>() {
                            @Override
                            public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                                try {
                                    if (!documentSnapshots.isEmpty()) {
                                        if (isFirstPageFirstLoad) {
                                        /*shimmerLayout.stopShimmerAnimation();
                                        shimmerLayout.setVisibility(View.GONE);*/
                                            Log.d("Notification", documentSnapshots+"");
                                            lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                                            notificationList.clear();
                                        }
                                        for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                                            if (documentSnapshots != null) {
                                                if (doc.getType() == DocumentChange.Type.ADDED) {
                                                    String notificationId = doc.getDocument().getId();
                                                    Notification notification =
                                                            doc.getDocument().toObject(Notification.class).withId(notificationId);
                                                    if (isFirstPageFirstLoad) {
                                                    /*shimmerLayout.stopShimmerAnimation();
                                                    shimmerLayout.setVisibility(View.GONE);*/
                                                        notificationList.add(notification);
                                                    } else {
                                                    /*shimmerLayout.stopShimmerAnimation();
                                                    shimmerLayout.setVisibility(View.GONE);*/
                                                        notificationList.add(0, notification);
                                                    }
                                                }
                                                notificationAdapter.notifyDataSetChanged();
                                            }
                                        }
                                        isFirstPageFirstLoad = false;
                                    }
                                } catch (Exception ex) {
                               /* shimmerLayout.stopShimmerAnimation();
                                shimmerLayout.setVisibility(View.GONE);*/
                                    Log.d("Error", "Error: " + ex);
                                }

                            }

                        });
                    }
                }).start();
            } catch(Exception er) {
                er.printStackTrace();
            }
        }
    }

    public void loadMoreQueries() {
        /*shimmerLayout.startShimmerAnimation();*/
        if (mFirebaseAuth.getCurrentUser() != null) {
            Query nextQuery = firebaseFirestore.collection("user_bio" + currentUserId + "notifications")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastVisible);

            nextQuery.addSnapshotListener(getActivity(), new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                    try {
                        if (!documentSnapshots.isEmpty()) {
                            lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                            for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                                if (documentSnapshots != null) {
                                    if (doc.getType() == DocumentChange.Type.ADDED) {
                                        /*shimmerLayout.stopShimmerAnimation();
                                        shimmerLayout.setVisibility(View.GONE);*/
                                        String notificationID = doc.getDocument().getId();
                                        Notification notification = doc.getDocument().toObject(Notification.class).withId(notificationID);
                                        notificationList.add(notification);
                                        notificationAdapter.notifyDataSetChanged();
                                    }
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Log.d("Logout Error", "Error: " + ex);
                    }
                }
            });
        }
    }
}