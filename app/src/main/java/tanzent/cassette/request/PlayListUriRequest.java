package tanzent.cassette.request;

import android.net.Uri;

import com.facebook.drawee.view.SimpleDraweeView;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import tanzent.cassette.request.network.RxUtil;
import tanzent.cassette.util.PlayListUtil;

import static tanzent.cassette.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2017/11/30.
 */

public class PlayListUriRequest extends LibraryUriRequest {
    public PlayListUriRequest(SimpleDraweeView image, UriRequest request, RequestConfig config) {
        super(image, request, config);
    }

    @Override
    public void onError(String errMsg) {
//        mImage.setImageURI(Uri.EMPTY);
    }

    @Override
    public Disposable load() {
        return Observable.concat(
                getCustomThumbObservable(mRequest),
                Observable.fromIterable(PlayListUtil.getMP3ListByIds(PlayListUtil.getSongIds(mRequest.getID()), mRequest.getID()))
                        .concatMapDelayError(song -> getCoverObservable(getSearchRequestWithAlbumType(song))))
                .firstOrError()
                .toObservable()
                .compose(RxUtil.applyScheduler())
                .subscribeWith(new DisposableObserver<String>() {
                    @Override
                    protected void onStart() {
                        mImage.setImageURI(Uri.EMPTY);
                    }

                    @Override
                    public void onNext(String s) {
                        onSuccess(s);
                    }

                    @Override
                    public void onError(Throwable e) {
                        PlayListUriRequest.this.onError(e.toString());
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

}
