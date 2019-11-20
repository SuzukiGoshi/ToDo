package com.todoapplication

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ToDoOpenHelper(context: Context) : SQLiteOpenHelper(context, DBName, null, VERSION) {

    // DBクリエイト処理
    override fun onCreate(db: SQLiteDatabase) {
        /**
         * テーブルを作成
         * id:キー
         * title:ToDo名
         * flag:チェック状態 0:非チェック 1:チェック
         */
        db.execSQL("CREATE TABLE TODO_TABLE (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "flag TEXT)")

    }

    // バージョンアップ時の処理
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        /**
         * テーブルを削除
         */
        db.execSQL("DROP TABLE IF EXISTS TODO_TABLE")

        // 新しくテーブルを作成する
        onCreate(db)
    }

    companion object {

        // データベース名
        private val DBName = "TODO_DB"
        // データベースのバージョン
        private val VERSION = 1

        enum class ToDoTable(val position : Int){
            id(0),title(1),flag(2)
        }
    }

}