package com.todoapplication

import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import android.os.Bundle
import android.content.Context
import android.graphics.Paint
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    internal var helper:ToDoOpenHelper?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // データベースから値を取得
        if(helper==null){
            helper = ToDoOpenHelper(this);
        }
        val todoList = ArrayList<todoItem>()
        val db = helper!!.writableDatabase
        try {
            // SELECT文を発行しデータを取得
            val c = db.rawQuery("select id,title, flag from TODO_TABLE order by id", null)
            var next = c.moveToFirst()
            // データを取り出しリストに設定
            while(next){
                val title = c.getString(ToDoOpenHelper.Companion.ToDoTable.title.position)
                var flag = false
                if(c.getString(ToDoOpenHelper.Companion.ToDoTable.flag.position).equals(FLAG_CHECK))
                {
                    flag = true
                }
                val data = todoItem(title,flag)
                todoList.add(data)
                next = c.moveToNext()
            }
        }finally {
            // DBをクローズ
            db.close()
        }
        // Adapterを生成しデータをリストにセット
        val simpleAdapter = MyArrayAdapter(this,0,helper!!,todoList).apply {
            todoList.forEach{i->
                add(i)
            }
        }
        listView.adapter = simpleAdapter
        btnAdd.setOnClickListener {
            var name = editName.text
            if (!name.isEmpty() or !name.isBlank()) {
                var newTitle = editName.text.toString()
                // 入力ToDo名と同名が存在しないかチェック
                var newFlag=false
                todoList.forEach{i->
                    if(newTitle.equals(i.title))newFlag = true
                }
                if(!newFlag) {
                    // 新しいToDo名の場合データベースに保存する
                    val db = helper!!.writableDatabase
                    try {
                        // 新規作成
                        db.execSQL("insert into TODO_TABLE(title, flag) VALUES('$newTitle', '$FLAG_NOCHECK')")
                    } finally {
                        db.close()
                    }
                    // 追加後リストに反映する
                    editName.setText("")
                    var item = todoItem(newTitle, false)
                    todoList.add(item)
                    simpleAdapter.add(item)
                    simpleAdapter.notifyDataSetInvalidated()
                }
                else
                {
                    // すでに存在する場合は警告を表示
                    AlertDialog.Builder(this)
                        .setTitle("Invalid name")
                        .setMessage("同名のタイトルは登録できません")
                        .setPositiveButton("OK",null)
                        .show()
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 入力項目外を触ったときキーボードとフォーカスを非表示
        if(getCurrentFocus() != null) {
            var manager =
                this.getSystemService((Context.INPUT_METHOD_SERVICE)) as InputMethodManager
            manager.hideSoftInputFromWindow(getCurrentFocus()?.getWindowToken(), 0)
            mainLayout.requestFocus()
        }
        return super.onTouchEvent(event)
    }

    // リスト項目のデータ
    class todoItem(val title : String, val check : Boolean) {
        var titleName : String = title
        var cheked : Boolean = check
    }

    // リスト項目を再利用するためのホルダー
    data class ViewHolder(val checkbox: CheckBox, val titleEdit: TextView, val img: ImageView)

    // リスト項目データを扱えるようにした ArrayAdapter
    class MyArrayAdapter : ArrayAdapter<todoItem> {

        private var inflater : LayoutInflater = LayoutInflater.from(context)
        internal var helper:ToDoOpenHelper?=null
        var todoList = ArrayList<todoItem>()

        constructor(context : Context, resource : Int , helper:ToDoOpenHelper , todoList:ArrayList<todoItem>) : super(context, resource) {
            this.helper = helper
            this.todoList = todoList
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

            var viewHolder : ViewHolder? = null
            var view = convertView

            // 再利用の設定
            if (view == null) {

                view = inflater!!.inflate(R.layout.list, parent, false)

                viewHolder = ViewHolder(
                    view.findViewById(R.id.checkList) as CheckBox,
                    view.findViewById(R.id.editListText) as TextView,
                    view.findViewById(R.id.imgDelete) as ImageView
                )
                view.tag = viewHolder
            } else {
                viewHolder = view.tag as ViewHolder
            }
            // 項目の情報を設定
            val listItem = getItem(position)
            var title = listItem!!.titleName
            viewHolder.checkbox.isChecked = listItem!!.cheked
            viewHolder.titleEdit.text = title
            var paint =viewHolder.titleEdit.paint
            changeAlias(paint, listItem!!.cheked)
            // チェックボックス押下処理を追加
            viewHolder.checkbox.setOnClickListener {view ->
                var check = FLAG_NOCHECK
                val cons = view.parent as ConstraintLayout
                val edittext = cons.findViewById(R.id.editListText) as TextView
                val clickCheck = view as CheckBox
                // 現チェック状態をDBに登録する
                var flag = clickCheck.isChecked()
                if (flag) {
                    check = FLAG_CHECK
                }
                changeAlias(edittext.paint, flag)
                // データベースを更新
                val db = helper!!.writableDatabase
                try {
                    // 更新処理
                    db.execSQL("update TODO_TABLE set flag = '$check'  where title = '$title'")
                } finally {
                    db.close()
                }
                // リストデータを更新
                todoList[position].cheked = flag
                this.notifyDataSetChanged()
            }
            // 削除画像押下処理を追加
            viewHolder.img.setOnClickListener { view ->
                // データベースから削除
                val db = helper!!.writableDatabase
                val cons = view.parent as ConstraintLayout
                val edittext = cons.findViewById(R.id.editListText) as TextView
                val deleteTitle = edittext.text
                try {
                    // 削除処理
                    db.execSQL("delete from TODO_TABLE where title = '$deleteTitle'")
                } finally {
                    db.close()
                }
                // リストからデータを削除
                todoList.remove(listItem)
                this.remove(listItem)
                this.notifyDataSetChanged()
            }

            return view!!
        }
        // チェックボックスON時に打ち消し線を表示
        fun changeAlias(paint : Paint , flag : Boolean){
            if(flag){
                paint.flags = paint.flags or Paint.STRIKE_THRU_TEXT_FLAG
                paint.isAntiAlias =  true
            }
            else{
                paint.flags = 0
            }
        }
    }

    // 定数定義
    companion object{
        const val FLAG_CHECK = "1"
        const val FLAG_NOCHECK = "0"
    }
}



