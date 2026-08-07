package com.network24.player.core.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.network24.player.core.database.dao.CategoryDao;
import com.network24.player.core.database.dao.CategoryDao_Impl;
import com.network24.player.core.database.dao.ChannelDao;
import com.network24.player.core.database.dao.ChannelDao_Impl;
import com.network24.player.core.database.dao.ContinueWatchingDao;
import com.network24.player.core.database.dao.ContinueWatchingDao_Impl;
import com.network24.player.core.database.dao.DownloadsDao;
import com.network24.player.core.database.dao.DownloadsDao_Impl;
import com.network24.player.core.database.dao.EpgDao;
import com.network24.player.core.database.dao.EpgDao_Impl;
import com.network24.player.core.database.dao.FavoritesDao;
import com.network24.player.core.database.dao.FavoritesDao_Impl;
import com.network24.player.core.database.dao.HistoryDao;
import com.network24.player.core.database.dao.HistoryDao_Impl;
import com.network24.player.core.database.dao.SyncMetaDao;
import com.network24.player.core.database.dao.SyncMetaDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile CategoryDao _categoryDao;

  private volatile ChannelDao _channelDao;

  private volatile EpgDao _epgDao;

  private volatile SyncMetaDao _syncMetaDao;

  private volatile FavoritesDao _favoritesDao;

  private volatile HistoryDao _historyDao;

  private volatile ContinueWatchingDao _continueWatchingDao;

  private volatile DownloadsDao _downloadsDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `categories` (`categoryId` TEXT NOT NULL, `name` TEXT NOT NULL, `parentId` INTEGER, `type` TEXT NOT NULL, PRIMARY KEY(`categoryId`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_type` ON `categories` (`type`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_categories_type_parentId` ON `categories` (`type`, `parentId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `channels` (`streamId` INTEGER NOT NULL, `name` TEXT, `categoryId` TEXT, `icon` TEXT, `streamType` TEXT, `epgChannelId` TEXT, `tvArchive` INTEGER, `tvArchiveDuration` INTEGER, `directSource` TEXT, `num` INTEGER, `added` TEXT, `customSid` TEXT, PRIMARY KEY(`streamId`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_categoryId` ON `channels` (`categoryId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_epgChannelId` ON `channels` (`epgChannelId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_channels_name` ON `channels` (`name`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `epg` (`id` TEXT NOT NULL, `streamId` INTEGER NOT NULL, `epgChannelId` TEXT, `title` TEXT, `description` TEXT, `start` TEXT, `end` TEXT, `startTimestamp` INTEGER, `stopTimestamp` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_epg_streamId` ON `epg` (`streamId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_epg_startTimestamp` ON `epg` (`startTimestamp`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_epg_epgChannelId` ON `epg` (`epgChannelId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_epg_epgChannelId_startTimestamp` ON `epg` (`epgChannelId`, `startTimestamp`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `history` (`key` TEXT NOT NULL, `itemType` TEXT NOT NULL, `itemId` TEXT NOT NULL, `lastPositionMs` INTEGER, `durationMs` INTEGER, `updatedAtMs` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_history_updatedAtMs` ON `history` (`updatedAtMs`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_history_itemType_itemId` ON `history` (`itemType`, `itemId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorites` (`key` TEXT NOT NULL, `itemType` TEXT NOT NULL, `itemId` TEXT NOT NULL, `createdAtMs` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_favorites_itemType_itemId` ON `favorites` (`itemType`, `itemId`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorites_createdAtMs` ON `favorites` (`createdAtMs`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `continue_watching` (`key` TEXT NOT NULL, `itemType` TEXT NOT NULL, `itemId` TEXT NOT NULL, `positionMs` INTEGER NOT NULL, `durationMs` INTEGER, `updatedAtMs` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_continue_watching_updatedAtMs` ON `continue_watching` (`updatedAtMs`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_continue_watching_itemType_itemId` ON `continue_watching` (`itemType`, `itemId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `downloads` (`key` TEXT NOT NULL, `itemType` TEXT NOT NULL, `itemId` TEXT NOT NULL, `localPath` TEXT NOT NULL, `status` TEXT NOT NULL, `progressPct` INTEGER NOT NULL, `createdAtMs` INTEGER NOT NULL, `updatedAtMs` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_status` ON `downloads` (`status`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_downloads_updatedAtMs` ON `downloads` (`updatedAtMs`)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_downloads_itemType_itemId` ON `downloads` (`itemType`, `itemId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sync_meta` (`key` TEXT NOT NULL, `lastSyncEpochMs` INTEGER NOT NULL, PRIMARY KEY(`key`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f05be6b20220e1da2fe4cf4ecaaa0385')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `categories`");
        db.execSQL("DROP TABLE IF EXISTS `channels`");
        db.execSQL("DROP TABLE IF EXISTS `epg`");
        db.execSQL("DROP TABLE IF EXISTS `history`");
        db.execSQL("DROP TABLE IF EXISTS `favorites`");
        db.execSQL("DROP TABLE IF EXISTS `continue_watching`");
        db.execSQL("DROP TABLE IF EXISTS `downloads`");
        db.execSQL("DROP TABLE IF EXISTS `sync_meta`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsCategories = new HashMap<String, TableInfo.Column>(4);
        _columnsCategories.put("categoryId", new TableInfo.Column("categoryId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("parentId", new TableInfo.Column("parentId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCategories.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCategories = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesCategories = new HashSet<TableInfo.Index>(2);
        _indicesCategories.add(new TableInfo.Index("index_categories_type", false, Arrays.asList("type"), Arrays.asList("ASC")));
        _indicesCategories.add(new TableInfo.Index("index_categories_type_parentId", false, Arrays.asList("type", "parentId"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoCategories = new TableInfo("categories", _columnsCategories, _foreignKeysCategories, _indicesCategories);
        final TableInfo _existingCategories = TableInfo.read(db, "categories");
        if (!_infoCategories.equals(_existingCategories)) {
          return new RoomOpenHelper.ValidationResult(false, "categories(com.network24.player.core.database.entity.CategoryEntity).\n"
                  + " Expected:\n" + _infoCategories + "\n"
                  + " Found:\n" + _existingCategories);
        }
        final HashMap<String, TableInfo.Column> _columnsChannels = new HashMap<String, TableInfo.Column>(12);
        _columnsChannels.put("streamId", new TableInfo.Column("streamId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("categoryId", new TableInfo.Column("categoryId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("icon", new TableInfo.Column("icon", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("streamType", new TableInfo.Column("streamType", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("epgChannelId", new TableInfo.Column("epgChannelId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("tvArchive", new TableInfo.Column("tvArchive", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("tvArchiveDuration", new TableInfo.Column("tvArchiveDuration", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("directSource", new TableInfo.Column("directSource", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("num", new TableInfo.Column("num", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("added", new TableInfo.Column("added", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChannels.put("customSid", new TableInfo.Column("customSid", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChannels = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChannels = new HashSet<TableInfo.Index>(3);
        _indicesChannels.add(new TableInfo.Index("index_channels_categoryId", false, Arrays.asList("categoryId"), Arrays.asList("ASC")));
        _indicesChannels.add(new TableInfo.Index("index_channels_epgChannelId", false, Arrays.asList("epgChannelId"), Arrays.asList("ASC")));
        _indicesChannels.add(new TableInfo.Index("index_channels_name", false, Arrays.asList("name"), Arrays.asList("ASC")));
        final TableInfo _infoChannels = new TableInfo("channels", _columnsChannels, _foreignKeysChannels, _indicesChannels);
        final TableInfo _existingChannels = TableInfo.read(db, "channels");
        if (!_infoChannels.equals(_existingChannels)) {
          return new RoomOpenHelper.ValidationResult(false, "channels(com.network24.player.core.database.entity.ChannelEntity).\n"
                  + " Expected:\n" + _infoChannels + "\n"
                  + " Found:\n" + _existingChannels);
        }
        final HashMap<String, TableInfo.Column> _columnsEpg = new HashMap<String, TableInfo.Column>(9);
        _columnsEpg.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpg.put("streamId", new TableInfo.Column("streamId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpg.put("epgChannelId", new TableInfo.Column("epgChannelId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpg.put("title", new TableInfo.Column("title", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpg.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpg.put("start", new TableInfo.Column("start", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpg.put("end", new TableInfo.Column("end", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpg.put("startTimestamp", new TableInfo.Column("startTimestamp", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEpg.put("stopTimestamp", new TableInfo.Column("stopTimestamp", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEpg = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEpg = new HashSet<TableInfo.Index>(4);
        _indicesEpg.add(new TableInfo.Index("index_epg_streamId", false, Arrays.asList("streamId"), Arrays.asList("ASC")));
        _indicesEpg.add(new TableInfo.Index("index_epg_startTimestamp", false, Arrays.asList("startTimestamp"), Arrays.asList("ASC")));
        _indicesEpg.add(new TableInfo.Index("index_epg_epgChannelId", false, Arrays.asList("epgChannelId"), Arrays.asList("ASC")));
        _indicesEpg.add(new TableInfo.Index("index_epg_epgChannelId_startTimestamp", false, Arrays.asList("epgChannelId", "startTimestamp"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoEpg = new TableInfo("epg", _columnsEpg, _foreignKeysEpg, _indicesEpg);
        final TableInfo _existingEpg = TableInfo.read(db, "epg");
        if (!_infoEpg.equals(_existingEpg)) {
          return new RoomOpenHelper.ValidationResult(false, "epg(com.network24.player.core.database.entity.EpgEntity).\n"
                  + " Expected:\n" + _infoEpg + "\n"
                  + " Found:\n" + _existingEpg);
        }
        final HashMap<String, TableInfo.Column> _columnsHistory = new HashMap<String, TableInfo.Column>(6);
        _columnsHistory.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("itemType", new TableInfo.Column("itemType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("itemId", new TableInfo.Column("itemId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("lastPositionMs", new TableInfo.Column("lastPositionMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHistory.put("updatedAtMs", new TableInfo.Column("updatedAtMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHistory = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHistory = new HashSet<TableInfo.Index>(2);
        _indicesHistory.add(new TableInfo.Index("index_history_updatedAtMs", false, Arrays.asList("updatedAtMs"), Arrays.asList("ASC")));
        _indicesHistory.add(new TableInfo.Index("index_history_itemType_itemId", true, Arrays.asList("itemType", "itemId"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoHistory = new TableInfo("history", _columnsHistory, _foreignKeysHistory, _indicesHistory);
        final TableInfo _existingHistory = TableInfo.read(db, "history");
        if (!_infoHistory.equals(_existingHistory)) {
          return new RoomOpenHelper.ValidationResult(false, "history(com.network24.player.core.database.entity.HistoryEntity).\n"
                  + " Expected:\n" + _infoHistory + "\n"
                  + " Found:\n" + _existingHistory);
        }
        final HashMap<String, TableInfo.Column> _columnsFavorites = new HashMap<String, TableInfo.Column>(4);
        _columnsFavorites.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("itemType", new TableInfo.Column("itemType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("itemId", new TableInfo.Column("itemId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavorites.put("createdAtMs", new TableInfo.Column("createdAtMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFavorites = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFavorites = new HashSet<TableInfo.Index>(2);
        _indicesFavorites.add(new TableInfo.Index("index_favorites_itemType_itemId", true, Arrays.asList("itemType", "itemId"), Arrays.asList("ASC", "ASC")));
        _indicesFavorites.add(new TableInfo.Index("index_favorites_createdAtMs", false, Arrays.asList("createdAtMs"), Arrays.asList("ASC")));
        final TableInfo _infoFavorites = new TableInfo("favorites", _columnsFavorites, _foreignKeysFavorites, _indicesFavorites);
        final TableInfo _existingFavorites = TableInfo.read(db, "favorites");
        if (!_infoFavorites.equals(_existingFavorites)) {
          return new RoomOpenHelper.ValidationResult(false, "favorites(com.network24.player.core.database.entity.FavoriteEntity).\n"
                  + " Expected:\n" + _infoFavorites + "\n"
                  + " Found:\n" + _existingFavorites);
        }
        final HashMap<String, TableInfo.Column> _columnsContinueWatching = new HashMap<String, TableInfo.Column>(6);
        _columnsContinueWatching.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContinueWatching.put("itemType", new TableInfo.Column("itemType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContinueWatching.put("itemId", new TableInfo.Column("itemId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContinueWatching.put("positionMs", new TableInfo.Column("positionMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContinueWatching.put("durationMs", new TableInfo.Column("durationMs", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsContinueWatching.put("updatedAtMs", new TableInfo.Column("updatedAtMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysContinueWatching = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesContinueWatching = new HashSet<TableInfo.Index>(2);
        _indicesContinueWatching.add(new TableInfo.Index("index_continue_watching_updatedAtMs", false, Arrays.asList("updatedAtMs"), Arrays.asList("ASC")));
        _indicesContinueWatching.add(new TableInfo.Index("index_continue_watching_itemType_itemId", true, Arrays.asList("itemType", "itemId"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoContinueWatching = new TableInfo("continue_watching", _columnsContinueWatching, _foreignKeysContinueWatching, _indicesContinueWatching);
        final TableInfo _existingContinueWatching = TableInfo.read(db, "continue_watching");
        if (!_infoContinueWatching.equals(_existingContinueWatching)) {
          return new RoomOpenHelper.ValidationResult(false, "continue_watching(com.network24.player.core.database.entity.ContinueWatchingEntity).\n"
                  + " Expected:\n" + _infoContinueWatching + "\n"
                  + " Found:\n" + _existingContinueWatching);
        }
        final HashMap<String, TableInfo.Column> _columnsDownloads = new HashMap<String, TableInfo.Column>(8);
        _columnsDownloads.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("itemType", new TableInfo.Column("itemType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("itemId", new TableInfo.Column("itemId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("localPath", new TableInfo.Column("localPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("progressPct", new TableInfo.Column("progressPct", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("createdAtMs", new TableInfo.Column("createdAtMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsDownloads.put("updatedAtMs", new TableInfo.Column("updatedAtMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysDownloads = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesDownloads = new HashSet<TableInfo.Index>(3);
        _indicesDownloads.add(new TableInfo.Index("index_downloads_status", false, Arrays.asList("status"), Arrays.asList("ASC")));
        _indicesDownloads.add(new TableInfo.Index("index_downloads_updatedAtMs", false, Arrays.asList("updatedAtMs"), Arrays.asList("ASC")));
        _indicesDownloads.add(new TableInfo.Index("index_downloads_itemType_itemId", true, Arrays.asList("itemType", "itemId"), Arrays.asList("ASC", "ASC")));
        final TableInfo _infoDownloads = new TableInfo("downloads", _columnsDownloads, _foreignKeysDownloads, _indicesDownloads);
        final TableInfo _existingDownloads = TableInfo.read(db, "downloads");
        if (!_infoDownloads.equals(_existingDownloads)) {
          return new RoomOpenHelper.ValidationResult(false, "downloads(com.network24.player.core.database.entity.DownloadEntity).\n"
                  + " Expected:\n" + _infoDownloads + "\n"
                  + " Found:\n" + _existingDownloads);
        }
        final HashMap<String, TableInfo.Column> _columnsSyncMeta = new HashMap<String, TableInfo.Column>(2);
        _columnsSyncMeta.put("key", new TableInfo.Column("key", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSyncMeta.put("lastSyncEpochMs", new TableInfo.Column("lastSyncEpochMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSyncMeta = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSyncMeta = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSyncMeta = new TableInfo("sync_meta", _columnsSyncMeta, _foreignKeysSyncMeta, _indicesSyncMeta);
        final TableInfo _existingSyncMeta = TableInfo.read(db, "sync_meta");
        if (!_infoSyncMeta.equals(_existingSyncMeta)) {
          return new RoomOpenHelper.ValidationResult(false, "sync_meta(com.network24.player.core.database.entity.SyncMetaEntity).\n"
                  + " Expected:\n" + _infoSyncMeta + "\n"
                  + " Found:\n" + _existingSyncMeta);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f05be6b20220e1da2fe4cf4ecaaa0385", "4a68da90452965df4f7c3240a5c936f6");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "categories","channels","epg","history","favorites","continue_watching","downloads","sync_meta");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `categories`");
      _db.execSQL("DELETE FROM `channels`");
      _db.execSQL("DELETE FROM `epg`");
      _db.execSQL("DELETE FROM `history`");
      _db.execSQL("DELETE FROM `favorites`");
      _db.execSQL("DELETE FROM `continue_watching`");
      _db.execSQL("DELETE FROM `downloads`");
      _db.execSQL("DELETE FROM `sync_meta`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(CategoryDao.class, CategoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ChannelDao.class, ChannelDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EpgDao.class, EpgDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SyncMetaDao.class, SyncMetaDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FavoritesDao.class, FavoritesDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HistoryDao.class, HistoryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ContinueWatchingDao.class, ContinueWatchingDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(DownloadsDao.class, DownloadsDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public CategoryDao categoryDao() {
    if (_categoryDao != null) {
      return _categoryDao;
    } else {
      synchronized(this) {
        if(_categoryDao == null) {
          _categoryDao = new CategoryDao_Impl(this);
        }
        return _categoryDao;
      }
    }
  }

  @Override
  public ChannelDao channelDao() {
    if (_channelDao != null) {
      return _channelDao;
    } else {
      synchronized(this) {
        if(_channelDao == null) {
          _channelDao = new ChannelDao_Impl(this);
        }
        return _channelDao;
      }
    }
  }

  @Override
  public EpgDao epgDao() {
    if (_epgDao != null) {
      return _epgDao;
    } else {
      synchronized(this) {
        if(_epgDao == null) {
          _epgDao = new EpgDao_Impl(this);
        }
        return _epgDao;
      }
    }
  }

  @Override
  public SyncMetaDao syncMetaDao() {
    if (_syncMetaDao != null) {
      return _syncMetaDao;
    } else {
      synchronized(this) {
        if(_syncMetaDao == null) {
          _syncMetaDao = new SyncMetaDao_Impl(this);
        }
        return _syncMetaDao;
      }
    }
  }

  @Override
  public FavoritesDao favoritesDao() {
    if (_favoritesDao != null) {
      return _favoritesDao;
    } else {
      synchronized(this) {
        if(_favoritesDao == null) {
          _favoritesDao = new FavoritesDao_Impl(this);
        }
        return _favoritesDao;
      }
    }
  }

  @Override
  public HistoryDao historyDao() {
    if (_historyDao != null) {
      return _historyDao;
    } else {
      synchronized(this) {
        if(_historyDao == null) {
          _historyDao = new HistoryDao_Impl(this);
        }
        return _historyDao;
      }
    }
  }

  @Override
  public ContinueWatchingDao continueWatchingDao() {
    if (_continueWatchingDao != null) {
      return _continueWatchingDao;
    } else {
      synchronized(this) {
        if(_continueWatchingDao == null) {
          _continueWatchingDao = new ContinueWatchingDao_Impl(this);
        }
        return _continueWatchingDao;
      }
    }
  }

  @Override
  public DownloadsDao downloadsDao() {
    if (_downloadsDao != null) {
      return _downloadsDao;
    } else {
      synchronized(this) {
        if(_downloadsDao == null) {
          _downloadsDao = new DownloadsDao_Impl(this);
        }
        return _downloadsDao;
      }
    }
  }
}
