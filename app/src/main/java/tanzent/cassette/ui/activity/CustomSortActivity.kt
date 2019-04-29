package tanzent.cassette.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import kotlinx.android.synthetic.main.activity_custom_sort.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import tanzent.cassette.R
import tanzent.cassette.bean.mp3.Song
import tanzent.cassette.db.room.DatabaseRepository
import tanzent.cassette.misc.interfaces.OnItemClickListener
import tanzent.cassette.theme.Theme
import tanzent.cassette.theme.ThemeStore
import tanzent.cassette.theme.TintHelper
import tanzent.cassette.ui.adapter.CustomSortAdapter
import tanzent.cassette.util.ColorUtil.isColorLight
import tanzent.cassette.util.ToastUtil
import tanzent.cassette.util.Util
import java.util.*
import kotlin.collections.ArrayList

class CustomSortActivity : ToolbarActivity() {

  private val adapter: CustomSortAdapter by lazy {
    CustomSortAdapter(mContext, R.layout.item_custom_sort)
  }
  private val mdDialog: MaterialDialog by lazy {
    Theme.getBaseDialog(mContext)
        .title(R.string.saveing)
        .content(R.string.please_wait)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build()
  }

  private lateinit var songs: ArrayList<Song>
  private var playlistId = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_custom_sort)
    ButterKnife.bind(this@CustomSortActivity)

    playlistId = intent.getIntExtra(EXTRA_ID, -1)
    val playlistName = intent.getStringExtra(EXTRA_NAME) ?: ""
    songs = intent.getSerializableExtra(EXTRA_LIST) as ArrayList<Song>

    setUpToolbar(playlistName)

    adapter.setData(songs)
    adapter.setOnItemClickListener(object : OnItemClickListener {
      override fun onItemLongClick(view: View?, position: Int) {
        Util.vibrate(mContext, 100)
      }

      override fun onItemClick(view: View?, position: Int) {

      }

    })

    ItemTouchHelper(object : ItemTouchHelper.Callback() {
      override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
        val dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.RIGHT
        return makeMovementFlags(dragFlag, 0)
      }

      override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        Collections.swap(adapter.datas, if (viewHolder.adapterPosition >= 0) viewHolder.adapterPosition else 0,
            if (target.adapterPosition >= 0) target.adapterPosition else 0)
        adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
      }

      override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

      }


    }).attachToRecyclerView(custom_sort_recyclerView)

    custom_sort_recyclerView.setHasFixedSize(true)
    custom_sort_recyclerView.layoutManager = LinearLayoutManager(this)
    custom_sort_recyclerView.itemAnimator = DefaultItemAnimator()
    custom_sort_recyclerView.adapter = adapter


    val accentColor = ThemeStore.getAccentColor()
    custom_sort_recyclerView.setBubbleColor(accentColor)
    custom_sort_recyclerView.setHandleColor(accentColor)
    custom_sort_recyclerView.setBubbleTextColor(resources.getColor(if (isColorLight(accentColor))
      R.color.light_text_color_primary
    else
      R.color.dark_text_color_primary))

    TintHelper.setTintAuto(findViewById<FloatingActionButton>(R.id.custom_sort_save), accentColor, true)
  }

  @OnClick(R.id.custom_sort_save)
  fun onClick() {
    doAsync {
      uiThread {
        mdDialog.show()
      }

      Thread.sleep(500)
      val result = DatabaseRepository.getInstance()
          .updatePlayListAudios(playlistId, songs.map { it.id })
          .blockingGet()

      uiThread {
        ToastUtil.show(mContext, if (result > 0) R.string.save_success else R.string.save_error)
        mdDialog.dismiss()
        finish()
      }
    }
  }

  companion object {
    @JvmStatic
    fun start(context: Context, id: Int, name: String, list: List<Song>) {
      context.startActivity(Intent(context, CustomSortActivity::class.java)
          .putExtra(EXTRA_ID, id)
          .putExtra(EXTRA_NAME, name)
          .putExtra(EXTRA_LIST, ArrayList(list)))
    }

    private const val EXTRA_ID = "id"
    private const val EXTRA_NAME = "name"
    private const val EXTRA_LIST = "list"
  }

}

