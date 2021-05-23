 package com.ahmetutlu.kotlinartbook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main2.*
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.jar.Manifest

class MainActivity2 : AppCompatActivity() {

    var selectedPicture :Uri? = null
    var selectedBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // intent ayrıştırma
        val intent= intent
        val info=intent.getStringExtra("info")

        // eğer kullanıcı yeni menuden geliyorsa
        if (info.equals("new")) {
            artText.setText("")
            artistText.setText("")
            yearText.setText("")
            button.visibility = View.VISIBLE

            val selectedImageBackground = BitmapFactory.decodeResource(applicationContext.resources,R.drawable.foto)
            imageView.setImageBitmap(selectedImageBackground)

        }
        // eğer kullanıcı eski menuden geliyorsa
        else {
            button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)


            val database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE,null)

            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?", arrayOf(selectedId.toString()))

            val artNameIx = cursor.getColumnIndex("artname")
            val artistNameIx = cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()) {
                artText.setText(cursor.getString(artNameIx))
                artistText.setText(cursor.getString(artistNameIx))
                yearText.setText(cursor.getString(yearIx))

                val byteArray = cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                imageView.setImageBitmap(bitmap)

            }

            cursor.close()

        }
    }

    // verileri SQlite'a kaydediyoruz
    fun save(view: View) {

        val artName = artText.text.toString()
        val artistName = artistText.text.toString()
        val year = yearText.text.toString()

        if (selectedBitmap != null) {
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {

                val database = this.openOrCreateDatabase("Arts", Context.MODE_PRIVATE, null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString =
                        "INSERT INTO arts (artname, artistname, year, image) VALUES (?, ?, ?, ?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1, artName)
                statement.bindString(2, artistName)
                statement.bindString(3, year)
                statement.bindBlob(4, byteArray)

                statement.execute()

            } catch (e: Exception) {
                e.printStackTrace()
            }


            // öncesindeki bütün aktiviteleri kapatmaya yarar
            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

            startActivity(intent)

            //finish()

        }



    }
    //Bitmap küçültmek
    fun makeSmallerBitmap(image: Bitmap, maximumSize : Int) : Bitmap {
        var width = image.width
        var height = image.height

        val bitmapRatio : Double = width.toDouble() / height.toDouble()
        if (bitmapRatio > 1) {
            width = maximumSize
            val scaledHeight = width / bitmapRatio
            height = scaledHeight.toInt()
        } else {
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()
        }

        return Bitmap.createScaledBitmap(image,width,height,true)

    }

    // burdan aşağısı izinlerle alakalı kodlar

    fun selectImage(view: View){
        // eğer izin vermediyse iste
        if (ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE )!= PackageManager.PERMISSION_GRANTED){
               ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),1)
        }
        // eğer izin verdiyse fotolara git
        else{
            val intentToGalery= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intentToGalery,2)
        }
    }

    // kullanıcı izin verdiyse direk galeriye yolla
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if(requestCode==1){
            if (grantResults.size>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                val intentToGalery= Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intentToGalery,2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // galeriye gitti geldi ve resim seçti ise
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
       if (requestCode==2 && resultCode==Activity.RESULT_OK && data != null){
           selectedPicture = data.data
          try {
              // burda ise sdk 28 den yeni ise yada eski ise hangi kod dizilerini çalıştırcağımız gösterilmiş
              if (selectedPicture != null) {
                  if (Build.VERSION.SDK_INT >= 28) {
                      val source =
                          ImageDecoder.createSource(this.contentResolver, selectedPicture!!)
                      selectedBitmap = ImageDecoder.decodeBitmap(source)
                      imageView.setImageBitmap(selectedBitmap)
                  } else {
                      selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,selectedPicture)
                      imageView.setImageBitmap(selectedBitmap)
                  }
              }

          }catch (e:Exception){

          }


       }
        super.onActivityResult(requestCode, resultCode, data)
    }
}