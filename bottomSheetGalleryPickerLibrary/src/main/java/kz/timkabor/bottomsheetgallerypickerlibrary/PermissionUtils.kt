package kz.timkabor.bottomsheetgallerypickerlibrary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

private fun Context.isPermissionGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

internal val Context.hasReadStoragePermission get() = isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
internal val Context.hasWriteStoragePermission get() = isPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)
internal val Context.hasCameraPermission get() = isPermissionGranted(Manifest.permission.CAMERA)


private fun Fragment.requestPermission(permission: String, requestCode: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    if (context?.isPermissionGranted(permission) == true) return
    requestPermissions(arrayOf(permission), requestCode)
}

internal fun Fragment.requestReadStorageAndCameraPreviewPermission(requestCode: Int) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
    if (requireContext().isPermissionGranted(Manifest.permission.READ_EXTERNAL_STORAGE)
        && requireContext().isPermissionGranted(Manifest.permission.CAMERA)
    ) {
        return
    }

    requestPermissions(
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        ), requestCode
    )
}

internal fun Fragment.requestReadStoragePermission(requestCode: Int) =
    requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, requestCode)

internal fun Fragment.requestWriteStoragePermission(requestCode: Int) =
    requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, requestCode)

internal val IntArray.isPermissionGranted get() = size > 0 && get(0) == PackageManager.PERMISSION_GRANTED