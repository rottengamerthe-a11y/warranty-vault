package com.warrantyvault.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.warrantyvault.R
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanReceiptScreen(
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onBack: () -> Unit,
    onManualEntry: () -> Unit,
    onScanReady: (ItemDraft) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasCameraPermission = it
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan receipt") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = onManualEntry) { Text("Manual") }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(padding)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onCameraReady = { capture, boundCamera ->
                        imageCapture = capture
                        camera = boundCamera
                    }
                )
                ReceiptScanOverlay()
                FilledIconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    enabled = camera?.cameraInfo?.hasFlashUnit() == true,
                    onClick = {
                        val next = !torchEnabled
                        camera?.cameraControl?.enableTorch(next)
                        torchEnabled = next
                    }
                ) {
                    Icon(
                        if (torchEnabled) Icons.Default.FlashOff else Icons.Default.FlashOn,
                        contentDescription = if (torchEnabled) "Turn flashlight off" else "Turn flashlight on"
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Camera permission is required", color = Color.White)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Allow camera")
                    }
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.56f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Align the receipt inside the frame",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                message?.let { Text(it, color = MaterialTheme.colorScheme.errorContainer) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledTonalButton(
                        enabled = hasCameraPermission && !scanning && imageCapture != null,
                        onClick = {
                            val capture = imageCapture ?: return@FilledTonalButton
                            scanning = true
                            message = null
                                captureReceipt(
                                    context = context,
                                    imageCapture = capture,
                                    outputFile = newScanFile(context.cacheDir),
                                    onSaved = { file ->
                                        scope.launch {
                                            try {
                                                val result = readReceipt(context, file)
                                                onScanReady(result.toItemDraft())
                                            } catch (t: Throwable) {
                                                message = t.message ?: "Could not read the receipt"
                                            } finally {
                                                try {
                                                    if (file.exists()) file.delete()
                                                } catch (_: Exception) {
                                                }
                                                scanning = false
                                            }
                                        }
                                    },
                                    onError = {
                                        scanning = false
                                        message = it.message ?: "Camera capture failed"
                                    }
                                )
                        }
                    ) {
                        if (scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .size(18.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.CameraAlt, contentDescription = stringResource(R.string.cd_camera))
                        }
                        Text(if (scanning) "Reading" else "Scan")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptScanOverlay() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameWidth = size.width * 0.78f
            val frameHeight = size.height * 0.52f
            val left = (size.width - frameWidth) / 2f
            val top = (size.height - frameHeight) / 2f - 36.dp.toPx()
            val cornerRadius = 24.dp.toPx()
            val frame = RoundRect(
                left = left,
                top = top,
                right = left + frameWidth,
                bottom = top + frameHeight,
                radiusX = cornerRadius,
                radiusY = cornerRadius
            )
            val overlay = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                addRoundRect(frame)
            }

            drawPath(overlay, Color.Black.copy(alpha = 0.58f))
            drawRoundRect(
                color = Color.White.copy(alpha = 0.92f),
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = Size(frameWidth, frameHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 3.dp.toPx())
            )
        }
        Box(
            modifier = Modifier
                .offset(y = (-36).dp)
                .fillMaxWidth(0.78f)
                .aspectRatio(0.72f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
        )
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    onCameraReady: (ImageCapture, Camera) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(factory = { previewView }, modifier = modifier)

    DisposableEffect(lifecycleOwner, previewView) {
        val executor = ContextCompat.getMainExecutor(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
                onCameraReady(imageCapture, camera)
            },
            executor
        )

        onDispose {
            if (cameraProviderFuture.isDone) cameraProviderFuture.get().unbindAll()
        }
    }
}

private fun captureReceipt(
    context: Context,
    imageCapture: ImageCapture,
    outputFile: File,
    onSaved: (File) -> Unit,
    onError: (ImageCaptureException) -> Unit
) {
    val options = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                onSaved(outputFile)
            }

            override fun onError(exception: ImageCaptureException) {
                onError(exception)
            }
        }
    )
}

private suspend fun readReceipt(context: Context, file: File): OcrScanResult {
    val image = InputImage.fromFilePath(context, Uri.fromFile(file))
    val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val barcodeScanner = BarcodeScanning.getClient()
    return try {
        val text = textRecognizer.process(image).await().text
        val barcodes = barcodeScanner.process(image).await().mapNotNull { it.rawValue }
        OcrScanResult(text = text, barcodes = barcodes)
    } finally {
        textRecognizer.close()
        barcodeScanner.close()
    }
}

private fun newScanFile(cacheDir: File): File {
    val dir = File(cacheDir, "camera").apply { mkdirs() }
    return File(dir, "receipt-${System.currentTimeMillis()}.jpg")
}
