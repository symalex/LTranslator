package ru.web24nsk.ltranslator.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.IntentFilter;
import android.util.Log;

import ru.web24nsk.ltranslator.common.helper;
import ru.web24nsk.ltranslator.data.LocalDataBaseTask.LocalDataBaseListener;
import ru.web24nsk.ltranslator.network.InternetReceiver;
import ru.web24nsk.ltranslator.network.YandexTranslateAPI;
import ru.web24nsk.ltranslator.ui.activities.MainActivity;

public class DataProvider implements LocalDataBaseListener
{
	private final String TAG = "DataProvider";

	private MainActivity.FragmentPage mActivePage = MainActivity.FragmentPage.MAIN_FRAGMENT;
	private final Context mCtx;
	private boolean mReceiverRegistered = false;
	private boolean mDataLoaded = false;
	private int mProgress = 0;
	private int mProgressMax = 5;
	private SettingsProvider mSettings;
	private InternetReceiver mInternetReceiver;
	private YandexTranslateAPI mTranslateAPI;
	private LocalDataBase mLocalDataBase;
	private final LinkedHashSet<DataProviderListener> mListeners;
	private Set<Integer> mHistorySelections;
	private Set<Integer> mFavoriteSelections;
	private boolean mForceTextTranslateFlag = false;

	public interface DataProviderListener
	{
		void onLoadDataComplete();
	}

	private ArrayList<HistoryRow> mHistoryList;
	private ArrayList<FavoriteRow> mFavoriteList;

	public ArrayList<HistoryRow> getHistoryList()
	{
		return mHistoryList;
	}

	public ArrayList<FavoriteRow> getFavoriteList()
	{
		return mFavoriteList;
	}

	public SettingsProvider getSettings()
	{
		return mSettings;
	}

	public InternetReceiver getInternetReceiver()
	{
		return mInternetReceiver;
	}

	public YandexTranslateAPI getTranslateAPI()
	{
		return mTranslateAPI;
	}

	public void addDataProviderListener(DataProviderListener listener)
	{
		mListeners.add(listener);
	}

	public void removeDataProviderListener(DataProviderListener listener)
	{
		mListeners.remove(listener);
	}

	public MainActivity.FragmentPage getActivePage()
	{
		return mActivePage;
	}

	public void setActivePage(MainActivity.FragmentPage activePage)
	{
		mActivePage = activePage;
	}

	public LocalDataBase getLocalDataBase()
	{
		return mLocalDataBase;
	}

	public Set<Integer> getHistorySelections()
	{
		return mHistorySelections;
	}

	public void setHistorySelections(Set<Integer> historySelections)
	{
		mHistorySelections = historySelections;
	}

	public Set<Integer> getFavoriteSelections()
	{
		return mFavoriteSelections;
	}

	public void setFavoriteSelections(Set<Integer> favoriteSelections)
	{
		mFavoriteSelections = favoriteSelections;
	}
	
	public boolean isForceTextTranslateFlag()
	{
		return mForceTextTranslateFlag;
	}

	public void setForceTextTranslateFlag(boolean forceTextTranslateFlag)
	{
		mForceTextTranslateFlag = forceTextTranslateFlag;
	}

	private DataProvider(Context ctx)
	{
		mCtx = ctx;
		mListeners = new LinkedHashSet<>();
		mHistoryList = new ArrayList<>();
		mHistorySelections = new HashSet<>();
		mFavoriteList = new ArrayList<>();
		mFavoriteSelections = new HashSet<>();
	}

	public static DataProvider getInstance(Context ctx)
	{
		DataProvider data = new DataProvider(ctx);
		data.onCreate();
		return data;
	}

	private void onCreate()
	{
		// application settings
		mSettings = SettingsProvider.getInstance(mCtx);

		// handling internet connection state
		mInternetReceiver = new InternetReceiver();
		mInternetReceiver.setContext(mCtx);
		registerInternetReceiver();
		mInternetReceiver.updateConnectionState();

		// translate API
		mTranslateAPI = YandexTranslateAPI.newInstance(mCtx);
		mLocalDataBase = LocalDataBase.newInstance(mCtx);
		mLocalDataBase.addListener(this);
	}

	private void registerInternetReceiver()
	{
		if (!mReceiverRegistered && mCtx != null && mInternetReceiver != null)
		{
			IntentFilter filter = new IntentFilter(InternetReceiver.FILTER_ACTION);
			mCtx.registerReceiver(mInternetReceiver, filter);
			mReceiverRegistered = true;
		}
	}

	protected void unregisterInternetReceiver()
	{
		if (mReceiverRegistered && mCtx != null && mInternetReceiver != null)
		{
			mCtx.unregisterReceiver(mInternetReceiver);
			mReceiverRegistered = false;
		}
	}

	public void onDestroy()
	{
		if (mTranslateAPI != null)
		{
			mTranslateAPI.onDestroy();
		}

		if (mLocalDataBase != null)
		{
			mLocalDataBase.removeListener(this);
			mLocalDataBase.close();
		}
	}

	public boolean isDataLoaded()
	{
		return mDataLoaded;
	}

	public int getProgress()
	{
		mProgress++;
		int v = mProgress;
		if (v >= mProgressMax)
		{
			mDataLoaded = true;
		}
		return v;
	}

	public int getProgressMax()
	{
		return mProgressMax;
	}

	public void loadData()
	{
		mProgress = 0;
		mSettings.ReadSettings();
		mTranslateAPI.setApiKey(mSettings.getTranslateAPIData().getApiKey());
		mTranslateAPI.setTranslateDirection(mSettings.getTranslateAPIData().getTranslateDirection());

		for (DataProviderListener listener : mListeners)
		{
			if (listener != null)
			{
				listener.onLoadDataComplete();
			}
		}
	}

	public void saveData()
	{
		mSettings.WriteSettings();
	}

	public void openDatabase()
	{
		mLocalDataBase.open();
		try
		{
			mProgressMax = 5 + (int) mLocalDataBase.getDbHelper().getHistoryDAO().getHistoryRecordCount() + (int) mLocalDataBase.getDbHelper().getFavoriteDAO().getFavoriteRecordCount();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		mProgress += 1;
	}

	public void downloadApiData()
	{
		if (mInternetReceiver != null && mInternetReceiver.isConnectionOk())
		{
			mProgress += 1;
			mTranslateAPI.update();
		}
	}

	public void readFavoriteAndHistoryData()
	{
		mProgress += 1;
		mLocalDataBase.getFavoriteAndHistoryData();
	}

	public void readHistoryData()
	{
		mLocalDataBase.readHistory();
	}

	public void readFavoriteData()
	{
		mLocalDataBase.readFavorite();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onDBReadFavoriteComplete(LocalDataBaseTask task, List<FavoriteRow> list)
	{
		mProgress += 1;
		mFavoriteList = (ArrayList<FavoriteRow>) ((ArrayList<FavoriteRow>) list).clone();
	}

	@Override
	public void onDBAddHistoryComplete(LocalDataBaseTask task, HistoryRow row)
	{
	}

	@Override
	public void onDBDelHistoryComplete(LocalDataBaseTask task, int result)
	{
	}

	@Override
	public void onDBAddFavoriteComplete(LocalDataBaseTask task, FavoriteRow row)
	{
	}

	@Override
	public void onDBDelFavoriteComplete(LocalDataBaseTask task, int result)
	{
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onDBReadHistoryComplete(LocalDataBaseTask task, List<HistoryRow> list)
	{
		mProgress = mProgressMax;
		mHistoryList = (ArrayList<HistoryRow>) ((ArrayList<HistoryRow>) list).clone();
	}

}
