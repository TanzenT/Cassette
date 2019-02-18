package tanzent.cassette.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import com.google.gson.Gson
import tanzent.cassette.App
import tanzent.cassette.R
import tanzent.cassette.misc.cache.DiskCache
import tanzent.cassette.theme.Theme
import tanzent.cassette.ui.adapter.LyricPriorityAdapter
import tanzent.cassette.util.SPUtil
import tanzent.cassette.util.ToastUtil
import java.util.*

class LyricPriorityDialog : android.support.v4.app.DialogFragment() {
    companion object {
        @JvmStatic
        fun newInstance(): LyricPriorityDialog {
            return LyricPriorityDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = activity.layoutInflater.inflate(R.layout.dialog_lyric_priority, null)

        val adapter = LyricPriorityAdapter(activity, R.layout.item_lyric_priority)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(activity)
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
        }).attachToRecyclerView(recyclerView)

        recyclerView.adapter = adapter

        return Theme.getBaseDialog(activity)
                .title(R.string.lrc_priority)
                .customView(view, false)
                .positiveText(R.string.confirm)
                .negativeText(R.string.cancel)
                .onPositive { dialog, which ->
                    try {
                        DiskCache.getLrcDiskCache().delete()
                        DiskCache.init(App.getContext())
                        SPUtil.deleteFile(App.getContext(), SPUtil.LYRIC_KEY.NAME)
                        SPUtil.putValue(activity, SPUtil.LYRIC_KEY.NAME, SPUtil.LYRIC_KEY.PRIORITY_LYRIC,
                                Gson().toJson(adapter.datas))
                    } catch (e: Exception) {
                        ToastUtil.show(context, R.string.save_error_arg, e.message)
                    }

                }
                .onNegative { dialog, which ->
                    dismiss()
                }
                .build()
    }


}