package tanzent.cassette.request;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import io.reactivex.disposables.Disposable;
import tanzent.cassette.request.network.RxUtil;

/**
 * Created by Remix on 2017/12/10.
 */

public abstract class RemoteUriRequest extends ImageUriRequest<Bitmap> {
    private UriRequest mRequest;

    public RemoteUriRequest(@NonNull UriRequest request, @NonNull RequestConfig config) {
        super(config);
        mRequest = request;
    }

    @Override
    public Disposable load() {
        return getThumbBitmapObservable(mRequest)
                .compose(RxUtil.applySchedulerToIO())
                .subscribe(this::onSuccess, throwable -> onError(throwable.toString()));
    }


}
