package com.tw.signpaddemo

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.drawToBitmap
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.DexterError
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    val TAG:String = this.javaClass.simpleName
    var dialogShownOnce = false
    private lateinit var signatureView : SignatureView
    private lateinit var mySignature : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnOpenSignPad = findViewById<TextView>(R.id.btnOpenSignPad)
        mySignature = findViewById<ImageView>(R.id.mySignature)

        btnOpenSignPad.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                //This is for Android 33 API Level bcz external storage permisssion is removed in this API level
                myPermissions33Above(1)
            } else {
                //ths will run upto android 32
                myPermissions(2)
            }
        }
    }



    private fun myPermissions33Above(type: Int) {
        Dexter.withContext(applicationContext).withPermissions(
            Manifest.permission.READ_MEDIA_IMAGES,
            )

            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        Log.e(TAG, "permissions Granted")
                        Toast.makeText(this@MainActivity, "Permissions Granted", Toast.LENGTH_LONG).show()

                        //once permissions granted then you can take picture from camera gallery or other thing
                        openSignatureDialog()
                    }
                    if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                        Log.e(TAG,"permissions Denied---> ${multiplePermissionsReport.deniedPermissionResponses}")
                        Toast.makeText(this@MainActivity, "Permissions Denied", Toast.LENGTH_LONG).show()
                        showSettingsDialogAll(multiplePermissionsReport.deniedPermissionResponses)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(list: List<PermissionRequest>, permissionToken: PermissionToken) {
                    permissionToken.continuePermissionRequest()
                }
            }).withErrorListener { dexterError: DexterError ->
                Log.e(TAG,"permissions dexterError :" + dexterError.name)
            }.onSameThread()
            .check()
    }

    //this function is almost same as above function
    private fun myPermissions(type: Int) {
        Dexter.withContext(applicationContext)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        Log.e(TAG, "permissions Granted")
                        Toast.makeText(this@MainActivity, "Permissions Granted", Toast.LENGTH_LONG).show()

                        //once permissions granted then you can take picture from camera gallery or other thing
                        openSignatureDialog()

                    }
                    if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied) {
                        Log.e(TAG,"permissions Denied")
                        Toast.makeText(this@MainActivity, "Permissions Denied", Toast.LENGTH_LONG).show()
                        showSettingsDialogAll(multiplePermissionsReport.deniedPermissionResponses)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    list: List<PermissionRequest>,
                    permissionToken: PermissionToken
                ) {
                    permissionToken.continuePermissionRequest()
                }
            }).withErrorListener { dexterError: DexterError ->
                Log.e(TAG, "permissions dexterError :" + dexterError.name)
            }
            .onSameThread()
            .check()
    }

    //if permission are denied then permission settings of this app will open
    fun showSettingsDialogAll(deniedPermissionResponses: MutableList<PermissionDeniedResponse>) {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Need Permissions")
        builder.setMessage(deniedPermissionResponses[0].permissionName)
        builder.setPositiveButton("GOTO SETTINGS") { dialog, _ ->
            dialog.cancel()
            openSettings()
        }
        builder.show()
    }

    private fun openSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", "com.example.myapplication", null)
        intent.data = uri
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun openSignatureDialog() {

        val dialog = Dialog(this)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.activity_signature)

        dialog.window?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.light_grey)))
        val header_text: TextView = dialog.findViewById(R.id.header_text)
        val buttonClear: TextView = dialog.findViewById(R.id.buttonClear)
        val buttonCreate: TextView = dialog.findViewById(R.id.buttonCreate)
        val frameLayout: FrameLayout = dialog.findViewById(R.id.frameLayout)
        val close: ImageView = dialog.findViewById(R.id.right_header_icon)
        header_text.text = resources.getString(R.string.signature_pad)

        signatureView = SignatureView(this).apply {
            frameLayout.addView(this)
        }

        buttonClear.setOnClickListener {
            signatureView.clear()
//            imageView.setImageBitmap(null)
        }


        buttonCreate.setOnClickListener {
            var currentPhotoPath=""
            var base64StringImage=""
            // Create an image file name
            @SuppressLint("SimpleDateFormat") val timeStamp =
                SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val imageFileName = "JPEG_" + timeStamp + "_"
            val storageDir: File? = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val image = File.createTempFile(imageFileName,  /* prefix */".jpg",  /* suffix */storageDir /* directory */)
            currentPhotoPath = image.absolutePath


            val signatureBitmap = signatureView.drawToBitmap(Bitmap.Config.ARGB_8888)
            val fileOutputStream = image.outputStream()
            val byteArrayOutputStream = ByteArrayOutputStream()
            signatureBitmap.compress(Bitmap.CompressFormat.PNG,100,byteArrayOutputStream)
            val bytearray = byteArrayOutputStream.toByteArray()
            fileOutputStream.write(bytearray)
            fileOutputStream.flush()
            fileOutputStream.close()
            byteArrayOutputStream.close()

            val signatureUri = image.toURI()
            Log.e(TAG, "openSignatureDialog signatureUri : $signatureUri" )

            base64StringImage = PathUtil.getBase64StringFile(File(currentPhotoPath))
            val bitmap : Bitmap = PathUtil.base64ToBitmap( base64StringImage)
            mySignature.setImageBitmap(bitmap)

            dialog.dismiss()
            dialogShownOnce = true
        }

        close.setOnClickListener {
            dialog.dismiss()
            dialogShownOnce = true
        }


        if (!dialog.isShowing) {
            dialog.show()
            dialogShownOnce = true
        }

    }

}