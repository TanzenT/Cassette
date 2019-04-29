package tanzent.cassette.request.network;

import android.support.annotation.Nullable;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import tanzent.cassette.BuildConfig;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;
import tanzent.cassette.util.Constants;

/**
 * Created by Remix on 2017/11/20.
 */

public interface ApiService {

  @POST("search/pc")
//    @Headers("Cookie: appver=1.5.0.75771")
  Observable<ResponseBody> getNeteaseSearch(@Query("s") String key, @Query("offset") int offset,
      @Query("limit") int limit, @Query("type") int type);

  @GET("song/lyric")
//    @Headers("Cookie: appver=1.5.0.75771")
  Observable<ResponseBody> getNeteaseLyric(@Query("os") String os, @Query("id") int id,
      @Query("lv") int lv, @Query("kv") int kv, @Query("tv") int tv);

  @GET("search")
  Observable<ResponseBody> getKuGouSearch(@Query("ver") int ver, @Query("man") String man,
      @Query("client") String client,
      @Query("keyword") String keyword, @Query("duration") long duration,
      @Query("hash") String hash);

  @GET("download")
  Observable<ResponseBody> getKuGouLyric(@Query("ver") int ver, @Query("client") String client,
      @Query("fmt") String fmt, @Query("charset") String charSet,
      @Query("id") int id, @Query("accesskey") String accessKey);

  String BASE_QUERY_PARAMETERS = "?format=json&autocorrect=1&api_key=" + Constants.LASTFM_API_KEY;

  @GET(BASE_QUERY_PARAMETERS + "&method=album.getinfo")
  Observable<ResponseBody> getAlbumInfo(@Query("album") String albumName,
      @Query("artist") String artistName, @Nullable @Query("lang") String language);

  @GET(BASE_QUERY_PARAMETERS + "&method=artist.getinfo")
  Observable<ResponseBody> getArtistInfo(@Query("artist") String artistName,
      @Nullable @Query("lang") String language);

}
