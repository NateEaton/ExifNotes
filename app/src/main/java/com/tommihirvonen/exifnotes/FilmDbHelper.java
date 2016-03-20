package com.tommihirvonen.exifnotes;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;


// Copyright 2015
// Tommi Hirvonen

public class FilmDbHelper extends SQLiteOpenHelper {

    public static final String TABLE_FRAMES = "frames";
    public static final String TABLE_LENSES = "lenses";
    public static final String TABLE_ROLLS = "rolls";
    public static final String TABLE_CAMERAS = "cameras";
    public static final String TABLE_MOUNTABLES = "mountables";

    public static final String KEY_FRAME_ID = "frame_id";
    public static final String KEY_COUNT = "count";
    public static final String KEY_DATE = "date";
    public static final String KEY_SHUTTER = "shutter";
    public static final String KEY_APERTURE = "aperture";
    public static final String KEY_FRAME_NOTE = "frame_note";
    public static final String KEY_LOCATION = "location";

    public static final String KEY_LENS_ID = "lens_id";
    public static final String KEY_LENS_MAKE = "lens_make";
    public static final String KEY_LENS_MODEL = "lens_model";

    public static final String KEY_CAMERA_ID = "camera_id";
    public static final String KEY_CAMERA_MAKE = "camera_make";
    public static final String KEY_CAMERA_MODEL = "camera_model";

    public static final String KEY_ROLL_ID = "roll_id";
    public static final String KEY_ROLLNAME = "rollname";
    public static final String KEY_ROLL_DATE = "roll_date";
    public static final String KEY_ROLL_NOTE = "roll_note";

    private static final String DATABASE_NAME = "filmnotes.db";
    private static final int DATABASE_VERSION = 13;

    private static final String CREATE_FRAME_TABLE = "create table " + TABLE_FRAMES
            + "(" + KEY_FRAME_ID + " integer primary key autoincrement, "
            + KEY_ROLL_ID + " integer not null, "
            + KEY_COUNT + " integer not null, "
            + KEY_DATE + " text not null, "
            + KEY_LENS_ID + " integer not null, "
            + KEY_SHUTTER + " text not null, "
            + KEY_APERTURE + " text not null, "
            + KEY_FRAME_NOTE + " text, "
            + KEY_LOCATION + " text"
            + ");";
    private static final String CREATE_LENS_TABLE = "create table " + TABLE_LENSES
            + "(" + KEY_LENS_ID + " integer primary key autoincrement, "
            + KEY_LENS_MAKE + " text not null, "
            + KEY_LENS_MODEL + " text not null"
            + ");";
    private static final String CREATE_CAMERA_TABLE = "create table " + TABLE_CAMERAS
            + "(" + KEY_CAMERA_ID + " integer primary key autoincrement, "
            + KEY_CAMERA_MAKE + " text not null, "
            + KEY_CAMERA_MODEL + " text not null"
            + ");";
    private static final String CREATE_ROLL_TABLE = "create table " + TABLE_ROLLS
            + "(" + KEY_ROLL_ID + " integer primary key autoincrement, "
            + KEY_ROLLNAME + " text not null, "
            + KEY_ROLL_DATE + " text not null, "
            + KEY_ROLL_NOTE + " text, "
            + KEY_CAMERA_ID + " integer not null"
            + ");";
    private static final String CREATE_MOUNTABLES_TABLE = "create table " + TABLE_MOUNTABLES
            + "(" + KEY_CAMERA_ID + " integer not null, "
            + KEY_LENS_ID + " integer not null"
            + ");";

    public FilmDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(CREATE_FRAME_TABLE);
        database.execSQL(CREATE_LENS_TABLE);
        database.execSQL(CREATE_ROLL_TABLE);
        database.execSQL(CREATE_CAMERA_TABLE);
        database.execSQL(CREATE_MOUNTABLES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // TODO: When a new version of the app is being launched, make sure DROP TABLE is not used!

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FRAMES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LENSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROLLS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CAMERAS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MOUNTABLES);
        onCreate(db);
    }

    // ******************** CRUD operations for the frames table ********************

    public void addFrame(Frame frame) {
        frame.setShutter(frame.getShutter().replace("\"", "q"));
        // Get reference to writable database
        SQLiteDatabase db = this.getWritableDatabase();
        // Create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_ROLL_ID, frame.getRoll());
        values.put(KEY_COUNT, frame.getCount());
        values.put(KEY_DATE, frame.getDate());
        values.put(KEY_LENS_ID, frame.getLensId());
        values.put(KEY_SHUTTER, frame.getShutter());
        values.put(KEY_APERTURE, frame.getAperture());
        values.put(KEY_FRAME_NOTE, frame.getNote());
        values.put(KEY_LOCATION, frame.getLocation());
        // Insert
        db.insert(TABLE_FRAMES, // table
                null, // nullColumnHack
                values); // key/value -> keys = column names/ value
        // Close
        db.close();
    }

    public Frame getLastFrame(){
        Frame frame = new Frame();
        String query = "SELECT * FROM " + TABLE_FRAMES + " ORDER BY " + KEY_FRAME_ID + " DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        frame.setId(cursor.getInt(cursor.getColumnIndex(KEY_FRAME_ID)));
        frame.setRoll(cursor.getInt(cursor.getColumnIndex(KEY_ROLL_ID)));
        frame.setCount(cursor.getInt(cursor.getColumnIndex(KEY_COUNT)));
        frame.setDate(cursor.getString(cursor.getColumnIndex(KEY_DATE)));
        frame.setLensId(cursor.getInt(cursor.getColumnIndex(KEY_LENS_ID)));
        frame.setShutter(cursor.getString(cursor.getColumnIndex(KEY_SHUTTER)).replace("q", "\""));
        frame.setAperture(cursor.getString(cursor.getColumnIndex(KEY_APERTURE)));
        frame.setNote(cursor.getString(cursor.getColumnIndex(KEY_FRAME_NOTE)));
        frame.setLocation(cursor.getString(cursor.getColumnIndex(KEY_LOCATION)));
        cursor.close();
        return frame;
    }

    public ArrayList<Frame> getAllFramesFromRoll(int roll_id){
        ArrayList<Frame> frames = new ArrayList<>();
        // Build the query
        String query = "SELECT * FROM " + TABLE_FRAMES + " WHERE " + KEY_ROLL_ID + " = " + roll_id + " ORDER BY " + KEY_COUNT;
        // Get reference to readable database
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Frame frame;
        // Go over each row, build list
        while ( cursor.moveToNext() ) {
            frame = new Frame();
            frame.setId(cursor.getInt(0));
            frame.setRoll(cursor.getInt(1));
            frame.setCount(cursor.getInt(2));
            frame.setDate(cursor.getString(3));
            frame.setLensId(cursor.getInt(4));
            frame.setShutter(cursor.getString(5).replace("q", "\""));
            frame.setAperture(cursor.getString(6));
            frame.setNote(cursor.getString(7));
            frame.setLocation(cursor.getString(8));
            frames.add(frame);
        }
        cursor.close();
        return frames;
    }

    public void updateFrame(Frame frame) {
        // Get reference to writable database
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_FRAMES + " SET "
                + KEY_ROLL_ID + "=" + frame.getRoll() + ", "
                + KEY_COUNT + "=" + frame.getCount() + ", "
                + KEY_DATE + "=\"" + frame.getDate() + "\", "
                + KEY_LENS_ID + "=\"" + frame.getLensId() + "\", "
                + KEY_SHUTTER + "=\"" + frame.getShutter().replace("\"", "q") + "\", "
                + KEY_APERTURE + "=\"" + frame.getAperture() + "\", "
                + KEY_FRAME_NOTE + "=\"" + frame.getNote() + "\", "
                + KEY_LOCATION + "=\"" + frame.getLocation() + "\""
                + " WHERE " + KEY_FRAME_ID + "=" + frame.getId();
        db.execSQL(query);
        db.close();
    }

    public void deleteFrame(Frame frame) {
        // Get reference to writable database
        SQLiteDatabase db = this.getWritableDatabase();
        // Delete
        db.delete(TABLE_FRAMES,
                KEY_FRAME_ID + " = ?",
                new String[]{String.valueOf(frame.getId())});
        // Close
        db.close();
    }

    public void deleteAllFramesFromRoll(int roll_id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FRAMES, KEY_ROLL_ID + " = ? ", new String[]{String.valueOf(roll_id)});
        db.close();
    }

    // ******************** CRUD operations for the lenses table ********************

    public void addLens(Lens lens){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_LENS_MAKE, lens.getMake());
        values.put(KEY_LENS_MODEL, lens.getModel());
        db.insert(TABLE_LENSES, null, values);
        db.close();
    }

    public Lens getLastLens(){
        Lens lens = new Lens();
        String query = "SELECT * FROM " + TABLE_LENSES + " ORDER BY " + KEY_LENS_ID + " DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        lens.setId(cursor.getInt(cursor.getColumnIndex(KEY_LENS_ID)));
        lens.setMake(cursor.getString(cursor.getColumnIndex(KEY_LENS_MAKE)));
        lens.setModel(cursor.getString(cursor.getColumnIndex(KEY_LENS_MODEL)));
        cursor.close();
        return lens;
    }

    public Lens getLens(int lens_id){
        SQLiteDatabase db = this.getReadableDatabase();
        Lens lens = new Lens();
        String query = "SELECT * FROM " + TABLE_LENSES + " WHERE "
                + KEY_LENS_ID + "=" + lens_id;
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        lens.setId(cursor.getInt(0));
        lens.setMake(cursor.getString(1));
        lens.setModel(cursor.getString(2));
        cursor.close();
        return lens;
    }

    public ArrayList<Lens> getAllLenses(){
        ArrayList<Lens> lenses = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_LENSES + " ORDER BY " + KEY_LENS_MAKE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Lens lens;
        while ( cursor.moveToNext() ) {
            lens = new Lens();
            lens.setId(cursor.getInt(0));
            lens.setMake(cursor.getString(1));
            lens.setModel(cursor.getString(2));
            lenses.add(lens);
        }
        cursor.close();
        return lenses;
    }

    public void deleteLens(Lens lens){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_LENSES, KEY_LENS_ID + " = ?", new String[]{String.valueOf(lens.getId())});
        db.delete(TABLE_MOUNTABLES, KEY_LENS_ID + " = ?", new String[]{String.valueOf(lens.getId())});
        db.close();
    }

    public boolean isLensInUse(Lens lens){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT 1 FROM " + TABLE_FRAMES + " WHERE "
                + KEY_LENS_ID + "=" + lens.getId();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor.moveToFirst() ) {
            cursor.close();
            return true;
        }
        else {
            cursor.close();
            return false;
        }
    }

    // ******************** CRUD operations for the cameras table ********************

    public void addCamera(Camera camera){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_CAMERA_MAKE, camera.getMake());
        values.put(KEY_CAMERA_MODEL, camera.getModel());
        db.insert(TABLE_CAMERAS, null, values);
        db.close();
    }

    public Camera getLastCamera(){
        Camera camera = new Camera();
        String query = "SELECT * FROM " + TABLE_CAMERAS + " ORDER BY " + KEY_CAMERA_ID + " DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if (cursor != null ) cursor.moveToFirst();
        camera.setId(cursor.getInt(cursor.getColumnIndex(KEY_CAMERA_ID)));
        camera.setMake(cursor.getString(cursor.getColumnIndex(KEY_CAMERA_MAKE)));
        camera.setModel(cursor.getString(cursor.getColumnIndex(KEY_CAMERA_MODEL)));
        cursor.close();
        return camera;
    }

    public Camera getCamera(int camera_id){
        SQLiteDatabase db = this.getReadableDatabase();
        Camera camera = new Camera();
        String query = "SELECT * FROM " + TABLE_CAMERAS + " WHERE "
                + KEY_CAMERA_ID + "=" + camera_id;
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        camera.setId(cursor.getInt(0));
        camera.setMake(cursor.getString(1));
        camera.setModel(cursor.getString(2));
        cursor.close();
        return camera;
    }

    public ArrayList<Camera> getAllCameras(){
        ArrayList<Camera> cameras = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_CAMERAS + " ORDER BY " + KEY_CAMERA_MAKE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Camera camera;
        while ( cursor.moveToNext() ) {
            camera = new Camera();
            camera.setId(cursor.getInt(0));
            camera.setMake(cursor.getString(1));
            camera.setModel(cursor.getString(2));
            cameras.add(camera);
        }
        cursor.close();
        return cameras;
    }

    public void deleteCamera(Camera camera){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CAMERAS, KEY_CAMERA_ID + " = ?", new String[]{String.valueOf(camera.getId())});
        db.delete(TABLE_MOUNTABLES, KEY_CAMERA_ID + " = ?", new String[]{String.valueOf(camera.getId())});
        db.close();
    }

    public boolean isCameraBeingUsed(Camera camera) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT 1 FROM " + TABLE_ROLLS + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.getId();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor.moveToFirst() ) {
            cursor.close();
            return true;
        }
        else {
            cursor.close();
            return false;
        }
    }

    // ******************** CRUD operations for the mountables table ********************

    public void addMountable(Camera camera, Lens lens){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "INSERT INTO " + TABLE_MOUNTABLES + "(" + KEY_CAMERA_ID + "," + KEY_LENS_ID
                + ") SELECT " + camera.getId() + ", " + lens.getId()
                + " WHERE NOT EXISTS(SELECT 1 FROM " + TABLE_MOUNTABLES + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.getId() + " AND " + KEY_LENS_ID + "=" + lens.getId() + ")";
        db.execSQL(query);
        db.close();
    }

    public void deleteMountable(Camera camera, Lens lens){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "DELETE FROM " + TABLE_MOUNTABLES + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.getId() + " AND "
                + KEY_LENS_ID + "=" + lens.getId();
        db.execSQL(query);
        db.close();
    }

    public ArrayList<Lens> getMountableLenses(Camera camera){
        ArrayList<Lens> lenses = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_LENSES + " WHERE " + KEY_LENS_ID + " IN "
                + "(" + "SELECT " + KEY_LENS_ID + " FROM " + TABLE_MOUNTABLES + " WHERE "
                + KEY_CAMERA_ID + "=" + camera.getId() + ") ORDER BY " + KEY_LENS_MAKE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Lens lens;
        while ( cursor.moveToNext() ) {
            lens = new Lens();
            lens.setId(cursor.getInt(0));
            lens.setMake(cursor.getString(1));
            lens.setModel(cursor.getString(2));
            lenses.add(lens);
        }
        cursor.close();
        return lenses;
    }

    public ArrayList<Camera> getMountableCameras(Lens lens){
        ArrayList<Camera> cameras = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_CAMERAS + " WHERE " + KEY_CAMERA_ID + " IN "
                + "(" + "SELECT " + KEY_CAMERA_ID + " FROM " + TABLE_MOUNTABLES + " WHERE "
                + KEY_LENS_ID + "=" + lens.getId() + ") ORDER BY " + KEY_CAMERA_MAKE;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Camera camera;
        while ( cursor.moveToNext() ) {
            camera = new Camera();
            camera.setId(cursor.getInt(0));
            camera.setMake(cursor.getString(1));
            camera.setModel(cursor.getString(2));
            cameras.add(camera);
        }
        cursor.close();
        return cameras;
    }

    // ******************** CRUD operations for the rolls table ********************

    public void addRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ROLLNAME, roll.getName());
        values.put(KEY_ROLL_DATE, roll.getDate());
        values.put(KEY_ROLL_NOTE, roll.getNote());
        values.put(KEY_CAMERA_ID, roll.getCamera_id());
        db.insert(TABLE_ROLLS, null, values);
        db.close();
    }

    public Roll getLastRoll(){
        Roll roll = new Roll();
        String query = "SELECT * FROM " + TABLE_ROLLS + " ORDER BY " + KEY_ROLL_ID + " DESC limit 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        roll.setId(cursor.getInt(0));
        roll.setName(cursor.getString(1));
        roll.setDate(cursor.getString(2));
        roll.setNote(cursor.getString(3));
        roll.setCamera_id(cursor.getInt(4));
        cursor.close();
        return roll;
    }

    public ArrayList<Roll> getAllRolls(){
        ArrayList<Roll> rolls = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_ROLLS + " ORDER BY " + KEY_ROLL_ID + " DESC";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);
        Roll roll;
        while ( cursor.moveToNext() ) {
            roll = new Roll();
            roll.setId(cursor.getInt(0));
            roll.setName(cursor.getString(1));
            roll.setDate(cursor.getString(2));
            roll.setNote(cursor.getString(3));
            roll.setCamera_id(cursor.getInt(4));
            rolls.add(roll);
        }
        cursor.close();
        return rolls;
    }

    public Roll getRoll(int id){
        SQLiteDatabase db = this.getReadableDatabase();
        Roll roll = new Roll();
        String query = "SELECT * FROM " + TABLE_ROLLS + " WHERE " + KEY_ROLL_ID + "=" + id;
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        roll.setId(cursor.getInt(0));
        roll.setName(cursor.getString(1));
        roll.setDate(cursor.getString(2));
        roll.setNote(cursor.getString(3));
        roll.setCamera_id(cursor.getInt(4));
        cursor.close();
        return roll;
    }

    public void deleteRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_ROLLS, KEY_ROLL_ID + " = ?", new String[]{String.valueOf(roll.getId())});
        db.close();
    }

    public void updateRoll(Roll roll){
        SQLiteDatabase db = this.getWritableDatabase();
        String query = "UPDATE " + TABLE_ROLLS + " SET "
                + KEY_ROLLNAME + "=\"" + roll.getName() + "\", "
                + KEY_ROLL_DATE + "=\"" + roll.getDate() + "\", "
                + KEY_ROLL_NOTE + "=\"" + roll.getNote() + "\", "
                + KEY_CAMERA_ID + "=" + roll.getCamera_id()
                + " WHERE " + KEY_ROLL_ID + "=" + roll.getId();
        db.execSQL(query);
        db.close();
    }

    public int getNumberOfFrames(Roll roll){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT COUNT(" + KEY_FRAME_ID + ") FROM " + TABLE_FRAMES
                + " WHERE " + KEY_ROLL_ID + "=" + roll.getId();
        Cursor cursor = db.rawQuery(query, null);
        if ( cursor != null ) cursor.moveToFirst();
        return cursor.getInt(0);
    }
}
