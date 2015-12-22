package ru.web24nsk.ltranslator.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

import ru.web24nsk.ltranslator.MainApp;
import ru.web24nsk.ltranslator.R;
import ru.web24nsk.ltranslator.network.YandexTranslateAPI;
import ru.web24nsk.ltranslator.network.YandexTranslateAPI.YandexTranslateApiListener;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData;

import org.json.JSONObject;

public class SettingsProvider implements YandexTranslateApiListener
{
	private final Context mCtx;
	private final SharedPreferences mPref;

	private final YandexTranslateAPIData mTranslateAPIData;

	private static final String PREFS_NAME = "settings";

	private final String EDIT_TEXT_SOURCE = "edit_text_source";
	private final String EDIT_TEXT_DESTINATION = "edit_text_destination";
	private final String YANDEX_API_KEY_TEXT = "yandex_translate_key";
	private final String YANDEX_API_TRANSLATE_DIRECTION = "yandex_translate_direction";
	private final String YANDEX_API_SUPPORTED_DIRECTIONS = "yandex_translate_directions";
	private final String YANDEX_API_SUPPORTED_LANGS = "yandex_translate_langs";
	private final String YANDEX_API_DETECTED_DIR = "yandex_translate_detected_dir";
	private final String YANDEX_API_CUR_TEXT_IN_HISTORY = "yandex_translate_cur_text_in_history";

	public YandexTranslateAPIData getTranslateAPIData()
	{
		return mTranslateAPIData;
	}

	private SettingsProvider(Context ctx)
	{
		mCtx = ctx;
		mPref = mCtx.getSharedPreferences(PREFS_NAME, 0);
		mTranslateAPIData = new YandexTranslateAPIData();
	}

	public static SettingsProvider getInstance(Context ctx)
	{
		return new SettingsProvider(ctx);
	}

	void ReadSettings()
	{
		mTranslateAPIData.setApiDataInitialized(true);

		((MainApp) mCtx).getDataProvider().getTranslateAPI().addApiListener(this);

		mTranslateAPIData.setApiKey(mPref.getString(YANDEX_API_KEY_TEXT, mCtx.getResources().getString(R.string.yandex_api_default_key)));
		if (mTranslateAPIData.getApiKey().trim().length() == 0)
		{
			mTranslateAPIData.setApiKey(mCtx.getResources().getString(R.string.yandex_api_default_key));
		}
		String sys_lang = Locale.getDefault().getLanguage();
		if (!sys_lang.equals("ru") && !sys_lang.equals("en"))
		{
			sys_lang = YandexTranslateAPIData.DEFAULT_DEST_VALUE;
		}
		mTranslateAPIData.setTranslateDirection(mPref.getString(YANDEX_API_TRANSLATE_DIRECTION, sys_lang));

		// read directions
		mTranslateAPIData.setDirs(mPref.getStringSet(YANDEX_API_SUPPORTED_DIRECTIONS, new HashSet<String>()));

		mTranslateAPIData.getLangs().clear();
		try
		{
			// read supported langs
			JSONObject json = new JSONObject(mPref.getString(YANDEX_API_SUPPORTED_LANGS, (new JSONObject()).toString()));
			Iterator<String> key_iter = json.keys();
			while (key_iter.hasNext())
			{
				String key = key_iter.next();
				String value = (String) json.get(key);
				mTranslateAPIData.getLangs().put(key, value);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		mTranslateAPIData.setSrcText(mPref.getString(EDIT_TEXT_SOURCE, ""));
		mTranslateAPIData.setDestText(mPref.getString(EDIT_TEXT_DESTINATION, ""));
		mTranslateAPIData.setDetectedDirection(mPref.getString(YANDEX_API_DETECTED_DIR, ""));
		mTranslateAPIData.setCurrentTextInHistory(mPref.getBoolean(YANDEX_API_CUR_TEXT_IN_HISTORY, true));

	}

	void WriteSettings()
	{
		SharedPreferences.Editor ed = mPref.edit();

		ed.putString(YANDEX_API_KEY_TEXT, mTranslateAPIData.getApiKey());
		ed.putString(YANDEX_API_TRANSLATE_DIRECTION, mTranslateAPIData.getTranslateDirection());
		ed.putStringSet(YANDEX_API_SUPPORTED_DIRECTIONS, mTranslateAPIData.getDirs());

		ed.putString(YANDEX_API_SUPPORTED_LANGS, (new JSONObject(mTranslateAPIData.getLangs())).toString());

		ed.putString(EDIT_TEXT_SOURCE, mTranslateAPIData.getSrcText());
		ed.putString(EDIT_TEXT_DESTINATION, mTranslateAPIData.getDestText());
		ed.putString(YANDEX_API_DETECTED_DIR, mTranslateAPIData.getDetectedDirection());
		ed.putBoolean(YANDEX_API_CUR_TEXT_IN_HISTORY, mTranslateAPIData.isCurrentTextInHistory());

		ed.commit();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onSupportedLangsUpdate(YandexTranslateAPI api, Set<String> dirs, Map<String, String> langs)
	{
		mTranslateAPIData.setDirs((HashSet) ((HashSet) dirs).clone());
		mTranslateAPIData.setLangs((HashMap) ((HashMap) langs).clone());
	}

	@Override
	public void onDetectedLangUpdate(YandexTranslateAPI api, String detected_lang)
	{

	}

	@Override
	public void onTranslationUpdate(YandexTranslateAPI api, String detected_lang, String detected_dir, String text)
	{
		mTranslateAPIData.setDestText(text);
		mTranslateAPIData.setDetectedDirection(detected_dir);
		mTranslateAPIData.setDisableComparatorUpdateOnce(false);
	}

	@Override
	public void onHttpRequestResultError(YandexTranslateAPI api, int http_status_code, String message)
	{

	}

}
