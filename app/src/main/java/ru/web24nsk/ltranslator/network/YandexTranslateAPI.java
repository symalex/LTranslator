package ru.web24nsk.ltranslator.network;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import ru.web24nsk.ltranslator.R;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData.LangDetectData;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData.LangsData;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData.TranslateData;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData.YandexApiResult;

import retrofit.Call;
import retrofit.Callback;
import retrofit.GsonConverterFactory;
import retrofit.Response;
import retrofit.Retrofit;


public class YandexTranslateAPI
{
	private String mApiKey;
	private String mTranslateDirection = "ru"; // example: ru, en-ru;
	private String mDetectedDirection = "";
	private String mUiLang = "ru";
	private String mBaseUrl = "";
	private String mLastSourceText = "";
	private String mLastResultText = "";

	private final LinkedHashSet<YandexTranslateApiListener> mListeners;

	public void addApiListener(YandexTranslateApiListener listener)
	{
		mListeners.add(listener);
	}

	public void removeApiListener(YandexTranslateApiListener listener)
	{
		mListeners.remove(listener);
	}

	public String getApiKey()
	{
		return mApiKey;
	}

	public void setApiKey(String apiKey)
	{
		mApiKey = apiKey;
	}

	public String getUiLang()
	{
		return mUiLang;
	}

	public String getTranslateDirection()
	{
		return mTranslateDirection;
	}

	public void setTranslateDirection(String translateDirection)
	{
		mTranslateDirection = translateDirection;
	}

	public String getLastSourceText()
	{
		return mLastSourceText;
	}

	public String getLastResultText()
	{
		return mLastResultText;
	}

	public String getDetectedDirection()
	{
		return mDetectedDirection;
	}

	public void setDetectedDirection(String detectedDirection)
	{
		mDetectedDirection = detectedDirection;
	}

	public enum YandexTranslateApiAction
	{
		YANDEX_TRANSLATE_API_ACTION_LANGS,
		YANDEX_TRANSLATE_API_ACTION_DETECT_LANG,
		YANDEX_TRANSLATE_API_ACTION_TRANSLATE
	}

	public interface YandexTranslateApiListener
	{
		void onSupportedLangsUpdate(YandexTranslateAPI api, Set<String> dirs, Map<String, String> langs);

		void onDetectedLangUpdate(YandexTranslateAPI api, String detected_lang);

		void onTranslationUpdate(YandexTranslateAPI api, String detected_lang, String detected_dir, String text);

		void onHttpRequestResultError(YandexTranslateAPI api, int http_status_code, String message);
	}

	private Retrofit mRetrofit;
	private YandexApiRequest mService;

	private Call<? extends YandexApiResult> mSupportLangsCall;
	private Call<? extends YandexApiResult> mDetectLangCall;
	private Call<? extends YandexApiResult> mTranslateCall;

	private YandexTranslateAPI(Context ctx)
	{
		mListeners = new LinkedHashSet<>();
		mBaseUrl = ctx.getResources().getString(R.string.yandex_api_base_host_url);
		updateUiLang();
	}

	public static YandexTranslateAPI newInstance(Context ctx)
	{
		return new YandexTranslateAPI(ctx);
	}

	public void onDestroy()
	{
		freeService();
	}

	private void CancelAllTasks()
	{
		if (mSupportLangsCall != null)
		{
			mSupportLangsCall.cancel();
			mSupportLangsCall = null;
		}

		if (mDetectLangCall != null)
		{
			mDetectLangCall.cancel();
			mDetectLangCall = null;
		}

		if (mTranslateCall != null)
		{
			mTranslateCall.cancel();
			mTranslateCall = null;
		}
	}

	public void onSupportedLangsUpdate(YandexTranslateAPI api, Set<String> dirs, Map<String, String> langs)
	{
		for (YandexTranslateApiListener listener : mListeners)
		{
			if (listener != null)
			{
				listener.onSupportedLangsUpdate(this, dirs, langs);
			}
		}
		mSupportLangsCall = null;
		checkStopService();
	}

	public void onDetectedLangUpdate(YandexTranslateAPI api, String detected_lang)
	{
		for (YandexTranslateApiListener listener : mListeners)
		{
			if (listener != null)
			{
				listener.onDetectedLangUpdate(this, detected_lang);
			}
		}
		mDetectLangCall = null;
		checkStopService();
	}

	public void onTranslationUpdate(YandexTranslateAPI api, String detected_lang, String detected_dir, String text)
	{
		mLastResultText = text;
		mDetectedDirection = detected_dir;
		for (YandexTranslateApiListener listener : mListeners)
		{
			if (listener != null)
			{
				listener.onTranslationUpdate(this, detected_lang, detected_dir, text);
			}
		}
		mTranslateCall = null;
		checkStopService();
	}

	public void onHttpRequestResultError(YandexTranslateAPI api, int http_status_code, String err)
	{
		for (YandexTranslateApiListener listener : mListeners)
		{
			if (listener != null)
			{
				listener.onHttpRequestResultError(this, http_status_code, err);
			}
		}
		mTranslateCall = null;
		checkStopService();
	}

	public boolean isServiceBusy()
	{
		return getApiCallsCount() != 0;
	}

	private int getApiCallsCount()
	{
		int cnt = 0;

		for (YandexTranslateApiAction action : YandexTranslateApiAction.values())
		{
			if (isTaskInProgress(action))
			{
				cnt++;
			}
		}

		return cnt;
	}

	public boolean isTaskInProgress(YandexTranslateApiAction action)
	{
		boolean in_progress = false;
		switch (action)
		{
			case YANDEX_TRANSLATE_API_ACTION_LANGS:
				in_progress = mSupportLangsCall != null;
				break;
			case YANDEX_TRANSLATE_API_ACTION_DETECT_LANG:
				in_progress = mDetectLangCall != null;
				break;
			case YANDEX_TRANSLATE_API_ACTION_TRANSLATE:
				in_progress = mTranslateCall != null;
				break;
		}
		return in_progress;
	}

	private void createService()
	{
		if (getApiCallsCount() == 0)
		{

			OkHttpClient client = new OkHttpClient();
			client.setConnectTimeout(30, TimeUnit.SECONDS);
			client.setReadTimeout(30, TimeUnit.SECONDS);

			mRetrofit = new Retrofit.Builder()
					.baseUrl(mBaseUrl)
					.client(client)
					.addConverterFactory(GsonConverterFactory.create())
					.build();

			mService = mRetrofit.create(YandexApiRequest.class);
		}
	}

	private void checkStopService()
	{
		if (getApiCallsCount() == 0)
		{
			freeService();
		}
	}

	private void freeService()
	{
		CancelAllTasks();

		mService = null;
		mRetrofit = null;
	}

	@SuppressWarnings("unchecked")
	private void updateSupportedLangs()
	{
		createService();
		updateUiLang();

		mSupportLangsCall = mService.listLangs(mApiKey, mUiLang);
		((Call<LangsData>) mSupportLangsCall).enqueue(new Callback<LangsData>()
		{
			@Override
			public void onResponse(Response<LangsData> response, Retrofit retrofit)
			{
				if (response.isSuccess())
				{
					onSupportedLangsUpdate(YandexTranslateAPI.this, response.body().dirs, response.body().langs);
				}
				else
				{
					// error response, no access to resource?
					onHttpRequestResultError(YandexTranslateAPI.this, response.code(), response.message());
				}
			}

			@Override
			public void onFailure(Throwable t)
			{
				// something went completely south (like no internet connection)
				onHttpRequestResultError(YandexTranslateAPI.this, 0, t.getMessage());
			}
		});
	}

	@SuppressWarnings("unchecked")
	public void detectLang(String text)
	{
		createService();
		updateUiLang();

		mDetectLangCall = mService.detectLang(mApiKey, text);
		((Call<LangDetectData>) mDetectLangCall).enqueue(new Callback<LangDetectData>()
		{
			@Override
			public void onResponse(Response<LangDetectData> response, Retrofit retrofit)
			{
				if (response.isSuccess())
				{
					onDetectedLangUpdate(YandexTranslateAPI.this, response.body().lang);
				}
				else
				{
					onHttpRequestResultError(YandexTranslateAPI.this, response.code(), response.message());
				}
			}

			@Override
			public void onFailure(Throwable t)
			{
				// something went completely south (like no internet connection)
				onHttpRequestResultError(YandexTranslateAPI.this, 0, t.getMessage());
			}
		});

	}

	@SuppressWarnings("unchecked")
	public void translate(String text)
	{
		createService();
		updateUiLang();
		mLastSourceText = text;
		mDetectedDirection = "";

		//https://translate.yandex.net/api/v1.5/tr.json/translate?key=%s&text=%s&lang=%s&format=plain&options=1
		mTranslateCall = mService.translate(mApiKey, text, mTranslateDirection, "plain", 1);
		((Call<TranslateData>) mTranslateCall).enqueue(new Callback<TranslateData>()
		{
			@Override
			public void onResponse(Response<TranslateData> response, Retrofit retrofit)
			{
				if (response.isSuccess())
				{
					String res = response.body().text.toString().trim();
					if (res.length() > 1)
					{
						res = res.substring(1, res.length() - 1);
					}
					onTranslationUpdate(YandexTranslateAPI.this, response.body().detected.lang, response.body().lang, res);
				}
				else
				{
					onHttpRequestResultError(YandexTranslateAPI.this, response.code(), response.message());
				}
			}

			@Override
			public void onFailure(Throwable t)
			{
				// something went completely south (like no internet connection)
				onHttpRequestResultError(YandexTranslateAPI.this, 0, t.getMessage());
			}
		});

	}

	private void updateUiLang()
	{
		mUiLang = Locale.getDefault().getLanguage();
	}

	public void update()
	{
		updateSupportedLangs();
	}

}
