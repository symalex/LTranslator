package ru.web24nsk.ltranslator.network;

import ru.web24nsk.ltranslator.network.YandexTranslateAPIData.LangDetectData;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData.LangsData;
import ru.web24nsk.ltranslator.network.YandexTranslateAPIData.TranslateData;

import retrofit.Call;
import retrofit.http.GET;
import retrofit.http.Query;

public interface YandexApiRequest
{
	//https://translate.yandex.net/api/v1.5/tr.json/getLangs?key=%s&ui=%s
	@GET("/api/v1.5/tr.json/getLangs")
	Call<LangsData> listLangs(
			@Query("key") String key,
			@Query("ui") String ui
	);

	//https://translate.yandex.net/api/v1.5/tr.json/detect?key=%s&text=%s
	@GET("/api/v1.5/tr.json/detect")
	Call<LangDetectData> detectLang(
			@Query("key") String key,
			@Query("text") String text
	);

	//https://translate.yandex.net/api/v1.5/tr.json/translate?key=%s&text=%s&lang=%s&format=plain&options=1
	@GET("/api/v1.5/tr.json/translate")
	Call<TranslateData> translate(
			@Query("key") String key,
			@Query("text") String text,
			@Query("lang") String direction,
			@Query("format") String format,
			@Query("options") int options
	);

}
