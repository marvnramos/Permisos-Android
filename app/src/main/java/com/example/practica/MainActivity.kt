package com.example.practica

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica.ui.theme.PracticaTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.Response as Res
import android.Manifest
import androidx.compose.ui.text.font.FontWeight


import android.content.Intent
import android.net.Uri
import android.provider.Settings


object RetrofitInstance {
    private const val API_URL = "https://catfact.ninja/"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder().baseUrl(API_URL).addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val ApiClient: APIRepo by lazy {
        retrofit.create(APIRepo::class.java)
    }
}

interface APIRepo {
    @GET("fact")
    fun getFact(): Call<Response>
}


class AppViewModel : ViewModel() {
    private val _factRes = MutableLiveData<Response?>()
    val factRes: LiveData<Response?> = _factRes

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        fetchData()
    }

    private fun fetchData() {
        try {
            val data = RetrofitInstance.ApiClient.getFact()
            data.enqueue(object : Callback<Response> {
                override fun onResponse(call: Call<Response>, response: Res<Response>) {
                    if (response.isSuccessful) {
                        _factRes.value = response.body()
                    } else {
                        _errorMessage.value =
                            "Doesn't work: ${response.code()} ${response.message()}"
                    }
                }

                override fun onFailure(call: Call<Response>, t: Throwable) {
                    _factRes.value = null
                    _errorMessage.value = "Failed to fetch data"
                }
            })
        } catch (e: Exception) {
            _errorMessage.value = "Failed to fetch data"
        }
    }
}


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PracticaTheme {
                Surface {
                    MainView()
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("ServiceCast")
@Composable
@Preview
fun MainView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val viewModel = viewModel<AppViewModel>()
        val factRes by viewModel.factRes.observeAsState()
        val errorMessage by viewModel.errorMessage.observeAsState()
        val context = LocalContext.current
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        Text(text = "PERMISOS NORMALES", color = Color.Black, fontWeight = FontWeight.Bold)
        if (factRes != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = factRes!!.fact, color = Color.Black)
        } else {
            Text(text = errorMessage ?: "Loading...", color = Color.Black)
        }

        Box(
            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = {
                    if (vibrator.hasVibrator()) {
                        vibrator.vibrate(
                            VibrationEffect.createOneShot(
                                500, VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                        Toast.makeText(context, "Vibración activada", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            context, "El dispositivo no tiene vibrador", Toast.LENGTH_SHORT
                        ).show()
                    }
                }, modifier = Modifier.padding(16.dp)
            ) {
                Text(text = "Vibrar")
            }
        }

        Text(text = "PERMISOS PELIGROSOS", color = Color.Black, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LocationPermissionComponent()
        }

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CameraPermissionComponent()
        }

        Text(text = "PERMISOS ESPECIALES", color = Color.Black, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(16.dp))

        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            OverlayPermissionComponent()
        }
        Text(
            text = "PERMISOS APAGADOS | SIMULACIÓN",
            color = Color.Black,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            SimulateRevokedPermissions()
        }
    }
}

@Composable
fun LocationPermissionComponent() {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    Toast.makeText(context, "Permiso de ubicación concedido", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT)
                        .show()
                }
            })

    Button(onClick = {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(context, "Permiso ya concedido", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                Toast.makeText(
                    context,
                    "Es necesario el permiso para acceder a la ubicación",
                    Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }) {
        Text(text = "Solicitar permiso de ubicación")
    }
}

@Composable
fun CameraPermissionComponent() {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity
    val permissionLauncher =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    Toast.makeText(context, "Permiso de cámara concedido", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
                }
            })

    Button(onClick = {
        when {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                Toast.makeText(context, "Permiso ya concedido", Toast.LENGTH_SHORT).show()
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                activity, Manifest.permission.CAMERA
            ) -> {
                Toast.makeText(
                    context, "Es necesario el permiso para acceder a la cámara", Toast.LENGTH_SHORT
                ).show()
            }

            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }) {
        Text(text = "Solicitar permiso de cámara")
    }
}

const val REQUEST_CODE_OVERLAY_PERMISSION = 123

@RequiresApi(Build.VERSION_CODES.M)
@Composable
fun OverlayPermissionComponent() {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity

    Button(onClick = {
        if (Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Permiso de superposición ya concedido", Toast.LENGTH_SHORT)
                .show()
        } else {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.data = Uri.parse("package:" + activity.packageName)
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }) {
        Text(text = "Solicitar permiso de superposición")
    }
}

@Composable
fun SimulateRevokedPermissions() {
    val context = LocalContext.current
    val activity = LocalContext.current as Activity

    Button(onClick = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Revocando permisos de ubicación", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
        } else {
            Toast.makeText(context, "Permiso de ubicación ya revocado", Toast.LENGTH_SHORT).show()
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, "Revocando permisos de cámara", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CAMERA), 2)
        } else {
            Toast.makeText(context, "Permiso de cámara ya revocado", Toast.LENGTH_SHORT).show()
        }

        if (Settings.canDrawOverlays(context)) {
            Toast.makeText(context, "Revocando permisos de superposición", Toast.LENGTH_SHORT)
                .show()
            val intent = Intent
                .makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)
            intent.data = Uri.parse("package:" + activity.packageName)
            activity.startActivity(intent)
        } else {
            Toast.makeText(context, "Permiso de superposición ya revocado", Toast.LENGTH_SHORT)
                .show()
        }
    }) {
        Text(text = "Simular permisos revocados")
    }
}
