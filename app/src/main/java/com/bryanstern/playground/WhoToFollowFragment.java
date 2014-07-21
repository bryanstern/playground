package com.bryanstern.playground;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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
import rx.observables.ConnectableObservable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public class WhoToFollowFragment extends Fragment {

    private static final String TAG = WhoToFollowFragment.class.getSimpleName();
    private static final String API_URL = "https://api.github.com";

    @InjectViews({R.id.row_one, R.id.row_two, R.id.row_three})
    List<ViewGroup> rowList;

    @InjectViews({R.id.name_one, R.id.name_two, R.id.name_three})
    List<TextView> nameViewList;

    @InjectViews({R.id.picture_one, R.id.picture_two, R.id.picture_three})
    List<ImageView> picViewList;

    @InjectViews({R.id.close_one, R.id.close_two, R.id.close_three})
    List<Button> closeViewList;

    GitHubService gitHubService;
    ConnectableObservable<List<GitHubUser>> responseStream;
    PublishSubject<Object> refreshClickStream = PublishSubject.create();

    Subscription suggest1Sub, suggest2Sub, suggest3Sub;


    public WhoToFollowFragment() {
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
                .flatMap(offset -> gitHubService.listUsers(randomIntInRange(0, 500)))
                .onErrorResumeNext(throwable -> null)
                .publish();

        Observable<Object> close1ClickStream = createObservableFromViewClick(closeViewList.get(0));
        Observable<Object> close2ClickStream = createObservableFromViewClick(closeViewList.get(1));
        Observable<Object> close3ClickStream = createObservableFromViewClick(closeViewList.get(2));

        Observable<GitHubUser> suggestion1Stream = createSuggestionStream(close1ClickStream);
        Observable<GitHubUser> suggestion2Stream = createSuggestionStream(close2ClickStream);
        Observable<GitHubUser> suggestion3Stream = createSuggestionStream(close3ClickStream);

        suggest1Sub = suggestion1Stream.subscribe(gitHubUser -> renderRow(0, gitHubUser));

        suggest2Sub = suggestion2Stream.subscribe(gitHubUser -> renderRow(1, gitHubUser));

        suggest3Sub = suggestion3Stream.subscribe(gitHubUser -> renderRow(2, gitHubUser));

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
        return Observable.combineLatest(
                closeClickStream.startWith(new Object()),
                responseStream,
                (o, gitHubUsers) -> {
                    if (o != null)
                        return gitHubUsers.get(randomIntInRange(0, gitHubUsers.size()));
                    return null;
                })
                .mergeWith(refreshClickStream.map(o -> null))
                .startWith((GitHubUser) null)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Observable<Object> createObservableFromViewClick(View v) {
        final PublishSubject<Object> publishSubject = PublishSubject.create();

        v.setOnClickListener(view -> publishSubject.onNext(new Object()));

        return publishSubject;
    }

    private void renderRow(int row, final GitHubUser user) {
        if (row > nameViewList.size()) {
            return;
        }

        Picasso.with(getActivity()).cancelRequest(picViewList.get(row));

        if (user == null) {
            rowList.get(row).setVisibility(View.INVISIBLE);
            return;
        }

        rowList.get(row).setVisibility(View.VISIBLE);
        nameViewList.get(row).setText(user.getLogin());

        Picasso.with(getActivity())
                .load(user.getAvatarUrl())
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