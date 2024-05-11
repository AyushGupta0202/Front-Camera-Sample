package com.example.frontcamerasample

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.frontcamerasample.ui.theme.FrontCameraSampleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


private const val REQUEST_CODE_CAMERA = 1

class MainActivity : ComponentActivity() {
    var cameraActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            Toast.makeText(this, "$it", Toast.LENGTH_SHORT).show()
        }

    fun openCameraForResult() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", CameraCharacteristics.LENS_FACING_FRONT)
        cameraActivityResultLauncher.launch(cameraIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FrontCameraSampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Greeting("Android")
                }
            }
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Box(modifier = modifier) {
            Button(
                onClick = {
                    openCameraForResult()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text("Click Me")
            }
        }
    }

    fun openFrontCamera() {
        Log.i("MainActivity", "openFrontCamera:")
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Specify the front camera (if available)
//        cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT)
        cameraIntent.putExtra("android.intent.extras.CAMERA_FACING", CameraCharacteristics.LENS_FACING_FRONT)
//        cameraIntent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
//        cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
//        cameraIntent.putExtra("android.intent.extra.USE_FRONT_CAMERA", 1)
        startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA)
    }

    private fun openFrontFacingCameraGingerbread() {
        var cameraCount = 0
        var cam: Camera? = null
        cameraCount = Camera.getNumberOfCameras()
        for (camIdx in 0 until cameraCount) {
            val cameraInfo = Camera.CameraInfo()
            Camera.getCameraInfo(camIdx, cameraInfo)
            if (cameraInfo.facing === Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    cam = Camera.open(camIdx)
                } catch (e: RuntimeException) {
                    Toast.makeText(this, "Camera failed to open: $e", Toast.LENGTH_LONG).show()
                }
            }
        }
        lifecycleScope.launch(Dispatchers.Main) {
            cam?.takePicture(null, null, object : Camera.PictureCallback {
                override fun onPictureTaken(data: ByteArray, camera: Camera) {
                    // Save the image or do something with it.
                    camera.startPreview()
                    Toast.makeText(this@MainActivity, "data: $data", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }

    @SuppressLint("NewApi", "MissingPermission")
    fun openFrontCamera(context: Context) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (cOrientation != null && cOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
                    // cameraId is the id of the front camera
                    // You can open the camera here with the openCamera method
                    cameraManager.openCamera(cameraId, mainExecutor, object : CameraDevice.StateCallback() {
                        override fun onOpened(camera: CameraDevice) {
                            // Use the camera object here
                            camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                            Toast.makeText(this@MainActivity, "camera opened", Toast.LENGTH_SHORT).show()
                        }

                        override fun onDisconnected(camera: CameraDevice) {
                            // Handle the camera being disconnected
                            Toast.makeText(this@MainActivity, "camera disconnected", Toast.LENGTH_SHORT).show()
                        }

                        override fun onError(camera: CameraDevice, error: Int) {
                            // Handle any errors
                            Toast.makeText(this@MainActivity, "camera error", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Set up the preview
            val preview = androidx.camera.core.Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(object: androidx.camera.core.Preview.SurfaceProvider {
                        override fun onSurfaceRequested(request: SurfaceRequest) {

                        }
                    })
                }

            // Select front camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                // Unbind any previous use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this@MainActivity, cameraSelector, preview)

            } catch (exc: Exception) {
                Toast.makeText(this, "Unable to bind camera", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

}

@Composable
fun CameraPreview() {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val previewView = androidx.camera.view.PreviewView(context)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build()
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                    .build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                cameraProvider.bindToLifecycle(
                    (context as ComponentActivity),
                    cameraSelector,
                    preview
                )
            }, context.mainExecutor)
            previewView
        }
    )
}