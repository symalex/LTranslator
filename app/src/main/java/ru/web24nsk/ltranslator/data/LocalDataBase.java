package ru.web24nsk.ltranslator.data;

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;

import ru.web24nsk.ltranslator.data.LocalDataBaseTask.LocalDataBaseListener;

// http://startandroid.ru/ru/uroki/vse-uroki-spiskom/112-urok-53-simplecursortreeadapter-primer-ispolzovanija.html

public class LocalDataBase implements LocalDataBaseListener
{
	// Database Name
	private static final String DATABASE_NAME = "history.db";

	// Database Version
	private static final int DATABASE_VERSION = 1;

	private static final String ASYNC_ACTION = "action";

	private enum RuningAction
	{
		RA_READ_FAVORITE,
		RA_READ_HISTORY,
		RA_ADD_HISTORY,
		RA_DEL_HISTORY,
		RA_ADD_FAVORITE,
		RA_DEL_FAVORITE
	}

	private final Context mCtx;
	private DataBaseHelper mDbHelper;
	private final LinkedHashSet<LocalDataBaseListener> mListeners;

	private LocalDataBaseTask mTask;
	private final LinkedList<ContentValues> mActions;

	public DataBaseHelper getDbHelper()
	{
		return mDbHelper;
	}

	public boolean isListenerSet(LocalDataBaseListener listener)
	{
		return mListeners.contains(listener);
	}

	public void addListener(LocalDataBaseListener listener)
	{
		mListeners.add(listener);
	}

	public void removeListener(LocalDataBaseListener listener)
	{
		mListeners.remove(listener);
	}

	private LocalDataBase(Context ctx)
	{
		mCtx = ctx;
		mActions = new LinkedList<>();
		mListeners = new LinkedHashSet<>();
	}

	public static LocalDataBase newInstance(Context ctx)
	{
		return new LocalDataBase(ctx);
	}

	public void open()
	{
		mDbHelper = new DataBaseHelper(mCtx);
	}

	public void close()
	{
		if (mDbHelper != null)
		{
			mDbHelper.close();
			mDbHelper = null;
		}
	}

	private void cancelTask(LocalDataBaseTask task)
	{
		if (task != null)
		{
			task.cancel(true);
		}
	}

	private void startNextAction(ContentValues action)
	{
		if (action != null)
		{
			mActions.addLast(action);
		}

		if (mActions.size() > 0)
		{
			if (mTask == null)
			{
				action = mActions.removeFirst();
				run(action);
			}
		}
		else
		{
			// run action
			run(action);
		}
	}

	private void run(ContentValues cv)
	{
		if (mTask == null && cv != null)
		{
			// get action
			switch (RuningAction.values()[(int) cv.get(ASYNC_ACTION)])
			{
				case RA_READ_FAVORITE:
					cancelTask(mTask);
					mTask = new LocalDataBaseTask(mDbHelper);
					mTask.setListener(this);
					mTask.getFavoriteData();
					break;

				case RA_READ_HISTORY:
					cancelTask(mTask);
					mTask = new LocalDataBaseTask(mDbHelper);
					mTask.setListener(this);
					mTask.getHistoryData();
					break;

				case RA_ADD_HISTORY:
					cancelTask(mTask);
					mTask = new LocalDataBaseTask(mDbHelper);
					mTask.setListener(this);
					mTask.addToHistory(
							(String) cv.get(HistoryRow.DIRECTION),
							(String) cv.get(HistoryRow.SOURCE),
							(String) cv.get(HistoryRow.DEST),
							(String) cv.get(HistoryRow.DET_DIRECTION)
					);
					break;

				case RA_DEL_HISTORY:
					cancelTask(mTask);
					mTask = new LocalDataBaseTask(mDbHelper);
					mTask.setListener(this);
					mTask.delFromHistory(cv.getAsLong(HistoryRow.KEY_ID));
					break;

				case RA_ADD_FAVORITE:
					cancelTask(mTask);
					mTask = new LocalDataBaseTask(mDbHelper);
					mTask.setListener(this);
					mTask.addToFavorite(cv.getAsLong(FavoriteRow.HIST_ID));
					break;

				case RA_DEL_FAVORITE:
					cancelTask(mTask);
					mTask = new LocalDataBaseTask(mDbHelper);
					mTask.setListener(this);
					mTask.delFromFavorite(cv.getAsLong(FavoriteRow.KEY_ID));
					break;
			}
		}
	}

	public void addToFavorite(long hist_id)
	{
		ContentValues cv = new ContentValues();
		cv.put(ASYNC_ACTION, RuningAction.RA_ADD_FAVORITE.ordinal());
		cv.put(FavoriteRow.HIST_ID, hist_id);
		startNextAction(cv);
	}

	public void delFromFavorite(long id)
	{
		ContentValues cv = new ContentValues();
		cv.put(ASYNC_ACTION, RuningAction.RA_DEL_FAVORITE.ordinal());
		cv.put(FavoriteRow.KEY_ID, id);
		startNextAction(cv);
	}

	public void addToHistory(String direction, String src_text, String dest_text, String detected_direction)
	{
		ContentValues cv = new ContentValues();
		cv.put(ASYNC_ACTION, RuningAction.RA_ADD_HISTORY.ordinal());
		cv.put(HistoryRow.DIRECTION, direction);
		cv.put(HistoryRow.SOURCE, src_text);
		cv.put(HistoryRow.DEST, dest_text);
		cv.put(HistoryRow.DET_DIRECTION, detected_direction);
		startNextAction(cv);
	}

	public void delFromHistory(long id)
	{
		ContentValues cv = new ContentValues();
		cv.put(ASYNC_ACTION, RuningAction.RA_DEL_HISTORY.ordinal());
		cv.put(HistoryRow.KEY_ID, id);
		startNextAction(cv);
	}

	public void getFavoriteAndHistoryData()
	{
		readFavorite();
		readHistory();
	}

	public void readFavorite()
	{
		ContentValues cv = new ContentValues();
		cv.put(ASYNC_ACTION, RuningAction.RA_READ_FAVORITE.ordinal());
		startNextAction(cv);
	}

	public void readHistory()
	{
		ContentValues cv = new ContentValues();
		cv.put(ASYNC_ACTION, RuningAction.RA_READ_HISTORY.ordinal());
		startNextAction(cv);
	}

	@SuppressWarnings("unchecked")
	private void notifyAll(RuningAction action, LocalDataBaseTask task, Object list)
	{
		mTask = null;
		for (LocalDataBaseListener listener : mListeners)
		{
			if (listener != null)
			{
				switch (action)
				{
					case RA_READ_FAVORITE:
						listener.onDBReadFavoriteComplete(task, (List<FavoriteRow>) list);
						break;

					case RA_READ_HISTORY:
						listener.onDBReadHistoryComplete(task, (List<HistoryRow>) list);
						break;

					case RA_ADD_HISTORY:
						listener.onDBAddHistoryComplete(task, (HistoryRow) list);
						getFavoriteAndHistoryData();
						break;

					case RA_DEL_HISTORY:
						listener.onDBDelHistoryComplete(task, (int) list);
						getFavoriteAndHistoryData();
						break;

					case RA_ADD_FAVORITE:
						listener.onDBAddFavoriteComplete(task, (FavoriteRow) list);
						getFavoriteAndHistoryData();
						break;

					case RA_DEL_FAVORITE:
						listener.onDBDelFavoriteComplete(task, (int) list);
						getFavoriteAndHistoryData();
						break;
				}
			}
		}
		startNextAction(null);
	}

	@Override
	public void onDBReadFavoriteComplete(LocalDataBaseTask task, List<FavoriteRow> list)
	{
		notifyAll(RuningAction.RA_READ_FAVORITE, task, list);
	}

	@Override
	public void onDBReadHistoryComplete(LocalDataBaseTask task, List<HistoryRow> list)
	{
		notifyAll(RuningAction.RA_READ_HISTORY, task, list);
	}

	@Override
	public void onDBAddHistoryComplete(LocalDataBaseTask task, HistoryRow row)
	{
		notifyAll(RuningAction.RA_ADD_HISTORY, task, row);
	}

	@Override
	public void onDBDelHistoryComplete(LocalDataBaseTask task, int result)
	{
		notifyAll(RuningAction.RA_DEL_HISTORY, task, result);
	}

	@Override
	public void onDBAddFavoriteComplete(LocalDataBaseTask task, FavoriteRow row)
	{
		notifyAll(RuningAction.RA_ADD_FAVORITE, task, row);
	}

	@Override
	public void onDBDelFavoriteComplete(LocalDataBaseTask task, int result)
	{
		notifyAll(RuningAction.RA_DEL_FAVORITE, task, result);
	}

}
