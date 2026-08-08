package com.network24.player.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.network24.player.core.database.entity.ChannelEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ChannelDao_Impl implements ChannelDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChannelEntity> __insertionAdapterOfChannelEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  private final SharedSQLiteStatement __preparedStmtOfClearByCategory;

  public ChannelDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChannelEntity = new EntityInsertionAdapter<ChannelEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `channels` (`streamId`,`name`,`categoryId`,`icon`,`streamType`,`epgChannelId`,`tvArchive`,`tvArchiveDuration`,`directSource`,`num`,`added`,`customSid`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChannelEntity entity) {
        statement.bindLong(1, entity.getStreamId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getCategoryId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getCategoryId());
        }
        if (entity.getIcon() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getIcon());
        }
        if (entity.getStreamType() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getStreamType());
        }
        if (entity.getEpgChannelId() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getEpgChannelId());
        }
        if (entity.getTvArchive() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getTvArchive());
        }
        if (entity.getTvArchiveDuration() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getTvArchiveDuration());
        }
        if (entity.getDirectSource() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getDirectSource());
        }
        if (entity.getNum() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getNum());
        }
        if (entity.getAdded() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getAdded());
        }
        if (entity.getCustomSid() == null) {
          statement.bindNull(12);
        } else {
          statement.bindString(12, entity.getCustomSid());
        }
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM channels";
        return _query;
      }
    };
    this.__preparedStmtOfClearByCategory = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM channels WHERE categoryId = ?";
        return _query;
      }
    };
  }

  @Override
  public Object upsertAll(final List<ChannelEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChannelEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearByCategory(final String categoryId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearByCategory.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, categoryId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClearByCategory.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByCategory(final String categoryId,
      final Continuation<? super List<ChannelEntity>> $completion) {
    final String _sql = "SELECT * FROM channels WHERE categoryId = ? ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, categoryId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfStreamId = CursorUtil.getColumnIndexOrThrow(_cursor, "streamId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfStreamType = CursorUtil.getColumnIndexOrThrow(_cursor, "streamType");
          final int _cursorIndexOfEpgChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgChannelId");
          final int _cursorIndexOfTvArchive = CursorUtil.getColumnIndexOrThrow(_cursor, "tvArchive");
          final int _cursorIndexOfTvArchiveDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "tvArchiveDuration");
          final int _cursorIndexOfDirectSource = CursorUtil.getColumnIndexOrThrow(_cursor, "directSource");
          final int _cursorIndexOfNum = CursorUtil.getColumnIndexOrThrow(_cursor, "num");
          final int _cursorIndexOfAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "added");
          final int _cursorIndexOfCustomSid = CursorUtil.getColumnIndexOrThrow(_cursor, "customSid");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final int _tmpStreamId;
            _tmpStreamId = _cursor.getInt(_cursorIndexOfStreamId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final String _tmpStreamType;
            if (_cursor.isNull(_cursorIndexOfStreamType)) {
              _tmpStreamType = null;
            } else {
              _tmpStreamType = _cursor.getString(_cursorIndexOfStreamType);
            }
            final String _tmpEpgChannelId;
            if (_cursor.isNull(_cursorIndexOfEpgChannelId)) {
              _tmpEpgChannelId = null;
            } else {
              _tmpEpgChannelId = _cursor.getString(_cursorIndexOfEpgChannelId);
            }
            final Integer _tmpTvArchive;
            if (_cursor.isNull(_cursorIndexOfTvArchive)) {
              _tmpTvArchive = null;
            } else {
              _tmpTvArchive = _cursor.getInt(_cursorIndexOfTvArchive);
            }
            final Integer _tmpTvArchiveDuration;
            if (_cursor.isNull(_cursorIndexOfTvArchiveDuration)) {
              _tmpTvArchiveDuration = null;
            } else {
              _tmpTvArchiveDuration = _cursor.getInt(_cursorIndexOfTvArchiveDuration);
            }
            final String _tmpDirectSource;
            if (_cursor.isNull(_cursorIndexOfDirectSource)) {
              _tmpDirectSource = null;
            } else {
              _tmpDirectSource = _cursor.getString(_cursorIndexOfDirectSource);
            }
            final Integer _tmpNum;
            if (_cursor.isNull(_cursorIndexOfNum)) {
              _tmpNum = null;
            } else {
              _tmpNum = _cursor.getInt(_cursorIndexOfNum);
            }
            final String _tmpAdded;
            if (_cursor.isNull(_cursorIndexOfAdded)) {
              _tmpAdded = null;
            } else {
              _tmpAdded = _cursor.getString(_cursorIndexOfAdded);
            }
            final String _tmpCustomSid;
            if (_cursor.isNull(_cursorIndexOfCustomSid)) {
              _tmpCustomSid = null;
            } else {
              _tmpCustomSid = _cursor.getString(_cursorIndexOfCustomSid);
            }
            _item = new ChannelEntity(_tmpStreamId,_tmpName,_tmpCategoryId,_tmpIcon,_tmpStreamType,_tmpEpgChannelId,_tmpTvArchive,_tmpTvArchiveDuration,_tmpDirectSource,_tmpNum,_tmpAdded,_tmpCustomSid);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<ChannelEntity>> $completion) {
    final String _sql = "SELECT * FROM channels ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ChannelEntity>>() {
      @Override
      @NonNull
      public List<ChannelEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfStreamId = CursorUtil.getColumnIndexOrThrow(_cursor, "streamId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCategoryId = CursorUtil.getColumnIndexOrThrow(_cursor, "categoryId");
          final int _cursorIndexOfIcon = CursorUtil.getColumnIndexOrThrow(_cursor, "icon");
          final int _cursorIndexOfStreamType = CursorUtil.getColumnIndexOrThrow(_cursor, "streamType");
          final int _cursorIndexOfEpgChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgChannelId");
          final int _cursorIndexOfTvArchive = CursorUtil.getColumnIndexOrThrow(_cursor, "tvArchive");
          final int _cursorIndexOfTvArchiveDuration = CursorUtil.getColumnIndexOrThrow(_cursor, "tvArchiveDuration");
          final int _cursorIndexOfDirectSource = CursorUtil.getColumnIndexOrThrow(_cursor, "directSource");
          final int _cursorIndexOfNum = CursorUtil.getColumnIndexOrThrow(_cursor, "num");
          final int _cursorIndexOfAdded = CursorUtil.getColumnIndexOrThrow(_cursor, "added");
          final int _cursorIndexOfCustomSid = CursorUtil.getColumnIndexOrThrow(_cursor, "customSid");
          final List<ChannelEntity> _result = new ArrayList<ChannelEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChannelEntity _item;
            final int _tmpStreamId;
            _tmpStreamId = _cursor.getInt(_cursorIndexOfStreamId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpCategoryId;
            if (_cursor.isNull(_cursorIndexOfCategoryId)) {
              _tmpCategoryId = null;
            } else {
              _tmpCategoryId = _cursor.getString(_cursorIndexOfCategoryId);
            }
            final String _tmpIcon;
            if (_cursor.isNull(_cursorIndexOfIcon)) {
              _tmpIcon = null;
            } else {
              _tmpIcon = _cursor.getString(_cursorIndexOfIcon);
            }
            final String _tmpStreamType;
            if (_cursor.isNull(_cursorIndexOfStreamType)) {
              _tmpStreamType = null;
            } else {
              _tmpStreamType = _cursor.getString(_cursorIndexOfStreamType);
            }
            final String _tmpEpgChannelId;
            if (_cursor.isNull(_cursorIndexOfEpgChannelId)) {
              _tmpEpgChannelId = null;
            } else {
              _tmpEpgChannelId = _cursor.getString(_cursorIndexOfEpgChannelId);
            }
            final Integer _tmpTvArchive;
            if (_cursor.isNull(_cursorIndexOfTvArchive)) {
              _tmpTvArchive = null;
            } else {
              _tmpTvArchive = _cursor.getInt(_cursorIndexOfTvArchive);
            }
            final Integer _tmpTvArchiveDuration;
            if (_cursor.isNull(_cursorIndexOfTvArchiveDuration)) {
              _tmpTvArchiveDuration = null;
            } else {
              _tmpTvArchiveDuration = _cursor.getInt(_cursorIndexOfTvArchiveDuration);
            }
            final String _tmpDirectSource;
            if (_cursor.isNull(_cursorIndexOfDirectSource)) {
              _tmpDirectSource = null;
            } else {
              _tmpDirectSource = _cursor.getString(_cursorIndexOfDirectSource);
            }
            final Integer _tmpNum;
            if (_cursor.isNull(_cursorIndexOfNum)) {
              _tmpNum = null;
            } else {
              _tmpNum = _cursor.getInt(_cursorIndexOfNum);
            }
            final String _tmpAdded;
            if (_cursor.isNull(_cursorIndexOfAdded)) {
              _tmpAdded = null;
            } else {
              _tmpAdded = _cursor.getString(_cursorIndexOfAdded);
            }
            final String _tmpCustomSid;
            if (_cursor.isNull(_cursorIndexOfCustomSid)) {
              _tmpCustomSid = null;
            } else {
              _tmpCustomSid = _cursor.getString(_cursorIndexOfCustomSid);
            }
            _item = new ChannelEntity(_tmpStreamId,_tmpName,_tmpCategoryId,_tmpIcon,_tmpStreamType,_tmpEpgChannelId,_tmpTvArchive,_tmpTvArchiveDuration,_tmpDirectSource,_tmpNum,_tmpAdded,_tmpCustomSid);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
