package ru.web24nsk.ltranslator.data;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.RawRowMapper;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedQuery;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

public class DataBaseHelper extends OrmLiteSqliteOpenHelper
{
	private static final String TAG = "DataBaseHelper";

	// Database Name
	public static final String DATABASE_NAME = "history.sqlite";

	// Database Version
	public static final int DATABASE_VERSION = 1;

	private HistoryDAO mHistoryDAO;
	private FavoriteDAO mFavoriteDAO;

	public static class HistoryDAO extends BaseDaoImpl<HistoryRow, Long>
	{
		protected HistoryDAO(ConnectionSource connectionSource, Class<HistoryRow> dataClass) throws SQLException
		{
			super(connectionSource, dataClass);
		}

		public List<HistoryRow> getAll(FavoriteDAO favoriteDAO) throws SQLException
		{
			QueryBuilder<HistoryRow, Long> builder = this.queryBuilder();

			String query = builder
					.selectRaw(
							String.format("`%s`.*, `%s`.`%s` as `%s`",
									HistoryRow.TABLE_NAME,
									FavoriteRow.TABLE_NAME,
									FavoriteRow.KEY_ID,
									HistoryRow.FAV_ID
							)
					)
					.leftJoin(favoriteDAO.queryBuilder())
					.prepareStatementString();

			RawRowMapper<HistoryRow> mapper = new RawRowMapper<HistoryRow>()
			{
				public HistoryRow mapRow(String[] columnNames, String[] resultColumns)
				{
					HistoryRow h = new HistoryRow();
					int i = 0;
					for (String name : columnNames)
					{
						String value = resultColumns[i];
						switch (name)
						{
							case HistoryRow.KEY_ID:
								h.setId(Long.parseLong(value));
								break;

							case HistoryRow.DATE_TIME:
								h.setDt(new Date(Long.parseLong(value)));
								break;

							case HistoryRow.DIRECTION:
								h.setDirection(value);
								break;

							case HistoryRow.DET_DIRECTION:
								h.setDetDirection(value);
								break;

							case HistoryRow.SOURCE:
								h.setSource(value);
								break;

							case HistoryRow.DEST:
								h.setDestination(value);
								break;

							case HistoryRow.WEIGHT:
								h.setWeight(Long.parseLong(value));
								break;

							case HistoryRow.FAV_ID:
								if (value != null)
								{
									h.setFavId(Long.parseLong(value));
								}
								break;
						}
						i++;
					}
					return h;
				}
			};

			return this.queryRaw(query, mapper).getResults();
		}

		public long getHistoryRecordCount()
		{
			long ret = 0;
			try
			{
				QueryBuilder<HistoryRow, Long> builder = this.queryBuilder();
				ret = builder.countOf();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			return ret;
		}

		public List<HistoryRow> getAll() throws SQLException
		{
			return this.queryForAll();
		}
	}

	public static class FavoriteDAO extends BaseDaoImpl<FavoriteRow, Long>
	{
		protected FavoriteDAO(ConnectionSource connectionSource, Class<FavoriteRow> dataClass) throws SQLException
		{
			super(connectionSource, dataClass);
		}

		public long getFavoriteRecordCount()
		{
			long ret = 0;
			try
			{
				QueryBuilder<FavoriteRow, Long> builder = this.queryBuilder();
				ret = builder.countOf();
			}
			catch (SQLException e)
			{
				e.printStackTrace();
			}
			return ret;
		}

		public long find_by_hist_id(long hist_id) throws SQLException
		{
			QueryBuilder<FavoriteRow, Long> builder = this.queryBuilder();
			PreparedQuery query = builder.where().eq(FavoriteRow.HIST_ID, hist_id).prepare();
			String[] r_arr = queryRaw(query.getStatement()).getFirstResult();
			return r_arr == null ? 0 : Long.valueOf(r_arr[0]);
		}

		public int delete_by_hist_id(long hist_id) throws SQLException
		{
			DeleteBuilder<FavoriteRow, Long> builder = this.deleteBuilder();
			builder.where().eq(FavoriteRow.HIST_ID, hist_id).prepare();
			return builder.delete();
		}

		public int insert_by_hist_id(long hist_id) throws SQLException
		{
			QueryBuilder<FavoriteRow, Long> builder = this.queryBuilder();
			PreparedQuery query = builder.where().eq(FavoriteRow.HIST_ID, hist_id).prepare();
			String[] r_arr = queryRaw(query.getStatement()).getFirstResult();
			if (r_arr == null)
			{
				queryRaw(String.format("INSERT INTO `%s` (`%s`) VALUES (%d)", FavoriteRow.TABLE_NAME, FavoriteRow.HIST_ID, hist_id));
			}
			return r_arr == null ? 0 : 1;
		}

		public List<FavoriteRow> getAll() throws SQLException
		{
			return this.queryForAll();
		}
	}

	public DataBaseHelper(Context context)
	{
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource)
	{
		try
		{
			TableUtils.createTable(connectionSource, HistoryRow.class);
			TableUtils.createTable(connectionSource, FavoriteRow.class);
		}
		catch (SQLException e)
		{
			Log.e(TAG, "error creating DB " + DATABASE_NAME);
			throw new RuntimeException(e);
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion)
	{
		try
		{
			switch (oldVersion)
			{
				case 0:
					// never execute
					TableUtils.dropTable(connectionSource, FavoriteRow.class, true);
					TableUtils.dropTable(connectionSource, HistoryRow.class, true);
					onCreate(db, connectionSource);

				case 1:
					// upgrade version 1 -> 2

				case 2:
					// upgrade version 2 -> 3

				case 3:

					break;
			}

		}
		catch (SQLException e)
		{
			Log.e(TAG, "error upgrading db " + DATABASE_NAME + "from ver " + oldVersion);
			throw new RuntimeException(e);
		}
	}

	public HistoryDAO getHistoryDAO() throws SQLException
	{
		if (mHistoryDAO == null)
		{
			mHistoryDAO = new HistoryDAO(getConnectionSource(), HistoryRow.class);
		}
		return mHistoryDAO;
	}

	public FavoriteDAO getFavoriteDAO() throws SQLException
	{
		if (mFavoriteDAO == null)
		{
			mFavoriteDAO = new FavoriteDAO(getConnectionSource(), FavoriteRow.class);
		}
		return mFavoriteDAO;
	}

	@Override
	public void close()
	{
		super.close();

		mFavoriteDAO = null;
		mHistoryDAO = null;
	}

}
