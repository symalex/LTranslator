package ru.web24nsk.ltranslator.data;

import java.util.Date;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = HistoryRow.TABLE_NAME)
public class HistoryRow
{
	public static final String TABLE_NAME = "history";

	public static final String KEY_ID = "id";
	public static final String DATE_TIME = "dt";
	public static final String DIRECTION = "dir";
	public static final String DET_DIRECTION = "det_dir";
	public static final String SOURCE = "src";
	public static final String DEST = "dest";
	public static final String WEIGHT = "weight";
	public static final String FAV_ID = "fav_id";

	@DatabaseField(columnName = KEY_ID, generatedId = true)
	private long mId;

	@DatabaseField(columnName = DATE_TIME, dataType = DataType.DATE_LONG, format = "YYYY-MM-DD HH:MM:SS", canBeNull = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL")
	private Date mDt;

	@DatabaseField(columnName = DIRECTION, dataType = DataType.STRING, canBeNull = false, columnDefinition = "VARCHAR(5)")
	private String mDirection;

	@DatabaseField(columnName = DET_DIRECTION, dataType = DataType.STRING, canBeNull = false, columnDefinition = "VARCHAR(5) DEFAULT \"\"")
	private String mDetDirection;

	@DatabaseField(columnName = SOURCE, dataType = DataType.STRING, canBeNull = false, columnDefinition = "TEXT")
	private String mSource;

	@DatabaseField(columnName = DEST, dataType = DataType.STRING, canBeNull = false, columnDefinition = "TEXT")
	private String mDestination;

	@DatabaseField(columnName = WEIGHT, canBeNull = false, columnDefinition = "INTEGER DEFAULT 0")
	private long mWeight;

	@DatabaseField(columnName = FAV_ID, persisted = false, canBeNull = false)
	private long mFavId;

	public long getId()
	{
		return mId;
	}

	public void setId(long id)
	{
		mId = id;
	}

	public Date getDt()
	{
		return mDt;
	}

	public void setDt(Date dt)
	{
		mDt = dt;
	}

	public String getDirection()
	{
		return mDirection;
	}

	public void setDirection(String direction)
	{
		mDirection = direction;
	}

	public String getDetDirection()
	{
		return mDetDirection;
	}

	public void setDetDirection(String detDirection)
	{
		mDetDirection = detDirection;
	}

	public String getSource()
	{
		return mSource;
	}

	public void setSource(String source)
	{
		mSource = source;
	}

	public String getDestination()
	{
		return mDestination;
	}

	public void setDestination(String destination)
	{
		mDestination = destination;
	}

	public long getFavId()
	{
		return mFavId;
	}

	public void setFavId(long favId)
	{
		mFavId = favId;
	}

	public long getWeight()
	{
		return mWeight;
	}

	public void setWeight(long weight)
	{
		mWeight = weight;
	}

	public HistoryRow now()
	{
		java.util.Date date = new java.util.Date();
		mDt = new Date(date.getTime());
		return this;
	}

	@Override
	public String toString()
	{
		return String.format("{\"%s\"=\"%d\", \"%s\"=\"%s\", \"%s\"=\"%s\", \"%s\"=\"%s\", \"%s\"=\"%s\", \"%s\"=\"%d\", \"%s\"=\"%d\"}",
				KEY_ID, mId,
				DATE_TIME, mDt,
				DIRECTION, mDirection,
				SOURCE, mSource,
				DEST, mDestination,
				WEIGHT, mWeight,
				FAV_ID, mFavId
		);
	}

}
