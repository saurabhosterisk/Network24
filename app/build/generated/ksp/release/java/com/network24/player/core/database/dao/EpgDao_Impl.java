package com.network24.player.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomDatabaseKt;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.network24.player.core.database.entity.EpgEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
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
public final class EpgDao_Impl implements EpgDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EpgEntity> __insertionAdapterOfEpgEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteForStream;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public EpgDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEpgEntity = new EntityInsertionAdapter<EpgEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `epg` (`id`,`streamId`,`epgChannelId`,`title`,`description`,`start`,`end`,`startTimestamp`,`stopTimestamp`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EpgEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindLong(2, entity.getStreamId());
        if (entity.getEpgChannelId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getEpgChannelId());
        }
        if (entity.getTitle() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTitle());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDescription());
        }
        if (entity.getStart() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getStart());
        }
        if (entity.getEnd() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getEnd());
        }
        if (entity.getStartTimestamp() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getStartTimestamp());
        }
        if (entity.getStopTimestamp() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getStopTimestamp());
        }
      }
    };
    this.__preparedStmtOfDeleteForStream = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM epg WHERE streamId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM epg";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<EpgEntity> items,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfEpgEntity.insert(items);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object replaceForStream(final int streamId, final List<EpgEntity> items,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> EpgDao.DefaultImpls.replaceForStream(EpgDao_Impl.this, streamId, items, __cont), $completion);
  }

  @Override
  public Object replaceAllEpgs(final List<EpgEntity> epgs,
      final Continuation<? super Unit> $completion) {
    return RoomDatabaseKt.withTransaction(__db, (__cont) -> EpgDao.DefaultImpls.replaceAllEpgs(EpgDao_Impl.this, epgs, __cont), $completion);
  }

  @Override
  public Object deleteForStream(final int streamId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteForStream.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, streamId);
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
          __preparedStmtOfDeleteForStream.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
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
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getByStream(final int streamId,
      final Continuation<? super List<EpgEntity>> $completion) {
    final String _sql = "SELECT * FROM epg WHERE streamId = ? ORDER BY startTimestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, streamId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EpgEntity>>() {
      @Override
      @NonNull
      public List<EpgEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStreamId = CursorUtil.getColumnIndexOrThrow(_cursor, "streamId");
          final int _cursorIndexOfEpgChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgChannelId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfStartTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimestamp");
          final int _cursorIndexOfStopTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "stopTimestamp");
          final List<EpgEntity> _result = new ArrayList<EpgEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EpgEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpStreamId;
            _tmpStreamId = _cursor.getInt(_cursorIndexOfStreamId);
            final String _tmpEpgChannelId;
            if (_cursor.isNull(_cursorIndexOfEpgChannelId)) {
              _tmpEpgChannelId = null;
            } else {
              _tmpEpgChannelId = _cursor.getString(_cursorIndexOfEpgChannelId);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpStart;
            if (_cursor.isNull(_cursorIndexOfStart)) {
              _tmpStart = null;
            } else {
              _tmpStart = _cursor.getString(_cursorIndexOfStart);
            }
            final String _tmpEnd;
            if (_cursor.isNull(_cursorIndexOfEnd)) {
              _tmpEnd = null;
            } else {
              _tmpEnd = _cursor.getString(_cursorIndexOfEnd);
            }
            final Long _tmpStartTimestamp;
            if (_cursor.isNull(_cursorIndexOfStartTimestamp)) {
              _tmpStartTimestamp = null;
            } else {
              _tmpStartTimestamp = _cursor.getLong(_cursorIndexOfStartTimestamp);
            }
            final Long _tmpStopTimestamp;
            if (_cursor.isNull(_cursorIndexOfStopTimestamp)) {
              _tmpStopTimestamp = null;
            } else {
              _tmpStopTimestamp = _cursor.getLong(_cursorIndexOfStopTimestamp);
            }
            _item = new EpgEntity(_tmpId,_tmpStreamId,_tmpEpgChannelId,_tmpTitle,_tmpDescription,_tmpStart,_tmpEnd,_tmpStartTimestamp,_tmpStopTimestamp);
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
  public Object getByEpgChannelId(final String epgChannelId,
      final Continuation<? super List<EpgEntity>> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM epg\n"
            + "        WHERE epgChannelId = ?\n"
            + "          AND startTimestamp IS NOT NULL\n"
            + "        ORDER BY startTimestamp ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, epgChannelId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EpgEntity>>() {
      @Override
      @NonNull
      public List<EpgEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStreamId = CursorUtil.getColumnIndexOrThrow(_cursor, "streamId");
          final int _cursorIndexOfEpgChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgChannelId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfStartTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimestamp");
          final int _cursorIndexOfStopTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "stopTimestamp");
          final List<EpgEntity> _result = new ArrayList<EpgEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EpgEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpStreamId;
            _tmpStreamId = _cursor.getInt(_cursorIndexOfStreamId);
            final String _tmpEpgChannelId;
            if (_cursor.isNull(_cursorIndexOfEpgChannelId)) {
              _tmpEpgChannelId = null;
            } else {
              _tmpEpgChannelId = _cursor.getString(_cursorIndexOfEpgChannelId);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpStart;
            if (_cursor.isNull(_cursorIndexOfStart)) {
              _tmpStart = null;
            } else {
              _tmpStart = _cursor.getString(_cursorIndexOfStart);
            }
            final String _tmpEnd;
            if (_cursor.isNull(_cursorIndexOfEnd)) {
              _tmpEnd = null;
            } else {
              _tmpEnd = _cursor.getString(_cursorIndexOfEnd);
            }
            final Long _tmpStartTimestamp;
            if (_cursor.isNull(_cursorIndexOfStartTimestamp)) {
              _tmpStartTimestamp = null;
            } else {
              _tmpStartTimestamp = _cursor.getLong(_cursorIndexOfStartTimestamp);
            }
            final Long _tmpStopTimestamp;
            if (_cursor.isNull(_cursorIndexOfStopTimestamp)) {
              _tmpStopTimestamp = null;
            } else {
              _tmpStopTimestamp = _cursor.getLong(_cursorIndexOfStopTimestamp);
            }
            _item = new EpgEntity(_tmpId,_tmpStreamId,_tmpEpgChannelId,_tmpTitle,_tmpDescription,_tmpStart,_tmpEnd,_tmpStartTimestamp,_tmpStopTimestamp);
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
  public Object getNowByEpgChannelId(final String epgChannelId, final long nowTs,
      final Continuation<? super EpgEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM epg\n"
            + "        WHERE epgChannelId = ?\n"
            + "          AND startTimestamp IS NOT NULL\n"
            + "          AND stopTimestamp IS NOT NULL\n"
            + "          AND startTimestamp <= ?\n"
            + "          AND stopTimestamp > ?\n"
            + "        ORDER BY startTimestamp DESC\n"
            + "        LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 3);
    int _argIndex = 1;
    _statement.bindString(_argIndex, epgChannelId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, nowTs);
    _argIndex = 3;
    _statement.bindLong(_argIndex, nowTs);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EpgEntity>() {
      @Override
      @Nullable
      public EpgEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStreamId = CursorUtil.getColumnIndexOrThrow(_cursor, "streamId");
          final int _cursorIndexOfEpgChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgChannelId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfStartTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimestamp");
          final int _cursorIndexOfStopTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "stopTimestamp");
          final EpgEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpStreamId;
            _tmpStreamId = _cursor.getInt(_cursorIndexOfStreamId);
            final String _tmpEpgChannelId;
            if (_cursor.isNull(_cursorIndexOfEpgChannelId)) {
              _tmpEpgChannelId = null;
            } else {
              _tmpEpgChannelId = _cursor.getString(_cursorIndexOfEpgChannelId);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpStart;
            if (_cursor.isNull(_cursorIndexOfStart)) {
              _tmpStart = null;
            } else {
              _tmpStart = _cursor.getString(_cursorIndexOfStart);
            }
            final String _tmpEnd;
            if (_cursor.isNull(_cursorIndexOfEnd)) {
              _tmpEnd = null;
            } else {
              _tmpEnd = _cursor.getString(_cursorIndexOfEnd);
            }
            final Long _tmpStartTimestamp;
            if (_cursor.isNull(_cursorIndexOfStartTimestamp)) {
              _tmpStartTimestamp = null;
            } else {
              _tmpStartTimestamp = _cursor.getLong(_cursorIndexOfStartTimestamp);
            }
            final Long _tmpStopTimestamp;
            if (_cursor.isNull(_cursorIndexOfStopTimestamp)) {
              _tmpStopTimestamp = null;
            } else {
              _tmpStopTimestamp = _cursor.getLong(_cursorIndexOfStopTimestamp);
            }
            _result = new EpgEntity(_tmpId,_tmpStreamId,_tmpEpgChannelId,_tmpTitle,_tmpDescription,_tmpStart,_tmpEnd,_tmpStartTimestamp,_tmpStopTimestamp);
          } else {
            _result = null;
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
  public Object getNextByEpgChannelId(final String epgChannelId, final long nowTs,
      final Continuation<? super EpgEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT * FROM epg\n"
            + "        WHERE epgChannelId = ?\n"
            + "          AND startTimestamp IS NOT NULL\n"
            + "          AND startTimestamp > ?\n"
            + "        ORDER BY startTimestamp ASC\n"
            + "        LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, epgChannelId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, nowTs);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EpgEntity>() {
      @Override
      @Nullable
      public EpgEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStreamId = CursorUtil.getColumnIndexOrThrow(_cursor, "streamId");
          final int _cursorIndexOfEpgChannelId = CursorUtil.getColumnIndexOrThrow(_cursor, "epgChannelId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfStart = CursorUtil.getColumnIndexOrThrow(_cursor, "start");
          final int _cursorIndexOfEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "end");
          final int _cursorIndexOfStartTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "startTimestamp");
          final int _cursorIndexOfStopTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "stopTimestamp");
          final EpgEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final int _tmpStreamId;
            _tmpStreamId = _cursor.getInt(_cursorIndexOfStreamId);
            final String _tmpEpgChannelId;
            if (_cursor.isNull(_cursorIndexOfEpgChannelId)) {
              _tmpEpgChannelId = null;
            } else {
              _tmpEpgChannelId = _cursor.getString(_cursorIndexOfEpgChannelId);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final String _tmpStart;
            if (_cursor.isNull(_cursorIndexOfStart)) {
              _tmpStart = null;
            } else {
              _tmpStart = _cursor.getString(_cursorIndexOfStart);
            }
            final String _tmpEnd;
            if (_cursor.isNull(_cursorIndexOfEnd)) {
              _tmpEnd = null;
            } else {
              _tmpEnd = _cursor.getString(_cursorIndexOfEnd);
            }
            final Long _tmpStartTimestamp;
            if (_cursor.isNull(_cursorIndexOfStartTimestamp)) {
              _tmpStartTimestamp = null;
            } else {
              _tmpStartTimestamp = _cursor.getLong(_cursorIndexOfStartTimestamp);
            }
            final Long _tmpStopTimestamp;
            if (_cursor.isNull(_cursorIndexOfStopTimestamp)) {
              _tmpStopTimestamp = null;
            } else {
              _tmpStopTimestamp = _cursor.getLong(_cursorIndexOfStopTimestamp);
            }
            _result = new EpgEntity(_tmpId,_tmpStreamId,_tmpEpgChannelId,_tmpTitle,_tmpDescription,_tmpStart,_tmpEnd,_tmpStartTimestamp,_tmpStopTimestamp);
          } else {
            _result = null;
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
