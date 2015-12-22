package ru.web24nsk.ltranslator.data;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.os.AsyncTask;
import android.util.Log;

import com.j256.ormlite.dao.CloseableWrappedIterable;

public class LocalDataBaseTask extends AsyncTask<Void, Integer, Object>
{
	private static final String TAG = "LocalDataBaseTask";

	public interface LocalDataBaseListener
	{
		void onDBReadHistoryComplete(LocalDataBaseTask task, List<HistoryRow> list);

		void onDBReadFavoriteComplete(LocalDataBaseTask task, List<FavoriteRow> list);

		void onDBAddHistoryComplete(LocalDataBaseTask task, HistoryRow row);

		void onDBDelHistoryComplete(LocalDataBaseTask task, int result);

		void onDBAddFavoriteComplete(LocalDataBaseTask task, FavoriteRow row);

		void onDBDelFavoriteComplete(LocalDataBaseTask task, int result);
	}

	public interface LocalDataBaseProgressListener
	{
		void onDbProgress(LocalDataBaseTask task, int current, int max);
	}

	public enum LocalDataBaseAction
	{
		DB_ACTION_NONE,
		DB_ACTION_READ_FAVORITE_DATA,
		DB_ACTION_WRITE_FAVORITE_DATA,
		DB_ACTION_READ_HISTORY_DATA,
		DB_ACTION_WRITE_HISTORY_DATA,
		DB_ACTION_ADD_HISTORY,
		DB_ACTION_DEL_HISTORY,
		DB_ACTION_ADD_FAVORITE,
		DB_ACTION_DEL_FAVORITE
	}

	private LocalDataBaseAction mAction = LocalDataBaseAction.DB_ACTION_NONE;
	private final DataBaseHelper mDbHelper;
	private final ContentValues mValues;

	private LocalDataBaseListener mListener;
	private LocalDataBaseProgressListener mProgressListener;

	public LocalDataBaseAction getAction()
	{
		return mAction;
	}

	public LocalDataBaseListener getListener()
	{
		return mListener;
	}

	public void setListener(LocalDataBaseListener listener)
	{
		mListener = listener;
	}

	public void setProgressListener(LocalDataBaseProgressListener progressListener)
	{
		mProgressListener = progressListener;
	}

	LocalDataBaseTask(DataBaseHelper helper)
	{
		mDbHelper = helper;
		mValues = new ContentValues();
	}

	public void getFavoriteData()
	{
		mAction = LocalDataBaseAction.DB_ACTION_READ_FAVORITE_DATA;
		execute();
	}

	public void getHistoryData()
	{
		mAction = LocalDataBaseAction.DB_ACTION_READ_HISTORY_DATA;
		execute();
	}

	public void addToHistory(String direction, String src_text, String dest_text, String detected_direction)
	{
		mAction = LocalDataBaseAction.DB_ACTION_ADD_HISTORY;
		mValues.clear();
		mValues.put(HistoryRow.DIRECTION, direction);
		mValues.put(HistoryRow.SOURCE, src_text);
		mValues.put(HistoryRow.DEST, dest_text);
		mValues.put(HistoryRow.DET_DIRECTION, detected_direction);
		execute();
	}

	public void delFromHistory(long id)
	{
		mAction = LocalDataBaseAction.DB_ACTION_DEL_HISTORY;
		mValues.clear();
		mValues.put(HistoryRow.KEY_ID, id);
		execute();
	}

	public void addToFavorite(long hist_id)
	{
		mAction = LocalDataBaseAction.DB_ACTION_ADD_FAVORITE;
		mValues.clear();
		mValues.put(FavoriteRow.HIST_ID, hist_id);
		execute();
	}

	public void delFromFavorite(long id)
	{
		mAction = LocalDataBaseAction.DB_ACTION_DEL_FAVORITE;
		mValues.clear();
		mValues.put(FavoriteRow.KEY_ID, id);
		execute();
	}

	private <T> ArrayList<T> readRecordsWithProgress(CloseableWrappedIterable<T> iterable)
	{
		ArrayList<T> list = null;
		try
		{
			list = new ArrayList<>();
			int count = (int) mDbHelper.getFavoriteDAO().getFavoriteRecordCount();
			for (T row : iterable)
			{
				publishProgress(list.size(), count);
				if (row instanceof FavoriteRow)
				{
					((FavoriteRow) row).getHistory().setFavId(((FavoriteRow) row).getId());
				}
				list.add(row);
			}
			iterable.close();
			iterable = null;
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				if (iterable != null)
				{
					iterable.close();
				}
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
		}
		return list;
	}

	@Override
	protected Object doInBackground(Void... params)
	{
		Object ret = null;
		List<ContentValues> list = new ArrayList<>();
		switch (mAction)
		{
			case DB_ACTION_READ_FAVORITE_DATA:
				try
				{
					ret = readRecordsWithProgress(mDbHelper.getFavoriteDAO().getWrappedIterable());
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				break;

			case DB_ACTION_READ_HISTORY_DATA:
				try
				{
					ret = mDbHelper.getHistoryDAO().getAll(mDbHelper.getFavoriteDAO());
					publishProgress();
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				break;

			case DB_ACTION_ADD_HISTORY:
				try
				{
					HistoryRow r = new HistoryRow();
					r.setDirection(mValues.getAsString(HistoryRow.DIRECTION));
					r.setSource(mValues.getAsString(HistoryRow.SOURCE));
					r.setDestination(mValues.getAsString(HistoryRow.DEST));
					r.setDetDirection(mValues.getAsString(HistoryRow.DET_DIRECTION));
					r.now();
					mDbHelper.getHistoryDAO().create(r);
					ret = r;
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				break;

			case DB_ACTION_DEL_HISTORY:
				try
				{
					HistoryRow r = new HistoryRow();
					r.setId(mValues.getAsLong(HistoryRow.KEY_ID));
					mDbHelper.getHistoryDAO().refresh(r);
					mDbHelper.getHistoryDAO().delete(r);
					ret = mDbHelper.getHistoryDAO().deleteById(r.getId());
					ret = mDbHelper.getFavoriteDAO().delete_by_hist_id(r.getId());
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				break;

			case DB_ACTION_ADD_FAVORITE:
				try
				{
					mDbHelper.getFavoriteDAO().insert_by_hist_id(mValues.getAsLong(FavoriteRow.HIST_ID));
					long id = mDbHelper.getFavoriteDAO().find_by_hist_id(mValues.getAsLong(FavoriteRow.HIST_ID));

					FavoriteRow fr = new FavoriteRow();
					HistoryRow hr = new HistoryRow();
					fr.setId(id);
					fr.setHistory(hr);
					mDbHelper.getFavoriteDAO().refresh(fr);
					ret = fr;
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				break;

			case DB_ACTION_DEL_FAVORITE:
				try
				{
					ret = mDbHelper.getFavoriteDAO().deleteById(mValues.getAsLong(FavoriteRow.KEY_ID));
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				break;

		}
		return ret;
	}

	@Override
	protected void onProgressUpdate(Integer... values)
	{
		super.onProgressUpdate(values);

		if (mProgressListener != null && values.length >= 2)
		{
			mProgressListener.onDbProgress(this, values[0], values[1]);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onPostExecute(Object obj)
	{
		super.onPostExecute(obj);
		List<ContentValues> list;
		switch (mAction)
		{
			case DB_ACTION_READ_FAVORITE_DATA:
				if (mListener != null)
				{
					mListener.onDBReadFavoriteComplete(this, (List<FavoriteRow>) obj);
				}
				break;

			case DB_ACTION_READ_HISTORY_DATA:
				if (mListener != null)
				{
					mListener.onDBReadHistoryComplete(this, (List<HistoryRow>) obj);
				}
				break;

			case DB_ACTION_ADD_HISTORY:
				if (mListener != null)
				{
					mListener.onDBAddHistoryComplete(this, (HistoryRow) obj);
				}
				break;

			case DB_ACTION_DEL_HISTORY:
				if (mListener != null)
				{
					mListener.onDBDelHistoryComplete(this, (int) obj);
				}
				break;

			case DB_ACTION_ADD_FAVORITE:
				if (mListener != null && obj != null)
				{
					mListener.onDBAddFavoriteComplete(this, (FavoriteRow) obj);
				}
				break;

			case DB_ACTION_DEL_FAVORITE:
				if (mListener != null)
				{
					mListener.onDBDelFavoriteComplete(this, (int) obj);
				}
				break;

		}

	}

}
