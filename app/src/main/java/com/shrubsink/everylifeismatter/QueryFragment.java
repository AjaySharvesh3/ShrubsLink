package com.shrubsink.everylifeismatter;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.shrubsink.everylifeismatter.adapter.QueryPostRecyclerAdapter;
import com.shrubsink.everylifeismatter.model.QueryPost;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

import de.hdodenhof.circleimageview.CircleImageView;


public class QueryFragment extends Fragment implements View.OnClickListener {

    CircleImageView mProfilePicture;
    TextView mUsernameTv;
    FirebaseAuth mFirebaseAuth;
    GoogleSignInClient googleSignInClient;

    private RecyclerView query_list_view;
    private List<QueryPost> query_list;
    static ProgressDialog mProgressDialog;

    private FirebaseFirestore firebaseFirestore;
    private QueryPostRecyclerAdapter queryPostRecyclerAdapter;

    private DocumentSnapshot lastVisible;
    private Boolean isFirstPageFirstLoad = true;

    public QueryFragment() {

    }

    @SuppressLint("SetTextI18n")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_query, container, false);

        mProfilePicture = view.findViewById(R.id.profile_image);
        mUsernameTv = view.findViewById(R.id.username_tv);

        view.findViewById(R.id.profile_image).setOnClickListener(this);

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), GoogleSignInOptions.DEFAULT_SIGN_IN);
        mFirebaseAuth = FirebaseAuth.getInstance();

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(requireActivity());
        if (acct != null) {
            String personName = acct.getDisplayName();
            Uri personPhoto = acct.getPhotoUrl();

            mUsernameTv.setText("Hello, " + personName);
            Glide.with(this).load(personPhoto).placeholder(R.drawable.profile_placeholder).into(mProfilePicture);
        }

        query_list = new ArrayList<>();
        query_list_view = view.findViewById(R.id.query_list_view);

        queryPostRecyclerAdapter = new QueryPostRecyclerAdapter(query_list);
        query_list_view.setLayoutManager(new LinearLayoutManager(container.getContext()));
        query_list_view.setAdapter(queryPostRecyclerAdapter);
        query_list_view.setHasFixedSize(true);

        if (mFirebaseAuth.getCurrentUser() != null) {
            firebaseFirestore = FirebaseFirestore.getInstance();
            query_list_view.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    Boolean reachedBottom = !recyclerView.canScrollVertically(1);
                    if (reachedBottom) {
                        loadMoreQueries();
                    }
                }
            });

            showProgressDialog(getActivity(), "Please wait...","Collecting your queries..",false);

            new Thread(new Runnable() {
                public void run() {
                    Query firstQuery = firebaseFirestore.collection("query_posts")
                            .orderBy("timestamp", Query.Direction.DESCENDING).limit(3);
                    firstQuery.addSnapshotListener(requireActivity(), new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                            if (!documentSnapshots.isEmpty()) {
                                if (isFirstPageFirstLoad) {
                                    lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                                    query_list.clear();
                                }
                                for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                                    if (documentSnapshots != null) {
                                        if (doc.getType() == DocumentChange.Type.ADDED) {
                                            String queryPostPostId = doc.getDocument().getId();
                                            QueryPost queryPost = doc.getDocument().toObject(QueryPost.class).withId(queryPostPostId);
                                            if (isFirstPageFirstLoad) {
                                                removeProgressDialog();
                                                query_list.add(queryPost);
                                            } else {
                                                removeProgressDialog();
                                                query_list.add(0, queryPost);
                                            }
                                            queryPostRecyclerAdapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                                isFirstPageFirstLoad = false;
                            }
                        }

                    });
                }
            }).start();
        }
        return view;
    }

    public void loadMoreQueries() {
        if (mFirebaseAuth.getCurrentUser() != null) {
            Query nextQuery = firebaseFirestore.collection("query_posts")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(3);

            nextQuery.addSnapshotListener(getActivity(), new EventListener<QuerySnapshot>() {
                @Override
                public void onEvent(QuerySnapshot documentSnapshots, FirebaseFirestoreException e) {
                    if (!documentSnapshots.isEmpty()) {
                        lastVisible = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);
                        for (DocumentChange doc : documentSnapshots.getDocumentChanges()) {
                            if (documentSnapshots != null) {
                                if (doc.getType() == DocumentChange.Type.ADDED) {
                                    removeProgressDialog();
                                    String queryPostId = doc.getDocument().getId();
                                    QueryPost queryPost = doc.getDocument().toObject(QueryPost.class).withId(queryPostId);
                                    query_list.add(queryPost);
                                    queryPostRecyclerAdapter.notifyDataSetChanged();
                                }
                            }
                        }
                    }
                }
            });
        }
    }

    public static void showProgressDialog(Context context, String title, String msg, boolean isCancelable) {
        try {
            if (mProgressDialog == null) {
                mProgressDialog = ProgressDialog.show(context, title, msg);
                mProgressDialog.setCancelable(isCancelable);
            }
            if (!mProgressDialog.isShowing()) {
                mProgressDialog.show();
            }
        } catch (IllegalArgumentException ie) {
            ie.printStackTrace();
        } catch (RuntimeException re) {
            re.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void removeProgressDialog() {
        try {
            if (mProgressDialog != null) {
                if (mProgressDialog.isShowing()) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            }
        } catch (IllegalArgumentException ie) {
            ie.printStackTrace();

        } catch (RuntimeException re) {
            re.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.profile_image:
                goToProfile();
                break;
        }
    }

    private void goToProfile() {
        Intent profileActivity = new Intent(getActivity(), ProfileActivity.class);
        startActivity(profileActivity);
    }
}