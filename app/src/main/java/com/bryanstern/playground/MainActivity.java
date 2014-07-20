package com.bryanstern.playground;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bryanstern.playground.api.GitHubService;
import com.bryanstern.playground.api.GitHubUser;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Random;

import butterknife.ButterKnife;
import butterknife.InjectViews;
import retrofit.RestAdapter;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.ConnectableObservable;
import rx.subjects.PublishSubject;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private static final String TAG = PlaceholderFragment.class.getSimpleName();
        private static final String API_URL = "https://api.github.com";

        @InjectViews({R.id.row_one, R.id.row_two, R.id.row_three})
        List<ViewGroup> rowList;

        @InjectViews({R.id.name_one, R.id.name_two, R.id.name_three})
        List<TextView> nameViewList;

        @InjectViews({R.id.picture_one, R.id.picture_two, R.id.picture_three})
        List<ImageView> picViewList;

        @InjectViews({R.id.close_one, R.id.close_two, R.id.close_three})
        List<View> closeViewList;

        GitHubService gitHubService;
        ConnectableObservable<List<GitHubUser>> responseStream;
        PublishSubject<Object> refreshClickStream = PublishSubject.create();

        Subscription suggest1Sub, suggest2Sub, suggest3Sub;


        public PlaceholderFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setHasOptionsMenu(true);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            ButterKnife.inject(this, rootView);
            return rootView;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_refresh:
                    refreshClickStream.onNext(new Object());
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        @Override
        public void onStart() {
            super.onStart();

            RestAdapter restAdapter = new RestAdapter.Builder()
                    .setLogLevel(RestAdapter.LogLevel.BASIC)
                    .setEndpoint(API_URL)
                    .build();

            gitHubService = restAdapter.create(GitHubService.class);

            responseStream = refreshClickStream
                    .startWith(new Object())
                    .flatMap(new Func1<Object, Observable<List<GitHubUser>>>() {
                        @Override
                        public Observable<List<GitHubUser>> call(Object offset) {
                            return gitHubService.listUsers(randomIntInRange(0, 500));
                        }
                    }).onErrorResumeNext(new Func1<Throwable, Observable<? extends List<GitHubUser>>>() {
                        @Override
                        public Observable<? extends List<GitHubUser>> call(Throwable throwable) {
                            return null;
                        }
                    }).publish();

            Observable<Object> close1ClickStream = createObservableFromViewClick(closeViewList.get(0));
            Observable<Object> close2ClickStream = createObservableFromViewClick(closeViewList.get(1));
            Observable<Object> close3ClickStream = createObservableFromViewClick(closeViewList.get(2));

            Observable<GitHubUser> suggestion1Stream = createSuggestionStream(close1ClickStream);
            Observable<GitHubUser> suggestion2Stream = createSuggestionStream(close2ClickStream);
            Observable<GitHubUser> suggestion3Stream = createSuggestionStream(close3ClickStream);

            suggest1Sub = suggestion1Stream.subscribe(new Action1<GitHubUser>() {
                @Override
                public void call(GitHubUser gitHubUser) {
                    renderRow(0, gitHubUser);
                }
            });

            suggest2Sub = suggestion2Stream.subscribe(new Action1<GitHubUser>() {
                @Override
                public void call(GitHubUser gitHubUser) {
                    renderRow(1, gitHubUser);
                }
            });

            suggest3Sub = suggestion3Stream.subscribe(new Action1<GitHubUser>() {
                @Override
                public void call(GitHubUser gitHubUser) {
                    renderRow(2, gitHubUser);
                }
            });

            responseStream.connect();
        }

        @Override
        public void onStop() {
            super.onStop();

            suggest1Sub.unsubscribe();
            suggest2Sub.unsubscribe();
            suggest3Sub.unsubscribe();
        }

        private Observable<GitHubUser> createSuggestionStream(Observable<Object> closeClickStream) {
            GitHubUser startWith = null;

            return Observable.combineLatest(
                    closeClickStream.startWith(new Object()),
                    responseStream,
                    new Func2<Object, List<GitHubUser>, GitHubUser>() {
                        public GitHubUser call(Object o, List<GitHubUser> gitHubUsers) {
                            if (o != null)
                                return gitHubUsers.get(randomIntInRange(0, gitHubUsers.size()));
                            return null;
                        }
                    }
            ).startWith(startWith)
                    .observeOn(AndroidSchedulers.mainThread());
        }

        private Observable<Object> createObservableFromViewClick(View v) {
            final PublishSubject<Object> publishSubject = PublishSubject.create();

            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    publishSubject.onNext(new Object());
                }
            });

            return publishSubject;
        }

        private void renderRow(int row, GitHubUser user) {
            if (row > nameViewList.size()) {
                Log.d(TAG, "invalid renderRow");
                return;
            }

            if (user == null) {
                rowList.get(row).setVisibility(View.INVISIBLE);
                return;
            }

            Log.d(TAG, user.getLogin() + " " + user.getAvatarUrl());

            rowList.get(row).setVisibility(View.VISIBLE);
            nameViewList.get(row).setText(user.getLogin());
            Picasso.with(getActivity())
                    .cancelRequest(picViewList.get(row));

            Picasso.with(getActivity())
                    .load(user.getAvatarUrl())
                    .noFade()
                    .into(picViewList.get(row));
        }

        /**
         * @param min inclusive
         * @param max exclusive
         * @return
         */
        private static int randomIntInRange(int min, int max) {
            Random random = new Random();
            return random.nextInt(max - min) + min;
        }
    }
}
