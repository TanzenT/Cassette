package tanzent.cassette.helper

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import tanzent.cassette.util.SPUtil
import tanzent.cassette.util.SPUtil.SETTING_KEY

object LanguageHelper {
  const val AUTO = 0
  const val KOREAN = 1
  const val ENGLISH = 2

  var sLocal: Locale = Locale.getDefault()

  private val TAG = "LanguageHelper"

//  fun getSelectLanguage(context: Context): String {
//    return when (SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, AUTO)) {
//      AUTO -> context.getString(R.string.auto)
//      KOREAN -> context.getString(R.string.ko)
//      ENGLISH -> context.getString(R.string.english)
//      else -> context.getString(R.string.ko)
//    }
//  }

  /**
   * 获取选择的语言设置
   */
  private fun getSetLanguageLocale(context: Context): Locale? {
    return when (SPUtil.getValue(context, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, AUTO)) {
      AUTO -> sLocal
      KOREAN -> Locale.KOREA
      ENGLISH -> Locale.ENGLISH
      else -> sLocal
    }
  }

  @JvmStatic
  fun saveSelectLanguage(context: Context, select: Int) {
    SPUtil.putValue(context, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, select)
    setApplicationLanguage(context)
  }

  @JvmStatic
  fun setLocal(context: Context): Context {
    return updateResources(context, getSetLanguageLocale(context))
  }

  private fun updateResources(context: Context, locale: Locale?): Context {
    Locale.setDefault(locale)

    val res = context.resources
    val config = Configuration(res.configuration)
    return if (Build.VERSION.SDK_INT >= 17) {
      config.setLocale(locale)
      context.createConfigurationContext(config)
    } else {
      config.locale = locale
      res.updateConfiguration(config, res.displayMetrics)
      context
    }
  }

  /**
   * 设置语言类型
   */
  @JvmStatic
  fun setApplicationLanguage(context: Context) {
    val resources = context.applicationContext.resources
    val dm = resources.displayMetrics
    val config = resources.configuration
    val locale = getSetLanguageLocale(context)
    config.locale = locale
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val localeList = LocaleList(locale!!)
      LocaleList.setDefault(localeList)
      config.locales = localeList
      context.applicationContext.createConfigurationContext(config)
      Locale.setDefault(locale)
    }
    resources.updateConfiguration(config, dm)
  }

  @JvmStatic
  fun saveSystemCurrentLanguage() {
    val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      LocaleList.getDefault().get(0)
    } else {
      Locale.getDefault()
    }
    sLocal = locale
  }

  @JvmStatic
  fun onConfigurationChanged(context: Context) {
    saveSystemCurrentLanguage()
    setLocal(context)
    setApplicationLanguage(context)
  }
}
