package tanzent.cassette.ui.adapter

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.TextView
import butterknife.BindView
import com.facebook.drawee.view.SimpleDraweeView
import io.reactivex.disposables.Disposable
import tanzent.cassette.R
import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.request.ImageUriRequest.SMALL_IMAGE_SIZE
import tanzent.cassette.request.LibraryUriRequest
import tanzent.cassette.request.RequestConfig
import tanzent.cassette.ui.adapter.holder.BaseViewHolder
import tanzent.cassette.util.ImageUriUtil.getSearchRequestWithAlbumType

/**
 * Created by Remix on 2018/3/15.
 */
class CustomSortAdapter(context: Context, layoutId: Int) : BaseAdapter<Song, CustomSortAdapter.CustomSortHolder>(context, layoutId) {

    override fun convert(holder: CustomSortHolder?, song: Song?, position: Int) {
        if (song == null || holder == null)
            return
        holder.mTitle.text = song.Title
        holder.mAlbum.text = song.album
        //封面

        val disposable = LibraryUriRequest(holder.mImage, getSearchRequestWithAlbumType(song), RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load()
        holder.mImage.tag = disposable
    }

    override fun onViewRecycled(holder: CustomSortHolder?) {
        super.onViewRecycled(holder)
        holder?.let {
            if (it.mImage.tag != null) {
                val disposable = it.mImage.tag as Disposable
                if (!disposable.isDisposed)
                    disposable.dispose()
            }
            holder.mImage.setImageURI(Uri.EMPTY)
        }

    }

    class CustomSortHolder(itemView: View) : BaseViewHolder(itemView) {
        @BindView(R.id.item_img)
        lateinit var mImage: SimpleDraweeView
        @BindView(R.id.item_song)
        lateinit var mTitle: TextView
        @BindView(R.id.item_album)
        lateinit var mAlbum: TextView
    }
}