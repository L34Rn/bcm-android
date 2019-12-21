package com.bcm.messenger.common.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bcm.messenger.utility.Base64;
import com.google.protobuf.ByteString;

import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos;
import org.whispersystems.signalservice.internal.util.Util;

import java.io.IOException;

import kotlin.Pair;

@Deprecated
public class PushDatabase extends Database {

    private static final String TAG = PushDatabase.class.getSimpleName();

    public static final String TABLE_NAME = "push";
    public static final String ID = "_id";
    public static final String TYPE = "type";
    public static final String SOURCE = "source";
    public static final String DEVICE_ID = "device_id";
    public static final String LEGACY_MSG = "body";
    public static final String CONTENT = "content";
    public static final String TIMESTAMP = "timestamp";
    public static final String SOURCE_REG_ID = "source_registration_id";

    public static final String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" + ID + " INTEGER PRIMARY KEY, " +
            TYPE + " INTEGER, " + SOURCE + " TEXT, " + DEVICE_ID + " INTEGER, " + LEGACY_MSG + " TEXT, " + CONTENT + " TEXT, " + SOURCE_REG_ID + " INTEGER, " + TIMESTAMP + " INTEGER );";

   
    public static final String DROP_TABLE = "DROP TABLE " + TABLE_NAME;

    public PushDatabase(Context context, SQLiteOpenHelper databaseHelper) {
        super(context, databaseHelper);
    }

    public long insert(@NonNull SignalServiceProtos.Envelope envelope) {
        Optional<Long> messageId = find(envelope);

        if (messageId.isPresent()) {
            return messageId.get();
        } else {
            ContentValues values = new ContentValues();
            values.put(TYPE, envelope.getType().getNumber());
            values.put(SOURCE, envelope.getSource());
            values.put(DEVICE_ID, envelope.getSourceDevice());
            values.put(LEGACY_MSG, envelope.hasLegacyMessage() ? Base64.encodeBytes(envelope.getLegacyMessage().toByteArray()) : "");
            values.put(CONTENT, envelope.hasContent() ? Base64.encodeBytes(envelope.getContent().toByteArray()) : "");
            values.put(TIMESTAMP, envelope.getTimestamp());
            values.put(SOURCE_REG_ID, envelope.getSourceRegistration());

            return databaseHelper.getWritableDatabase().insert(TABLE_NAME, null, values);
        }
    }

    public SignalServiceProtos.Envelope get(long id) throws NoSuchMessageException {
        Cursor cursor = null;

        try {
            cursor = databaseHelper.getReadableDatabase().query(TABLE_NAME, null, ID_WHERE,
                    new String[]{String.valueOf(id)},
                    null, null, null);

            if (cursor != null && cursor.moveToNext()) {
                return envelopeFromCursor(cursor);
            }
        } catch (IOException e) {
            Log.w(TAG, e);
            throw new NoSuchMessageException(e);
        } finally {
            if (cursor != null)
                cursor.close();
        }

        throw new NoSuchMessageException("Not found");
    }

    public Cursor getPending() {
        return databaseHelper.getReadableDatabase().query(TABLE_NAME, null, null, null, null, null, null);
    }

    public void delete(long id) {
        databaseHelper.getWritableDatabase().delete(TABLE_NAME, ID_WHERE, new String[]{id + ""});
    }

    public Reader readerFor(Cursor cursor) {
        return new Reader(cursor);
    }

    private Optional<Long> find(SignalServiceProtos.Envelope envelope) {
        SQLiteDatabase database = databaseHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = database.query(TABLE_NAME, null, TYPE + " = ? AND " + SOURCE + " = ? AND " +
                            DEVICE_ID + " = ? AND " + LEGACY_MSG + " = ? AND " +
                            CONTENT + " = ? AND " + TIMESTAMP + " = ?",
                    new String[]{String.valueOf(envelope.getType()),
                            envelope.getSource(),
                            String.valueOf(envelope.getSourceDevice()),
                            envelope.hasLegacyMessage() ? Base64.encodeBytes(envelope.getLegacyMessage().toByteArray()) : "",
                            envelope.hasContent() ? Base64.encodeBytes(envelope.getContent().toByteArray()) : "",
                            String.valueOf(envelope.getTimestamp())},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                return Optional.of(cursor.getLong(cursor.getColumnIndexOrThrow(ID)));
            } else {
                return Optional.absent();
            }
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static class Reader {
        private final Cursor cursor;

        public Reader(Cursor cursor) {
            this.cursor = cursor;
        }

        public Pair<Long, SignalServiceProtos.Envelope> getNextEnvelop() {
            try {
                if (cursor == null || !cursor.moveToNext())
                    return null;

                long id = cursor.getLong(cursor.getColumnIndexOrThrow(ID));

                return new Pair<>(id, envelopeFromCursor(cursor));
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        public SignalServiceProtos.Envelope getNext() {
            try {
                if (cursor == null || !cursor.moveToNext())
                    return null;
                return envelopeFromCursor(cursor);
            } catch (IOException e) {
                throw new AssertionError(e);
            }
        }

        public void close() {
            this.cursor.close();
        }
    }

    private static SignalServiceProtos.Envelope envelopeFromCursor(Cursor cursor) throws IOException {
        if (cursor == null)
            return null;
        int type = cursor.getInt(cursor.getColumnIndexOrThrow(TYPE));
        String source = cursor.getString(cursor.getColumnIndexOrThrow(SOURCE));
        int deviceId = cursor.getInt(cursor.getColumnIndexOrThrow(DEVICE_ID));
        String legacyMessage = cursor.getString(cursor.getColumnIndexOrThrow(LEGACY_MSG));
        String content = cursor.getString(cursor.getColumnIndexOrThrow(CONTENT));
        long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(TIMESTAMP));
        int sourceRegId = cursor.getInt(cursor.getColumnIndexOrThrow(SOURCE_REG_ID));


        return SignalServiceProtos.Envelope.newBuilder()
                .setType(SignalServiceProtos.Envelope.Type.valueOf(type))
                .setSource(source)
                .setSourceDevice(deviceId)
                .setRelay("")
                .setTimestamp(timestamp)
                .setLegacyMessage(Util.isEmpty(legacyMessage) ? null : ByteString.copyFrom(Base64.decode(legacyMessage)))
                .setContent(Util.isEmpty(content) ? null : ByteString.copyFrom(Base64.decode(content)))
                .setSourceRegistration(sourceRegId)
                .build();

    }
}
